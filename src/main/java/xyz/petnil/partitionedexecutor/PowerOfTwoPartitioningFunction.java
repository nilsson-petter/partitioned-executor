package xyz.petnil.partitionedexecutor;

public class PowerOfTwoPartitioningFunction implements PartitioningFunction {
    @Override
    public int getPartition(Object partitionKey, int maxPartitions) {
        if (partitionKey == null) {
            throw new NullPointerException("partitionKey must not be null");
        }
        if (maxPartitions < 1) {
            throw new IllegalArgumentException("maxPartitions must be greater than 0");
        }

        // Check if maxPartitions is a power of two
        if ((maxPartitions & (maxPartitions - 1)) != 0) {
            throw new IllegalArgumentException("maxPartitions must be a power of two");
        }

        return partitionKey.hashCode() & (maxPartitions - 1);
    }
}
