package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

class SingleThreadedPartitionWorker implements Partition, PartitionQueue.Callback {
    private final Lock mainLock = new ReentrantLock();
    private final AtomicBoolean isShutdownSignaled = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final PartitionQueue partitionQueue;
    private final ThreadFactory threadFactory;
    private final Set<Callback> callbacks = ConcurrentHashMap.newKeySet();

    private Thread thread;

    public SingleThreadedPartitionWorker(
            PartitionQueue partitionQueue,
            ThreadFactory threadFactory
    ) {
        this.partitionQueue = Objects.requireNonNull(partitionQueue);
        this.partitionQueue.addCallback(this);
        this.threadFactory = Objects.requireNonNull(threadFactory);
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
    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }

    private void callback(Consumer<Callback> consumer) {
        callbacks.forEach(consumer);
    }

    private void onSubmitted(PartitionedRunnable task) {
        callback(c -> c.onSubmitted(task));
    }


    private void onSuccess(PartitionedRunnable task) {
        callback(c -> c.onSuccess(task));
    }

    private void onError(PartitionedRunnable task, Exception e) {
        callback(c -> c.onError(task, e));

    }

    private void onRejected(PartitionedRunnable task) {
        callback(c -> c.onRejected(task));
    }

    @Override
    public void onDropped(PartitionedRunnable task) {
        callback(c -> c.onDropped(task));
    }

}
