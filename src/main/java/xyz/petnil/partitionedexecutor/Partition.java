package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Queue;

public interface Partition extends AutoCloseable {
    int getPartitionNumber();

    void startPartition();

    PartitionQueue getPartitionQueue();

    void submitForExecution(PartitionedRunnable partitionedRunnable);

    void shutdown();

    boolean awaitTermination(Duration duration) throws InterruptedException;

    Queue<PartitionedRunnable> terminateForcibly();

    @Override
    default void close() throws Exception {
        shutdown();
        if (!awaitTermination(Duration.ofSeconds(30))) {
            terminateForcibly();
        }
    }

    interface Callback {
        default void onSuccess(PartitionedRunnable partitionedRunnable) {

        }

        default void onError(PartitionedRunnable partitionedRunnable) {

        }

        default void onDropped(PartitionedRunnable partitionedRunnable) {

        }

        default void onSubmitted(PartitionedRunnable partitionedRunnable) {

        }

        default void onTerminated() {

        }
    }
}
