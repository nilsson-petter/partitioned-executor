package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

class SingleThreadedPartitionWorker<T extends PartitionedTask> implements Partition<T> {
    private final Lock mainLock = new ReentrantLock();
    private final PartitionQueue<T> partitionQueue;
    private final Set<Callback<T>> callbacks = ConcurrentHashMap.newKeySet();
    private final ExecutorService executorService;

    public SingleThreadedPartitionWorker(
            PartitionQueue<T> partitionQueue,
            ThreadFactory threadFactory
    ) {
        this.partitionQueue = Objects.requireNonNull(partitionQueue);
        this.partitionQueue.addCallback(new PartitionQueueCallback());
        this.executorService = Executors.newSingleThreadExecutor(threadFactory);
        executorService.execute(this::pollAndProcess);
    }

    private void pollAndProcess() {
        while (true) {
            try {
                // Make polling interval configurable
                T nextTask = partitionQueue.getNextTask(Duration.ofSeconds(1));

                if (isShutdown() && nextTask == null) {
                    break;
                }

                if (nextTask != null) {
                    safeGuardedRun(nextTask);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!isTerminated()) {
            callback(Callback::onTerminated);
        }
    }

    @Override
    public PartitionQueue<T> getPartitionQueue() {
        return partitionQueue;
    }

    @Override
    public void submitForExecution(T task) {
        Objects.requireNonNull(task);
        // Reject if shutdown or terminated
        if (!isShutdown() && partitionQueue.enqueue(task)) {
            callback(c -> c.onSubmitted(task));
        } else {
            callback(c -> c.onRejected(task));
        }
    }


    private void safeGuardedRun(T task) {
        try {
            task.run();
            callback(c -> c.onSuccess(task));
        } catch (Exception e) {
            callback(c -> c.onError(task, e));
        }
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public void shutdown() {
        mainLock.lock();
        try {
            if (!isShutdown()) {
                executorService.shutdown();
                callback(Callback::onShutdown);
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        Objects.requireNonNull(duration);
        mainLock.lock();
        try {
           return executorService.awaitTermination(duration.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public Queue<T> shutdownNow() {
        shutdown();
        mainLock.lock();
        try {
            if (!isTerminated()) {
                executorService.shutdownNow();
            }
            return partitionQueue.getQueue();
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void addCallback(Callback<T> callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeCallback(Callback<T> callback) {
        callbacks.remove(callback);
    }

    private void callback(Consumer<Callback<T>> consumer) {
        callbacks.forEach(consumer);
    }

    private class PartitionQueueCallback implements PartitionQueue.Callback<T> {

        @Override
        public void onDropped(T task) {
            callback(c -> c.onDropped(task));
        }
    }

}
