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

    @Override
    public void execute(PartitionedRunnable task) {
        mainLock.lock();
        try {
            int partitionNumber = partitioningFunction.getPartition(task.getPartitionKey());
            Partition partition = partitions.computeIfAbsent(partitionNumber, partitionCreator::create);
            partition.startPartition();
            partition.submitForExecution(task);
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public PartitioningFunction getPartitioningFunction() {
        return partitioningFunction;
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
            mainLock.unlock();
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
            mainLock.unlock();
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
