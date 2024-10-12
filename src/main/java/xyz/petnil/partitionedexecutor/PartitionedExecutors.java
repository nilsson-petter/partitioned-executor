package xyz.petnil.partitionedexecutor;

public class PartitionedExecutors {
    private PartitionedExecutors() {
    }

    public static PartitionedExecutor unbounded(int maxPartitions) {
        return unbounded(maxPartitions, "PartitionedExecutor");
    }

    public static PartitionedExecutor unbounded(int maxPartitions, String name) {
        return unbounded(maxPartitions, name, Partitioners.generalPurpose(maxPartitions));
    }

    public static PartitionedExecutor unbounded(int maxPartitions, String name, Partitioner partitioner) {
        return PartitionedExecutorBuilder
                .newBuilder(maxPartitions)
                .withPartitioner(partitioner)
                .configurePartitionCreator()
                .withPartitionQueue(PartitionQueues.unbounded())
                .withThreadFactory(PartitionThreadFactoryCreators.virtualThread(name))
                .buildPartitionCreator()
                .build();
    }

}
