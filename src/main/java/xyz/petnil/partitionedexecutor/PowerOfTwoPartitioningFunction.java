package xyz.petnil.partitionedexecutor;

public class PowerOfTwoPartitioningFunction implements PartitioningFunction {

    private final int maxPartitions;

    public PowerOfTwoPartitioningFunction(int maxPartitions) {
        if (maxPartitions < 1) {
            throw new IllegalArgumentException("maxPartitions must be greater than 0");
        }

        // Check if maxPartitions is a power of two
        if ((maxPartitions & (maxPartitions - 1)) != 0) {
            throw new IllegalArgumentException("maxPartitions must be a power of two");
        }
        this.maxPartitions = maxPartitions;
    }

    @Override
    public int getPartition(Object partitionKey) {
        if (partitionKey == null) {
            throw new NullPointerException("partitionKey must not be null");
        }

        return partitionKey.hashCode() & (maxPartitions - 1);
    }

    @Override
    public int getMaxNumberOfPartitions() {
        return maxPartitions;
    }
}
