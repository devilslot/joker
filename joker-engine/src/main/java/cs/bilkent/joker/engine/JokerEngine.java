package cs.bilkent.joker.engine;


import java.util.List;
import java.util.concurrent.Future;
import javax.inject.Inject;

import cs.bilkent.joker.engine.exception.InitializationException;
import cs.bilkent.joker.engine.pipeline.PipelineId;
import cs.bilkent.joker.engine.region.FlowDefOptimizer;
import cs.bilkent.joker.engine.region.RegionConfig;
import cs.bilkent.joker.engine.region.RegionConfigFactory;
import cs.bilkent.joker.engine.region.RegionDef;
import cs.bilkent.joker.engine.region.RegionDefFormer;
import cs.bilkent.joker.engine.supervisor.impl.SupervisorImpl;
import cs.bilkent.joker.flow.FlowDef;
import cs.bilkent.joker.utils.Pair;

public class JokerEngine
{

    private final RegionDefFormer regionDefFormer;

    private final FlowDefOptimizer flowDefOptimizer;

    private final RegionConfigFactory regionConfigFactory;

    private final SupervisorImpl supervisor;

    @Inject
    public JokerEngine ( final RegionDefFormer regionDefFormer, final FlowDefOptimizer flowDefOptimizer,
                         final RegionConfigFactory regionConfigFactory,
                         final SupervisorImpl supervisor )
    {
        this.regionDefFormer = regionDefFormer;
        this.flowDefOptimizer = flowDefOptimizer;
        this.regionConfigFactory = regionConfigFactory;
        this.supervisor = supervisor;
    }

    public void run ( final FlowDef flow ) throws InitializationException
    {
        final Pair<FlowDef, List<RegionDef>> result = flowDefOptimizer.optimize( flow, regionDefFormer.createRegions( flow ) );
        final List<RegionConfig> regionConfigs = regionConfigFactory.createRegionConfigs( result._2 );
        supervisor.start( result._1, result._2, regionConfigs );
    }

    public FlowStatus getStatus ()
    {
        return supervisor.getFlowStatus();
    }

    public Future<Void> shutdown ()
    {
        return supervisor.shutdown();
    }

    public Future<Void> mergePipelines ( final List<PipelineId> pipelineIds )
    {
        return supervisor.mergePipelines( pipelineIds );
    }

    public Future<Void> splitPipeline ( final PipelineId pipelineId, final List<Integer> pipelineOperatorIndices )
    {
        return supervisor.splitPipeline( pipelineId, pipelineOperatorIndices );
    }

    public Future<Void> rebalanceRegion ( final int regionId, final int newReplicaCount )
    {
        return supervisor.rebalanceRegion( regionId, newReplicaCount );
    }

}
