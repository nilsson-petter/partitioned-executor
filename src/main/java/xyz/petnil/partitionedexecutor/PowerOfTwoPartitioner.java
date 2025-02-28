package xyz.petnil.partitionedexecutor;

import java.util.Objects;

/**
 * A {@link Partitioner} implementation that assigns partitions based on the power of two constraint.
 * This partitioner ensures that the number of partitions is always a power of two, which allows for
 * efficient bitwise operations when computing the partition index.
 */
class PowerOfTwoPartitioner implements Partitioner {

    private final int maxPartitions;

    /**
     * Constructs a {@code PowerOfTwoPartitioner} with the specified maximum number of partitions.
     *
     * @param maxPartitions the maximum number of partitions, must be a power of two and greater than 0
     * @throws IllegalArgumentException if {@code maxPartitions} is not a power of two or is less than 1
     */
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

    /**
     * Determines the partition for a given partition key using bitwise operations.
     *
     * @param partitionKey the key to determine the partition, must not be null
     * @return the computed partition index within the range [0, maxPartitions - 1]
     * @throws NullPointerException if {@code partitionKey} is null
     */
    @Override
    public int getPartition(Object partitionKey) {
        Objects.requireNonNull(partitionKey);
        return partitionKey.hashCode() & (maxPartitions - 1);
    }

    /**
     * Returns the maximum number of partitions that this partitioner supports.
     *
     * @return the maximum number of partitions
     */
    @Override
    public int getMaxNumberOfPartitions() {
        return maxPartitions;
    }
}

