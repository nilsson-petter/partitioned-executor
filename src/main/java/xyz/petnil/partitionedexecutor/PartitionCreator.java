package xyz.petnil.partitionedexecutor;

/**
 * The {@code PartitionCreator} interface is a functional interface responsible
 * for creating new {@link Partition} instances. It is typically used in
 * {@link PartitionedExecutor} implementations where partitions are created
 * dynamically, such as in lazy or on-demand partitioning strategies.
 *
 * <p>This interface allows for flexibility in how partitions are instantiated,
 * decoupling the logic of partition creation from the executor's core functionality.
 *
 * <p><b>Usage:</b> Implementations of this interface should provide the logic
 * to create a partition for the given partition number.
 *
 * @see PartitionedExecutor
 */
@FunctionalInterface
public interface PartitionCreator<T extends PartitionedTask> {

    /**
     * Creates a new {@link Partition} for the specified partition number.
     *
     * @param partitionNumber the number of the partition to be created, must be a non-negative integer
     * @return a new {@code Partition} instance corresponding to the given partition number
     * @throws IllegalArgumentException if the partition number is invalid (e.g., negative)
     */
    Partition<T> create(int partitionNumber);
}
