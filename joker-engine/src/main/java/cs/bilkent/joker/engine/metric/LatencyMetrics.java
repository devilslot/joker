package cs.bilkent.joker.engine.metric;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class LatencyMetrics
{

    private final String sinkOperatorId;

    private final int replicaIndex;

    private final int flowVersion;

    private final LatencyRecord tupleLatency;

    private final Map<String, LatencyRecord> invocationLatencies;

    private final Map<String, LatencyRecord> queueLatencies;

    public LatencyMetrics ( final String sinkOperatorId,
                            final int replicaIndex,
                            final int flowVersion,
                            final LatencyRecord tupleLatency,
                            final Map<String, LatencyRecord> invocationLatencies,
                            final Map<String, LatencyRecord> queueLatencies )
    {
        this.sinkOperatorId = sinkOperatorId;
        this.replicaIndex = replicaIndex;
        this.flowVersion = flowVersion;
        this.invocationLatencies = unmodifiableMap( new HashMap<>( invocationLatencies ) );
        this.queueLatencies = unmodifiableMap( new HashMap<>( queueLatencies ) );
        this.tupleLatency = tupleLatency;
    }

    public String getSinkOperatorId ()
    {
        return sinkOperatorId;
    }

    public int getReplicaIndex ()
    {
        return replicaIndex;
    }

    public int getFlowVersion ()
    {
        return flowVersion;
    }

    public LatencyRecord getTupleLatency ()
    {
        return tupleLatency;
    }

    public Map<String, LatencyRecord> getInvocationLatencies ()
    {
        return invocationLatencies;
    }

    public Map<String, LatencyRecord> getQueueLatencies ()
    {
        return queueLatencies;
    }

    public LatencyRecord getInvocationLatency ( final String operatorId )
    {
        return invocationLatencies.get( operatorId );
    }

    public LatencyRecord getQueueLatency ( final String operatorId )
    {
        return queueLatencies.get( operatorId );
    }

    @Override
    public String toString ()
    {
        return "LatencyMetrics{" + "sinkOperatorId='" + sinkOperatorId + '\'' + ", replicaIndex=" + replicaIndex + ", flowVersion="
               + flowVersion + ", tupleLatency=" + tupleLatency + ", invocationLatencies=" + invocationLatencies + ", queueLatencies="
               + queueLatencies + '}';
    }

    public static class LatencyRecord
    {

        private final long mean;

        private final long stdDev;

        private final long median;

        private final long min;

        private final long max;

        private final long percentile75;

        private final long percentile95;

        private final long percentile98;

        private final long percentile99;


        public LatencyRecord ( final long mean,
                               final long stdDev,
                               final long median,
                               final long min,
                               final long max,
                               final long percentile75,
                               final long percentile95,
                               final long percentile98, final long percentile99 )
        {
            this.mean = mean;
            this.stdDev = stdDev;
            this.median = median;
            this.min = min;
            this.max = max;
            this.percentile75 = percentile75;
            this.percentile95 = percentile95;
            this.percentile98 = percentile98;
            this.percentile99 = percentile99;
        }

        public long getMean ()
        {
            return mean;
        }

        public long getStdDev ()
        {
            return stdDev;
        }

        public long getMedian ()
        {
            return median;
        }

        public long getMin ()
        {
            return min;
        }

        public long getMax ()
        {
            return max;
        }

        public long getPercentile75 ()
        {
            return percentile75;
        }

        public long getPercentile95 ()
        {
            return percentile95;
        }

        public long getPercentile98 ()
        {
            return percentile98;
        }

        public long getPercentile99 ()
        {
            return percentile99;
        }

        @Override
        public String toString ()
        {
            return "LatencyRecord{" + "mean=" + mean + ", stdDev=" + stdDev + ", median=" + median + ", min=" + min + ", max=" + max
                   + ", percentile75=" + percentile75 + ", percentile95=" + percentile95 + ", percentile98=" + percentile98
                   + ", percentile99=" + percentile99 + '}';
        }
    }

}
