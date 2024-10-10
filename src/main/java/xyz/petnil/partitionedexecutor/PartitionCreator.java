package xyz.petnil.partitionedexecutor;

public interface PartitionCreator {
    Partition create(int partitionNumber);
}
