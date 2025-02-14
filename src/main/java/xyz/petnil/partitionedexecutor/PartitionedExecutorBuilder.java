package xyz.petnil.partitionedexecutor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PartitionedExecutorBuilder<T extends PartitionedTask> {
    private Partitioner partitioner = new PowerOfTwoPartitioner(4);

    private PartitionCreator<T> partitionCreator = i -> new SingleThreadedPartitionWorker<>(
            PartitionQueues.fifo(Integer.MAX_VALUE),
            Thread.ofPlatform().name("partition-worker-" + i).factory()
    );

    private final Set<PartitionedExecutor.Callback<T>> callbacks = new HashSet<>();

    private PartitionedExecutorBuilder() {
    }

    public static <T extends PartitionedTask> PartitionedExecutorBuilder<T> newBuilder() {
        return new PartitionedExecutorBuilder<>();
    }

    public PartitionedExecutorBuilder<T> withPartitioner(Partitioner partitioner) {
        this.partitioner = Objects.requireNonNull(partitioner, "partitioner must not be null");
        return this;
    }

    public PartitionedExecutorBuilder<T> withPartitionCreator(PartitionCreator<T> partitionCreator) {
        this.partitionCreator = Objects.requireNonNull(partitionCreator, "partitionCreator must not be null");
        return this;
    }

    public PartitionedExecutorBuilder<T> withCallback(PartitionedExecutor.Callback<T> callback) {
        callbacks.add(Objects.requireNonNull(callback, "callback must not be null"));
        return this;
    }

    public PartitionedExecutor<T> build() {
        LazyLoadingPartitionedExecutor<T> executor = new LazyLoadingPartitionedExecutor<>(partitioner, partitionCreator);
        callbacks.forEach(executor::addCallback);
        return executor;
    }
}

