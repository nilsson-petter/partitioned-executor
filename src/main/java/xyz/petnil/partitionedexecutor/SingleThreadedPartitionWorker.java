package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SingleThreadedPartitionWorker implements Partition, PartitionQueue.Callback {
    private final Lock mainLock = new ReentrantLock();
    private final AtomicBoolean isShutdownSignaled = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final PartitionQueue partitionQueue;
    private final ThreadFactory threadFactory;
    private final AtomicReference<Callback> callback;

    private Thread thread;

    public SingleThreadedPartitionWorker(
            PartitionQueue partitionQueue,
            ThreadFactory threadFactory,
            Callback callback
    ) {
        this.partitionQueue = Objects.requireNonNull(partitionQueue);
        this.partitionQueue.addCallback(this);
        this.threadFactory = Objects.requireNonNull(threadFactory);
        this.callback = new AtomicReference<>(callback);
    }

    @Override
    public void startPartition() {
        mainLock.lock();
        try {
            if (isRunning.compareAndSet(false, true)) {
                thread = threadFactory.newThread(this::pollAndProcess);
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
        Objects.requireNonNull(task);
        if (isShutdownSignaled.get() || !partitionQueue.enqueue(task)) {
            onRejected(task);
        } else {
            onSubmitted(task);
        }
    }


    private void pollAndProcess() {
        isRunning.set(true);
        while (true) {
            try {
                PartitionedRunnable nextTask = partitionQueue.getNextTask(Duration.ofSeconds(5));
                if (isShutdownSignaled.get() && nextTask == null) {
                    break;
                }

                if (nextTask != null) {
                    safeGuardedRun(nextTask);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        isRunning.set(false);
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
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public boolean isShutdownInProgress() {
        return isShutdownSignaled.get();
    }

    @Override
    public void initiateShutdown() {
        isShutdownSignaled.set(true);
    }

    @Override
    public boolean awaitTaskCompletion(Duration duration) throws InterruptedException {
        Objects.requireNonNull(duration);
        mainLock.lock();
        try {
            if (thread != null) {
                return thread.join(duration);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
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
            cb.onSubmitted(task);
        }
    }

    private void onSuccess(PartitionedRunnable task) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onSuccess(task);
        }
    }

    private void onError(PartitionedRunnable task, Exception e) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onError(task, e);
        }
    }

    private void onRejected(PartitionedRunnable task) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onRejected(task);
        }
    }

    @Override
    public void onDropped(PartitionedRunnable task) {
        Callback cb = callback.get();
        if (cb != null) {
            cb.onDropped(task);
        }
    }

}
