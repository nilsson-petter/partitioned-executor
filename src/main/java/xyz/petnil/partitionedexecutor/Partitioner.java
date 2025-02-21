package xyz.petnil.partitionedexecutor;

/**
 * The Partitioner interface defines a method to compute
 * the partition index for a given partition key.
 */
public interface Partitioner {
    /**
     * Returns the partition index for the given partition key.
     * <p>
     * Constraints:
     * <ul>
     *   <li>partitionKey must not be null</li>
     * </ul>
     *
     * @param partitionKey  the key used to determine the partition index, must not be null
     * @return the partition index for the given key
     * @throws NullPointerException     if partitionKey is null
     */
    int getPartition(Object partitionKey);

    int getMaxNumberOfPartitions();

    static Partitioner mostSuitableFor(int maxPartitions) {
        return PowerOfTwo.isPowerOfTwo(maxPartitions) ?
                Partitioner.powerOfTwo(maxPartitions) :
                Partitioner.generalPurpose(maxPartitions);
    }

    /**
     * Returns a {@link Partitioner} optimized for scenarios where the number of partitions
     * is a power of two (e.g., 2, 4, 8, 16, etc.). This partitioner uses bitwise operations
     * for faster computation compared to the general-purpose modulo-based strategy.
     *
     * @return a {@link Partitioner} for power-of-two partitioning
     * @see Partitioner
     */
    static Partitioner powerOfTwo(int maxPartitions) {
        return new PowerOfTwoPartitioner(maxPartitions);
    }

    /**
     * Returns a general-purpose {@link Partitioner} that can be used for any partition count.
     * This implementation uses a hash-based strategy with modulo operation to determine the partition index.
     * It works well for both small and large numbers of partitions.
     *
     * @return a general-purpose {@link Partitioner} for partitioning
     * @see Partitioner
     */
    static Partitioner generalPurpose(int maxPartitions) {
        return new GeneralPurposePartitioner(maxPartitions);
    }
}


