package xyz.petnil.partitionedexecutor;

public class PartitionedExecutors {
    private PartitionedExecutors() {
    }

    public static <T extends PartitionedTask> PartitionedExecutor<T> fifo(int maxPartitions, int maxQueueSize) {
        return PartitionedExecutorBuilder.<T>newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(() -> PartitionQueues.fifo(maxQueueSize))
                .buildPartitionCreator()
                .build();
    }

    public static <T extends PartitionedTask> PartitionedExecutor<T> trailingThrottled(int maxPartitions, ThrottlingFunction throttlingFunction) {
        return PartitionedExecutorBuilder.<T>newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(() -> PartitionQueues.trailingThrottled(throttlingFunction))
                .buildPartitionCreator()
                .build();
    }

    private static PartitionerCreator getPartitioner(int maxPartitions) {
        return PowerOfTwo.isPowerOfTwo(maxPartitions) ? Partitioners::powerOfTwo : Partitioners::generalPurpose;
    }

}
