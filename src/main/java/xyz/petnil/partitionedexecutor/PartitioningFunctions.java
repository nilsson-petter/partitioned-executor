package xyz.petnil.partitionedexecutor;

/**
 * The {@code PartitioningFunctions} class provides factory methods to create instances
 * of commonly used {@link PartitioningFunction} implementations. These partitioning functions
 * can be used to distribute tasks or data across multiple partitions.
 * <p>
 * This class contains static utility methods and is not meant to be instantiated.
 * The provided methods offer different strategies for partitioning:
 * <ul>
 *   <li>{@code powerOfTwo()} - Optimized for partition counts that are powers of two.</li>
 *   <li>{@code generalPurpose()} - A general-purpose partitioning function suitable for any partition count.</li>
 * </ul>
 * </p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 *     PartitioningFunction function = PartitioningFunctions.powerOfTwo(16);
 *     int partition = function.getPartition(key); // Example with 16 partitions
 * </pre>
 */
public class PartitioningFunctions {

    private PartitioningFunctions() {
    }

    /**
     * Returns a {@link PartitioningFunction} optimized for scenarios where the number of partitions
     * is a power of two (e.g., 2, 4, 8, 16, etc.). This partitioning function uses bitwise operations
     * for faster computation compared to the general-purpose modulo-based strategy.
     *
     * @return a {@link PartitioningFunction} for power-of-two partitioning
     * @see PartitioningFunction
     */
    public static PartitioningFunction powerOfTwo(int maxPartitions) {
        return new PowerOfTwoPartitioningFunction(maxPartitions);
    }

    /**
     * Returns a general-purpose {@link PartitioningFunction} that can be used for any partition count.
     * This implementation uses a hash-based strategy with modulo operation to determine the partition index.
     * It works well for both small and large numbers of partitions.
     *
     * @return a general-purpose {@link PartitioningFunction} for partitioning
     * @see PartitioningFunction
     */
    public static PartitioningFunction generalPurpose(int maxPartitions) {
        return new GeneralPurposePartitioningFunction(maxPartitions);
    }
}
