package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

class BoundedFifoPartitionQueue implements PartitionQueue {
    private final ArrayBlockingQueue<PartitionedRunnable> taskQueue;

    public BoundedFifoPartitionQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        taskQueue = new ArrayBlockingQueue<>(capacity);
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
    public void setOnDroppedCallback(OnDroppedCallback callback) {
        // Not implemented
    }

    @Override
    public Queue<PartitionedRunnable> getQueue() {
        return new LinkedList<>(taskQueue);
    }

    @Override
    public int getQueueSize() {
        return taskQueue.size();
    }


}
