package cs.bilkent.zanza.engine.tuplequeue.impl.drainer;

import org.junit.Test;

import cs.bilkent.zanza.engine.tuplequeue.TupleQueue;
import cs.bilkent.zanza.engine.tuplequeue.impl.queue.SingleThreadedTupleQueue;
import static cs.bilkent.zanza.flow.Port.DEFAULT_PORT_INDEX;
import cs.bilkent.zanza.operator.Tuple;
import cs.bilkent.zanza.operator.impl.TuplesImpl;
import static cs.bilkent.zanza.operator.scheduling.ScheduleWhenTuplesAvailable.TupleAvailabilityByCount.AT_LEAST;
import static cs.bilkent.zanza.operator.scheduling.ScheduleWhenTuplesAvailable.TupleAvailabilityByCount.AT_LEAST_BUT_SAME_ON_ALL_PORTS;
import static cs.bilkent.zanza.operator.scheduling.ScheduleWhenTuplesAvailable.TupleAvailabilityByCount.EXACT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class NonBlockingSinglePortDrainerTest
{

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWithNullTupleQueues ()
    {
        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( AT_LEAST, 1 );
        drainer.drain( null, null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWithEmptyTupleQueues ()
    {
        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( AT_LEAST, 1 );
        drainer.drain( null, new TupleQueue[] {} );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWithMultipleTupleQueues ()
    {
        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( AT_LEAST, 1 );
        drainer.drain( null, new TupleQueue[] { new SingleThreadedTupleQueue( 1 ), new SingleThreadedTupleQueue( 1 ) } );
    }

    @Test
    public void shouldDrainAllTuplesWithAtLeastTupleAvailabilityByCountSatisfied ()
    {
        final TupleQueue tupleQueue = new SingleThreadedTupleQueue( 2 );
        final Tuple tuple1 = new Tuple();
        tupleQueue.offerTuple( tuple1 );
        final Tuple tuple2 = new Tuple();
        tupleQueue.offerTuple( tuple2 );

        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( AT_LEAST, 1 );
        drainer.drain( null, new TupleQueue[] { tupleQueue } );

        final TuplesImpl tuples = drainer.getResult();
        assertNotNull( tuples );
        assertThat( tuples.getNonEmptyPortCount(), equalTo( 1 ) );
        assertThat( tuples.getTupleCount( DEFAULT_PORT_INDEX ), equalTo( 2 ) );
        assertTrue( tuple1 == tuples.getTupleOrFail( DEFAULT_PORT_INDEX, 0 ) );
        assertTrue( tuple2 == tuples.getTupleOrFail( DEFAULT_PORT_INDEX, 1 ) );
        assertThat( tupleQueue.size(), equalTo( 0 ) );
    }

    @Test
    public void shouldNotDrainAnyTupleWithAtLeastTupleAvailabilityByCountUnsatisfied ()
    {
        final TupleQueue tupleQueue = new SingleThreadedTupleQueue( 2 );

        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( AT_LEAST, 1 );
        drainer.drain( null, new TupleQueue[] { tupleQueue } );

        final TuplesImpl tuples = drainer.getResult();
        assertNull( tuples );
    }

    @Test
    public void shouldDrainTuplesWithExactButSameOnAllPortsTupleAvailabilityByCountSatisfied ()
    {
        final TupleQueue tupleQueue = new SingleThreadedTupleQueue( 2 );
        final Tuple tuple1 = new Tuple();
        tupleQueue.offerTuple( tuple1 );
        final Tuple tuple2 = new Tuple();
        tupleQueue.offerTuple( tuple2 );

        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( AT_LEAST_BUT_SAME_ON_ALL_PORTS, 1 );
        drainer.drain( null, new TupleQueue[] { tupleQueue } );

        final TuplesImpl tuples = drainer.getResult();
        assertNotNull( tuples );
        assertThat( tuples.getNonEmptyPortCount(), equalTo( 1 ) );
        assertThat( tuples.getTupleCount( DEFAULT_PORT_INDEX ), equalTo( 2 ) );
        assertTrue( tuple1 == tuples.getTupleOrFail( DEFAULT_PORT_INDEX, 0 ) );
        assertTrue( tuple2 == tuples.getTupleOrFail( DEFAULT_PORT_INDEX, 1 ) );
        assertThat( tupleQueue.size(), equalTo( 0 ) );
    }

    @Test
    public void shouldNotDrainAnyTupleWithExactButSameOnAllPortsTupleAvailabilityByCountUnsatisfied ()
    {
        final TupleQueue tupleQueue = new SingleThreadedTupleQueue( 2 );

        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( AT_LEAST_BUT_SAME_ON_ALL_PORTS, 1 );
        drainer.drain( null, new TupleQueue[] { tupleQueue } );

        final TuplesImpl tuples = drainer.getResult();
        assertNull( tuples );
    }

    @Test
    public void shouldDrainTuplesWithExactTupleAvailabilityByCountSatisfied ()
    {
        final TupleQueue tupleQueue = new SingleThreadedTupleQueue( 2 );
        final Tuple tuple1 = new Tuple();
        tupleQueue.offerTuple( tuple1 );
        tupleQueue.offerTuple( new Tuple() );

        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( EXACT, 1 );
        drainer.drain( null, new TupleQueue[] { tupleQueue } );

        final TuplesImpl tuples = drainer.getResult();
        assertNotNull( tuples );
        assertThat( tuples.getNonEmptyPortCount(), equalTo( 1 ) );
        assertThat( tuples.getTupleCount( DEFAULT_PORT_INDEX ), equalTo( 1 ) );
        assertTrue( tuple1 == tuples.getTupleOrFail( DEFAULT_PORT_INDEX, 0 ) );

        assertThat( tupleQueue.size(), equalTo( 1 ) );
    }

    @Test
    public void shouldNotDrainAnyTupleWithExactTupleAvailabilityByCountUnsatisfied ()
    {
        final TupleQueue tupleQueue = new SingleThreadedTupleQueue( 2 );

        final NonBlockingSinglePortDrainer drainer = new NonBlockingSinglePortDrainer();
        drainer.setParameters( EXACT, 1 );
        drainer.drain( null, new TupleQueue[] { tupleQueue } );

        final TuplesImpl tuples = drainer.getResult();
        assertNull( tuples );
    }

}