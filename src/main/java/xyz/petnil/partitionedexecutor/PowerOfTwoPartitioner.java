package xyz.petnil.partitionedexecutor;

import java.util.Objects;

class PowerOfTwoPartitioner implements Partitioner {

    private final int maxPartitions;

    public PowerOfTwoPartitioner(int maxPartitions) {
        if (maxPartitions < 1) {
            throw new IllegalArgumentException("maxPartitions must be greater than 0");
        }

        // Check if maxPartitions is a power of two
        if (!PowerOfTwo.isPowerOfTwo(maxPartitions)) {
            throw new IllegalArgumentException("maxPartitions must be a power of two");
        }
        this.maxPartitions = maxPartitions;
    }

    @Override
    public int getPartition(Object partitionKey) {
        Objects.requireNonNull(partitionKey);
        return partitionKey.hashCode() & (maxPartitions - 1);
    }

    @Override
    public int getMaxNumberOfPartitions() {
        return maxPartitions;
    }
}
