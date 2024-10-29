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
import static org.mockito.Mockito.when;

class TrailingThrottledPartitionQueueTest {
    private static final String PARTITION_KEY = "AAPL";
    private TrailingThrottledPartitionQueue trailingThrottledPartitionQueue;
    private ThrottlingFunction throttlingFunction;
    private PartitionedRunnable mockTask;

    @BeforeEach
    void setUp() {
        throttlingFunction = mock(ThrottlingFunction.class);
        when(throttlingFunction.getThrottlingInterval(any())).thenReturn(Duration.ofMillis(50));
        mockTask = mock(PartitionedRunnable.class);
        when(mockTask.getPartitionKey()).thenReturn(PARTITION_KEY);
        trailingThrottledPartitionQueue = new TrailingThrottledPartitionQueue(throttlingFunction);
    }

    @Test
    void enqueue_shouldAddTask_whenNewPartitionKey() {
        boolean result = trailingThrottledPartitionQueue.enqueue(mockTask);
        assertThat(result).isTrue();
        assertThat(trailingThrottledPartitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void enqueue_taskIsReplacedAndDropped() throws InterruptedException {
        String partitionKey = "TSLA";
        PartitionedRunnable firstTask = mock(PartitionedRunnable.class);
        PartitionedRunnable secondTask = mock(PartitionedRunnable.class);
        when(firstTask.getPartitionKey()).thenReturn(partitionKey);
        when(secondTask.getPartitionKey()).thenReturn(partitionKey);
        PartitionQueue.Callback callback = mock(PartitionQueue.Callback.class);
        trailingThrottledPartitionQueue.addCallback(callback);

        // Enqueue both tasks
        trailingThrottledPartitionQueue.enqueue(firstTask);
        boolean result = trailingThrottledPartitionQueue.enqueue(secondTask);

        // Assert
        assertThat(result).isTrue();
        assertThat(trailingThrottledPartitionQueue.getQueueSize()).isEqualTo(1); // Only the new task should remain

        assertThat(trailingThrottledPartitionQueue.getState())
                .hasSize(1)
                .containsValue(secondTask)
                .containsKey(partitionKey);

        PartitionedRunnable firstInQueue = trailingThrottledPartitionQueue.getNextTask(Duration.ofSeconds(1));
        assertEquals(secondTask, firstInQueue);

        verify(callback, times(1)).onDropped(firstTask);
    }

    @Test
    void getNextTask_shouldReturnTask_whenAvailable() throws InterruptedException {
        when(throttlingFunction.getThrottlingInterval(any())).thenReturn(Duration.ofMillis(20));
        trailingThrottledPartitionQueue.enqueue(mockTask);
        PartitionedRunnable retrievedTask = trailingThrottledPartitionQueue.getNextTask(Duration.ofMillis(40));
        assertThat(retrievedTask).isEqualTo(mockTask);
        assertThat(trailingThrottledPartitionQueue.getQueueSize()).isEqualTo(0);
    }

    @Test
    void getNextTask_shouldReturnNull_whenNoTaskAvailable() throws InterruptedException {
        PartitionedRunnable retrievedTask = trailingThrottledPartitionQueue.getNextTask(Duration.ofMillis(100));
        assertThat(retrievedTask).isNull();
    }

    @Test
    void getNextTask_shouldReturnNull_whenNotAvailableYet() throws InterruptedException {
        when(throttlingFunction.getThrottlingInterval(any())).thenReturn(Duration.ofDays(1));
        trailingThrottledPartitionQueue.enqueue(mockTask);
        PartitionedRunnable retrievedTask = trailingThrottledPartitionQueue.getNextTask(Duration.ofMillis(1));
        // Assert
        assertThat(retrievedTask).isNull();
    }

    @Test
    void getQueueSize_shouldReturnCorrectSize() {
        trailingThrottledPartitionQueue.enqueue(mockTask);
        assertThat(trailingThrottledPartitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void getQueue_shouldReturnAllQueuedTasks() {
        // Arrange
        when(throttlingFunction.getThrottlingInterval(any())).thenReturn(Duration.ofDays(1));
        List<PartitionedRunnable> expectedList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PartitionedRunnable task = mock(PartitionedRunnable.class);
            when(task.getPartitionKey()).thenReturn(i);
            expectedList.add(task);
            trailingThrottledPartitionQueue.enqueue(task);
        }

        // Assert
        assertThat(trailingThrottledPartitionQueue.getQueueSize()).isEqualTo(10);
        assertThat(trailingThrottledPartitionQueue.getQueue()).hasSize(10).containsSequence(expectedList);
    }
}
