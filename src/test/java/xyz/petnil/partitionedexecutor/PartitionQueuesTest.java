package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionQueuesTest {

    @Test
    void fifo() {
        PartitionQueue<?> partitionQueue = PartitionQueues.fifo(10);
        assertThat(partitionQueue).isInstanceOf(FifoPartitionQueue.class);
        assertThat(((FifoPartitionQueue<?>) partitionQueue).getCapacity()).isEqualTo(10);
    }

    @Test
    void throttles() {
        ThrottlingFunction throttlingFunction = o -> Duration.ZERO;
        PartitionQueue<PartitionedTask> partitionQueue = PartitionQueues.throttled(throttlingFunction);
        assertThat(partitionQueue).isInstanceOf(ThrottledPartitionQueue.class);
        assertThat(((ThrottledPartitionQueue<?>) partitionQueue).getThrottlingFunction()).isEqualTo(throttlingFunction);
    }

}