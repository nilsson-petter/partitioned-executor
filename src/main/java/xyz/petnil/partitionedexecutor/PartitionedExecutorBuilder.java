package xyz.petnil.partitionedexecutor;

public class PartitionedExecutorBuilder {
    private final int maxPartitions;
    private PartitionerCreator partitioner;
    private PartitionCreator partitionCreator;

    private PartitionedExecutorBuilder(int maxPartitions) {
        this.maxPartitions = maxPartitions;
        this.partitioner = Partitioners::generalPurpose;
        this.partitionCreator = new PartitionCreatorBuilder(this).createPartitionCreator();
    }

    public static PartitionedExecutorBuilder newBuilder(int maxPartitions) {
        return new PartitionedExecutorBuilder(maxPartitions);
    }

    public PartitionedExecutorBuilder withPartitioner(PartitionerCreator partitioner) {
        this.partitioner = partitioner;
        return this;
    }

    public PartitionedExecutorBuilder withPartitionCreator(PartitionCreator partitionCreator) {
        this.partitionCreator = partitionCreator;
        return this;
    }

    public PartitionCreatorBuilder configurePartitionCreator() {
        return new PartitionCreatorBuilder(this);
    }

    public PartitionedExecutor build() {
        return new LazyLoadingPartitionedExecutor(partitioner.createPartitioner(maxPartitions), partitionCreator);
    }

    public static class PartitionCreatorBuilder {
        private final PartitionedExecutorBuilder parentBuilder;
        private PartitionThreadFactoryCreator threadFactory;
        private String threadNamePrefix = "SingleThreadedPartitionWorker";
        private PartitionQueueCreator partitionQueueCreator = PartitionQueues::unbounded;
        private Partition.Callback callback;


        private PartitionCreatorBuilder(PartitionedExecutorBuilder parentBuilder) {
            this.parentBuilder = parentBuilder;
        }

        public PartitionCreatorBuilder withPartitionQueueCreator(PartitionQueueCreator partitionQueueCreator) {
            this.partitionQueueCreator = partitionQueueCreator;
            return this;
        }

        public PartitionCreatorBuilder withThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public PartitionCreatorBuilder withCallback(Partition.Callback callback) {
            this.callback = callback;
            return this;
        }

        public PartitionCreatorBuilder withThreadFactory(PartitionThreadFactoryCreator threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        // Finalize PartitionCreator and return control to the main builder
        public PartitionedExecutorBuilder buildPartitionCreator() {
            parentBuilder.partitionCreator = createPartitionCreator();
            return parentBuilder;
        }

        private PartitionCreator createPartitionCreator() {
            if (threadFactory == null) {
                threadFactory = PartitionThreadFactoryCreators.virtualThread(threadNamePrefix);
            }

            return i -> new SingleThreadedPartitionWorker(i, partitionQueueCreator.create(), threadFactory.createThreadFactory(i), callback);
        }
    }
}
