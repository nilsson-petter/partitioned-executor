package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SingleThreadedPartitionWorker implements Partition, PartitionQueue.OnDroppedCallback {
    private final Lock mainLock = new ReentrantLock();
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private final int partitionNumber;
    private final PartitionQueue partitionQueue;
    private final ThreadFactory threadFactory;
    private final AtomicReference<Callback> callback;

    private Thread thread;

    public SingleThreadedPartitionWorker(
            int partitionNumber,
            PartitionQueue partitionQueue,
            ThreadFactory threadFactory,
            Callback callback
    ) {
        this.partitionNumber = partitionNumber;
        this.partitionQueue = Objects.requireNonNull(partitionQueue);
        this.partitionQueue.setOnDroppedCallback(this);
        this.threadFactory = Objects.requireNonNull(threadFactory);
        this.callback = new AtomicReference<>(callback);
    }

    @Override
    public int getPartitionNumber() {
        return partitionNumber;
    }

    @Override
    public void startPartition() {
        interrupted.set(false);
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
    public void submitForExecution(PartitionedRunnable task) {
        if (!interrupted.get()) {
            if (partitionQueue.enqueue(task)) {
                onSubmitted(task);
            } else {
                onRejected(task);
            }
        } else {
            onRejected(task);
        }
    }

    private void pollAndProcessOnce() {
        while (true) {
            try {
                PartitionedRunnable nextTask = partitionQueue.getNextTask(Duration.ofSeconds(5));
                if (interrupted.get() && nextTask == null) {
                    break;
                }

                if (nextTask != null) {
                    safeGuardedRun(nextTask);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void safeGuardedRun(PartitionedRunnable task) {
        try {
            task.run();
            onSuccess(task);
        } catch (Exception e) {
            onError(task, e);
        }
    }

    @Override
    public void initiateShutdown() {
        interrupted.set(true);
    }

    @Override
    public boolean awaitTaskCompletion(Duration duration) throws InterruptedException {
        if (thread != null) {
            return thread.join(duration);
        }
        return true;
    }

    @Override
    public Queue<PartitionedRunnable> forceShutdownAndGetPending() {
        mainLock.lock();
        try {
            thread.interrupt();
            return partitionQueue.getQueue();
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback.set(callback);
    }

    private void onSubmitted(PartitionedRunnable task) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onSubmitted(partitionNumber, task);
        }
    }

    private void onSuccess(PartitionedRunnable task) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onSuccess(partitionNumber, task);
        }
    }

    private void onError(PartitionedRunnable task, Exception e) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onError(partitionNumber, task, e);
        }
    }

    private void onRejected(PartitionedRunnable task) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onRejected(partitionNumber, task);
        }
    }

    @Override
    public void onDropped(PartitionedRunnable task) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onDropped(partitionNumber, task);
        }
    }

}
