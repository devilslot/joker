package cs.bilkent.joker.engine.tuplequeue.impl.drainer;

import cs.bilkent.joker.engine.tuplequeue.TupleQueue;
import cs.bilkent.joker.engine.util.concurrent.BackoffIdleStrategy;
import cs.bilkent.joker.engine.util.concurrent.IdleStrategy;

public class BlockingMultiPortConjunctiveDrainer extends MultiPortDrainer
{

    private final IdleStrategy idleStrategy = BackoffIdleStrategy.newDefaultInstance();

    public BlockingMultiPortConjunctiveDrainer ( final String operatorId, final int inputPortCount, final int maxBatchSize )
    {
        super( operatorId, inputPortCount, maxBatchSize );
    }

    @Override
    protected int[] checkQueueSizes ( final TupleQueue[] tupleQueues )
    {
        idleStrategy.reset();

        boolean idle = false;
        while ( true )
        {
            int satisfied = 0;
            for ( int i = 0; i < limit; i += 2 )
            {
                final int portIndex = tupleCountsToCheck[ i ];
                final int tupleCount = tupleCountsToCheck[ i + 1 ];
                final TupleQueue tupleQueue = tupleQueues[ portIndex ];
                switch ( tupleCount )
                {
                    case NO_TUPLES_AVAILABLE:
                        satisfied++;
                        break;
                    case 1:
                        if ( tupleQueue.isNonEmpty() )
                        {
                            satisfied++;
                        }
                        break;
                    default:
                        if ( tupleQueue.size() >= tupleCount )
                        {
                            satisfied++;
                        }
                }
            }

            if ( satisfied == inputPortCount )
            {
                return tupleCountsToDrain;
            }
            else
            {
                if ( idle )
                {
                    return null;
                }

                idle = idleStrategy.idle();
            }
        }
    }

}
