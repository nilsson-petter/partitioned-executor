package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PartitionedExecutorTest {

    @Test
    void callbackCoverage() {
        PartitionedExecutor.Callback<PartitionedTask> callback = new PartitionedExecutor.Callback<>() {
        };

        assertDoesNotThrow(() -> callback.onTerminated());
        assertDoesNotThrow(() -> callback.onTaskSubmitted(1, null));
        assertDoesNotThrow(() -> callback.onShutdown());
        assertDoesNotThrow(() -> callback.onTaskRejected(1, null));
        assertDoesNotThrow(() -> callback.onTaskDropped(1, null));
        assertDoesNotThrow(() -> callback.onTaskError(1, null, null));
        assertDoesNotThrow(() -> callback.onTaskSuccess(1, null));
        assertDoesNotThrow(() -> callback.onPartitionCreated(1));
        assertDoesNotThrow(() -> callback.onPartitionTerminated(1));
        assertDoesNotThrow(() -> callback.onPartitionShutdown(1));
    }
}