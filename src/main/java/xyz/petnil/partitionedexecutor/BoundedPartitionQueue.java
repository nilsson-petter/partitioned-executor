package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

class BoundedPartitionQueue implements PartitionQueue {
    private final ArrayBlockingQueue<PartitionedRunnable> taskQueue;

    public BoundedPartitionQueue(int capacity) {
        taskQueue = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public boolean enqueue(PartitionedRunnable partitionedRunnable) {
        return taskQueue.offer(partitionedRunnable);
    }

    @Override
    public PartitionedRunnable getNextTask(Duration timeout) throws InterruptedException {
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
