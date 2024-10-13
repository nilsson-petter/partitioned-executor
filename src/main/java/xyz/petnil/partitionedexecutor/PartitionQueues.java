package xyz.petnil.partitionedexecutor;

import java.util.Comparator;

public class PartitionQueues {
    private PartitionQueues() {
    }

    public static PartitionQueue unboundedFifo() {
        return new UnboundedFifoPartitionQueue();
    }

    public static PartitionQueue boundedFifo(int capacity) {
        return new BoundedFifoPartitionQueue(capacity);
    }

    public static PartitionQueue sampled(SamplingFunction samplingFunction) {
        return new SampledPartitionQueue(samplingFunction);
    }

    public static PartitionQueue priority(Comparator<PartitionedRunnable> comparator) {
        return new PriorityPartitionQueue(comparator);
    }
}
