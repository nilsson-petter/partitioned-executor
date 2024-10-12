package xyz.petnil.partitionedexecutor;

/**
 * The {@code Partitioners} class provides factory methods to create instances
 * of commonly used {@link Partitioner} implementations. These partitioners
 * can be used to distribute tasks or data across multiple partitions.
 * <p>
 * This class contains static utility methods and is not meant to be instantiated.
 * The provided methods offer different strategies for partitioning:
 * <ul>
 *   <li>{@code powerOfTwo()} - Optimized for partition counts that are powers of two.</li>
 *   <li>{@code generalPurpose()} - A general-purpose partitioner suitable for any partition count.</li>
 * </ul>
 * </p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 *     Partitioner partitioner = Partitioners.powerOfTwo(16);
 *     int partition = partitioner.getPartition(key); // Example with 16 partitions
 * </pre>
 */
public class Partitioners {

    private Partitioners() {
    }

    /**
     * Returns a {@link Partitioner} optimized for scenarios where the number of partitions
     * is a power of two (e.g., 2, 4, 8, 16, etc.). This partitioner uses bitwise operations
     * for faster computation compared to the general-purpose modulo-based strategy.
     *
     * @return a {@link Partitioner} for power-of-two partitioning
     * @see Partitioner
     */
    public static Partitioner powerOfTwo(int maxPartitions) {
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
    public static Partitioner generalPurpose(int maxPartitions) {
        return new GeneralPurposePartitioner(maxPartitions);
    }
}
