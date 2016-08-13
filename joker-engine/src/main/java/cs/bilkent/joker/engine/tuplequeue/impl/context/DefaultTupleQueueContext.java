package cs.bilkent.joker.engine.tuplequeue.impl.context;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import cs.bilkent.joker.engine.config.ThreadingPreference;
import static cs.bilkent.joker.engine.config.ThreadingPreference.MULTI_THREADED;
import cs.bilkent.joker.engine.tuplequeue.TupleQueue;
import cs.bilkent.joker.engine.tuplequeue.TupleQueueContext;
import cs.bilkent.joker.engine.tuplequeue.TupleQueueDrainer;
import cs.bilkent.joker.operator.Tuple;
import cs.bilkent.joker.operator.scheduling.ScheduleWhenTuplesAvailable.TupleAvailabilityByPort;


public class DefaultTupleQueueContext implements TupleQueueContext
{

    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultTupleQueueContext.class );

    private final String operatorId;

    private final TupleQueue[] tupleQueues;

    private final ThreadingPreference threadingPreference;

    public DefaultTupleQueueContext ( final String operatorId,
                                      final int inputPortCount,
                                      final ThreadingPreference threadingPreference,
                                      final Function<Boolean, TupleQueue> tupleQueueConstructor

    )
    {
        this.operatorId = operatorId;
        this.threadingPreference = threadingPreference;
        this.tupleQueues = new TupleQueue[ inputPortCount ];
        for ( int portIndex = 0; portIndex < inputPortCount; portIndex++ )
        {
            this.tupleQueues[ portIndex ] = tupleQueueConstructor.apply( true );
        }
    }

    @Override
    public String getOperatorId ()
    {
        return operatorId;
    }

    @Override
    public int getInputPortCount ()
    {
        return tupleQueues.length;
    }

    @Override
    public void offer ( final int portIndex, final List<Tuple> tuples )
    {
        final TupleQueue[] tupleQueues = getTupleQueues( tuples );

        if ( tupleQueues != null )
        {
            tupleQueues[ portIndex ].offerTuples( tuples );
        }
    }

    @Override
    public int tryOffer ( final int portIndex, final List<Tuple> tuples, final long timeoutInMillis )
    {
        if ( tuples == null )
        {
            return -1;
        }

        final TupleQueue[] tupleQueues = getTupleQueues( tuples );

        if ( tupleQueues != null )
        {
            return tupleQueues[ portIndex ].tryOfferTuples( tuples, timeoutInMillis );
        }

        return -1;
    }

    @Override
    public void forceOffer ( final int portIndex, final List<Tuple> tuples )
    {
        final TupleQueue[] tupleQueues = getTupleQueues( tuples );

        if ( tupleQueues == null )
        {
            return;
        }

        tupleQueues[ portIndex ].forceOfferTuples( tuples );
    }

    private TupleQueue[] getTupleQueues ( final List<Tuple> tuples )
    {
        return ( tuples == null || tuples.isEmpty() ) ? null : tupleQueues;
    }

    @Override
    public void drain ( TupleQueueDrainer drainer )
    {
        drainer.drain( null, tupleQueues );
    }

    @Override
    public void ensureCapacity ( final int portIndex, final int capacity )
    {
        tupleQueues[ portIndex ].ensureCapacity( capacity );
    }

    @Override
    public void clear ()
    {
        LOGGER.info( "Clearing tuple queues of operator: {}", operatorId );

        for ( TupleQueue tupleQueue : tupleQueues )
        {
            tupleQueue.clear();
        }
    }

    @Override
    public void setTupleCounts ( final int[] tupleCounts, final TupleAvailabilityByPort tupleAvailabilityByPort )
    {
        if ( threadingPreference == MULTI_THREADED )
        {
            for ( int portIndex = 0; portIndex < getInputPortCount(); portIndex++ )
            {
                tupleQueues[ portIndex ].ensureCapacity( tupleCounts[ portIndex ] );
            }
        }
    }

    @Override
    public void prepareGreedyDraining ()
    {

    }

    @Override
    public void enableCapacityCheck ( final int portIndex )
    {
        checkState( threadingPreference == MULTI_THREADED,
                    "Cannot enable capacity check for single threaded tuple queue of operator %s",
                    operatorId );
        tupleQueues[ portIndex ].enableCapacityCheck();
    }

    @Override
    public void disableCapacityCheck ( final int portIndex )
    {
        checkState( threadingPreference == MULTI_THREADED,
                    "Cannot disable capacity check for single threaded tuple queue of operator %s",
                    operatorId );
        tupleQueues[ portIndex ].disableCapacityCheck();
    }

    @Override
    public boolean isCapacityCheckEnabled ( final int portIndex )
    {
        checkState( threadingPreference == MULTI_THREADED,
                    "Cannot check if capacity enabled for single threaded tuple queue of operator %s",
                    operatorId );
        return tupleQueues[ portIndex ].isCapacityCheckEnabled();
    }

    public ThreadingPreference getThreadingPreference ()
    {
        return threadingPreference;
    }

}