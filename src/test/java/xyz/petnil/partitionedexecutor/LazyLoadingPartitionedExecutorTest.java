package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.petnil.partitionedexecutor.testdata.TestTask;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static xyz.petnil.partitionedexecutor.testdata.TestTask.TEST_TASK;
import static xyz.petnil.partitionedexecutor.testdata.TestTask.newTestTask;

class LazyLoadingPartitionedExecutorTest {

    private LazyLoadingPartitionedExecutor<TestTask> executor;
    private Partitioner partitioner;
    private PartitionedExecutor.Callback<TestTask> callback;

    @BeforeEach
    void setUp() {
        PartitionCreator<TestTask> partitionCreator = i -> {
            ThreadFactory factory = Thread.ofPlatform().name("partition-" + i).factory();
            PartitionQueue<TestTask> fifo = PartitionQueue.fifo(Integer.MAX_VALUE);
            return new SingleThreadedPartitionWorker<>(fifo, factory);
        };

        partitioner = mock(Partitioner.class);
        when(partitioner.getPartition(any())).thenReturn(0);

        this.executor = new LazyLoadingPartitionedExecutor<>(partitioner, partitionCreator);
        this.callback = mock(PartitionedExecutor.Callback.class);
        executor.addCallback(callback);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void partitioner() {
        assertThat(executor.getPartitioner()).isEqualTo(partitioner);
    }

    @Test
    void nullTask() {
        assertThrows(NullPointerException.class, () -> executor.execute(null));
    }

    @Test
    void maximumNumberOfPartitions() {
        when(partitioner.getMaxNumberOfPartitions()).thenReturn(2);
        assertThat(executor.getMaxPartitionsCount()).isEqualTo(2);
    }

    @Test
    void createdPartitionsCount() {
        when(partitioner.getPartition(any())).thenReturn(0, 1);
        assertThat(executor.getCreatedPartitionsCount()).isEqualTo(0);
        executor.execute(TEST_TASK);
        assertThat(executor.getCreatedPartitionsCount()).isEqualTo(1);
        executor.execute(TEST_TASK);
        assertThat(executor.getCreatedPartitionsCount()).isEqualTo(2);
        assertThat(executor.getPartitions()).hasSize(2);

        verify(callback).onPartitionCreated(0);
        verify(callback).onPartitionCreated(1);
        verify(callback).onTaskSubmitted(0, TEST_TASK);
        verify(callback).onTaskSubmitted(1, TEST_TASK);
        verify(callback).onTaskSuccess(0, TEST_TASK);
        verify(callback).onTaskSuccess(1, TEST_TASK);
    }

    @Test
    void shutdownWasInvoked() {
        when(partitioner.getPartition(any())).thenReturn(0, 1);
        executor.execute(TEST_TASK);
        executor.execute(TEST_TASK);
        executor.shutdown();
        assertTrue(executor.isShutdown());
        executor.getPartitions().forEach(p -> assertTrue(p.isShutdown()));
    }

    @Test
    void awaitTermination() throws InterruptedException {
        when(partitioner.getPartition(any())).thenReturn(0, 1);
        executor.execute(TEST_TASK);
        executor.execute(TEST_TASK);
        executor.shutdown();
        assertTrue(executor.awaitTermination(Duration.ofSeconds(2)));
        assertTrue(executor.isShutdown());
        assertTrue(executor.isTerminated());
        executor.getPartitions().forEach(p -> assertTrue(p.isTerminated()));
    }

    @Test
    void rejectedAfterShutdown() {
        executor.shutdown();
        executor.execute(TEST_TASK);
        verify(callback).onTaskRejected(0, TEST_TASK);
    }

    @Test
    void errorCallback() {
        var testTask = new TestTask(1, () -> {
            throw new RuntimeException("RuntimeException");
        });

        executor.execute(testTask);

        verify(callback).onTaskSubmitted(eq(0), eq(testTask));
        verify(callback, timeout(100).times(1)).onTaskError(eq(0), eq(testTask), any(RuntimeException.class));
    }

    @Test
    void autoclosable() {
        try (PartitionedExecutor<TestTask> paex = PartitionedExecutors.fifo(1, 10)) {
            paex.execute(newTestTask());
            paex.execute(newTestTask());
            paex.execute(newTestTask());
            paex.execute(newTestTask());
        }
    }

}