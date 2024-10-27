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
    void example2() throws Exception {
        PartitionedExecutor trailingThrottled = PartitionedExecutors.trailingThrottled(2, o -> Duration.ofSeconds(1));
        trailingThrottled.execute(new TestPartitionedRunnable(0, 0));
        trailingThrottled.execute(new TestPartitionedRunnable(0, 1));
        trailingThrottled.execute(new TestPartitionedRunnable(0, 99));
        trailingThrottled.execute(new TestPartitionedRunnable(1, 0));
        trailingThrottled.execute(new TestPartitionedRunnable(1, 1));
        trailingThrottled.execute(new TestPartitionedRunnable(1, 99));
        trailingThrottled.close();
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
