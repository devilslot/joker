package cs.bilkent.joker.engine.region;

import static com.google.common.base.Preconditions.checkArgument;
import cs.bilkent.joker.engine.flow.PipelineId;
import cs.bilkent.joker.engine.flow.RegionDef;
import cs.bilkent.joker.engine.flow.RegionExecPlan;
import cs.bilkent.joker.engine.pipeline.PipelineReplica;
import cs.bilkent.joker.engine.pipeline.PipelineReplicaId;
import cs.bilkent.joker.engine.pipeline.UpstreamCtx;
import cs.bilkent.joker.operator.scheduling.ScheduleWhenAvailable;
import cs.bilkent.joker.operator.scheduling.ScheduleWhenTuplesAvailable;
import cs.bilkent.joker.operator.scheduling.ScheduleWhenTuplesAvailable.TupleAvailabilityByCount;
import static cs.bilkent.joker.operator.scheduling.ScheduleWhenTuplesAvailable.TupleAvailabilityByCount.AT_LEAST;
import cs.bilkent.joker.operator.scheduling.SchedulingStrategy;
import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOf;

public class Region
{

    private final RegionExecPlan execPlan;

    private final SchedulingStrategy[] schedulingStrategies;

    private final UpstreamCtx[] upstreamCtxes;

    private final int[] fusionStartIndices;

    // [pipelineIndex, replicaIndex]
    private final PipelineReplica[][] pipelines;

    public Region ( final RegionExecPlan execPlan, final SchedulingStrategy[] schedulingStrategies, final UpstreamCtx[] upstreamCtxes,
                    final PipelineReplica[][] pipelines )
    {
        this.execPlan = execPlan;
        this.schedulingStrategies = copyOf( schedulingStrategies, schedulingStrategies.length );
        this.fusionStartIndices = findFusionStartIndices( schedulingStrategies );
        this.upstreamCtxes = copyOf( upstreamCtxes, upstreamCtxes.length );
        this.pipelines = copyOf( pipelines, pipelines.length );
        for ( int i = 0; i < pipelines.length; i++ )
        {
            this.pipelines[ i ] = copyOf( pipelines[ i ], pipelines[ i ].length );
        }
    }

    public int getRegionId ()
    {
        return execPlan.getRegionId();
    }

    public RegionExecPlan getExecPlan ()
    {
        return execPlan;
    }

    public RegionDef getRegionDef ()
    {
        return execPlan.getRegionDef();
    }

    public PipelineReplica[] getReplicaPipelines ( final int replicaIndex )
    {
        final PipelineReplica[] p = new PipelineReplica[ execPlan.getPipelineCount() ];
        for ( int i = 0; i < execPlan.getPipelineCount(); i++ )
        {
            p[ i ] = pipelines[ i ][ replicaIndex ];
        }
        return p;
    }

    public PipelineReplica[] getPipelineReplicas ( final int pipelineIndex )
    {
        return pipelines[ pipelineIndex ];
    }

    public PipelineReplica[] getPipelineReplicas ( final PipelineId pipelineId )
    {
        for ( int i = 0; i < execPlan.getPipelineCount(); i++ )
        {
            if ( pipelines[ i ][ 0 ].id().pipelineId.equals( pipelineId ) )
            {
                return copyOf( pipelines[ i ], execPlan.getReplicaCount() );
            }
        }

        throw new IllegalArgumentException( "Invalid pipeline id: " + pipelineId );
    }

    public PipelineReplica getPipelineReplica ( final PipelineReplicaId pipelineReplicaId )
    {
        return getPipelineReplicas( pipelineReplicaId.pipelineId )[ pipelineReplicaId.replicaIndex ];
    }

    public SchedulingStrategy[] getSchedulingStrategies ()
    {
        return copyOf( schedulingStrategies, schedulingStrategies.length );
    }

    public int[] getFusionStartIndices ()
    {
        return copyOf( fusionStartIndices, fusionStartIndices.length );
    }

    public SchedulingStrategy getSchedulingStrategy ( final int operatorIndex )
    {
        return schedulingStrategies[ operatorIndex ];
    }

    public SchedulingStrategy[] getSchedulingStrategies ( final PipelineId pipelineId )
    {
        final int pipelineStartIndex = pipelineId.getPipelineStartIndex();
        final int operatorCount = execPlan.getOperatorCountByPipelineStartIndex( pipelineStartIndex );

        final SchedulingStrategy[] schedulingStrategies = new SchedulingStrategy[ operatorCount ];
        arraycopy( this.schedulingStrategies, pipelineStartIndex, schedulingStrategies, 0, operatorCount );

        return schedulingStrategies;
    }

    public SchedulingStrategy[][] getFusedSchedulingStrategies ( final PipelineId pipelineId )
    {
        final SchedulingStrategy[] schedulingStrategies = getSchedulingStrategies( pipelineId );
        final int[] fusionStartIndices = findFusionStartIndices( schedulingStrategies );
        final SchedulingStrategy[][] fusedSchedulingStrategies = new SchedulingStrategy[ fusionStartIndices.length ][];
        for ( int i = 0; i < fusionStartIndices.length; i++ )
        {
            final int fusionStartIndex = fusionStartIndices[ i ];
            final int pipelineOperatorCount = execPlan.getOperatorCountByPipelineStartIndex( pipelineId.getPipelineStartIndex() );
            final int length =
                    ( i == ( fusionStartIndices.length - 1 ) ? pipelineOperatorCount : fusionStartIndices[ i + 1 ] ) - fusionStartIndex;
            SchedulingStrategy[] s = new SchedulingStrategy[ length ];
            arraycopy( schedulingStrategies, fusionStartIndex, s, 0, length );
            fusedSchedulingStrategies[ i ] = s;
        }

        return fusedSchedulingStrategies;
    }

    public UpstreamCtx[][] getFusedUpstreamCtxes ( final PipelineId pipelineId )
    {
        final UpstreamCtx[] upstreamCtxes = getUpstreamCtxes( pipelineId );
        final int[] fusionStartIndices = findFusionStartIndices( getSchedulingStrategies( pipelineId ) );
        final UpstreamCtx[][] fusedUpstreamCtxes = new UpstreamCtx[ fusionStartIndices.length ][];
        for ( int i = 0; i < fusionStartIndices.length; i++ )
        {
            final int fusionStartIndex = fusionStartIndices[ i ];
            final int pipelineOperatorCount = execPlan.getOperatorCountByPipelineStartIndex( pipelineId.getPipelineStartIndex() );
            final int length =
                    ( i == ( fusionStartIndices.length - 1 ) ? pipelineOperatorCount : fusionStartIndices[ i + 1 ] ) - fusionStartIndex;
            UpstreamCtx[] u = new UpstreamCtx[ length ];
            arraycopy( upstreamCtxes, fusionStartIndex, u, 0, length );
            fusedUpstreamCtxes[ i ] = u;
        }

        return fusedUpstreamCtxes;
    }


    public UpstreamCtx[] getUpstreamCtxes ()
    {
        return copyOf( upstreamCtxes, upstreamCtxes.length );
    }

    public UpstreamCtx[] getUpstreamCtxes ( final PipelineId pipelineId )
    {
        return getUpstreamCtxes( pipelineId.getPipelineStartIndex() );
    }

    public UpstreamCtx[] getUpstreamCtxes ( final int pipelineStartIndex )
    {
        final int operatorCount = execPlan.getOperatorCountByPipelineStartIndex( pipelineStartIndex );

        final UpstreamCtx[] upstreamCtxes = new UpstreamCtx[ operatorCount ];
        arraycopy( this.upstreamCtxes, pipelineStartIndex, upstreamCtxes, 0, operatorCount );

        return upstreamCtxes;
    }

    public static int[] findFusionStartIndices ( final SchedulingStrategy[] operatorSchedulingStrategies )
    {
        final int[] indices = new int[ operatorSchedulingStrategies.length ];
        indices[ 0 ] = 0;
        int j = 1;
        for ( int i = 1; i < operatorSchedulingStrategies.length; i++ )
        {
            if ( !isFusible( operatorSchedulingStrategies[ i ] ) )
            {
                indices[ j++ ] = i;
            }
        }

        return copyOf( indices, j );
    }

    public static boolean isFusible ( SchedulingStrategy strategy )
    {
        if ( strategy instanceof ScheduleWhenAvailable )
        {
            return false;
        }

        checkArgument( strategy instanceof ScheduleWhenTuplesAvailable );

        final ScheduleWhenTuplesAvailable st = (ScheduleWhenTuplesAvailable) strategy;
        final TupleAvailabilityByCount tupleAvailabilityByCount = st.getTupleAvailabilityByCount();
        if ( tupleAvailabilityByCount != AT_LEAST )
        {
            return false;
        }

        for ( int portIndex = 0; portIndex < st.getPortCount(); portIndex++ )
        {
            if ( st.getTupleCount( portIndex ) > 1 )
            {
                return false;
            }
        }

        return true;
    }

}
