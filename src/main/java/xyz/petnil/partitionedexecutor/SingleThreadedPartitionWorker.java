package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

class SingleThreadedPartitionWorker implements Partition {
    private final Lock mainLock = new ReentrantLock();
    private final PartitionQueue partitionQueue;
    private final ThreadFactory threadFactory;
    private final Set<Callback> callbacks = ConcurrentHashMap.newKeySet();
    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    private Thread thread;

    public SingleThreadedPartitionWorker(
            PartitionQueue partitionQueue,
            ThreadFactory threadFactory
    ) {
        this.partitionQueue = Objects.requireNonNull(partitionQueue);
        this.partitionQueue.addCallback(new PartitionQueueCallback());
        this.threadFactory = Objects.requireNonNull(threadFactory);
    }

    /**
     * Starts the processing thread if the current state is NEW.
     * <p>
     * This method acquires a lock to ensure thread safety while checking and updating the state.
     * If the state is successfully changed from NEW to RUNNING, a new thread is created using the
     * specified thread factory and starts executing the pollAndProcess method.
     */
    @Override
    public void start() {
        mainLock.lock();
        try {
            computeState(State.NEW, State.RUNNING, () -> {
                thread = threadFactory.newThread(this::pollAndProcess);
                thread.start();
                onStarted();
            });
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public PartitionQueue getPartitionQueue() {
        return partitionQueue;
    }

    @Override
    public void submitForExecution(PartitionedTask task) {
        Objects.requireNonNull(task);
        State s = state.get();
        if (s == State.SHUTDOWN || s == State.TERMINATED || !partitionQueue.enqueue(task)) {
            onRejected(task);
        } else {
            onSubmitted(task);
        }
    }


    private void pollAndProcess() {
        while (true) {
            try {
                PartitionedTask nextTask = partitionQueue.getNextTask(Duration.ofSeconds(5));
                State s = state.get();
                if ((s == State.SHUTDOWN || s == State.TERMINATED) && nextTask == null) {
                    break;
                }

                if (nextTask != null) {
                    safeGuardedRun(nextTask);
                }
            } catch (InterruptedException e) {
                setState(State.TERMINATED, this::onInterrupted);
                Thread.currentThread().interrupt();
                break;
            }
        }

        setState(State.TERMINATED, this::onTerminated);
    }

    private void safeGuardedRun(PartitionedTask task) {
        try {
            task.run();
            onSuccess(task);
        } catch (Exception e) {
            onError(task, e);
        }
    }

    @Override
    public boolean isShutdown() {
        return state.get() == State.SHUTDOWN || state.get() == State.TERMINATED;
    }

    @Override
    public boolean isTerminated() {
        return state.get() == State.TERMINATED;
    }

    @Override
    public void shutdown() {
        mainLock.lock();
        try {
            if (state.compareAndSet(State.NEW, State.SHUTDOWN) || state.compareAndSet(State.RUNNING, State.SHUTDOWN)) {
                onShutdown();
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
            if (thread != null) {
                return thread.join(duration);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public Queue<PartitionedTask> shutdownNow() {
        shutdown();
        mainLock.lock();
        try {
            thread.interrupt();
            return partitionQueue.getQueue();
        } finally {
            computeState(State.SHUTDOWN, State.TERMINATED, this::onTerminated);
            mainLock.unlock();
        }
    }

    private void setState(State newState, Runnable postStateTask) {
        state.set(newState);
        postStateTask.run();
    }


    private void computeState(State expectedState, State newState, Runnable postStateTask) {
        if (state.compareAndSet(expectedState, newState)) {
            postStateTask.run();
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

    private void onSubmitted(PartitionedTask task) {
        callback(c -> c.onSubmitted(task));
    }


    private void onSuccess(PartitionedTask task) {
        callback(c -> c.onSuccess(task));
    }

    private void onError(PartitionedTask task, Exception e) {
        callback(c -> c.onError(task, e));

    }

    private void onRejected(PartitionedTask task) {
        callback(c -> c.onRejected(task));
    }

    private void onInterrupted() {
        callback(Callback::onInterrupted);
    }

    private void onTerminated() {
        callback(Callback::onTerminated);
    }

    private void onStarted() {
        callback(Callback::onStarted);
    }

    private void onShutdown() {
        callback(Callback::onShutdown);
    }

    private enum State {
        NEW,
        RUNNING,
        SHUTDOWN,
        TERMINATED
    }

    private class PartitionQueueCallback implements PartitionQueue.Callback {

        @Override
        public void onDropped(PartitionedTask task) {
            callback(c -> c.onDropped(task));
        }
    }

}
