package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionQueuesTest {

    @Test
    void fifo() {
        PartitionQueue partitionQueue = PartitionQueues.fifo(10);
        assertThat(partitionQueue).isInstanceOf(FifoPartitionQueue.class);
        assertThat(((FifoPartitionQueue) partitionQueue).getCapacity()).isEqualTo(10);
    }

    @Test
    void trailingThrottled() {
        ThrottlingFunction throttlingFunction = o -> Duration.ZERO;
        PartitionQueue partitionQueue = PartitionQueues.trailingThrottled(throttlingFunction);
        assertThat(partitionQueue).isInstanceOf(TrailingThrottledPartitionQueue.class);
        assertThat(((TrailingThrottledPartitionQueue) partitionQueue).getThrottlingFunction()).isEqualTo(throttlingFunction);
    }

    @Test
    void priority() {
        Comparator<PartitionedRunnable> comparator = (p1, p2) -> 0;
        PartitionQueue partitionQueue = PartitionQueues.priority(comparator);
        assertThat(partitionQueue).isInstanceOf(PriorityPartitionQueue.class);
        assertThat(((PriorityPartitionQueue) partitionQueue).getComparator()).isEqualTo(comparator);
    }
}