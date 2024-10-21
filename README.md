# Partitioned Executor Library

[![Maven Central](https://img.shields.io/maven-central/v/xyz.petnil/partitioned-executor.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:xyz.petnil%20a:partitioned-executor)
[![Build Status](https://img.shields.io/github/actions/workflow/status/nilsson-petter/partitioned-executor/ci.yml?branch=main)](https://github.com/nilsson-petter/partitioned-executor/actions)
[![License](https://img.shields.io/github/license/nilsson-petter/partitioned-executor)](https://opensource.org/licenses/MIT)

---

## Overview

Partitioned Executor is a lightweight Java library designed for executing tasks in parallel across different logical partitions. 
In the most common use case, tasks that belong to the same logical partition are executed synchronously, ensuring strict ordering. 
However, the library offers users the flexibility to implement their own rules for how and when tasks are executed within each partition. 
This allows for greater customization and adaptability to various concurrency requirements, making it suitable for scenarios where tasks need to be run concurrently across multiple partitions while maintaining control over their execution order.


### Key Features

- **Task Partitioning**: Tasks are routed to specific partitions based on a user-defined partitioning function.
- **Parallel Execution**: Partitions execute tasks concurrently, allowing for efficient utilization of resources.
- **Synchronous within Partition**: Tasks can be executed synchronously in order of arrival, depending on implementation chosen.
- **Debouncing**: Ensures that only the latest task per partition key is executed within a specified timeframe.
- **Graceful Shutdown**: Provides mechanisms to await task completion or force shutdown and retrieve pending tasks.
- **Callbacks**: Supports task execution callbacks.
- **Customization**: Users can implement custom partitioning strategies, partitions and partition queues to control behaviour.

---

## Requirements

- **Java Version**: This library requires **Java 21** or higher to use features like Virtual Threads. Please ensure you have the appropriate version installed.

---

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>xyz.petnil</groupId>
  <artifactId>partitioned-executor</artifactId>
  <version>0.0.1-SNAPSHOT</version>
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
    private final DocumentRepository repository;
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
            return () -> repository.save(document);
        }
    }
}
```

2. **Create a PartitionedExecutor**: Instantiate the `PartitionedExecutor`, either with the predefined executors in `PartitionedExecutors`
or with `PartitionedExecutorBuilder`.

```java
PartitionedExecutor executor = PartitionedExecutors.unboundedFifo(8);
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
   - This queue is backed by a `DelayQueue` of partition keys, and a `ConcurrentHashMap` of tasks.
   - Newer tasks supersedes older in the map. Partition keys already in the queue will not be queued again, limiting the queue size to the amount of partition keys.
   - The provided `SamplingFunction` controls how long the partition key should be delayed for.
   - Usage: `PartitionQueues.sampled(key -> Duration.ofSeconds(1))`

4**Priority Partition Queue**:
   - This queue is backed by a `PriorityBlockingQueue`.
   - The provided `Comparator<PartitionedRunnable>` defines the priority of the task.
   - Usage: `PartitionQueues.priority((t1, t2) -> 0)`

### Examples

#### 1. FIFO In Each Partition
Useful for when each task matters.
```java
private record TestPartitionedRunnable(Object partitionKey, Object id) implements PartitionedRunnable {
   @Override
   public Object getPartitionKey() {
      return partitionKey;
   }

   @Override
   public Runnable getDelegate() {
      return () ->
              System.out.printf(
                      "[%s] [%s] Task with partitionKey=%s id=%s running%n",
                      LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                      Thread.currentThread().getName(),
                      partitionKey,
                      id
              );
   }
}
...
...
PartitionedExecutor unbounded = PartitionedExecutors.unboundedFifo(5);
unbounded.execute(new TestPartitionedRunnable("AAPL", 235.00));
unbounded.execute(new TestPartitionedRunnable("MSFT", 418.16));
unbounded.execute(new TestPartitionedRunnable("AAPL", 234.93));
unbounded.execute(new TestPartitionedRunnable("MSFT", 418.11));
unbounded.close();
```
Output
```
[2024-10-19T22:20:15.494] [SingleThreadedPartitionWorker-4] Task with partitionKey=MSFT id=418.16 running
[2024-10-19T22:20:15.495] [SingleThreadedPartitionWorker-4] Task with partitionKey=MSFT id=418.11 running
[2024-10-19T22:20:15.494] [SingleThreadedPartitionWorker-1] Task with partitionKey=AAPL id=235.0 running
[2024-10-19T22:20:15.495] [SingleThreadedPartitionWorker-1] Task with partitionKey=AAPL id=234.93 running
```

#### 2. Sampled
Useful when you want to control how often tasks with the same partition key is executed.

```java

```

#### Callbacks

#### 
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