package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class LazyPartitionedExecutor implements PartitionedExecutor {
    private final Lock mainLock = new ReentrantLock();
    private final Map<Integer, Partition> partitions;

    private final PartitionCreator partitionCreator;
    private final PartitioningFunction partitioningFunction;

    public LazyPartitionedExecutor(PartitioningFunction partitioningFunction,
                                   PartitionCreator partitionCreator
    ) {
        this.partitions = new ConcurrentHashMap<>();
        this.partitioningFunction = partitioningFunction;
        this.partitionCreator = partitionCreator;
    }

    public void execute(Runnable task, Object partitionKey) {
        execute(new PartitionedRunnable() {
            @Override
            public Object getPartitionKey() {
                return partitionKey;
            }

            @Override
            public Runnable getDelegate() {
                return task;
            }
        });
    }

    @Override
    public void execute(PartitionedRunnable partitionedRunnable) {
        mainLock.lock();
        try {
            int partitionNumber = partitioningFunction.getPartition(partitionedRunnable.getPartitionKey());
            Partition partition = partitions.computeIfAbsent(partitionNumber, partitionCreator::create);
            partition.startPartition();
            partition.submitForExecution(partitionedRunnable);
        } finally {
            mainLock.unlock();
        }
    }


    public void shutdown() {
        mainLock.lock();
        try {
            partitions.values().forEach(Partition::initiateShutdown);
        } finally {
            mainLock.unlock();
        }
    }

    public boolean awaitTermination(Duration duration) throws InterruptedException {
        mainLock.lock();
        try {
            return partitions.values().stream()
                    .allMatch(p -> {
                        try {
                            return p.awaitTaskCompletion(duration);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    });
        } finally {
            mainLock.lock();
        }
    }

    @Override
    public List<Partition> getPartitions() {
        return new ArrayList<>(partitions.values());
    }

    @Override
    public Map<Integer, Queue<PartitionedRunnable>> shutdownNow() {
        mainLock.lock();
        try {
            HashMap<Integer, Queue<PartitionedRunnable>> tasksPerPartition = new HashMap<>();
            partitions.values().forEach(p -> tasksPerPartition.put(p.getPartitionNumber(), p.forceShutdownAndGetPending()));
            return tasksPerPartition;
        } finally {
            mainLock.lock();
        }
    }

    @Override
    public int getCreatedPartitionsCount() {
        return partitions.size();
    }

    @Override
    public int getMaxPartitionsCount() {
        return partitioningFunction.getMaxNumberOfPartitions();
    }
}
