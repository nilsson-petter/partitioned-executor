package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LazyLoadingPartitionedExecutorTest {

    private LazyLoadingPartitionedExecutor<PartitionedTask> executor;
    private Partitioner partitioner;
    private Partition<PartitionedTask> mockedPartition;
    private Map<Integer, Partition<PartitionedTask>> createdPartitions;
    private PartitionCreator<PartitionedTask> partitionCreator;

    @BeforeEach
    void setUp() {
        createdPartitions = new HashMap<>();
        partitionCreator = new MockPartitionCreator();
        partitioner = mock(Partitioner.class);
        this.executor = new LazyLoadingPartitionedExecutor<>(partitioner, partitionCreator);
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
        executor.execute(mock(PartitionedTask.class));
        assertThat(executor.getCreatedPartitionsCount()).isEqualTo(1);
        executor.execute(mock(PartitionedTask.class));
        assertThat(executor.getCreatedPartitionsCount()).isEqualTo(2);
        assertThat(executor.getPartitions()).hasSize(2);
    }

    @Test
    void shutdownWasInvoked() {
        when(partitioner.getPartition(any())).thenReturn(0, 1);
        executor.execute(mock(PartitionedTask.class));
        executor.execute(mock(PartitionedTask.class));
        executor.shutdown();

        createdPartitions.values().forEach(p -> {
            verify(p, times(1)).shutdown();
        });
    }

    @Test
    void awaitTermination() throws InterruptedException {
        when(partitioner.getPartition(any())).thenReturn(0, 1);
        executor.execute(mock(PartitionedTask.class));
        executor.execute(mock(PartitionedTask.class));

        when(createdPartitions.get(0).awaitTermination(any())).thenReturn(false);
        when(createdPartitions.get(1).awaitTermination(any())).thenReturn(false);
        assertThat(executor.awaitTermination(Duration.ofMillis(5))).isFalse();

        when(createdPartitions.get(0).awaitTermination(any())).thenReturn(true);
        when(createdPartitions.get(1).awaitTermination(any())).thenReturn(true);

        assertThat(executor.awaitTermination(Duration.ofMillis(5))).isTrue();
    }

    @Test
    void shutdownNow() throws InterruptedException {
        when(partitioner.getPartition(any())).thenReturn(0, 1);
        executor.execute(mock(PartitionedTask.class));
        executor.execute(mock(PartitionedTask.class));

        LinkedList<PartitionedTask> tasks1 = new LinkedList<>();
        var task1 = mock(PartitionedTask.class);
        var task2 = mock(PartitionedTask.class);
        tasks1.add(task1);
        tasks1.add(task2);

        LinkedList<PartitionedTask> tasks2 = new LinkedList<>();
        var task3 = mock(PartitionedTask.class);
        var task4 = mock(PartitionedTask.class);
        tasks2.add(task3);
        tasks2.add(task4);

        when(createdPartitions.get(0).shutdownNow()).thenReturn(tasks1);
        when(createdPartitions.get(1).shutdownNow()).thenReturn(tasks2);

        Map<Integer, Queue<PartitionedTask>> map = executor.shutdownNow();
        assertThat(map).containsEntry(0, tasks1);
        assertThat(map).containsEntry(1, tasks2);
    }

    @Test
    void isTerminated_shutdownNotInvoked() throws InterruptedException {
        when(partitioner.getPartition(any())).thenReturn(0, 1);
        executor.execute(mock(PartitionedTask.class));
        executor.execute(mock(PartitionedTask.class));
        assertThat(executor.isTerminated()).isFalse();
        verify(createdPartitions.get(0), never()).isTerminated();
        verify(createdPartitions.get(1), never()).isTerminated();
    }

    @Test
    void isTerminated_partitionNotYetDone() throws InterruptedException {
        when(partitioner.getPartition(any())).thenReturn(0);
        executor.execute(mock(PartitionedTask.class));
        executor.shutdown();
        when(createdPartitions.get(0).isTerminated()).thenReturn(false);
        assertThat(executor.isTerminated()).isFalse();

        verify(createdPartitions.get(0), times(1)).isTerminated();
    }

    @Test
    void notQueuedWhenInterrupted() {
        PartitionCreator<PartitionedTask> spy = spy(partitionCreator);
        executor.shutdown();
        executor.execute(mock(PartitionedTask.class));
        assertThat(createdPartitions).isEmpty();
        verify(spy, never()).create(anyInt());
    }

    @Test
    void gracefulShutdown_noOutstandingTasks() throws Exception {
        when(partitioner.getPartition(any())).thenReturn(0);
        executor.execute(mock(PartitionedTask.class));
        when(createdPartitions.get(0).awaitTermination(any())).thenReturn(false);

        LazyLoadingPartitionedExecutor<PartitionedTask> spy = spy(executor);
        spy.close();
        verify(spy, times(2)).shutdown();
        verify(spy, times(1)).awaitTermination(any());
        verify(spy, times(1)).shutdownNow();
    }

    @Test
    void gracefulShutdown_shutdownNow() throws Exception {
        LazyLoadingPartitionedExecutor<PartitionedTask> spy = spy(executor);
        spy.close();

        verify(spy, times(1)).shutdown();
        verify(spy, times(1)).awaitTermination(any());
        verify(spy, never()).shutdownNow();
    }

    private class MockPartitionCreator implements PartitionCreator<PartitionedTask> {

        @Override
        public Partition<PartitionedTask> create(int partitionNumber) {
            Partition<PartitionedTask> mock = mock(Partition.class);
            createdPartitions.put(partitionNumber, mock);
            return mock;
        }
    }


}