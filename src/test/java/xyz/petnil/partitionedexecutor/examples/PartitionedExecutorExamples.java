package xyz.petnil.partitionedexecutor.examples;

import org.junit.jupiter.api.Test;
import xyz.petnil.partitionedexecutor.PartitionedExecutor;
import xyz.petnil.partitionedexecutor.PartitionedExecutors;
import xyz.petnil.partitionedexecutor.PartitionedRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class PartitionedExecutorExamples {


    @Test
    void example1() throws Exception {
        PartitionedExecutor unbounded = PartitionedExecutors.unboundedFifo(5);
        unbounded.execute(new TestPartitionedRunnable("AAPL", 235.00));
        unbounded.execute(new TestPartitionedRunnable("MSFT", 418.16));
        unbounded.execute(new TestPartitionedRunnable("AAPL", 234.93));
        unbounded.execute(new TestPartitionedRunnable("MSFT", 418.11));
        unbounded.close();
    }

    @Test
    void example2() throws Exception {
        PartitionedExecutor sampled = PartitionedExecutors.sampled(2, o -> Duration.ofSeconds(1));
        sampled.execute(new TestPartitionedRunnable(0, 0));
        sampled.execute(new TestPartitionedRunnable(0, 1));
        sampled.execute(new TestPartitionedRunnable(0, 99));
        sampled.execute(new TestPartitionedRunnable(1, 0));
        sampled.execute(new TestPartitionedRunnable(1, 1));
        sampled.execute(new TestPartitionedRunnable(1, 99));
        sampled.close();
    }

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
}
