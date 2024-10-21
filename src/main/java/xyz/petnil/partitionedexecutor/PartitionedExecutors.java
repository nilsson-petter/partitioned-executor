package xyz.petnil.partitionedexecutor;

public class PartitionedExecutors {
    private PartitionedExecutors() {
    }

    public static PartitionedExecutor unboundedFifo(int maxPartitions) {
        return PartitionedExecutorBuilder.newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(PartitionQueues::unboundedFifo)
                .buildPartitionCreator()
                .build();
    }

    public static PartitionedExecutor boundedFifo(int maxPartitions, int maxQueueSize) {
        return PartitionedExecutorBuilder.newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(() -> PartitionQueues.boundedFifo(maxQueueSize))
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
