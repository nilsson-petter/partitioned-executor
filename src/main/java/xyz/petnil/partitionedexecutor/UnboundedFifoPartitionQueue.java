package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class UnboundedFifoPartitionQueue implements PartitionQueue {

    private final LinkedBlockingQueue<PartitionedRunnable> taskQueue;

    public UnboundedFifoPartitionQueue() {
        taskQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public boolean enqueue(PartitionedRunnable partitionedRunnable) {
        return taskQueue.add(partitionedRunnable);
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
