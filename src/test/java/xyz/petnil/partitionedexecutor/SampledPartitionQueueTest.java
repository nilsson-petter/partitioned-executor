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
    private SampledPartitionQueue sampledPartitionQueue;
    private SamplingFunction samplingFunction;
    private PartitionedRunnable mockTask;

    @BeforeEach
    void setUp() {
        samplingFunction = mock(SamplingFunction.class);
        sampledPartitionQueue = new SampledPartitionQueue(samplingFunction);
        mockTask = mock(PartitionedRunnable.class);
    }

    @Test
    void enqueue_shouldAddTask_whenNewPartitionKey() {
        // Arrange
        Object partitionKey = "key1";
        when(mockTask.getPartitionKey()).thenReturn(partitionKey);
        when(samplingFunction.getSamplingTime(partitionKey)).thenReturn(Duration.ofMillis(100));

        // Act
        boolean result = sampledPartitionQueue.enqueue(mockTask);

        // Assert
        assertThat(result).isTrue();
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void enqueue_shouldReplaceExistingTask_whenSamePartitionKey() throws InterruptedException {
        // Arrange
        Object partitionKey = "key1";
        PartitionedRunnable firstTask = mock(PartitionedRunnable.class);
        PartitionedRunnable secondTask = mock(PartitionedRunnable.class);
        when(firstTask.getPartitionKey()).thenReturn(partitionKey);
        when(secondTask.getPartitionKey()).thenReturn(partitionKey);
        when(samplingFunction.getSamplingTime(partitionKey)).thenReturn(Duration.ofMillis(100));
        PartitionQueue.OnDroppedCallback onDroppedCallback = mock(PartitionQueue.OnDroppedCallback.class);
        sampledPartitionQueue.setOnDroppedCallback(onDroppedCallback);

        // Enqueue both tasks
        sampledPartitionQueue.enqueue(firstTask);
        boolean result = sampledPartitionQueue.enqueue(secondTask);

        // Assert
        assertThat(result).isTrue();
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(1); // Only the new task should remain

        PartitionedRunnable firstInQueue = sampledPartitionQueue.getNextTask(Duration.ofSeconds(1));
        assertEquals(secondTask, firstInQueue);

        // Verify that the previous task was dropped
        verify(onDroppedCallback, times(1)).onDropped(firstTask);
    }

    @Test
    void getNextTask_shouldReturnTask_whenAvailable() throws InterruptedException {
        // Arrange
        Object partitionKey = "key1";
        when(mockTask.getPartitionKey()).thenReturn(partitionKey);
        when(samplingFunction.getSamplingTime(partitionKey)).thenReturn(Duration.ofMillis(100));
        sampledPartitionQueue.enqueue(mockTask);

        // Act
        PartitionedRunnable retrievedTask = sampledPartitionQueue.getNextTask(Duration.ofMillis(150));

        // Assert
        assertThat(retrievedTask).isEqualTo(mockTask);
        assertThat(sampledPartitionQueue.getQueueSize()).isEqualTo(0);
    }

    @Test
    void getNextTask_shouldReturnNull_whenNoTaskAvailable() throws InterruptedException {
        // Act
        PartitionedRunnable retrievedTask = sampledPartitionQueue.getNextTask(Duration.ofMillis(100));

        // Assert
        assertThat(retrievedTask).isNull();
    }

    @Test
    void getQueueSize_shouldReturnCorrectSize() {
        // Arrange
        when(mockTask.getPartitionKey()).thenReturn("key1");
        when(samplingFunction.getSamplingTime("key1")).thenReturn(Duration.ofMillis(100));

        sampledPartitionQueue.enqueue(mockTask);

        // Act
        int size = sampledPartitionQueue.getQueueSize();

        // Assert
        assertThat(size).isEqualTo(1);
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
