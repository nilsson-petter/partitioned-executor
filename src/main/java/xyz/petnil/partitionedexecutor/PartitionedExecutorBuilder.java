package xyz.petnil.partitionedexecutor;

public class PartitionedExecutorBuilder {
    private final int maxPartitions;
    private PartitioningFunction partitioningFunction = PartitioningFunctions.powerOfTwo(); // Default
    private PartitionCreator partitionCreator;

    private PartitionedExecutorBuilder(int maxPartitions) {
        this.maxPartitions = maxPartitions;
    }

    public static PartitionedExecutorBuilder newBuilder(int maxPartitions) {
        return new PartitionedExecutorBuilder(maxPartitions);
    }

    public PartitionedExecutorBuilder withPartitioningFunction(PartitioningFunction partitioningFunction) {
        this.partitioningFunction = partitioningFunction;
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
        if (partitionCreator == null) {
            throw new IllegalStateException("PartitionCreator must be configured.");
        }
        return new LazyPartitionedExecutor(maxPartitions, partitioningFunction, partitionCreator);
    }

    public static class PartitionCreatorBuilder {
        private PartitionQueue partitionQueue = PartitionQueues.unbounded();
        private PartitionThreadFactoryCreator threadFactory = PartitionThreadFactoryCreators.virtualThread("SingleThreadedPartitionWorker");
        private final PartitionedExecutorBuilder parentBuilder;

        private PartitionCreatorBuilder(PartitionedExecutorBuilder parentBuilder) {
            this.parentBuilder = parentBuilder;
        }

        public PartitionCreatorBuilder withPartitionQueue(PartitionQueue partitionQueue) {
            this.partitionQueue = partitionQueue;
            return this;
        }

        public PartitionCreatorBuilder withThreadFactory(PartitionThreadFactoryCreator threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        // Finalize PartitionCreator and return control to the main builder
        public PartitionedExecutorBuilder buildPartitionCreator() {
            parentBuilder.partitionCreator = i -> new SingleThreadedPartitionWorker(i, partitionQueue, threadFactory.createThreadFactory(i));
            return parentBuilder;
        }
    }
}
