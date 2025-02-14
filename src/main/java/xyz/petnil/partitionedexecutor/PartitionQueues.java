package xyz.petnil.partitionedexecutor;

public class PartitionQueues {
    private PartitionQueues() {
    }

    public static <T extends PartitionedTask> PartitionQueue<T> fifo(int capacity) {
        return new FifoPartitionQueue<>(capacity);
    }

    public static <T extends PartitionedTask> PartitionQueue<T> throttled(ThrottlingFunction throttlingFunction) {
        return new ThrottledPartitionQueue<>(throttlingFunction);
    }
}
