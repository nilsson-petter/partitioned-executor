package xyz.petnil.partitionedexecutor;

public class PartitionedExecutors {
    private PartitionedExecutors() {
    }

    /**
     * Creates a {@link PartitionedExecutor} with a First-In-First-Out (FIFO) partition queue strategy.
     *
     * <p>This method allows for the creation of a partitioned executor where tasks are processed
     * in the order they are submitted within their respective partitions. The number of partitions
     * and the maximum queue size per partition are configurable.
     *
     * @param <T>            the type of task that extends {@link PartitionedTask}
     * @param maxPartitions  the maximum number of partitions allowed in the executor
     * @param maxQueueSize   the maximum number of tasks allowed in the queue for a single partition
     * @return a {@link PartitionedExecutor} configured with the specified number of partitions
     *         and queue size, using a FIFO queue strategy
     *
     * @throws IllegalArgumentException if {@code maxPartitions} or {@code maxQueueSize} is non-positive
     *
     * <p>Example usage:
     * <pre>{@code
     * PartitionedExecutor<MyTask> executor = PartitionedExecutor.fifo(10, 100);
     * executor.execute(myTask);
     * }</pre>
     */
    public static <T extends PartitionedTask> PartitionedExecutor<T> fifo(int maxPartitions, int maxQueueSize) {
        return new LazyLoadingPartitionedExecutor<>(
                Partitioners.mostSuitableFor(maxPartitions),
                i -> new SingleThreadedPartitionWorker<>(
                        PartitionQueue.fifo(maxQueueSize),
                        Thread.ofPlatform().name("partition-" + i).factory()
        ));
    }


    /**
     * Creates a {@link PartitionedExecutor} with a throttling strategy for partition queues.
     *
     * <p>This method configures a partitioned executor where tasks within a partition are throttled
     * based on a {@link ThrottlingFunction}. This ensures that tasks are processed sequentially within
     * their respective partitions, but with control over the task rate or delay as defined by the
     * throttling function.
     *<p>
     * The last task submitted is guaranteed to be processed.
     *
     * @param <T>                 the type of task that extends {@link PartitionedTask}
     * @param maxPartitions       the maximum number of partitions allowed in the executor
     * @param throttlingFunction  a {@link ThrottlingFunction} defining how tasks are throttled in each partition
     * @return a {@link PartitionedExecutor} configured with the specified number of partitions
     *         and throttling strategy
     *
     * @throws IllegalArgumentException if {@code maxPartitions} is non-positive
     * @throws NullPointerException if {@code throttlingFunction} is null
     *
     * <p>Example usage:
     * <pre>{@code
     * ThrottlingFunction throttlingFunction = o -> Duration.ofMillis(100);
     * PartitionedExecutor<MyTask> executor = PartitionedExecutor.throttled(10, throttlingFunction);
     * executor.execute(myTask);
     * }</pre>
     */
    public static <T extends PartitionedTask> PartitionedExecutor<T> throttled(int maxPartitions, ThrottlingFunction throttlingFunction) {
        return new LazyLoadingPartitionedExecutor<>(
                Partitioners.mostSuitableFor(maxPartitions),
                i -> new SingleThreadedPartitionWorker<>(
                        PartitionQueue.throttled(throttlingFunction),
                        Thread.ofPlatform().name("partition-" + i).factory()
                )
        );
    }

}
