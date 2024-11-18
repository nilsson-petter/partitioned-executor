package xyz.petnil.partitionedexecutor.examples;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import xyz.petnil.partitionedexecutor.PartitionedExecutors;
import xyz.petnil.partitionedexecutor.PartitionedTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Disabled
public class PartitionedExecutorExamples {


    @Test
    void example2() throws Exception {
        try (var executor = PartitionedExecutors.trailingThrottled(2, o -> Duration.ofSeconds(1))) {
            // Add tasks for partitionKey=0
            executor.execute(new TestPartitionedTask(0, 0));
            executor.execute(new TestPartitionedTask(0, 25));
            executor.execute(new TestPartitionedTask(0, 50));
            executor.execute(new TestPartitionedTask(0, 75));
            executor.execute(new TestPartitionedTask(0, 100));

            // Add tasks for partitionKey=1
            executor.execute(new TestPartitionedTask(1, 0));
            executor.execute(new TestPartitionedTask(1, 25));
            executor.execute(new TestPartitionedTask(1, 50));
            executor.execute(new TestPartitionedTask(1, 75));
            executor.execute(new TestPartitionedTask(1, 100));
        }
    }

    private record TestPartitionedTask(Object partitionKey, Object id) implements PartitionedTask {
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
