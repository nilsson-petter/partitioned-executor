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
- **Throttling**: Ensures that only the latest task per partition key is executed within a specified timeframe.
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
implementation 'xyz.petnil:partitioned-executor:0.0.1-SNAPSHOT'
```

---

## Quick Start

### Implement a PartitionedTask
```java
public record PersistStockQuoteTask(String ticker, Runnable task) implements PartitionedTask {
    @Override
    public Object getPartitionKey() {
        return ticker;
    }

    @Override
    public Runnable getDelegate() {
        return task;
    }
}
```

### Create a PartitionedExecutor
```java
// An executor with FIFO semantics, 32 partitions and a maximum queue size of 10 000. 
PartitionedExecutor<PersistStockQuoteTask> executor = PartitionedExecutors.fifo(32, 10_000);

// Persist four stock quotes, two for AAPL and two for MSFT.
var task1 = new PersistStockQuoteTask("AAPL", () -> persistQuote("AAPL", BigDecimal.valueOf(130.3d)));
var task2 = new PersistStockQuoteTask("MSFT", () -> persistQuote("MSFT", BigDecimal.valueOf(209.83d)));
var task3 = new PersistStockQuoteTask("MSFT", () -> persistQuote("MSFT", BigDecimal.valueOf(208.51d)));
var task4 = new PersistStockQuoteTask("AAPL", () -> persistQuote("AAPL", BigDecimal.valueOf(131.3d)));
executor.execute(task1);
executor.execute(task2);
executor.execute(task3);
executor.execute(task4);

// Graceful shutdown
executor.close();
...
...
private void persistQuote(String ticker, BigDecimal lastPrice) {
    System.out.println(Thread.currentThread().getName() + "|" + ticker + "|" + lastPrice);
}
```

### Output
```text
SingleThreadedPartitionWorker-20|MSFT|209.83
SingleThreadedPartitionWorker-20|MSFT|208.51
SingleThreadedPartitionWorker-28|AAPL|130.3
SingleThreadedPartitionWorker-28|AAPL|131.3
```
Tasks for "AAPL" runs synchronously in partition #28. In parallel, tasks for "MSFT" runs in partition #20. 

---

## License

This library is licensed under the [MIT License](LICENSE).

---

## Contribution

Feel free to fork this repository and submit pull requests. Contributions are welcome!

---