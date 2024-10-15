package xyz.petnil.partitionedexecutor;

import java.util.concurrent.ThreadFactory;

/**
 * The {@code PartitionThreadFactoryCreator} interface defines a factory method
 * for creating {@link ThreadFactory} instances associated with a specific partition.
 * This is typically used in partitioned execution environments where each partition
 * needs its own thread or set of threads.
 *
 * <p>The primary use of this interface is to generate {@code ThreadFactory}
 * objects that are responsible for producing threads to execute tasks within
 * a given partition, identified by a partition number.
 *
 * @see ThreadFactory
 */
@FunctionalInterface
public interface PartitionThreadFactoryCreator {

    /**
     * Creates a {@link ThreadFactory} for the given partition number.
     *
     * <p>The returned {@code ThreadFactory} can be used to create threads
     * that execute tasks in the specified partition, ensuring that the tasks
     * are associated with the correct partitioning context.
     *
     * @param partitionNumber the identifier for the partition, must be a valid partition index
     * @return a {@link ThreadFactory} instance for creating threads in the specified partition
     */
    ThreadFactory createThreadFactory(int partitionNumber);
}
