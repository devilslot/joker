package cs.bilkent.joker;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import cs.bilkent.joker.Joker.JokerBuilder;
import cs.bilkent.joker.engine.adaptation.impl.adaptationtracker.SmartAdaptationTracker;
import cs.bilkent.joker.engine.config.JokerConfig;
import cs.bilkent.joker.engine.config.JokerConfigBuilder;
import cs.bilkent.joker.engine.flow.FlowExecPlan;
import cs.bilkent.joker.engine.flow.RegionExecPlan;
import cs.bilkent.joker.engine.metric.PipelineMetrics;
import cs.bilkent.joker.engine.metric.PipelineMetricsHistory;
import cs.bilkent.joker.engine.region.impl.DefaultRegionExecPlanFactory;
import cs.bilkent.joker.flow.FlowDef;
import cs.bilkent.joker.flow.FlowDefBuilder;
import static cs.bilkent.joker.impl.com.google.common.base.Preconditions.checkArgument;
import cs.bilkent.joker.operator.InitCtx;
import cs.bilkent.joker.operator.InvocationCtx;
import cs.bilkent.joker.operator.Operator;
import cs.bilkent.joker.operator.OperatorConfig;
import cs.bilkent.joker.operator.OperatorDef;
import cs.bilkent.joker.operator.OperatorDefBuilder;
import cs.bilkent.joker.operator.Tuple;
import static cs.bilkent.joker.operator.scheduling.ScheduleWhenTuplesAvailable.scheduleWhenTuplesAvailableOnDefaultPort;
import cs.bilkent.joker.operator.scheduling.SchedulingStrategy;
import cs.bilkent.joker.operator.schema.runtime.OperatorRuntimeSchema;
import cs.bilkent.joker.operator.schema.runtime.OperatorRuntimeSchemaBuilder;
import cs.bilkent.joker.operator.schema.runtime.TupleSchema;
import cs.bilkent.joker.operator.spec.OperatorSpec;
import cs.bilkent.joker.operator.spec.OperatorType;
import static cs.bilkent.joker.operator.spec.OperatorType.PARTITIONED_STATEFUL;
import static cs.bilkent.joker.operator.spec.OperatorType.STATELESS;
import cs.bilkent.joker.operators.BeaconOperator;
import static cs.bilkent.joker.operators.BeaconOperator.TUPLE_COUNT_CONFIG_PARAMETER;
import static cs.bilkent.joker.operators.BeaconOperator.TUPLE_POPULATOR_CONFIG_PARAMETER;
import cs.bilkent.joker.operators.ForEachOperator;
import static cs.bilkent.joker.operators.ForEachOperator.CONSUMER_FUNCTION_CONFIG_PARAMETER;
import static cs.bilkent.joker.operators.MapperOperator.MAPPER_CONFIG_PARAMETER;
import cs.bilkent.joker.test.AbstractJokerTest;
import cs.bilkent.joker.test.category.SlowTest;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;

@Ignore
@Category( SlowTest.class )
public class JokerDemo extends AbstractJokerTest
{

    private static final OperatorType MIDDLE_REGION_TYPE = PARTITIONED_STATEFUL;

    private static final int KEY_RANGE = 1000000;

    private static final int PARTITIONER_COST = 128;

    private static final int MULTIPLIER_COST = 128;

    private final FlowExample flowExample = new FlowExample();

    //
    //                           |                                                       |
    // beacon (key, val1, val2) ---> partitioner (key, val) ---> multiplier1 (key, val) ---> sink
    //                           |                                                       |
    //          STATEFUL         |                  PARTITIONED STATEFUL                 |   STATEFUL
    //                           |                                                       |
    //

    @Test
    public void testDefaultExecutionModel () throws InterruptedException, ExecutionException, TimeoutException
    {
        final FlowDef flow = flowExample.build();

        final Joker joker = newJokerInstance( true );

        joker.run( flow );

        sleepUninterruptibly( 30000, SECONDS );

        joker.shutdown().get( 60, SECONDS );

        System.out.println( "Total: " + flowExample.getProcessedTupleCount() + " tuples" );
    }

    @Test
    public void testAutoShutdown () throws InterruptedException, ExecutionException, TimeoutException
    {
        final FlowDef flow = flowExample.build();

        final JokerConfigBuilder configBuilder = new JokerConfigBuilder();
        configBuilder.getAdaptationConfigBuilder().enableAdaptation().enableVisualization();
        configBuilder.getFlowDefOptimizerConfigBuilder().disableMergeRegions();
        final JokerConfig jokerConfig = configBuilder.build();

        final SmartAdaptationTracker adaptationTracker = new SmartAdaptationTracker( jokerConfig );

        final Joker joker = new JokerBuilder().setJokerConfig( jokerConfig )
                                              .setRegionExecPlanFactory( new DefaultRegionExecPlanFactory( jokerConfig ) )
                                              .setAdaptationTracker( adaptationTracker )
                                              .build();

        joker.run( flow );

        while ( !adaptationTracker.isShutdownTriggered() )
        {
            sleepUninterruptibly( 1, SECONDS );
        }

        joker.shutdown().get( 60, SECONDS );

        logFinalMetrics( adaptationTracker );
    }

    private void logFinalMetrics ( final SmartAdaptationTracker adaptationTracker )
    {
        for ( RegionExecPlan regionExecPlan : adaptationTracker.getInitialExecPlan().getRegionExecPlans() )
        {
            if ( regionExecPlan.getRegionDef().isSource() )
            {
                continue;
            }

            final int regionId = regionExecPlan.getRegionId();
            final PipelineMetricsHistory initialMetricsHistory = adaptationTracker.getInitialMetrics()
                                                                                  .getRegionMetrics( regionId )
                                                                                  .get( 0 );
            final PipelineMetricsHistory finalMetricsHistory = adaptationTracker.getFinalMetrics().getRegionMetrics( regionId ).get( 0 );

            for ( int portIndex = 0; portIndex < initialMetricsHistory.getInputPortCount(); portIndex++ )
            {
                final PipelineMetrics initialMetrics = initialMetricsHistory.getLatest();
                final PipelineMetrics finalMetrics = finalMetricsHistory.getLatest();

                final long initialThroughput = initialMetrics.getTotalInboundThroughput( portIndex );
                final long finalThroughput = finalMetrics.getTotalInboundThroughput( portIndex );
                final double ratio = ( (double) finalThroughput ) / initialThroughput;
                System.out.println( "Region: " + regionId + " portIndex: " + portIndex + " initial throughput: " + initialThroughput
                                    + " final throughput: " + finalThroughput + " ratio: " + ratio );
            }
        }
    }

    //
    //                           |                           |                           |
    // beacon (key, val1, val2) ---> partitioner (key, val) ---> multiplier1 (key, val) ---> sink
    //                           |                           |                           |
    //          STATEFUL         |                  PARTITIONED STATEFUL                 |   STATEFUL
    //                           |                           |                           |
    //

    @Test
    public void testPipelineSplit () throws InterruptedException, ExecutionException, TimeoutException
    {
        final FlowDef flow = flowExample.build();

        final Joker joker = newJokerInstance( false );
        final FlowExecPlan flowExecPlan = joker.run( flow );

        sleepUninterruptibly( 40, SECONDS );

        splitPipeline( joker, flowExecPlan );

        System.out.println( "#############################################" );

        sleepUninterruptibly( 40, SECONDS );

        joker.shutdown().get( 60, SECONDS );

        System.out.println( "Total: " + flowExample.getProcessedTupleCount() + " tuples" );
    }

    //
    //                           |                                                       |
    // beacon (key, val1, val2) ---> partitioner (key, val) ---> multiplier1 (key, val) ---> sink
    //                        \  |                                                       |  /
    //                         \---> partitioner (key, val) ---> multiplier1 (key, val) ---/
    //                           |                                                       |
    //          STATEFUL         |                  PARTITIONED STATEFUL                 |   STATEFUL
    //                           |                                                       |
    //

    @Test
    public void testRegionRebalance () throws InterruptedException, ExecutionException, TimeoutException
    {
        checkArgument( MIDDLE_REGION_TYPE == PARTITIONED_STATEFUL );

        final FlowDef flow = flowExample.build();

        final Joker joker = newJokerInstance( false );
        final FlowExecPlan flowExecPlan = joker.run( flow );

        sleepUninterruptibly( 40, SECONDS );

        final RegionExecPlan regionExecPlan = getMiddleRegionExecPlan( flowExecPlan );

        joker.rebalanceRegion( flowExecPlan.getVersion(), regionExecPlan.getRegionId(), 2 );

        System.out.println( "#############################################" );

        sleepUninterruptibly( 40, SECONDS );

        joker.shutdown().get( 60, SECONDS );

        System.out.println( "Total: " + flowExample.getProcessedTupleCount() + " tuples" );
    }

    private Joker newJokerInstance ( final boolean enabled )
    {
        final JokerConfigBuilder configBuilder = new JokerConfigBuilder();
        if ( enabled )
        {
            configBuilder.getAdaptationConfigBuilder().enableAdaptation().enableVisualization();
        }

        configBuilder.getFlowDefOptimizerConfigBuilder().disableMergeRegions();

        final JokerConfig jokerConfig = configBuilder.build();
        return new JokerBuilder().setJokerConfig( jokerConfig ).setRegionExecPlanFactory( new DefaultRegionExecPlanFactory( jokerConfig ) )
                                 .build();
    }

    private RegionExecPlan getMiddleRegionExecPlan ( FlowExecPlan flowExecPlan )
    {
        for ( RegionExecPlan regionExecPlan : flowExecPlan.getRegionExecPlans() )
        {
            if ( regionExecPlan.getRegionDef().getRegionType() == MIDDLE_REGION_TYPE )
            {
                return regionExecPlan;
            }
        }

        throw new IllegalArgumentException();
    }

    private void splitPipeline ( Joker joker, FlowExecPlan flowExecPlan ) throws InterruptedException, ExecutionException, TimeoutException
    {
        RegionExecPlan regionExecPlan = getMiddleRegionExecPlan( flowExecPlan );
        joker.splitPipeline( flowExecPlan.getVersion(), regionExecPlan.getPipelineIds().get( 0 ), singletonList( 1 ) ).get( 60, SECONDS );
    }


    public static class BeaconFn implements Consumer<Tuple>
    {

        private final Random random = new Random();

        private int key = random.nextInt( KEY_RANGE );

        private int val = random.nextInt( 10 );

        private int limit = 8;

        private int count;

        @Override
        public void accept ( final Tuple tuple )
        {
            double val = this.val;
            tuple.set( "key", key ).set( "val1", val ).set( "val2", val );
            if ( count++ > limit )
            {
                key = random.nextInt( KEY_RANGE );
                this.val = random.nextInt( 10 );
                count = 0;
            }
        }

    }


    public static class BasePartitionerOperator implements Operator
    {

        private TupleSchema outputSchema;

        @Override
        public SchedulingStrategy init ( final InitCtx ctx )
        {
            outputSchema = ctx.getOutputPortSchema( 0 );
            return scheduleWhenTuplesAvailableOnDefaultPort( 1 );
        }

        @Override
        public void invoke ( final InvocationCtx ctx )
        {
            for ( Tuple input : ctx.getInputTuplesByDefaultPort() )
            {
                Object pKey = input.get( "key" );
                Tuple summed = Tuple.of( outputSchema, "key", pKey );
                summed.attachTo( input );
                double sum = input.getDouble( "val1" ) + input.getDouble( "val2" );
                for ( int i = 0; i < PARTITIONER_COST; i++ )
                {
                    sum *= input.getDouble( "val1" ) / 4;
                }
                summed.set( "val", sum );
                ctx.output( summed );
            }
        }

    }


    @OperatorSpec( inputPortCount = 1, outputPortCount = 1, type = STATELESS )
    public static class StatelessPartitionerOperator extends BasePartitionerOperator
    {

    }


    @OperatorSpec( inputPortCount = 1, outputPortCount = 1, type = PARTITIONED_STATEFUL )
    public static class PartitionedStatefulPartitionerOperator extends BasePartitionerOperator
    {

    }


    public static class MultiplierFn implements BiConsumer<Tuple, Tuple>
    {

        @Override
        public void accept ( final Tuple input, final Tuple output )
        {
            double val = input.getDouble( "val" );
            for ( int i = 0; i < MULTIPLIER_COST; i++ )
            {
                val *= ( val / 2 );
            }

            output.set( "key", input.get( "key" ) ).set( "val", val );
        }

    }


    public static class TupleCounterFn implements Consumer<Tuple>
    {

        private final AtomicInteger lazyCounter = new AtomicInteger();

        @Override
        public void accept ( final Tuple tuple )
        {
            lazyCounter.lazySet( lazyCounter.get() + 1 );
        }

        int getCount ()
        {
            return lazyCounter.get();
        }

    }


    static class FlowExample
    {

        private final TupleCounterFn tupleCounterFn = new TupleCounterFn();

        int getProcessedTupleCount ()
        {
            return tupleCounterFn.getCount();
        }

        private FlowDef build ()
        {
            OperatorConfig beaconConfig = new OperatorConfig().set( TUPLE_COUNT_CONFIG_PARAMETER, 4096 * 4 )
                                                              .set( TUPLE_POPULATOR_CONFIG_PARAMETER, new BeaconFn() );

            OperatorRuntimeSchemaBuilder beaconSchemaBuilder = new OperatorRuntimeSchemaBuilder( 0, 1 );
            beaconSchemaBuilder.addOutputField( 0, "key", Integer.class )
                               .addOutputField( 0, "val1", Double.class )
                               .addOutputField( 0, "val2", Double.class );

            OperatorDef beacon = OperatorDefBuilder.newInstance( "bc", BeaconOperator.class )
                                                   .setConfig( beaconConfig )
                                                   .setExtendingSchema( beaconSchemaBuilder )
                                                   .build();

            OperatorRuntimeSchemaBuilder partitionerSchemaBuilder = new OperatorRuntimeSchemaBuilder( 1, 1 );
            partitionerSchemaBuilder.addInputField( 0, "key", Integer.class )
                                    .addInputField( 0, "val1", Double.class )
                                    .addInputField( 0, "val2", Double.class )
                                    .addOutputField( 0, "key", Integer.class )
                                    .addOutputField( 0, "val", Double.class );

            boolean statelessMiddleRegion = MIDDLE_REGION_TYPE == STATELESS;
            Class<? extends BasePartitionerOperator> partitionerClazz = statelessMiddleRegion
                                                                        ? StatelessPartitionerOperator.class
                                                                        : PartitionedStatefulPartitionerOperator.class;
            OperatorDef partitioner = OperatorDefBuilder.newInstance( "pt", partitionerClazz )
                                                        .setExtendingSchema( partitionerSchemaBuilder )
                                                        .setPartitionFieldNames( statelessMiddleRegion
                                                                                 ? emptyList()
                                                                                 : singletonList( "key" ) )
                                                        .build();

            OperatorConfig multiplierConfig = new OperatorConfig().set( MAPPER_CONFIG_PARAMETER, new MultiplierFn() );

            OperatorRuntimeSchemaBuilder multiplierSchemaBuilder = new OperatorRuntimeSchemaBuilder( 1, 1 );
            OperatorRuntimeSchema multiplierSchema = multiplierSchemaBuilder.addInputField( 0, "key", Integer.class )
                                                                            .addInputField( 0, "val", Double.class )
                                                                            .addOutputField( 0, "key", Integer.class )
                                                                            .addOutputField( 0, "val", Double.class )
                                                                            .build();

            OperatorDef multiplier1 = OperatorDefBuilder.newInstance( "m1", MapperOperator2.class )
                                                        .setConfig( multiplierConfig )
                                                        .setExtendingSchema( multiplierSchema )
                                                        .build();

            OperatorDef multiplier2 = OperatorDefBuilder.newInstance( "m2", MapperOperator2.class )
                                                        .setConfig( multiplierConfig )
                                                        .setExtendingSchema( multiplierSchema )
                                                        .build();

            OperatorDef multiplier3 = OperatorDefBuilder.newInstance( "m3", MapperOperator2.class )
                                                        .setConfig( multiplierConfig )
                                                        .setExtendingSchema( multiplierSchema )
                                                        .build();

            OperatorConfig tupleCounterConfig = new OperatorConfig().set( CONSUMER_FUNCTION_CONFIG_PARAMETER, tupleCounterFn );
            OperatorDef tupleCounter = OperatorDefBuilder.newInstance( "tc", ForEachOperator.class )
                                                         .setConfig( tupleCounterConfig )
                                                         .build();

            return new FlowDefBuilder().add( beacon )
                                       .add( partitioner )
                                       .add( multiplier1 )
                                       .add( multiplier2 )
                                       .add( multiplier3 )
                                       .add( tupleCounter )
                                       .connect( "bc", "pt" )
                                       .connect( "pt", "m1" )
                                       .connect( "m1", "m2" )
                                       .connect( "m2", "m3" )
                                       .connect( "m3", "tc" )
                                       .build();
        }

    }


    @OperatorSpec( type = STATELESS, inputPortCount = 1, outputPortCount = 1 )
    public static class MapperOperator2 implements Operator
    {

        public static final String MAPPER_CONFIG_PARAMETER = "mapper";

        private static final int DEFAULT_TUPLE_COUNT_CONFIG_VALUE = 1;


        private BiConsumer<Tuple, Tuple> mapper;

        private TupleSchema outputSchema;

        private Histogram histogram = new Histogram( new ExponentiallyDecayingReservoir() );

        @Override
        public SchedulingStrategy init ( final InitCtx ctx )
        {
            final OperatorConfig config = ctx.getConfig();

            this.mapper = config.getOrFail( MAPPER_CONFIG_PARAMETER );
            this.outputSchema = ctx.getOutputPortSchema( 0 );
            return scheduleWhenTuplesAvailableOnDefaultPort( DEFAULT_TUPLE_COUNT_CONFIG_VALUE );
        }

        @Override
        public void invoke ( final InvocationCtx ctx )
        {
            final List<Tuple> tuples = ctx.getInputTuplesByDefaultPort();
            for ( Tuple input : tuples )
            {
                final Tuple mapped = new Tuple( outputSchema );
                mapped.attachTo( input );
                mapper.accept( input, mapped );
                ctx.output( mapped );
            }

            histogram.update( tuples.size() );
        }

        @Override
        public void shutdown ()
        {
            Snapshot snapshot = histogram.getSnapshot();
            System.out.printf( "STATS -> min: %d max: %d mean: %s std dev: %f median: %f .75: %f .95: %f .99: %f .999: %f",
                               snapshot.getMin(),
                               snapshot.getMax(),
                               snapshot.getMean(),
                               snapshot.getStdDev(),
                               snapshot.getMedian(),
                               snapshot.get75thPercentile(),
                               snapshot.get95thPercentile(),
                               snapshot.get99thPercentile(),
                               snapshot.get999thPercentile() );
        }
    }

}
