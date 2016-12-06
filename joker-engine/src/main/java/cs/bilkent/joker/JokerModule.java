package cs.bilkent.joker;

import java.util.UUID;

import com.google.inject.AbstractModule;

import static com.google.inject.name.Names.named;
import cs.bilkent.joker.engine.config.JokerConfig;
import static cs.bilkent.joker.engine.config.JokerConfig.JOKER_ID;
import static cs.bilkent.joker.engine.config.JokerConfig.JOKER_THREAD_GROUP_NAME;
import cs.bilkent.joker.engine.kvstore.OperatorKVStoreManager;
import cs.bilkent.joker.engine.kvstore.impl.OperatorKVStoreManagerImpl;
import cs.bilkent.joker.engine.partition.PartitionKeyExtractorFactory;
import cs.bilkent.joker.engine.partition.PartitionService;
import cs.bilkent.joker.engine.partition.impl.PartitionKeyExtractorFactoryImpl;
import cs.bilkent.joker.engine.partition.impl.PartitionServiceImpl;
import cs.bilkent.joker.engine.pipeline.DownstreamTupleSenderFailureFlag;
import cs.bilkent.joker.engine.pipeline.PipelineManager;
import cs.bilkent.joker.engine.pipeline.impl.PipelineManagerImpl;
import cs.bilkent.joker.engine.region.FlowDefOptimizer;
import cs.bilkent.joker.engine.region.PipelineTransformer;
import cs.bilkent.joker.engine.region.RegionConfigFactory;
import cs.bilkent.joker.engine.region.RegionDefFormer;
import cs.bilkent.joker.engine.region.RegionManager;
import cs.bilkent.joker.engine.region.impl.FlowDefOptimizerImpl;
import cs.bilkent.joker.engine.region.impl.IdGenerator;
import cs.bilkent.joker.engine.region.impl.InteractiveRegionConfigFactory;
import cs.bilkent.joker.engine.region.impl.PipelineTransformerImpl;
import cs.bilkent.joker.engine.region.impl.RegionDefFormerImpl;
import cs.bilkent.joker.engine.region.impl.RegionManagerImpl;
import cs.bilkent.joker.engine.supervisor.Supervisor;
import cs.bilkent.joker.engine.supervisor.impl.SupervisorImpl;
import cs.bilkent.joker.engine.tuplequeue.OperatorTupleQueueManager;
import cs.bilkent.joker.engine.tuplequeue.impl.OperatorTupleQueueManagerImpl;

public class JokerModule extends AbstractModule
{

    private Object jokerId;

    private final JokerConfig config;

    private final RegionConfigFactory regionConfigFactory;

    public JokerModule ( final JokerConfig config )
    {
        this( UUID.randomUUID().toString(), config, null );
    }

    public JokerModule ( final Object jokerId, final JokerConfig config, final RegionConfigFactory regionConfigFactory )
    {
        this.jokerId = jokerId;
        this.config = config;
        this.regionConfigFactory = regionConfigFactory;
    }

    public Object getJokerId ()
    {
        return jokerId;
    }

    public JokerConfig getConfig ()
    {
        return config;
    }

    public RegionConfigFactory getRegionConfigFactory ()
    {
        return regionConfigFactory;
    }

    @Override
    protected void configure ()
    {
        bind( PartitionService.class ).to( PartitionServiceImpl.class );
        bind( OperatorKVStoreManager.class ).to( OperatorKVStoreManagerImpl.class );
        bind( OperatorTupleQueueManager.class ).to( OperatorTupleQueueManagerImpl.class );
        bind( RegionManager.class ).to( RegionManagerImpl.class );
        bind( RegionDefFormer.class ).to( RegionDefFormerImpl.class );
        bind( Supervisor.class ).to( SupervisorImpl.class );
        bind( PartitionKeyExtractorFactory.class ).to( PartitionKeyExtractorFactoryImpl.class );
        bind( PipelineManager.class ).to( PipelineManagerImpl.class );
        bind( FlowDefOptimizer.class ).to( FlowDefOptimizerImpl.class );
        bind( PipelineTransformer.class ).to( PipelineTransformerImpl.class );
        if ( regionConfigFactory != null )
        {
            bind( RegionConfigFactory.class ).toInstance( regionConfigFactory );
        }
        else
        {
            bind( RegionConfigFactory.class ).to( InteractiveRegionConfigFactory.class );
        }
        bind( JokerConfig.class ).toInstance( config );
        bind( ThreadGroup.class ).annotatedWith( named( JOKER_THREAD_GROUP_NAME ) ).toInstance( new ThreadGroup( "Joker" ) );
        bind( IdGenerator.class ).toInstance( new IdGenerator() );
        bind( DownstreamTupleSenderFailureFlag.class ).toInstance( new DownstreamTupleSenderFailureFlag() );
        bind( Object.class ).annotatedWith( named( JOKER_ID ) ).toInstance( jokerId );
    }

}
