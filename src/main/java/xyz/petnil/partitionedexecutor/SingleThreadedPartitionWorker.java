package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SingleThreadedPartitionWorker implements Partition {
    private final Lock mainLock = new ReentrantLock();
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private final int partitionNumber;
    private final PartitionQueue partitionQueue;
    private final ThreadFactory threadFactory;
    private final Callback callback;

    private Thread thread;

    public SingleThreadedPartitionWorker(
            int partitionNumber,
            PartitionQueue partitionQueue,
            ThreadFactory threadFactory,
            Callback callback
    ) {
        this.partitionNumber = partitionNumber;
        this.partitionQueue = partitionQueue;
        this.threadFactory = threadFactory;
        this.callback = callback;
    }

    public SingleThreadedPartitionWorker(
            int partitionNumber,
            PartitionQueue partitionQueue,
            ThreadFactory threadFactory
    ) {
        this(partitionNumber, partitionQueue, threadFactory, new Callback() {
        });
    }

    @Override
    public int getPartitionNumber() {
        return partitionNumber;
    }

    @Override
    public void startPartition() {
        mainLock.lock();
        try {
            if (thread == null || !thread.isAlive()) {
                thread = threadFactory.newThread(this::pollAndProcessOnce);
                thread.start();
            }
        } finally {
            mainLock.unlock();
        }

    }

    @Override
    public PartitionQueue getPartitionQueue() {
        return partitionQueue;
    }

    @Override
    public void submitForExecution(PartitionedRunnable partitionedRunnable) {
        if (!interrupted.get()) {
            partitionQueue.enqueue(partitionedRunnable);
            callback.onSubmitted(partitionedRunnable);
        } else {
            callback.onDropped(partitionedRunnable);
        }
    }

    private void pollAndProcessOnce() {
        while (!interrupted.get()) {
            try {
                PartitionedRunnable nextTask = partitionQueue.getNextTask(Duration.ofSeconds(5));
                safeGuardedRun(nextTask);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // TODO Callback?
                throw new RuntimeException(e);
            }
        }
    }

    private void safeGuardedRun(PartitionedRunnable task) {
        try {
            task.run();
        } catch (Exception e) {
            callback.onError(task);
        }
    }

    @Override
    public void shutdown() {
        //TODO Implement
    }

    @Override
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        // TODO Implement
        return true;
    }

    @Override
    public Queue<PartitionedRunnable> terminateForcibly() {
        mainLock.lock();
        try {
            thread.interrupt();
            return partitionQueue.getQueue();
        } finally {
            mainLock.unlock();
        }
    }

}
