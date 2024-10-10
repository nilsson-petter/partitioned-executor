package xyz.petnil.partitionedexecutor;

public class GeneralPurposePartitioningFunction implements PartitioningFunction {
    @Override
    public int getPartition(Object partitionKey, int maxPartitions) {
        if (partitionKey == null) {
            throw new NullPointerException("partitionKey must not be null");
        }
        if (maxPartitions < 1) {
            throw new IllegalArgumentException("maxPartitions must be greater than 0");
        }

        int hash = partitionKey.hashCode();
        if (hash == Integer.MIN_VALUE) {
            hash = 0;  // Handle the special case for Integer.MIN_VALUE
        }

        return Math.abs(hash) % maxPartitions;
    }
}

