package xyz.petnil.partitionedexecutor;

import java.time.Duration;

public class PartitionedExecutors {
    private PartitionedExecutors() {
    }

    public static PartitionedExecutor unbounded(int maxPartitions) {
        return PartitionedExecutorBuilder.newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(PartitionQueues::unboundedFifo)
                .buildPartitionCreator()
                .build();
    }

    public static PartitionedExecutor bounded(int maxPartitions, int maxQueueSize) {
        return PartitionedExecutorBuilder.newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(() -> PartitionQueues.boundedFifo(maxQueueSize))
                .buildPartitionCreator()
                .build();
    }

    public static PartitionedExecutor sampled(int maxPartitions, Duration sampleTime) {
        return PartitionedExecutorBuilder.newBuilder(maxPartitions)
                .withPartitioner(getPartitioner(maxPartitions))
                .configurePartitionCreator()
                .withPartitionQueueCreator(() -> PartitionQueues.sampled(o -> sampleTime))
                .buildPartitionCreator()
                .build();
    }

    private static PartitionerCreator getPartitioner(int maxPartitions) {
        return PowerOfTwo.isPowerOfTwo(maxPartitions) ? Partitioners::powerOfTwo : Partitioners::generalPurpose;
    }


}
