package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityPartitionQueueTest {

    private PriorityPartitionQueue queue;

    @BeforeEach
    void setUp() {
        Comparator<PartitionedTask> comparator = (o1, o2) -> {
            if (o1 instanceof PriorityPartitionedTask o1p && o2 instanceof PriorityPartitionedTask o2p) {
                return o1p.priority - o2p.priority;
            }
            return 0;
        };
        queue = new PriorityPartitionQueue(comparator);
        assertThat(queue.getComparator()).isEqualTo(comparator);
    }

    @Test
    void callbacksDoNothing() {
        PartitionQueue.Callback cb = task -> {
        };
        queue.addCallback(cb);
        queue.removeCallback(cb);

    }

    @Test
    void nullTaskOnTimeout() throws InterruptedException {
        var nextTask = queue.getNextTask(Duration.ofMillis(1));
        assertThat(nextTask).isNull();
    }

    @Test
    void emptyQueue() {
        assertThat(queue.getQueueSize()).isZero();
        assertThat(queue.getQueue()).isEmpty();
    }

    @Test
    void addAndGet() throws InterruptedException {
        var task1 = new PriorityPartitionedTask(1, 1);
        queue.enqueue(task1);
        var nextTask = queue.getNextTask(Duration.ofSeconds(1));

        assertThat(nextTask).isEqualTo(task1);
    }

    @Test
    void priorityIsRespected() throws InterruptedException {
        var task1 = new PriorityPartitionedTask(1, 1);
        var task2 = new PriorityPartitionedTask(2, 2);
        queue.enqueue(task2);
        queue.enqueue(task1);
        assertThat(queue.getQueueSize()).isEqualTo(2);
        assertThat(queue.getQueue()).hasSize(2);
        assertThat(queue.getQueue()).containsExactly(task1, task2);
        var nextTask = queue.getNextTask(Duration.ofSeconds(1));

        assertThat(nextTask).isEqualTo(task1);
    }

    private static class PriorityPartitionedTask implements PartitionedTask {

        private final Object partitionKey;
        private final int priority;

        public PriorityPartitionedTask(Object partitionKey, int priority) {
            this.partitionKey = partitionKey;
            this.priority = priority;
        }

        @Override
        public Object getPartitionKey() {
            return partitionKey;
        }

        @Override
        public Runnable getDelegate() {
            return () -> {
            };
        }

    }

}