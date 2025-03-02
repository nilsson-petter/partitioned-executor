package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Queue;

/**
 * The {@code Partition} interface represents an individual partition in a partitioned execution system.
 * Each partition handles the execution of tasks in isolation, managing its own task queue and lifecycle.
 * The interface provides methods to start the partition, submit tasks for execution, and manage shutdowns.
 *
 * <p>Partitions are responsible for processing {@link PartitionedTask} tasks, with task execution
 * being controlled via a {@link PartitionQueue}. They also support lifecycle management such as
 * starting, shutting down, and awaiting completion of tasks.
 *
 * <p>This interface extends {@link AutoCloseable} to ensure partitions can be closed and shutdown
 * gracefully. If a partition is unable to complete within a specified timeout during shutdown,
 * any remaining tasks can be forcibly retrieved.
 *
 * @see PartitionQueue
 * @see PartitionedTask
 * @see PartitionedExecutor
 */
public interface Partition<T extends PartitionedTask> extends AutoCloseable {

    /**
     * Returns the {@link PartitionQueue} associated with this partition.
     * The queue holds tasks that are waiting to be executed.
     *
     * @return the partition's task queue
     */
    PartitionQueue<T> getPartitionQueue();

    /**
     * Submits a {@link PartitionedTask} task for execution in this partition.
     * The task is added to the partition's task queue and will be processed
     * by the partition when resources are available.
     *
     * @param task the partitioned task to be executed, must not be null
     * @throws NullPointerException if the task is null.
     */
    void submitForExecution(T task);


    /**
     * Indicates whether this partition has been shutdown.
     * Will return true if shutdown is in progress or if shutdown is complete.
     *
     * @return true if the partition has been shutdown, otherwise false.
     */
    boolean isShutdown();

    /**
     * Indicates whether this partition has been terminated.
     *
     * @return true if the partition has been terminated, otherwise false.
     */
    boolean isTerminated();

    /**
     * Initiates the shutdown of this partition. The partition will stop accepting new tasks,
     * but will continue processing of completing any tasks already in the queue.
     */
    void shutdown();

    /**
     * Waits for all tasks in the partition to complete execution, or until the specified
     * timeout elapses. This method blocks until either all tasks are finished or the timeout occurs.
     *
     * @param duration the maximum time to wait for task completion
     * @return {@code true} if all tasks completed, {@code false} if the timeout elapsed before completion
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    boolean awaitTermination(Duration duration) throws InterruptedException;

    /**
     * Forces the shutdown of the partition and retrieves any pending tasks that have not yet been executed.
     * This method is typically used after a timeout or when a graceful shutdown could not be achieved.
     *
     * @return a {@link Queue} of {@link PartitionedTask} tasks that were pending at the time of shutdown
     */
    Queue<T> shutdownNow();

    /**
     * Adds a {@link Callback} to handle various partition-level events, such as task submission,
     * completion, errors, and shutdown events.
     *
     * @param callback the {@link Callback} to be added
     * @throws NullPointerException if the {@link Callback} is null
     */
    void addCallback(Callback<T> callback);

    /**
     * Removes the provided {@link Callback}.
     *
     * @param callback the {@link Callback} to be added
     * @throws NullPointerException if the {@link Callback} is null.
     */
    void removeCallback(Callback<T> callback);

    /**
     * Closes this partition, ensuring an orderly shutdown.
     * <p>
     * If the resource is not already terminated, it will be shut down
     * and then waited upon until termination. If interrupted during
     * the waiting process, a forced shutdown is initiated, and the
     * thread's interrupt status is restored before returning.
     * </p>
     *
     */
    @Override
    default void close() {
        boolean terminated = isTerminated();
        if (!terminated) {
            shutdown();
            boolean interrupted = false;
            while (!terminated) {
                try {
                    terminated = awaitTermination(Duration.ofDays(1));
                } catch (InterruptedException e) {
                    if (!interrupted) {
                        shutdownNow();
                        interrupted = true;
                    }
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * The {@code Callback} interface defines event handlers for various partition-level events,
     * such as task success, failure, rejection, and shutdown.
     */
    interface Callback<T extends PartitionedTask> {

        /**
         * Called when {@link Partition#shutdown()} has been invoked, meaning that it will no longer
         * accept new tasks.
         */
        default void onShutdown() {
        }

        /**
         * Called when the partition has terminated, meaning that it has finished executing all
         * tasks (either forcefully or gracefully) and is shutting down completely.
         */
        default void onTerminated() {
        }

        /**
         * Called when a task has successfully completed execution in this partition.
         *
         * @param task the {@link PartitionedTask} task that was completed
         */
        default void onSuccess(T task) {
        }

        /**
         * Called when a task execution results in an error or exception.
         *
         * @param task      the {@link PartitionedTask} task that caused the error
         * @param exception the exception that occurred during execution
         */
        default void onError(T task, Exception exception) {
        }

        /**
         * Called when a task is rejected from execution in this partition.
         * This can happen when the queue is full or resources are unavailable.
         *
         * @param task the {@link PartitionedTask} task that was rejected
         */
        default void onRejected(T task) {
        }

        /**
         * Called when a task is dropped from the partition's queue. Dropped tasks may occur
         * due to queue overflow or other capacity constraints.
         *
         * @param task the {@link PartitionedTask} task that was dropped
         */
        default void onDropped(T task) {
        }

        /**
         * Called when a task has been successfully submitted to the partition's queue for execution.
         *
         * @param task the {@link PartitionedTask} task that was submitted
         */
        default void onSubmitted(T task) {
        }
    }
}

