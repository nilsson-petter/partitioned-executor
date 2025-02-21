package xyz.petnil.partitionedexecutor;

import java.util.Queue;

/**
 * The {@code PartitionQueue} interface defines a contract for managing
 * a queue of {@link PartitionedTask} tasks in a partitioned execution environment.
 * Tasks are added to the queue and later retrieved for execution, typically within
 * a specific partition.
 *
 * <p>This interface allows tasks to be enqueued, retrieved, and provides mechanisms
 * for handling cases where tasks are dropped. It also supports inspecting
 * the current state of the queue.
 *
 * @see PartitionedTask
 * @see Partition
 * @see PartitionedExecutor
 */
public interface PartitionQueue<T extends PartitionedTask> {

    /**
     * Enqueues a {@link PartitionedTask} task into the queue.
     *
     * @param task the partitioned task to be added to the queue, must not be null
     * @return {@code true} if the task was successfully added to the queue,
     * {@code false} if the queue is full or if the task could not be enqueued
     * @throws NullPointerException if the task is null
     */
    boolean enqueue(T task);

    /**
     * Retrieves and removes the next {@link PartitionedTask} task from the queue,
     * waiting until a tasks becomes available.
     *
     * @return the next task
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    T getNextTask() throws InterruptedException;

    /**
     * Returns a snapshot of the current tasks in the queue.
     *
     * <p>The returned queue is typically used for inspection purposes
     * (e.g., during shutdown), and modifications to the returned queue
     * do not affect the underlying queue.
     *
     * @return a {@link Queue} containing the current {@link PartitionedTask} tasks in the queue
     */
    Queue<T> getQueue();

    /**
     * Returns the current size of the task queue.
     *
     * @return the number of tasks currently in the queue
     */
    int getQueueSize();

    /**
     * Removes the provided callback. Has no effect if the {@link Callback}
     * is not registered.
     *
     * @param callback the {@link Callback} to be removed.
     */
    void removeCallback(Callback<T> callback);

    /**
     * Adds a callback to be invoked when tasks are dropped from the queue
     * (e.g., due to throttling). The callback will be called
     * with the task that was dropped.
     *
     * @param callback the {@link Callback} to be added.
     */
    void addCallback(Callback<T> callback);

    /**
     * The {@code Callback} interface defines a callback to handle situations
     * where a task is dropped from the queue. This can be useful for handling cases
     * where tasks are superseded by e.g. throttling behaviour.
     */
    interface Callback<T extends PartitionedTask> {
        /**
         * Called when a {@link PartitionedTask} task is dropped from the queue.
         *
         * @param task the task that was dropped, must not be null
         */
        void onDropped(T task);
    }

    static <T extends PartitionedTask> PartitionQueue<T> fifo(int capacity) {
        return new FifoPartitionQueue<>(capacity);
    }

    static <T extends PartitionedTask> PartitionQueue<T> throttled(ThrottlingFunction throttlingFunction) {
        return new ThrottledPartitionQueue<>(throttlingFunction);
    }
}

