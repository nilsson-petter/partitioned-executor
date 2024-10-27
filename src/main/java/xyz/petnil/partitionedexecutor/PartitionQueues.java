package xyz.petnil.partitionedexecutor;

import java.util.Comparator;

public class PartitionQueues {
    private PartitionQueues() {
    }

    public static PartitionQueue fifo(int capacity) {
        return new FifoPartitionQueue(capacity);
    }

    public static PartitionQueue trailingThrottled(ThrottlingFunction throttlingFunction) {
        return new TrailingThrottledPartitionQueue(throttlingFunction);
    }

    public static PartitionQueue priority(Comparator<PartitionedRunnable> comparator) {
        return new PriorityPartitionQueue(comparator);
    }
}
