package xyz.petnil.partitionedexecutor;

public class PartitionedExecutorBuilder<T extends PartitionedTask> {
    private final int maxPartitions;
    private PartitionerCreator partitioner;
    private PartitionCreator<T> partitionCreator;

    private PartitionedExecutorBuilder(int maxPartitions) {
        this.maxPartitions = maxPartitions;
        this.partitioner = Partitioners::generalPurpose;
    }

    public static <T extends PartitionedTask> PartitionedExecutorBuilder<T> newBuilder(int maxPartitions) {
        return new PartitionedExecutorBuilder<>(maxPartitions);
    }

    public PartitionedExecutorBuilder<T> withPartitioner(PartitionerCreator partitioner) {
        this.partitioner = partitioner;
        return this;
    }

    public PartitionedExecutorBuilder<T> withPartitionCreator(PartitionCreator<T> partitionCreator) {
        this.partitionCreator = partitionCreator;
        return this;
    }

    public PartitionCreatorBuilder<T> configurePartitionCreator() {
        return new PartitionCreatorBuilder<>(this);
    }

    public PartitionedExecutor<T> build() {
        if (partitionCreator == null) {
            partitionCreator = new PartitionCreatorBuilder<>(this).createPartitionCreator();
        }
        return new LazyLoadingPartitionedExecutor<>(partitioner.createPartitioner(maxPartitions), partitionCreator);
    }

    public static class PartitionCreatorBuilder<T extends PartitionedTask> {
        private final PartitionedExecutorBuilder<T> parentBuilder;
        private PartitionThreadFactoryCreator threadFactory;
        private String threadNamePrefix = "SingleThreadedPartitionWorker";
        private PartitionQueueCreator<T> partitionQueueCreator = () -> PartitionQueues.fifo(Integer.MAX_VALUE);

        private PartitionCreatorBuilder(PartitionedExecutorBuilder<T> parentBuilder) {
            this.parentBuilder = parentBuilder;
        }

        public PartitionCreatorBuilder<T> withPartitionQueueCreator(PartitionQueueCreator<T> partitionQueueCreator) {
            this.partitionQueueCreator = partitionQueueCreator;
            return this;
        }

        public PartitionCreatorBuilder<T> withThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public PartitionCreatorBuilder<T> withThreadFactory(PartitionThreadFactoryCreator threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public PartitionedExecutorBuilder<T> buildPartitionCreator() {
            parentBuilder.partitionCreator = createPartitionCreator();
            return parentBuilder;
        }

        private PartitionCreator<T> createPartitionCreator() {
            if (threadFactory == null) {
                threadFactory = PartitionThreadFactoryCreators.virtualThread(threadNamePrefix);
            }
            return i -> new SingleThreadedPartitionWorker<>(partitionQueueCreator.create(), threadFactory.createThreadFactory(i));
        }
    }
}
