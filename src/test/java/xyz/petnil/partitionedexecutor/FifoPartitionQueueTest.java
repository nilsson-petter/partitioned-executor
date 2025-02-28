package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.petnil.partitionedexecutor.testdata.TestTask;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static xyz.petnil.partitionedexecutor.testdata.TestTask.TEST_TASK;

class FifoPartitionQueueTest {

    private FifoPartitionQueue<TestTask> partitionQueue;

    @BeforeEach
    void setUp() {
        partitionQueue = new FifoPartitionQueue<>(3); // Queue with a capacity of 3
        partitionQueue.addCallback(mock(PartitionQueue.Callback.class));
        partitionQueue.removeCallback(mock(PartitionQueue.Callback.class));
    }

    @Test
    void queueCapacity() {
        assertThat(partitionQueue.getCapacity()).isEqualTo(3);
    }

    @Test
    void negativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new FifoPartitionQueue<>(0));
    }

    @Test
    void shouldEnqueueTaskSuccessfully() {
        boolean result = partitionQueue.enqueue(TEST_TASK);
        assertThat(result).isTrue();
        assertThat(partitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void queueShouldContainTasks() {
        // Given
        TestTask task1 = new TestTask(1, () -> {
        });
        TestTask task2 = new TestTask(2, () -> {
        });

        // When
        partitionQueue.enqueue(task1);
        partitionQueue.enqueue(task2);

        // Then
        assertThat(partitionQueue.getQueue()).containsExactly(task1, task2);
    }

    @Test
    void shouldReturnFalseWhenEnqueueExceedsCapacity() {
        // Given
        partitionQueue.enqueue(new TestTask(1, () -> {
        }));
        partitionQueue.enqueue(new TestTask(2, () -> {
        }));
        partitionQueue.enqueue(new TestTask(3, () -> {
        }));


        // When
        boolean result = partitionQueue.enqueue(new TestTask(4, () -> {
        }));

        // Then
        assertThat(result).isFalse(); // Queue is full, should return false
        assertThat(partitionQueue.getQueueSize()).isEqualTo(3); // Size remains at max capacity
    }

    @Test
    void shouldThrowExceptionWhenTaskIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> partitionQueue.enqueue(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRetrieveTask() throws InterruptedException {
        // Given
        partitionQueue.enqueue(TEST_TASK);

        // When
        TestTask nextTask = partitionQueue.getNextTask(Duration.ofMillis(1));

        // Then
        assertThat(nextTask).isNotNull();
        assertThat(nextTask).isEqualTo(TEST_TASK);
        assertThat(partitionQueue.getQueueSize()).isEqualTo(0); // Queue should be empty
    }

    @Test
    void shouldReturnCorrectQueueSize() {
        // Given
        partitionQueue.enqueue(TEST_TASK);
        partitionQueue.enqueue(TEST_TASK);

        // When
        int size = partitionQueue.getQueueSize();

        // Then
        assertThat(size).isEqualTo(2);
    }
}
