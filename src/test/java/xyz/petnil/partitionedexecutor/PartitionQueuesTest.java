package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

class PartitionQueuesTest {

    @Test
    void unboundedFifo() {
        PartitionQueue partitionQueue = PartitionQueues.unboundedFifo();
        assertThat(partitionQueue).isInstanceOf(UnboundedFifoPartitionQueue.class);
    }

    @Test
    void boundedFifo() {
        PartitionQueue partitionQueue = PartitionQueues.boundedFifo(10);
        assertThat(partitionQueue).isInstanceOf(BoundedFifoPartitionQueue.class);
        assertThat(((BoundedFifoPartitionQueue) partitionQueue).getCapacity()).isEqualTo(10);
    }

    @Test
    void sampled() {
        SamplingFunction samplingFunction = o -> Duration.ZERO;
        PartitionQueue partitionQueue = PartitionQueues.sampled(samplingFunction);
        assertThat(partitionQueue).isInstanceOf(SampledPartitionQueue.class);
        assertThat(((SampledPartitionQueue) partitionQueue).getSamplingFunction()).isEqualTo(samplingFunction);
    }

    @Test
    void priority() {
        Comparator<PartitionedRunnable> comparator = (p1, p2) -> 0;
        PartitionQueue partitionQueue = PartitionQueues.priority(comparator);
        assertThat(partitionQueue).isInstanceOf(PriorityPartitionQueue.class);
        assertThat(((PriorityPartitionQueue) partitionQueue).getComparator()).isEqualTo(comparator);
    }
}