package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Queue;

/**
 * The {@code Partition} interface represents an individual partition in a partitioned execution system.
 * Each partition handles the execution of tasks in isolation, managing its own task queue and lifecycle.
 * The interface provides methods to start the partition, submit tasks for execution, and manage shutdowns.
 *
 * <p>Partitions are responsible for processing {@link PartitionedRunnable} tasks, with task execution
 * being controlled via a {@link PartitionQueue}. They also support lifecycle management such as
 * starting, shutting down, and awaiting completion of tasks.
 *
 * <p>This interface extends {@link AutoCloseable} to ensure partitions can be closed and shutdown
 * gracefully. If a partition is unable to complete within a specified timeout during shutdown,
 * any remaining tasks can be forcibly retrieved.
 *
 * <p>This interface extends {@link PartitionQueue.Callback} to ensure callbacks get propagated
 * to {@link Callback}.
 *
 * @see PartitionQueue
 * @see PartitionedRunnable
 * @see PartitionedExecutor
 */
public interface Partition extends AutoCloseable, PartitionQueue.Callback {

    /**
     * Starts the execution of tasks in this partition. This typically involves starting the
     * thread(s) that will consume and execute tasks from the partition's {@link PartitionQueue}.
     */
    void startPartition();

    /**
     * Returns the {@link PartitionQueue} associated with this partition.
     * The queue holds tasks that are waiting to be executed.
     *
     * @return the partition's task queue
     */
    PartitionQueue getPartitionQueue();

    /**
     * Submits a {@link PartitionedRunnable} task for execution in this partition.
     * The task is added to the partition's task queue and will be processed
     * by the partition when resources are available.
     *
     * @param task the partitioned task to be executed, must not be null
     * @throws NullPointerException if the task is null.

     */
    void submitForExecution(PartitionedRunnable task);

    boolean isRunning();

    boolean isShutdownInProgress();

    boolean isTerminated();

    /**
     * Initiates the shutdown of this partition. The partition will stop accepting new tasks
     * and will begin the process of completing any tasks already in the queue.
     */
    void initiateShutdown();

    /**
     * Waits for all tasks in the partition to complete execution, or until the specified
     * timeout elapses. This method blocks until either all tasks are finished or the timeout occurs.
     *
     * @param duration the maximum time to wait for task completion
     * @return {@code true} if all tasks completed, {@code false} if the timeout elapsed before completion
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    boolean awaitTaskCompletion(Duration duration) throws InterruptedException;

    /**
     * Forces the shutdown of the partition and retrieves any pending tasks that have not yet been executed.
     * This method is typically used after a timeout or when a graceful shutdown could not be achieved.
     *
     * @return a {@link Queue} of {@link PartitionedRunnable} tasks that were pending at the time of shutdown
     */
    Queue<PartitionedRunnable> forceShutdownAndGetPending();

    /**
     * Adds a {@link Callback} to handle various partition-level events, such as task submission,
     * completion, errors, and shutdown events.
     *
     * @param callback the {@link Callback} to be added
     * @throws NullPointerException if the {@link Callback} is null
     */
    void addCallback(Callback callback);

    /**
     * Removes the provided {@link Callback}.
     *
     * @param callback the {@link Callback} to be added
     * @throws NullPointerException if the {@link Callback} is null.
     */
    void removeCallback(Callback callback);

    /**
     * Initiates the shutdown of the partition and attempts to await task completion.
     * If tasks are not completed within 30 minutes, it forces a shutdown and retrieves any pending tasks.
     *
     * @throws Exception if an error occurs during shutdown
     */
    @Override
    default void close() throws Exception {
        initiateShutdown();
        if (!awaitTaskCompletion(Duration.ofMinutes(30))) {
            forceShutdownAndGetPending();
        }
    }

    /**
     * The {@code Callback} interface defines event handlers for various partition-level events,
     * such as task success, failure, rejection, and shutdown.
     */
    interface Callback {

        /**
         * Called when a task has successfully completed execution in this partition.
         *
         * @param task the {@link PartitionedRunnable} task that was completed
         */
        default void onSuccess(PartitionedRunnable task) {
        }

        /**
         * Called when a task execution results in an error or exception.
         *
         * @param task      the {@link PartitionedRunnable} task that caused the error
         * @param exception the exception that occurred during execution
         */
        default void onError(PartitionedRunnable task, Exception exception) {
        }

        /**
         * Called when the partition is interrupted during execution.
         */
        default void onInterrupted() {
        }

        /**
         * Called when a task is rejected from execution in this partition.
         * This can happen when the queue is full or resources are unavailable.
         *
         * @param task the {@link PartitionedRunnable} task that was rejected
         */
        default void onRejected(PartitionedRunnable task) {
        }

        /**
         * Called when a task is dropped from the partition's queue. Dropped tasks may occur
         * due to queue overflow or other capacity constraints.
         *
         * @param task the {@link PartitionedRunnable} task that was dropped
         */
        default void onDropped(PartitionedRunnable task) {
        }

        /**
         * Called when a task has been successfully submitted to the partition's queue for execution.
         *
         * @param task the {@link PartitionedRunnable} task that was submitted
         */
        default void onSubmitted(PartitionedRunnable task) {
        }

        /**
         * Called when the partition has terminated, meaning that it has finished executing all
         * tasks and is shutting down completely.
         */
        default void onTerminated() {
        }
    }
}

