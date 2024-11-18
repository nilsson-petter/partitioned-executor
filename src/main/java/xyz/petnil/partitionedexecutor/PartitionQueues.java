package xyz.petnil.partitionedexecutor;

public class PartitionQueues {
    private PartitionQueues() {
    }

    public static <T extends PartitionedTask> PartitionQueue<T> fifo(int capacity) {
        return new FifoPartitionQueue<>(capacity);
    }

    public static <T extends PartitionedTask> PartitionQueue<T> trailingThrottled(ThrottlingFunction throttlingFunction) {
        return new TrailingThrottledPartitionQueue<>(throttlingFunction);
    }
}
