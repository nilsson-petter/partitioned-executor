package xyz.petnil.partitionedexecutor;

@FunctionalInterface
public interface PartitionerCreator {
    Partitioner createPartitioner(int maxPartitions);
}
