# Partitioned Executor Library

[![Maven Central](https://img.shields.io/maven-central/v/com.example/partitioned-executor.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.example%20a:partitioned-executor)
[![Build Status](https://img.shields.io/github/workflow/status/your-repo/partitioned-executor/CI)](https://github.com/your-repo/partitioned-executor/actions)
[![License](https://img.shields.io/github/license/your-repo/partitioned-executor)](https://opensource.org/licenses/MIT)

---

## Overview

`Partitioned Executor` is a lightweight Java library designed for executing tasks in parallel, while ensuring that tasks belonging to the same logical "partition" are executed synchronously. This is useful in scenarios where you need to run tasks concurrently across multiple partitions but must maintain strict ordering within each partition.

### Key Features

- **Task Partitioning**: Tasks are routed to specific partitions based on a user-defined partitioning function.
- **Parallel Execution**: Partitions execute tasks concurrently, allowing for efficient utilization of resources.
- **Synchronous within Partition**: Tasks can be executed synchronously in order of arrival, depending on implementation chosen.
- **Graceful Shutdown**: Provides mechanisms to await task completion or force shutdown and retrieve pending tasks.
- **Callbacks**: Supports task execution callbacks.
- **Customization**: Users can implement custom partitioning strategies, partitions and partition queues to control behaviour.

---

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>xyz.petnil</groupId>
  <artifactId>partitioned-executor</artifactId>
  <version>1.0.0</version>
</dependency>
```

Or with Gradle:

```gradle
implementation 'xyz.petnil:partitioned-executor:1.0.0'
```

---

## Usage

### Quick Start

1. **Define a PartitionedRunnable**: A task that implements the `PartitionedRunnable` interface. Each task has a partition key, which is used to determine which partition it will run on.

```java
public class DocumentService {
    private final DocumentDao dao;
    // ...
    // ...
    private final class PersistDocumentTask implements PartitionedRunnable {
        private final Document document;

        public PersistDocumentTask(Document document) {
            this.document = document;
        }

        @Override
        public Object getPartitionKey() {
            return document.id();
        }

        @Override
        public Runnable getDelegate() {
            return () -> dao.save(document);
        }
    }
}
```

2. **Create a PartitionedExecutor**: Instantiate the `PartitionedExecutor`, specifying your partitioning function and partition creation logic.

```java
PartitionedExecutors.unbounded(8);
PartitionedExecutors.bounded(8, 100_000);
PartitionedExecutors.sampled(8, Duration.ofSeconds(1));
```

3. **Submit Tasks**: Submit tasks for execution.

```java
executor.execute(new PersistDocumentTask(document1));
executor.execute(new PersistDocumentTask(document2));
```

4. **Shutdown Gracefully**: Ensure that tasks complete before shutting down.

```java
executor.shutdown();
if (!executor.awaitTermination(Duration.ofSeconds(30))) {
    executor.shutdownNow(); // Force shutdown and retrieve pending tasks
}
```

### Partitioning Strategies

By default, the library provides two partitioning strategies:

1. **General-Purpose Partitioning**:
    - This is a simple, general-purpose partitioning function that computes the partition number by using the modulus operator.
    - Usage: `Partitioners.generalPurpose(10)`

2. **Power-of-Two Partitioning**:
    - Inspired by `HashMap`'s approach, this strategy efficiently computes the partition number using bitwise operations. It’s intended for partition counts that are powers of two.
    - Usage: `Partitioners.powerOfTwo(8)`

You can also define your own partitioning strategies by implementing the `Partitioner` interface.

```java
public class CustomPartitioner implements Partitioner {
    @Override
    public int getPartition(Object partitionKey) {
        return Math.abs(partitionKey.hashCode()) % getMaxNumberOfPartitions();
    }

    @Override
    public int getMaxNumberOfPartitions() {
        return 8; // For example, 8 partitions
    }
}
```

### Partition Queues

Four implementations of `PartitionQueue` are provided. Users are free to implement their own.

1. **Unbounded Fifo Partition Queue**:
   - This is a simple queue backed by a `LinkedBlockingQueue`.
   - Usage: `PartitionQueues.unboundedFifo()`

2. **Bounded Fifo Partition Queue**:
   - This is a simple queue backed by a `ArrayBlockingQueue`.
   - Usage: `PartitionQueues.boundedFifo(100_000)`

3**Sampled Partition Queue**:
   - This is a simple queue backed by a `DelayQueue` of partition keys, and a `ConcurrentHashMap` of tasks.
   - Newer tasks replace.
   - The provided `SamplingFunction` controls how long the partition key should be delayed for.
   - Already queued partition keys will not be queued again, limiting the queue size to the amount of partition keys.
   - Usage: `PartitionQueues.sampled(key -> Duration.ofSeconds(1))`
4**Priority Partition Queue**:
   - This is a simple queue backed by a `PriorityBlockingQueue`.
   - The provided `Comparator<PartitionedRunnable>` defines the priority of the task.
   - Usage: `PartitionQueues.priority((t1, t2) -> 0)`

### More Examples
```java
class DocumentService implements Partition.Callback {
    private final PartitionedExecutor executor;
    public DocumentService() {
        executor = PartitionedExecutorBuilder.newBuilder(8)
                .withPartitioner(Partitioners::powerOfTwo)
                .configurePartitionCreator()
                    .withPartitionQueueCreator(PartitionQueues::unbounded)
                    .withThreadNamePrefix("my-executor")
                    .withCallback(this)
                .buildPartitionCreator()
                .build();
    }

    @Override
    public void onSuccess(int partition, PartitionedRunnable task) {
        if (task instanceof PersistDocumentTask pdt) {
            // Handle success
        }
    }

    @Override
    public void onError(int partition, PartitionedRunnable task, Exception exception) {
        if (task instanceof PersistDocumentTask pdt) {
           // Handle error
        }
    }
}
```

---

## License

This library is licensed under the [MIT License](LICENSE).

---

## Contribution

Feel free to fork this repository and submit pull requests. Contributions are welcome!

---