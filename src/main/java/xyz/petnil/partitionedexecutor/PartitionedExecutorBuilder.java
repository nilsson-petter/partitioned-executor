package xyz.petnil.partitionedexecutor;

public class PartitionedExecutorBuilder {
    private final int maxPartitions;
    private PartitioningFunction partitioningFunction;
    private PartitionCreator partitionCreator;

    private PartitionedExecutorBuilder(int maxPartitions) {
        this.maxPartitions = maxPartitions;
        partitioningFunction = PartitioningFunctions.generalPurpose(maxPartitions);
        this.partitionCreator = new PartitionCreatorBuilder(this).createPartitionCreator();
    }

    public static PartitionedExecutorBuilder newBuilder(int maxPartitions) {
        return new PartitionedExecutorBuilder(maxPartitions);
    }

    public PartitionedExecutorBuilder withPartitioningFunction(PartitioningFunction partitioningFunction) {
        this.partitioningFunction =  partitioningFunction;
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
        if (maxPartitions != partitioningFunction.getMaxNumberOfPartitions()) {
            throw new IllegalStateException("maxPartitions and partitioningFunction.getMaxNumberOfPartitions does not align");
        }
        return new LazyPartitionedExecutor(partitioningFunction, partitionCreator);
    }

    public static class PartitionCreatorBuilder {
        private PartitionQueue partitionQueue = PartitionQueues.unbounded();
        private PartitionThreadFactoryCreator threadFactory;
        private final PartitionedExecutorBuilder parentBuilder;
        private String threadNamePrefix = "SingleThreadedPartitionWorker";


        private PartitionCreatorBuilder(PartitionedExecutorBuilder parentBuilder) {
            this.parentBuilder = parentBuilder;
        }

        public PartitionCreatorBuilder withPartitionQueue(PartitionQueue partitionQueue) {
            this.partitionQueue = partitionQueue;
            return this;
        }

        public PartitionCreatorBuilder withThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
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
            return i -> new SingleThreadedPartitionWorker(i, partitionQueue, threadFactory.createThreadFactory(i));
        }
    }
}
