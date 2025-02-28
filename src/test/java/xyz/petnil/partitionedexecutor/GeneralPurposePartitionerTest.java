package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneralPurposePartitionerTest {

    private GeneralPurposePartitioner partitioner;

    @BeforeEach
    void setUp() {
        partitioner = new GeneralPurposePartitioner(5);  // Example setup with 5 partitions
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException if maxPartitions is less than 1")
    void constructor_shouldThrowException_whenMaxPartitionsLessThanOne() {
        // when / then
        assertThatThrownBy(() -> new GeneralPurposePartitioner(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getMaxNumberOfPartitions should return maxPartitions")
    void getMaxNumberOfPartitions_shouldReturnMaxPartitions() {
        assertThat(partitioner.getMaxNumberOfPartitions()).isEqualTo(5);
    }

    @Test
    @DisplayName("getPartition should return correct partition for a given key")
    void getPartition_shouldReturnCorrectPartition() {
        // given
        Object key1 = "test-key-1";
        Object key2 = 12345;

        // when
        int partition1 = partitioner.getPartition(key1);
        int partition2 = partitioner.getPartition(key2);

        // then
        assertThat(partition1).isGreaterThanOrEqualTo(0).isLessThan(5);
        assertThat(partition2).isGreaterThanOrEqualTo(0).isLessThan(5);
    }

    @Test
    @DisplayName("getPartition should throw NullPointerException when partitionKey is null")
    void getPartition_shouldThrowException_whenPartitionKeyIsNull() {
        // when / then
        assertThatThrownBy(() -> partitioner.getPartition(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getPartition should handle Integer.MIN_VALUE correctly")
    void getPartition_shouldHandleIntegerMinValue() {
        // given
        Object key = Integer.MIN_VALUE;

        // when
        int partition = partitioner.getPartition(key);

        // then
        assertThat(partition).isGreaterThanOrEqualTo(0).isLessThan(5);
    }

    @Test
    @DisplayName("getPartition should handle negative hash codes correctly")
    void getPartition_shouldHandleNegativeHashCodes() {
        // given
        Object key = new Object() {
            @Override
            public int hashCode() {
                return -12345;
            }
        };

        // when
        int partition = partitioner.getPartition(key);

        // then
        assertThat(partition).isGreaterThanOrEqualTo(0).isLessThan(5);
    }

    @Test
    @DisplayName("getPartition should return the same partition for the same hashCode")
    void getPartition_samePartitionForSameHashCode() {
        // given
        Object key1 = new Object() {
            @Override
            public int hashCode() {
                return 123;
            }
        };

        Object key2 = new Object() {
            @Override
            public int hashCode() {
                return 123;
            }
        };

        // when
        int partition1 = partitioner.getPartition(key1);
        int partition2 = partitioner.getPartition(key2);

        // then
        assertThat(partition1).isEqualTo(partition2);  // Same hash code, same partition
    }

    @Test
    void getPartition_allPartitionsAreUsed() {
        Map<Integer, AtomicInteger> frequencyMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            frequencyMap.computeIfAbsent(partitioner.getPartition(i), j -> new AtomicInteger(0)).incrementAndGet();
        }

        assertThat(frequencyMap).hasSize(5);
        assertThat(frequencyMap.values().stream().map(AtomicInteger::get).toList()).allMatch(i -> i == 200);
    }
}
