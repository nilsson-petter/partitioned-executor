package xyz.petnil.partitionedexecutor;

public class PartitionQueues {
    private PartitionQueues() {
    }

    public static PartitionQueue unbounded() {
        return new UnboundedPartitionQueue();
    }

    public static PartitionQueue bounded(int capacity) {
        return new BoundedPartitionQueue(capacity);
    }

    public static PartitionQueue sampled(SamplingFunction samplingFunction) {
        return new SampledPartitionQueue(samplingFunction);
    }
}
