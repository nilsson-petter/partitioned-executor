package xyz.petnil.partitionedexecutor;

import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * Utility class for creating {@link PartitionedExecutor} instances with different partitioning strategies.
 *
 * <p>This class provides factory methods for creating partitioned executors using FIFO queues,
 * throttled queues, and fully customizable configurations.
 */
public class PartitionedExecutors {

    private PartitionedExecutors() {
    }

    /**
     * Creates a {@link PartitionedExecutor} with a First-In-First-Out (FIFO) partition queue strategy.
     *
     * <p>This method allows for the creation of a partitioned executor where tasks are processed
     * in the order they are submitted within their respective partitions. The number of partitions and the maximum
     * queue size per partition are configurable.
     *
     * @param <T>
     *         the type of task that extends {@link PartitionedTask}
     * @param maxPartitions
     *         the maximum number of partitions allowed in the executor
     * @param maxQueueSize
     *         the maximum number of tasks allowed in the queue for a single partition
     *
     * @return a {@link PartitionedExecutor} configured with the specified number of partitions and queue size, using a
     *         FIFO queue strategy
     *
     * @throws IllegalArgumentException
     *         if {@code maxPartitions} or {@code maxQueueSize} is non-positive
     *
     *         <p><b>Example usage:</b>
     *         <pre>{@code
     *         PartitionedExecutor<MyTask> executor = PartitionedExecutors.fifo(10, 100);
     *         executor.execute(myTask);
     *         }</pre>
     */
    public static <T extends PartitionedTask> PartitionedExecutor<T> fifo(int maxPartitions, int maxQueueSize) {
        return custom(
                maxPartitions,
                i -> PartitionQueue.fifo(maxQueueSize),
                i -> Thread.ofPlatform().name("partition-" + i).factory()
        );
    }

    /**
     * Creates a {@link PartitionedExecutor} with a throttling strategy for partition queues.
     *
     * <p>This method configures a partitioned executor where tasks within a partition are throttled
     * based on a {@link ThrottlingFunction}. This ensures that tasks are processed sequentially within their respective
     * partitions, with control over the task rate or delay as defined by the throttling function.
     *
     * <p>The last task submitted is guaranteed to be processed.
     *
     * @param <T>
     *         the type of task that extends {@link PartitionedTask}
     * @param maxPartitions
     *         the maximum number of partitions allowed in the executor
     * @param throttlingFunction
     *         a {@link ThrottlingFunction} defining how tasks are throttled in each partition
     *
     * @return a {@link PartitionedExecutor} configured with the specified number of partitions and throttling strategy
     *
     * @throws IllegalArgumentException
     *         if {@code maxPartitions} is non-positive
     * @throws NullPointerException
     *         if {@code throttlingFunction} is null
     *
     *         <p><b>Example usage:</b>
     *         <pre>{@code
     *         ThrottlingFunction throttlingFunction = o -> Duration.ofMillis(100);
     *         PartitionedExecutor<MyTask> executor = PartitionedExecutors.throttled(10, throttlingFunction);
     *         executor.execute(myTask);
     *         }</pre>
     */
    public static <T extends PartitionedTask> PartitionedExecutor<T> throttled(
            int maxPartitions, ThrottlingFunction throttlingFunction) {
        return custom(
                maxPartitions,
                i -> PartitionQueue.throttled(throttlingFunction),
                i -> Thread.ofPlatform().name("partition-" + i).factory()
        );
    }

    /**
     * Creates a {@link PartitionedExecutor} with custom partition queues and thread factories.
     *
     * <p>This method provides flexibility to define how partition queues and thread factories are
     * created for each partition.
     *
     * @param <T>
     *         the type of task that extends {@link PartitionedTask}
     * @param maxPartitions
     *         the maximum number of partitions allowed in the executor
     * @param queueFunction
     *         a function that creates a {@link PartitionQueue} for each partition
     * @param threadFactoryFunction
     *         a function that provides a {@link ThreadFactory} for each partition
     *
     * @return a {@link PartitionedExecutor} with the specified partition queue and thread factory settings
     *
     *         <p><b>Example usage:</b>
     *         <pre>{@code
     *         PartitionedExecutor<MyTask> executor = PartitionedExecutors.custom(
     *             10,
     *             i -> PartitionQueue.fifo(100),
     *             i -> Thread.ofPlatform().name("partition-" + i).factory()
     *         );
     *         executor.execute(myTask);
     *         }</pre>
     */
    public static <T extends PartitionedTask> PartitionedExecutor<T> custom(
            int maxPartitions,
            Function<Integer, PartitionQueue<T>> queueFunction,
            Function<Integer, ThreadFactory> threadFactoryFunction
    ) {
        return custom(Partitioner.mostSuitableFor(maxPartitions), queueFunction, threadFactoryFunction);
    }

    /**
     * Creates a {@link PartitionedExecutor} with a specified {@link Partitioner}, partition queue function, and thread
     * factory function.
     *
     * @param <T>
     *         the type of task that extends {@link PartitionedTask}
     * @param partitioner
     *         the partitioning strategy to be used
     * @param queueFunction
     *         a function that provides a {@link PartitionQueue} for each partition
     * @param threadFactoryFunction
     *         a function that provides a {@link ThreadFactory} for each partition
     *
     * @return a {@link PartitionedExecutor} configured with the specified partitioner and settings
     */
    public static <T extends PartitionedTask> PartitionedExecutor<T> custom(
            Partitioner partitioner,
            Function<Integer, PartitionQueue<T>> queueFunction,
            Function<Integer, ThreadFactory> threadFactoryFunction
    ) {
        return custom(partitioner, i -> new SingleThreadedPartitionWorker<>(queueFunction.apply(i), threadFactoryFunction.apply(i)));
    }

    /**
     * Creates a {@link PartitionedExecutor} using a specified {@link Partitioner} and {@link PartitionCreator}.
     *
     * @param <T>
     *         the type of task that extends {@link PartitionedTask}
     * @param partitioner
     *         the partitioning strategy to be used
     * @param partitionCreator
     *         a function that creates partitions dynamically
     *
     * @return a {@link PartitionedExecutor} configured with the given partitioner and partition creator
     */
    public static <T extends PartitionedTask> PartitionedExecutor<T> custom(
            Partitioner partitioner,
            PartitionCreator<T> partitionCreator
    ) {
        return new LazyLoadingPartitionedExecutor<>(partitioner, partitionCreator);
    }
}
