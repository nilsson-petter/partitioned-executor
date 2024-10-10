package xyz.petnil.partitionedexecutor;

public class PartitionedExecutors {
    private PartitionedExecutors() {
    }

    public static PartitionedExecutor unbounded(int maxPartitions) {
        return unbounded(maxPartitions, "PartitionedExecutor");
    }

    public static PartitionedExecutor unbounded(int maxPartitions, String name) {
        return unbounded(maxPartitions, name, PartitioningFunctions.generalPurpose());
    }

    public static PartitionedExecutor unbounded(int maxPartitions, String name, PartitioningFunction partitioningFunction) {
        return PartitionedExecutorBuilder
                .newBuilder(maxPartitions)
                .withPartitioningFunction(partitioningFunction)
                .configurePartitionCreator()
                .withPartitionQueue(PartitionQueues.unbounded())
                .withThreadFactory(PartitionThreadFactoryCreators.virtualThread(name))
                .buildPartitionCreator()
                .build();
    }

}
