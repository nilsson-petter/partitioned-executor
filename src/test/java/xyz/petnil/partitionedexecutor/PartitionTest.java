package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PartitionTest {
    @Test
    void callbackCoverage() {
        Partition.Callback<PartitionedTask> callback = new Partition.Callback<>() {
        };

        assertDoesNotThrow(callback::onShutdown);
        assertDoesNotThrow(callback::onTerminated);
        assertDoesNotThrow(() -> callback.onError(null, null));
        assertDoesNotThrow(() -> callback.onDropped(null));
        assertDoesNotThrow(() -> callback.onSuccess(null));
        assertDoesNotThrow(() -> callback.onSubmitted(null));
        assertDoesNotThrow(() -> callback.onRejected(null));
    }

}