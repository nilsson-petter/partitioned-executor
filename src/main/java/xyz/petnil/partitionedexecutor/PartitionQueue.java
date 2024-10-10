package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Queue;

public interface PartitionQueue {
    boolean enqueue(PartitionedRunnable partitionedRunnable);

    PartitionedRunnable getNextTask(Duration timeout) throws InterruptedException;

    void registerOnDroppedCallback(OnDroppedCallback callback);

    void clearOnDroppedCallback();

    Queue<PartitionedRunnable> getQueue();

    interface OnDroppedCallback {
        void onDropped(PartitionedRunnable partitionedRunnable);
    }
}
