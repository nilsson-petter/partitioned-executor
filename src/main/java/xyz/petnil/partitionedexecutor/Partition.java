package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Queue;

public interface Partition extends AutoCloseable {
    int getPartitionNumber();

    void startPartition();

    PartitionQueue getPartitionQueue();

    void submitForExecution(PartitionedRunnable task);

    void initiateShutdown();

    boolean awaitTaskCompletion(Duration duration) throws InterruptedException;

    Queue<PartitionedRunnable> forceShutdownAndGetPending();

    void setCallback(Callback callback);

    @Override
    default void close() throws Exception {
        initiateShutdown();
        if (!awaitTaskCompletion(Duration.ofMinutes(30))) {
            forceShutdownAndGetPending();
        }
    }

    interface Callback {
        default void onSuccess(int partition, PartitionedRunnable task) {

        }

        default void onError(int partition, PartitionedRunnable task, Exception exception) {

        }

        default void onInterrupted(int partition) {

        }

        default void onRejected(int partition, PartitionedRunnable task) {

        }

        default void onDropped(int partition, PartitionedRunnable task) {

        }

        default void onSubmitted(int partition, PartitionedRunnable task) {

        }

        default void onTerminated(int partition) {

        }
    }
}
