package cs.bilkent.joker.engine.pipeline.impl.downstreamcollector;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkArgument;
import static cs.bilkent.joker.JokerModule.DOWNSTREAM_FAILURE_FLAG_NAME;
import cs.bilkent.joker.engine.partition.PartitionKeyExtractor;
import cs.bilkent.joker.engine.tuplequeue.OperatorQueue;
import cs.bilkent.joker.operator.impl.TuplesImpl;

public class PartitionedDownstreamCollectorN extends AbstractPartitionedDownstreamCollector
{

    private final int[] ports;

    private final int limit;

    public PartitionedDownstreamCollectorN ( @Named( DOWNSTREAM_FAILURE_FLAG_NAME ) final AtomicBoolean failureFlag,
                                             final int[] sourcePorts,
                                             final int[] destinationPorts,
                                             final int partitionCount,
                                             final int[] partitionDistribution,
                                             final OperatorQueue[] operatorQueues,
                                             final PartitionKeyExtractor partitionKeyExtractor )
    {
        super( failureFlag, partitionCount, partitionDistribution, operatorQueues, partitionKeyExtractor );
        checkArgument( sourcePorts.length == destinationPorts.length,
                       "source ports size = %s and destination ports = %s ! destination operatorId=%s",
                       sourcePorts.length,
                       destinationPorts.length,
                       operatorQueues[ 0 ].getOperatorId() );
        final int portCount = sourcePorts.length;
        this.ports = new int[ portCount * 2 ];
        this.limit = this.ports.length - 1;
        for ( int i = 0; i < portCount; i++ )
        {
            ports[ i * 2 ] = sourcePorts[ i ];
            ports[ i * 2 + 1 ] = destinationPorts[ i ];
        }
    }

    @Override
    public void accept ( final TuplesImpl tuples )
    {
        for ( int i = 0; i < limit; i += 2 )
        {
            send( tuples, ports[ i ], ports[ i + 1 ] );
        }
    }
}
