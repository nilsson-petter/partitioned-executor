package xyz.petnil.partitionedexecutor;

public class GeneralPurposePartitioningFunction implements PartitioningFunction {

    private final int maxPartitions;

    public GeneralPurposePartitioningFunction(int maxPartitions) {
        if (maxPartitions < 1) {
            throw new IllegalArgumentException("maxPartitions must be greater than 0");
        }

        this.maxPartitions = maxPartitions;
    }

    @Override
    public int getPartition(Object partitionKey) {
        if (partitionKey == null) {
            throw new NullPointerException("partitionKey must not be null");
        }

        int hash = partitionKey.hashCode();
        if (hash == Integer.MIN_VALUE) {
            hash = 0;  // Handle the special case for Integer.MIN_VALUE
        }

        return Math.abs(hash) % maxPartitions;
    }

    @Override
    public int getMaxNumberOfPartitions() {
        return maxPartitions;
    }
}

