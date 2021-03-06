package cs.bilkent.joker.experiment.wordcount;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.io.Files.readLines;
import cs.bilkent.joker.operator.InitCtx;
import cs.bilkent.joker.operator.InvocationCtx;
import cs.bilkent.joker.operator.Operator;
import cs.bilkent.joker.operator.OperatorConfig;
import cs.bilkent.joker.operator.Tuple;
import cs.bilkent.joker.operator.scheduling.ScheduleWhenAvailable;
import cs.bilkent.joker.operator.scheduling.SchedulingStrategy;
import cs.bilkent.joker.operator.schema.annotation.OperatorSchema;
import cs.bilkent.joker.operator.schema.annotation.PortSchema;
import static cs.bilkent.joker.operator.schema.annotation.PortSchemaScope.EXACT_FIELD_SET;
import cs.bilkent.joker.operator.schema.annotation.SchemaField;
import cs.bilkent.joker.operator.schema.runtime.TupleSchema;
import cs.bilkent.joker.operator.spec.OperatorSpec;
import static cs.bilkent.joker.operator.spec.OperatorType.STATEFUL;
import static java.lang.Thread.currentThread;
import static java.util.Collections.shuffle;
import static java.util.concurrent.locks.LockSupport.parkNanos;

@OperatorSpec( type = STATEFUL, inputPortCount = 0, outputPortCount = 1 )
@OperatorSchema( outputs = { @PortSchema( portIndex = 0, scope = EXACT_FIELD_SET, fields = { @SchemaField( name = SentenceBeaconOperator2.SENTENCE_FIELD, type = String.class ),
                                                                                             @SchemaField( name = SentenceBeaconOperator2.PARTITION_INDEX_FIELD, type = Integer.class ) } ) } )
public class SentenceBeaconOperator2 implements Operator
{

    private static final Logger LOGGER = LoggerFactory.getLogger( SentenceBeaconOperator2.class );

    public static final String MIN_SENTENCE_LENGTH_PARAM = "minSentenceLength";

    public static final String MAX_SENTENCE_LENGTH_PARAM = "maxSentenceLength";

    public static final String SENTENCE_COUNT_PER_LENGTH_PARAM = "sentenceCountPerLength";

    public static final String SHUFFLED_SENTENCE_COUNT_PARAM = "shuffledSentenceCount";

    public static final String MAX_PARTITION_INDEX_PARAM = "maxPartitionIndex";

    public static final String SENTENCE_COUNT_PER_INVOCATION_PARAM = "sentenceCountPerInvocation";

    public static final String SENTENCE_FIELD = "sentence";

    public static final String PARTITION_INDEX_FIELD = "partitionIndex";


    private TupleSchema outputSchema;

    private int maxPartitionIndex;

    private List<String> sentences;

    private int sentenceCountPerInvocation;

    private int sentenceIndex;

    private int partitionIndex;

    private final AtomicReference<List<String>> shuffledSentencesRef = new AtomicReference<>();

    private Thread shuffler;

    private volatile boolean shutdown;

    @Override
    public SchedulingStrategy init ( final InitCtx ctx )
    {
        this.outputSchema = ctx.getOutputPortSchema( 0 );

        final OperatorConfig config = ctx.getConfig();
        this.maxPartitionIndex = config.getInteger( MAX_PARTITION_INDEX_PARAM );
        this.sentenceCountPerInvocation = config.getInteger( SENTENCE_COUNT_PER_INVOCATION_PARAM );
        final int minSentenceLength = config.getInteger( MIN_SENTENCE_LENGTH_PARAM );
        final int maxSentenceLength = config.getInteger( MAX_SENTENCE_LENGTH_PARAM );
        final int sentenceCountPerLength = config.getInteger( SENTENCE_COUNT_PER_LENGTH_PARAM );
        final int shuffledSentenceCount = config.getInteger( SHUFFLED_SENTENCE_COUNT_PARAM );

        this.shuffler = new Thread( new Shuffler( minSentenceLength, maxSentenceLength, sentenceCountPerLength, shuffledSentenceCount ) );
        shuffler.start();

        while ( true )
        {
            if ( getSentences() )
            {
                break;
            }
        }

        return ScheduleWhenAvailable.INSTANCE;
    }

    @Override
    public void invoke ( final InvocationCtx ctx )
    {
        for ( int i = 0; i < sentenceCountPerInvocation; i++ )
        {
            final Tuple result = Tuple.of( outputSchema, PARTITION_INDEX_FIELD, partitionIndex++ );
            if ( partitionIndex == maxPartitionIndex )
            {
                partitionIndex = 0;
            }

            result.set( SENTENCE_FIELD, sentences.get( sentenceIndex++ ) );
            if ( sentenceIndex == sentenceCountPerInvocation )
            {
                sentenceIndex = 0;
                getSentences();
            }

            ctx.output( result );
        }
    }

    @Override
    public void shutdown ()
    {
        shutdown = true;
        try
        {
            shuffler.join( TimeUnit.SECONDS.toMillis( 30 ) );
        }
        catch ( InterruptedException e )
        {
            currentThread().interrupt();
            LOGGER.error( "shuffler join failed" );
        }
    }

    private boolean getSentences ()
    {
        final List<String> shuffled = shuffledSentencesRef.get();
        if ( shuffled != null && shuffledSentencesRef.compareAndSet( shuffled, null ) )
        {
            sentences = shuffled;
            return true;
        }

        return false;
    }

    private class Shuffler implements Runnable
    {

        private final int minSentenceLength, maxSentenceLength, sentenceCountPerLength;

        private final int shuffledSentenceCount;

        private final List<String> words;

        private final List<Integer> sentences;

        private int wordIdx;

        private int sentenceIdx;

        Shuffler ( final int minSentenceLength,
                   final int maxSentenceLength,
                   final int sentenceCountPerLength,
                   final int shuffledSentenceCount )
        {
            this.minSentenceLength = minSentenceLength;
            this.maxSentenceLength = maxSentenceLength;
            this.sentenceCountPerLength = sentenceCountPerLength;
            this.shuffledSentenceCount = shuffledSentenceCount;
            this.words = new ArrayList<>( 20000 );
            this.sentences = new ArrayList<>( ( maxSentenceLength - minSentenceLength + 1 ) * sentenceCountPerLength );
            init();
        }

        @Override
        public void run ()
        {
            while ( !shutdown )
            {
                final List<String> sentences = getRandomizedSentences();

                while ( !shuffledSentencesRef.compareAndSet( null, sentences ) )
                {
                    if ( shutdown )
                    {
                        break;
                    }

                    parkNanos( 1000 );
                }

                //                LOGGER.info( "Shuffled..." );
            }
        }

        private void init ()
        {
            try
            {
                final List<String> words = readLines( new File( "./20k.txt" ), Charset.defaultCharset() );
                this.words.addAll( words );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }

            //            char[] letters = { 'a',
            //                               'b',
            //                               'c',
            //                               'd',
            //                               'e',
            //                               'f',
            //                               'g',
            //                               'h',
            //                               'i',
            //                               'j',
            //                               'k',
            //                               'l',
            //                               'm',
            //                               'n',
            //                               'o',
            //                               'p',
            //                               'q',
            //                               'r',
            //                               's',
            //                               't',
            //                               'u',
            //                               'v',
            //                               'w',
            //                               'x',
            //                               'y',
            //                               'z',
            //                               'A',
            //                               'B',
            //                               'C',
            //                               'D',
            //                               'E',
            //                               'F',
            //                               'G',
            //                               'H',
            //                               'I',
            //                               'J',
            //                               'K',
            //                               'L',
            //                               'M',
            //                               'N',
            //                               'O',
            //                               'P',
            //                               'Q',
            //                               'R',
            //                               'S',
            //                               'T',
            //                               'U',
            //                               'V',
            //                               'W',
            //                               'X',
            //                               'Y',
            //                               'Z',
            //                               '0',
            //                               '1',
            //                               '2',
            //                               '3',
            //                               '4',
            //                               '5',
            //                               '6',
            //                               '7',
            //                               '8',
            //                               '9' };
            //            final Random random = new Random();
            //            for ( int len = minWordLength; len <= maxWordLength; len++ )
            //            {
            //                Set<String> w = new HashSet<>();
            //
            //                while ( w.size() < wordCountPerLength )
            //                {
            //                    StringBuilder sb = new StringBuilder();
            //                    for ( int i = 0; i < len; i++ )
            //                    {
            //                        sb.append( letters[ random.nextInt( letters.length ) ] );
            //                    }
            //
            //                    w.add( sb.toString() );
            //                }
            //
            //                words.add( w );
            //            }
            //
            //            shuffle( words );

            for ( int len = minSentenceLength; len <= maxSentenceLength; len++ )
            {
                for ( int j = 0; j < sentenceCountPerLength; j++ )
                {
                    sentences.add( len );
                }
            }

            shuffle( sentences );
        }

        private List<String> getRandomizedSentences ()
        {
            shuffle( words );
            shuffle( sentences );

            final List<String> sentences = new ArrayList<>( shuffledSentenceCount );
            for ( int i = 0; i < shuffledSentenceCount; i++ )
            {
                final int sentenceLength = this.sentences.get( sentenceIdx++ );
                if ( sentenceIdx == this.sentences.size() )
                {
                    sentenceIdx = 0;
                }

                final StringBuilder sb = new StringBuilder();
                for ( int j = 0; j < sentenceLength; j++ )
                {
                    final String word = words.get( wordIdx++ );
                    if ( wordIdx == words.size() )
                    {
                        wordIdx = 0;
                    }

                    sb.append( word );

                    if ( j < ( sentenceLength - 1 ) )
                    {
                        sb.append( " " );
                    }
                }

                sentences.add( sb.toString() );
            }

            return sentences;
        }
    }

}
