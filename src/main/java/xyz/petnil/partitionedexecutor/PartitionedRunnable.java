package xyz.petnil.partitionedexecutor;

/**
 * The {@code PartitionedRunnable} interface extends {@link Runnable} and represents
 * a task that is associated with a specific partition key. It is used in partitioned
 * execution environments where tasks are distributed across partitions based on the key.
 *
 * <p>The {@code PartitionedRunnable} defines two key methods:
 * <ul>
 *   <li>{@code getPartitionKey()}: Returns the key that determines the partition
 *   to which this task will be routed.</li>
 *   <li>{@code getDelegate()}: Returns the underlying {@link Runnable} delegate
 *   that contains the actual logic to execute.</li>
 * </ul>
 *
 * <p>When the {@code run()} method is called, it delegates the execution to
 * the {@code getDelegate()} method. This ensures that partitioning logic remains
 * separate from the actual task execution logic.
 *
 * <p><b>Usage:</b> The {@code PartitionedRunnable} is useful in systems
 * that distribute work across multiple partitions, such as in partitioned executors
 * or parallel processing frameworks.
 *
 * @see Runnable
 * @see PartitionedExecutor
 */
public interface PartitionedRunnable extends Runnable {

    /**
     * Returns the partition key associated with this task.
     * The partition key is used to determine which partition
     * this task will be routed to in a partitioned execution system.
     *
     * @return the partition key, must not be null
     * @throws NullPointerException if the partition key is null
     */
    Object getPartitionKey();

    /**
     * Returns the underlying {@link Runnable} delegate that contains
     * the actual execution logic for this task.
     *
     * <p>The {@code run()} method of this interface delegates to the
     * {@code run()} method of the object returned by {@code getDelegate()}.
     *
     * @return the delegate {@code Runnable} instance, must not be null
     * @throws NullPointerException if the delegate is null
     */
    Runnable getDelegate();

    /**
     * Executes the task by delegating to the {@code run()} method
     * of the {@code getDelegate()} object.
     *
     * <p>This method is called by the partitioned executor when the task is executed.
     * It ensures that the task logic is run in the context of the partition
     * to which it was assigned.
     */
    @Override
    default void run() {
        getDelegate().run();
    }
}
