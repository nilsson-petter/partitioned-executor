package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of {@link PartitionedExecutor} where partitions are created lazily on-demand when a task is
 * assigned to a new partition. This ensures that resources are only allocated when necessary.
 *
 * <p>This executor uses a {@link Partitioner} to determine the partition for
 * each task, and a {@link PartitionCreator} to instantiate partitions when required.
 *
 * <p>It supports graceful shutdown with {@link #shutdown()} and forced shutdown
 * with {@link #shutdownNow()}, providing control over the lifecycle of partitioned tasks.
 *
 * <p><b>Thread Safety:</b> The class ensures thread safety by acquiring a main lock
 * for critical sections like partition creation, task submission, and shutdown.
 */
class LazyLoadingPartitionedExecutor<T extends PartitionedTask> implements PartitionedExecutor<T> {

    private final Lock mainLock = new ReentrantLock();
    private final Map<Integer, Partition<T>> partitions;
    private final PartitionCreator<T> partitionCreator;
    private final Partitioner partitioner;

    private final AtomicBoolean interrupted = new AtomicBoolean();

    private final Set<Callback<T>> callbacks = ConcurrentHashMap.newKeySet();

    /**
     * Creates a {@code LazyPartitionedExecutor} with the specified partitioner and partition creator.
     *
     * @param partitioner
     *         the function used to assign tasks to partitions, must not be null
     * @param partitionCreator
     *         the factory used to create partitions when needed, must not be null
     *
     * @throws NullPointerException
     *         if either {@code partitioner} or {@code partitionCreator} is null
     */
    public LazyLoadingPartitionedExecutor(Partitioner partitioner,
                                          PartitionCreator<T> partitionCreator) {
        this.partitions = new ConcurrentHashMap<>(partitioner.getMaxNumberOfPartitions());
        this.partitioner = Objects.requireNonNull(partitioner);
        this.partitionCreator = Objects.requireNonNull(partitionCreator);
    }

    /**
     * Executes the given {@link PartitionedTask} by determining its partition and submitting it for execution. If the
     * corresponding partition does not exist, it is created lazily.
     *
     * @param task
     *         the partitioned task to execute, must not be null
     *
     * @throws NullPointerException
     *         if the task is null
     */
    @Override
    public void execute(T task) {
        Objects.requireNonNull(task);
        mainLock.lock();
        try {
            int partitionNumber = partitioner.getPartition(task.getPartitionKey());

            if (!interrupted.get()) {
                partitions.computeIfAbsent(partitionNumber, key -> {
                    Partition<T> createdPartition = partitionCreator.create(key);
                    callbacks.forEach(c -> c.onPartitionCreated(key));
                    PartitionCallbackDecorator partitionCallbackDecorator = new PartitionCallbackDecorator(key);
                    createdPartition.addCallback(partitionCallbackDecorator);
                    return createdPartition;
                }).submitForExecution(task);
            } else {
                callbacks.forEach(c -> c.onTaskRejected(partitionNumber, task));
            }

        } finally {
            mainLock.unlock();
        }

    }

    /**
     * Returns the {@link Partitioner} used by this executor to route tasks to partitions.
     *
     * @return the partitioner
     */
    @Override
    public Partitioner getPartitioner() {
        return partitioner;
    }

    /**
     * Initiates an orderly shutdown of the executor. All tasks that have been submitted will continue to execute, but
     * no new tasks will be accepted.
     */
    @Override
    public void shutdown() {
        mainLock.lock();
        try {
            interrupted.set(true);
            partitions.forEach((partitionNumber, partition) -> partition.shutdown());
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Blocks until all tasks have completed execution or the timeout occurs, whichever happens first.
     *
     * @param duration
     *         the maximum time to wait for termination
     *
     * @return {@code true} if all partitions terminated, {@code false} if the timeout elapsed
     *
     * @throws InterruptedException
     *         if the current thread is interrupted while waiting
     */
    @Override
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        mainLock.lock();
        try {
            long remainingNanos = duration.toNanos();
            long startTime = System.nanoTime();

            for (Map.Entry<Integer, Partition<T>> partitionEntry : partitions.entrySet()) {
                boolean completed = partitionEntry.getValue().awaitTermination(Duration.ofNanos(remainingNanos));
                if (!completed) {
                    return false;
                }

                long elapsedTime = System.nanoTime() - startTime;
                remainingNanos -= elapsedTime;

                if (remainingNanos <= 0) {
                    return false;
                }
                startTime = System.nanoTime();
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }


    /**
     * Returns a list of partitions managed by this executor.
     *
     * @return the list of partitions
     */
    @Override
    public List<Partition<T>> getPartitions() {
        return new ArrayList<>(partitions.values());
    }

    /**
     * Forces an immediate shutdown of the executor, stopping all tasks and returning the remaining unexecuted tasks in
     * each partition.
     *
     * @return a map of partition indices to the remaining tasks in each partition
     */
    @Override
    public Map<Integer, Queue<T>> shutdownNow() {
        shutdown();
        mainLock.lock();
        try {
            HashMap<Integer, Queue<T>> tasksPerPartition = new HashMap<>();
            partitions.forEach((key, value) -> tasksPerPartition.put(key, value.shutdownNow()));
            return tasksPerPartition;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the number of partitions created so far by this executor.
     *
     * @return the number of partitions
     */
    @Override
    public int getCreatedPartitionsCount() {
        return partitions.size();
    }

    /**
     * Returns the maximum number of partitions supported by the executor, as defined by the {@link Partitioner}.
     *
     * @return the maximum number of partitions
     */
    @Override
    public int getMaxPartitionsCount() {
        return partitioner.getMaxNumberOfPartitions();
    }

    @Override
    public boolean isShutdown() {
        return interrupted.get();
    }

    @Override
    public boolean isTerminated() {
        // Check if the executor has been shut down
        if (!isShutdown()) {
            return false;
        }

        // Ensure that all partitions have completed their tasks
        mainLock.lock();
        try {
            for (Partition<T> partition : partitions.values()) {
                if (!partition.isTerminated()) {
                    return false;
                }
            }
            return true;
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

    private class PartitionCallbackDecorator implements Partition.Callback<T> {

        private final int partitionNumber;

        public PartitionCallbackDecorator(int partitionNumber) {
            this.partitionNumber = partitionNumber;
        }

        @Override
        public void onSuccess(T task) {
            callbacks.forEach(c -> c.onTaskSuccess(partitionNumber, task));
        }

        @Override
        public void onError(T task, Exception exception) {
            callbacks.forEach(c -> c.onTaskError(partitionNumber, task, exception));
        }

        @Override
        public void onRejected(T task) {
            callbacks.forEach(c -> c.onTaskRejected(partitionNumber, task));
        }

        @Override
        public void onDropped(T task) {
            callbacks.forEach(c -> c.onTaskDropped(partitionNumber, task));
        }

        @Override
        public void onSubmitted(T task) {
            callbacks.forEach(c -> c.onTaskSubmitted(partitionNumber, task));
        }

        @Override
        public void onTerminated() {
            callbacks.forEach(c -> c.onPartitionTerminated(partitionNumber));
            if (partitions.values().stream().allMatch(Partition::isTerminated)) {
                callbacks.forEach(Callback::onTerminated);
            }
        }

        @Override
        public void onShutdown() {
            callbacks.forEach(c -> c.onPartitionShutdown(partitionNumber));
            if (partitions.values().stream().allMatch(Partition::isShutdown)) {
                callbacks.forEach(Callback::onShutdown);
            }
        }
    }
}

