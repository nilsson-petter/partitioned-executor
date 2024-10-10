package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public interface PartitionedExecutor extends AutoCloseable {
    void execute(Runnable task, Object partitionKey);

    void execute(PartitionedRunnable partitionedRunnable);

    void shutdown();

    boolean awaitTermination(Duration duration) throws InterruptedException;

    Map<Integer, Queue<PartitionedRunnable>> shutdownNow();

    List<Partition> getPartitions();

    int getCreatedPartitionsCount();

    int getMaxPartitionsCount();

    default void close() throws Exception {
        shutdown();
        if (!awaitTermination(Duration.ofDays(1))) {
            shutdownNow();
        }
    }
}