package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class FifoPartitionQueue implements PartitionQueue {
    private final LinkedBlockingQueue<PartitionedRunnable> taskQueue;
    private final int capacity;

    public FifoPartitionQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.capacity = capacity;
        taskQueue = new LinkedBlockingQueue<>(capacity);
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean enqueue(PartitionedRunnable task) {
        Objects.requireNonNull(task);
        return taskQueue.offer(task);
    }

    @Override
    public PartitionedRunnable getNextTask(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout);
        return taskQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Queue<PartitionedRunnable> getQueue() {
        return new LinkedList<>(taskQueue);
    }

    @Override
    public int getQueueSize() {
        return taskQueue.size();
    }

    @Override
    public void removeCallback(Callback callback) {
        // Not implemented
    }

    @Override
    public void addCallback(Callback callback) {
        // Not implemented
    }


}
