package xyz.petnil.partitionedexecutor;

/**
 * The PartitioningFunction interface defines a method to compute
 * the partition index for a given partition key.
 */
public interface PartitioningFunction {
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
}


