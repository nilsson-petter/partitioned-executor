package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ThrottledPartitionQueueTest {
    private static final String PARTITION_KEY = "AAPL";
    private ThrottledPartitionQueue<PartitionedTask> throttledPartitionQueue;
    private ThrottlingFunction throttlingFunction;
    private PartitionedTask mockTask;

    @BeforeEach
    void setUp() {
        throttlingFunction = mock(ThrottlingFunction.class);
        when(throttlingFunction.getThrottlingInterval(any())).thenReturn(Duration.ofMillis(50));
        mockTask = mock(PartitionedTask.class);
        when(mockTask.getPartitionKey()).thenReturn(PARTITION_KEY);
        throttledPartitionQueue = new ThrottledPartitionQueue<>(throttlingFunction);
    }

    @Test
    void throttlingFunction() {
        ThrottlingFunction s = o -> Duration.ofSeconds(1);
        var queue = new ThrottledPartitionQueue<>(s);
        assertThat(queue.getThrottlingFunction()).isEqualTo(s);
    }

    @Test
    void enqueue_shouldAddTask_whenNewPartitionKey() {
        boolean result = throttledPartitionQueue.enqueue(mockTask);
        assertThat(result).isTrue();
        assertThat(throttledPartitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void enqueue_taskIsReplacedAndDropped() throws InterruptedException {
        String partitionKey = "TSLA";
        PartitionedTask firstTask = mock(PartitionedTask.class);
        PartitionedTask secondTask = mock(PartitionedTask.class);
        when(firstTask.getPartitionKey()).thenReturn(partitionKey);
        when(secondTask.getPartitionKey()).thenReturn(partitionKey);
        PartitionQueue.Callback<PartitionedTask> callback = mock(PartitionQueue.Callback.class);
        throttledPartitionQueue.addCallback(callback);

        // Enqueue both tasks
        throttledPartitionQueue.enqueue(firstTask);
        boolean result = throttledPartitionQueue.enqueue(secondTask);

        // Assert
        assertThat(result).isTrue();
        assertThat(throttledPartitionQueue.getQueueSize()).isEqualTo(1); // Only the new task should remain

        assertThat(throttledPartitionQueue.getState())
                .hasSize(1)
                .containsValue(secondTask)
                .containsKey(partitionKey);

        PartitionedTask firstInQueue = throttledPartitionQueue.getNextTask(Duration.ofSeconds(1));
        assertEquals(secondTask, firstInQueue);

        verify(callback, times(1)).onDropped(firstTask);
    }

    @Test
    void getNextTask_shouldReturnTask_whenAvailable() throws InterruptedException {
        when(throttlingFunction.getThrottlingInterval(any())).thenReturn(Duration.ofMillis(20));
        throttledPartitionQueue.enqueue(mockTask);
        PartitionedTask retrievedTask = throttledPartitionQueue.getNextTask(Duration.ofSeconds(1));
        assertThat(retrievedTask).isEqualTo(mockTask);
        assertThat(throttledPartitionQueue.getQueueSize()).isEqualTo(0);
    }

    @Test
    void getQueueSize_shouldReturnCorrectSize() {
        throttledPartitionQueue.enqueue(mockTask);
        assertThat(throttledPartitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void getQueue_shouldReturnAllQueuedTasks() {
        // Arrange
        when(throttlingFunction.getThrottlingInterval(any())).thenReturn(Duration.ofDays(1));
        List<PartitionedTask> expectedList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PartitionedTask task = mock(PartitionedTask.class);
            when(task.getPartitionKey()).thenReturn(i);
            expectedList.add(task);
            throttledPartitionQueue.enqueue(task);
        }

        // Assert
        assertThat(throttledPartitionQueue.getQueueSize()).isEqualTo(10);
        assertThat(throttledPartitionQueue.getQueue()).hasSize(10).containsSequence(expectedList);
    }

    @Test
    void removeCallback() {
        String partitionKey = "TSLA";
        var firstTask = mock(PartitionedTask.class);
        var secondTask = mock(PartitionedTask.class);
        var thirdTask = mock(PartitionedTask.class);
        when(firstTask.getPartitionKey()).thenReturn(partitionKey);
        when(secondTask.getPartitionKey()).thenReturn(partitionKey);
        when(thirdTask.getPartitionKey()).thenReturn(partitionKey);

        PartitionQueue.Callback<PartitionedTask> callback = mock(PartitionQueue.Callback.class);
        throttledPartitionQueue.addCallback(callback);

        throttledPartitionQueue.enqueue(firstTask);
        throttledPartitionQueue.enqueue(secondTask);
        verify(callback, times(1)).onDropped(firstTask);

        throttledPartitionQueue.removeCallback(callback);
        throttledPartitionQueue.enqueue(thirdTask);

        verifyNoMoreInteractions(callback);
    }
}
