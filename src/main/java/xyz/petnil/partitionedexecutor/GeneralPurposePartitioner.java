package xyz.petnil.partitionedexecutor;

import java.util.Objects;

/**
 * A general purpose partitioner that partitions tasks based on their hashCode.
 * <p>
 * This partitioner is useful when the partition key is an arbitrary object, and no specific
 * partitioning is required.
 * <p>
 * This partitioner is thread safe.
 * <p>
 */
class GeneralPurposePartitioner implements Partitioner {

    private final int maxPartitions;

    public GeneralPurposePartitioner(int maxPartitions) {
        if (maxPartitions < 1) {
            throw new IllegalArgumentException("maxPartitions must be greater than 0");
        }

        this.maxPartitions = maxPartitions;
    }

    @Override
    public int getPartition(Object partitionKey) {
        Objects.requireNonNull(partitionKey);

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

