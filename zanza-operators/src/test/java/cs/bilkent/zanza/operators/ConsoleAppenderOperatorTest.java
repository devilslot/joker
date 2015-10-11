package cs.bilkent.zanza.operators;

import java.io.PrintStream;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import cs.bilkent.zanza.operator.PortsToTuples;
import cs.bilkent.zanza.operator.ProcessingResult;
import cs.bilkent.zanza.operator.Tuple;
import cs.bilkent.zanza.operator.invocationreason.SuccessfulInvocation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;

@RunWith( MockitoJUnitRunner.class )
public class ConsoleAppenderOperatorTest
{

    @Mock
    private PrintStream sysOut;

    private PrintStream orgSysOut;

    private final ConsoleAppenderOperator operator = new ConsoleAppenderOperator();

    private final SimpleOperatorContext operatorContext = new SimpleOperatorContext();

    @Before
    public void init ()
    {
        orgSysOut = System.out;
        System.setOut( sysOut );
    }

    @After
    public void after ()
    {
        System.setOut( orgSysOut );
    }

    @Test
    public void shouldPrintTuplesToConsoleWithTupleToString ()
    {
        operator.init( operatorContext );

        final Tuple tuple1 = new Tuple( "k1", "v1" );
        final Tuple tuple2 = new Tuple( "k2", "v2" );
        final PortsToTuples input = new PortsToTuples( tuple1, tuple2 );
        final ProcessingResult output = operator.process( input, SuccessfulInvocation.INSTANCE );

        assertThat( output.getPortsToTuples(), equalTo( input ) );
        verify( sysOut ).println( tuple1.toString() );
        verify( sysOut ).println( tuple2.toString() );
    }

    @Test
    public void shouldPrintTuplesToConsoleWithToStringFunction ()
    {
        final Function<Tuple, String> toStringFunc = ( tuple ) -> tuple.toString().toUpperCase();
        operatorContext.getConfig().set( ConsoleAppenderOperator.TO_STRING_FUNCTION_CONFIG_PARAMETER, toStringFunc );
        operator.init( operatorContext );

        final Tuple tuple1 = new Tuple( "k1", "v1" );
        final Tuple tuple2 = new Tuple( "k2", "v2" );
        final PortsToTuples input = new PortsToTuples( tuple1, tuple2 );
        final ProcessingResult output = operator.process( input, SuccessfulInvocation.INSTANCE );

        assertThat( output.getPortsToTuples(), equalTo( input ) );
        verify( sysOut ).println( toStringFunc.apply( tuple1 ) );
        verify( sysOut ).println( toStringFunc.apply( tuple2 ) );
    }

}