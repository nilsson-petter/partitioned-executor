package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FifoPartitionQueueTest {

    private FifoPartitionQueue<PartitionedTask> partitionQueue;

    @BeforeEach
    void setUp() {
        partitionQueue = new FifoPartitionQueue<>(3); // Queue with a capacity of 3
    }

    @Test
    void negativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new FifoPartitionQueue<>(0));
    }

    @Test
    void shouldEnqueueTaskSuccessfully() {
        // Given
        PartitionedTask task = createTask("key1");

        // When
        boolean result = partitionQueue.enqueue(task);

        // Then
        assertThat(result).isTrue();
        assertThat(partitionQueue.getQueueSize()).isEqualTo(1);
    }

    @Test
    void queueShouldContainTasks() {
        // Given
        PartitionedTask task1 = createTask("key1");
        PartitionedTask task2 = createTask("key1");

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
    void shouldRetrieveTask() throws InterruptedException {
        // Given
        PartitionedTask task = createTask("key1");
        partitionQueue.enqueue(task);

        // When
        PartitionedTask nextTask = partitionQueue.getNextTask();

        // Then
        assertThat(nextTask).isNotNull();
        assertThat(nextTask.getPartitionKey()).isEqualTo("key1");
        assertThat(partitionQueue.getQueueSize()).isEqualTo(0); // Queue should be empty
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

    private PartitionedTask createTask(String partitionKey) {
        return new PartitionedTask() {
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
