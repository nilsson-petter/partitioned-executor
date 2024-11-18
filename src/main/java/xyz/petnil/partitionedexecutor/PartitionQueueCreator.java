package xyz.petnil.partitionedexecutor;

public interface PartitionQueueCreator<T extends PartitionedTask> {
    PartitionQueue<T> create();
}
