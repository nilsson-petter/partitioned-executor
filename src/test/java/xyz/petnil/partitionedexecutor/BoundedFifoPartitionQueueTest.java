package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class BoundedFifoPartitionQueueTest {

    private BoundedFifoPartitionQueue partitionQueue;

    @BeforeEach
    void setUp() {
        partitionQueue = new BoundedFifoPartitionQueue(3); // Queue with a capacity of 3
    }

    @Test
    void shouldEnqueueTaskSuccessfully() {
        // Given
        PartitionedRunnable task = createTask("key1");

        // When
        boolean result = partitionQueue.enqueue(task);

        // Then
        assertThat(result).isTrue();
        assertThat(partitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void queueShouldContainTasks() {
        // Given
        PartitionedRunnable task1 = createTask("key1");
        PartitionedRunnable task2 = createTask("key1");

        // When
        partitionQueue.enqueue(task1);
        partitionQueue.enqueue(task2);

        // Then
        assertThat(partitionQueue.getQueue()).containsExactly(task1, task2);
    }

    @Test
    void shouldReturnFalseWhenEnqueueExceedsCapacity() {
        // Given
        partitionQueue.enqueue(createTask("key1"));
        partitionQueue.enqueue(createTask("key2"));
        partitionQueue.enqueue(createTask("key3"));

        // When
        boolean result = partitionQueue.enqueue(createTask("key4"));

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
    void shouldRetrieveTaskWithTimeout() throws InterruptedException {
        // Given
        PartitionedRunnable task = createTask("key1");
        partitionQueue.enqueue(task);

        // When
        PartitionedRunnable nextTask = partitionQueue.getNextTask(Duration.ofMillis(500));

        // Then
        assertThat(nextTask).isNotNull();
        assertThat(nextTask.getPartitionKey()).isEqualTo("key1");
        assertThat(partitionQueue.getQueueSize()).isEqualTo(0); // Queue should be empty
    }

    @Test
    void shouldReturnNullWhenTimeoutExpiresWithoutTask() throws InterruptedException {
        // When
        PartitionedRunnable nextTask = partitionQueue.getNextTask(Duration.ofMillis(500));

        // Then
        assertThat(nextTask).isNull(); // No tasks, should return null
    }

    @Test
    void shouldThrowExceptionWhenTimeoutIsNull() {
        // Given / When / Then
        assertThatThrownBy(() -> partitionQueue.getNextTask(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldReturnCorrectQueueSize() {
        // Given
        partitionQueue.enqueue(createTask("key1"));
        partitionQueue.enqueue(createTask("key2"));

        // When
        int size = partitionQueue.getQueueSize();

        // Then
        assertThat(size).isEqualTo(2);
    }

    // Helper method to create a PartitionedRunnable
    private PartitionedRunnable createTask(String partitionKey) {
        return new PartitionedRunnable() {
            @Override
            public Object getPartitionKey() {
                return partitionKey;
            }

            @Override
            public Runnable getDelegate() {
                return () -> System.out.println("Task executed for " + partitionKey);
            }
        };
    }
}
