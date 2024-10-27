package xyz.petnil.partitionedexecutor;

public class PartitionedExecutors {
    private PartitionedExecutors() {
    }

    public static PartitionedExecutor fifo(int maxPartitions, int maxQueueSize) {
        return PartitionedExecutorBuilder.newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(() -> PartitionQueues.fifo(maxQueueSize))
                .buildPartitionCreator()
                .build();
    }

    public static PartitionedExecutor sampled(int maxPartitions, SamplingFunction samplingFunction) {
        return PartitionedExecutorBuilder.newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(() -> PartitionQueues.sampled(samplingFunction))
                .buildPartitionCreator()
                .build();
    }

    private static PartitionerCreator getPartitioner(int maxPartitions) {
        return PowerOfTwo.isPowerOfTwo(maxPartitions) ? Partitioners::powerOfTwo : Partitioners::generalPurpose;
    }

}
