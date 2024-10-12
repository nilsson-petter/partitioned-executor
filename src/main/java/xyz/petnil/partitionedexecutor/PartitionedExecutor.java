package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * The PartitionedExecutor interface provides an abstraction for executing tasks in parallel,
 * while routing each task to a designated partition based on its partition key.
 *
 * <p>Each partition can handle tasks synchronously, but tasks across different partitions
 * can execute in parallel. The routing logic is determined by the {@link PartitioningFunction}.
 */
public interface PartitionedExecutor extends AutoCloseable {

    /**
     * Executes the given partitioned task. The task will be routed to a partition based
     * on its partition key, as determined by the {@link PartitioningFunction}.
     *
     * @param task the task to execute, must not be null
     * @throws NullPointerException if the task is null
     */
    void execute(PartitionedRunnable task);

    /**
     * Returns the partitioning function that is used to map partition keys to partition indices.
     *
     * @return the partitioning function
     */
    PartitioningFunction getPartitioningFunction();

    /**
     * Initiates an orderly shutdown of the executor, where previously submitted tasks are executed,
     * but no new tasks are accepted.
     */
    void shutdown();

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the specified timeout occurs, or the current thread is interrupted.
     *
     * @param duration the maximum time to wait for termination
     * @return true if the executor terminated successfully, false if the timeout elapsed before termination
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    boolean awaitTermination(Duration duration) throws InterruptedException;

    /**
     * Attempts to stop all actively executing tasks and halts the processing of waiting tasks.
     * Returns a map of partition indices to the remaining queued tasks.
     *
     * @return a map of partition indices to remaining tasks that were not executed
     */
    Map<Integer, Queue<PartitionedRunnable>> shutdownNow();

    /**
     * Returns the list of partitions currently managed by this executor. This provides insight into
     * the structure of the partitioning, but modifications to the partitions may affect executor behavior.
     *
     * @return the list of partitions managed by the executor
     */
    List<Partition> getPartitions();

    /**
     * Returns the number of partitions created.
     *
     * @return the count of created partitions
     */
    int getCreatedPartitionsCount();

    /**
     * Returns the maximum number of partitions the executor can support.
     *
     * @return the maximum number of partitions
     */
    int getMaxPartitionsCount();

    /**
     * Closes the executor, attempting a graceful shutdown first. If the executor does not
     * terminate within a day, it invokes {@link #shutdownNow()} to force termination.
     */
    default void close() throws Exception {
        shutdown();
        if (!awaitTermination(Duration.ofDays(1))) {
            shutdownNow();
        }
    }
}
