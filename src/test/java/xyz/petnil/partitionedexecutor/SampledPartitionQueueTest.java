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

class SampledPartitionQueueTest {
    private static final String PARTITION_KEY = "AAPL";
    private SampledPartitionQueue sampledPartitionQueue;
    private SamplingFunction samplingFunction;
    private PartitionedRunnable mockTask;

    @BeforeEach
    void setUp() {
        samplingFunction = mock(SamplingFunction.class);
        when(samplingFunction.getSamplingTime(any())).thenReturn(Duration.ofMillis(50));
        mockTask = mock(PartitionedRunnable.class);
        when(mockTask.getPartitionKey()).thenReturn(PARTITION_KEY);
        sampledPartitionQueue = new SampledPartitionQueue(samplingFunction);
    }

    @Test
    void enqueue_shouldAddTask_whenNewPartitionKey() {
        boolean result = sampledPartitionQueue.enqueue(mockTask);
        assertThat(result).isTrue();
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void enqueue_taskIsReplacedAndDropped() throws InterruptedException {
        String partitionKey = "TSLA";
        PartitionedRunnable firstTask = mock(PartitionedRunnable.class);
        PartitionedRunnable secondTask = mock(PartitionedRunnable.class);
        when(firstTask.getPartitionKey()).thenReturn(partitionKey);
        when(secondTask.getPartitionKey()).thenReturn(partitionKey);
        PartitionQueue.Callback callback = mock(PartitionQueue.Callback.class);
        sampledPartitionQueue.setCallback(callback);

        // Enqueue both tasks
        sampledPartitionQueue.enqueue(firstTask);
        boolean result = sampledPartitionQueue.enqueue(secondTask);

        // Assert
        assertThat(result).isTrue();
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(1); // Only the new task should remain

        assertThat(sampledPartitionQueue.getState())
                .hasSize(1)
                .containsValue(secondTask)
                .containsKey(partitionKey);

        PartitionedRunnable firstInQueue = sampledPartitionQueue.getNextTask(Duration.ofSeconds(1));
        assertEquals(secondTask, firstInQueue);

        verify(callback, times(1)).onDropped(firstTask);
    }

    @Test
    void getNextTask_shouldReturnTask_whenAvailable() throws InterruptedException {
        when(samplingFunction.getSamplingTime(any())).thenReturn(Duration.ofMillis(20));
        sampledPartitionQueue.enqueue(mockTask);
        PartitionedRunnable retrievedTask = sampledPartitionQueue.getNextTask(Duration.ofMillis(40));
        assertThat(retrievedTask).isEqualTo(mockTask);
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(0);
    }

    @Test
    void getNextTask_shouldReturnNull_whenNoTaskAvailable() throws InterruptedException {
        PartitionedRunnable retrievedTask = sampledPartitionQueue.getNextTask(Duration.ofMillis(100));
        assertThat(retrievedTask).isNull();
    }

    @Test
    void getNextTask_shouldReturnNull_whenNotAvailableYet() throws InterruptedException {
        when(samplingFunction.getSamplingTime(any())).thenReturn(Duration.ofDays(1));
        sampledPartitionQueue.enqueue(mockTask);
        PartitionedRunnable retrievedTask = sampledPartitionQueue.getNextTask(Duration.ofMillis(1));
        // Assert
        assertThat(retrievedTask).isNull();
    }

    @Test
    void getQueueSize_shouldReturnCorrectSize() {
        sampledPartitionQueue.enqueue(mockTask);
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void getQueue_shouldReturnAllQueuedTasks() {
        // Arrange
        when(samplingFunction.getSamplingTime(any())).thenReturn(Duration.ofDays(1));
        List<PartitionedRunnable> expectedList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PartitionedRunnable task = mock(PartitionedRunnable.class);
            when(task.getPartitionKey()).thenReturn(i);
            expectedList.add(task);
            sampledPartitionQueue.enqueue(task);
        }

        // Assert
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(10);
        assertThat(sampledPartitionQueue.getQueue()).hasSize(10).containsSequence(expectedList);
    }
}
