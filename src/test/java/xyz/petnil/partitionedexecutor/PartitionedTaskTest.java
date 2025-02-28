package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PartitionedTaskTest {

    @Test
    void delegatesRun() {
        AtomicBoolean run = new AtomicBoolean(false);
        var task = new PartitionedTask() {
            @Override
            public Object getPartitionKey() {
                return null;
            }

            @Override
            public Runnable getDelegate() {
                return () -> run.set(true);
            }
        };

        task.run();

        assertTrue(run.get());
    }

}