package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartitionTest {

    @Test
    void defaultCallback() {
        TestCallback testCallback = new TestCallback();
        testCallback.onStarted();
        testCallback.onShutdown();
        testCallback.onTerminated();
        testCallback.onInterrupted();

        testCallback.onSubmitted(mock(PartitionedTask.class));
        testCallback.onRejected(mock(PartitionedTask.class));
        testCallback.onDropped(mock(PartitionedTask.class));
        testCallback.onSuccess(mock(PartitionedTask.class));
        testCallback.onError(mock(PartitionedTask.class), new RuntimeException());
    }

    @Test
    void close_true() throws Exception {
        Partition<PartitionedTask> partition = mock(Partition.class, Mockito.CALLS_REAL_METHODS);
        when(partition.awaitTermination(Duration.ofMinutes(30))).thenReturn(true);

        partition.close();

        verify(partition).shutdown();
        verify(partition).awaitTermination(Duration.ofMinutes(30));
    }

    @Test
    void close_false() throws Exception {
        Partition<PartitionedTask> partition = mock(Partition.class, Mockito.CALLS_REAL_METHODS);
        when(partition.awaitTermination(Duration.ofMinutes(30))).thenReturn(false);

        partition.close();

        verify(partition).shutdown();
        verify(partition).awaitTermination(Duration.ofMinutes(30));
        verify(partition).shutdownNow();
    }

    private static class TestCallback implements Partition.Callback<PartitionedTask> {

    }
}