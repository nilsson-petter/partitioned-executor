package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Queue;

/**
 * The {@code PartitionQueue} interface defines a contract for managing
 * a queue of {@link PartitionedRunnable} tasks in a partitioned execution environment.
 * Tasks are added to the queue and later retrieved for execution, typically within
 * a specific partition.
 *
 * <p>This interface allows tasks to be enqueued, retrieved, and provides mechanisms
 * for handling cases where tasks are dropped. It also supports inspecting
 * the current state of the queue.
 *
 * @see PartitionedRunnable
 * @see Partition
 * @see PartitionedExecutor
 */
public interface PartitionQueue {

    /**
     * Enqueues a {@link PartitionedRunnable} task into the queue.
     *
     * @param task the partitioned task to be added to the queue, must not be null
     * @return {@code true} if the task was successfully added to the queue,
     *         {@code false} if the queue is full or if the task could not be enqueued
     * @throws NullPointerException if the task is null
     */
    boolean enqueue(PartitionedRunnable task);

    /**
     * Retrieves and removes the next {@link PartitionedRunnable} task from the queue,
     * waiting for the specified timeout if necessary for a task to become available.
     *
     * @param timeout the maximum time to wait for a task, must not be null
     * @return the next task, or {@code null} if the specified waiting time elapses before a task is available
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    PartitionedRunnable getNextTask(Duration timeout) throws InterruptedException;

    /**
     * Returns a snapshot of the current tasks in the queue.
     *
     * <p>The returned queue is typically used for inspection purposes
     * (e.g., during shutdown), and modifications to the returned queue
     * do not affect the underlying queue.
     *
     * @return a {@link Queue} containing the current {@link PartitionedRunnable} tasks in the queue
     */
    Queue<PartitionedRunnable> getQueue();

    /**
     * Returns the current size of the task queue.
     *
     * @return the number of tasks currently in the queue
     */
    int getQueueSize();

    /**
     * Sets a callback to be invoked when tasks are dropped from the queue
     * (e.g., due to sampling behaviour). The callback will be called
     * with the task that was dropped.
     *
     * @param callback the {@link Callback} to be invoked when tasks are dropped
     */
    void setCallback(Callback callback);

    /**
     * The {@code Callback} interface defines a callback to handle situations
     * where a task is dropped from the queue. This can be useful for handling cases
     * where tasks are superseded by e.g. debouncing behaviour.
     */
    interface Callback {
        /**
         * Called when a {@link PartitionedRunnable} task is dropped from the queue.
         *
         * @param task the task that was dropped, must not be null
         */
        void onDropped(PartitionedRunnable task);
    }
}

