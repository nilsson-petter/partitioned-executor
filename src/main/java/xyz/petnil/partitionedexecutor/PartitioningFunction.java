package xyz.petnil.partitionedexecutor;

/**
 * The PartitioningFunction interface defines a method to compute
 * the partition index for a given partition key. This is typically
 * used in systems that distribute data or tasks across multiple partitions.
 */
public interface PartitioningFunction {
    /**
     * Returns the partition index for the given partition key.
     * The index must be a valid partition number between 0 (inclusive)
     * and maxPartitions (exclusive).
     * <p>
     * Constraints:
     * <ul>
     *   <li>partitionKey must not be null</li>
     *   <li>maxPartitions must be greater than 0</li>
     * </ul>
     *
     * @param partitionKey  the key used to determine the partition index, must not be null
     * @param maxPartitions the total number of available partitions, must be > 0
     * @return the partition index for the given key, in the range
     * 0 (inclusive) to maxPartitions (exclusive)
     * @throws NullPointerException     if partitionKey is null
     * @throws IllegalArgumentException if maxPartitions is < 1
     */
    int getPartition(Object partitionKey, int maxPartitions);
}


