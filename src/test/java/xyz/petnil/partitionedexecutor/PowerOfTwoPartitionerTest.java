package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PowerOfTwoPartitionerTest {

    @Test
    void shouldThrowExceptionWhenMaxPartitionsIsLessThanOne() {
        // Given / When / Then
        assertThatThrownBy(() -> new PowerOfTwoPartitioner(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowExceptionWhenMaxPartitionsIsNotPowerOfTwo() {
        // Given / When / Then
        assertThatThrownBy(() -> new PowerOfTwoPartitioner(3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreatePartitionerWhenMaxPartitionsIsPowerOfTwo() {
        // Given / When
        PowerOfTwoPartitioner partitioner = new PowerOfTwoPartitioner(8);

        // Then
        assertThat(partitioner.getMaxNumberOfPartitions()).isEqualTo(8);
    }

    @Test
    void shouldThrowExceptionWhenPartitionKeyIsNull() {
        // Given
        PowerOfTwoPartitioner partitioner = new PowerOfTwoPartitioner(8);

        // When / Then
        assertThatThrownBy(() -> partitioner.getPartition(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldReturnCorrectPartitionBasedOnHashCode() {
        // Given
        PowerOfTwoPartitioner partitioner = new PowerOfTwoPartitioner(8);

        // When / Then
        assertThat(partitioner.getPartition("key1")).isEqualTo("key1".hashCode() & 7);
        assertThat(partitioner.getPartition("key2")).isEqualTo("key2".hashCode() & 7);
        assertThat(partitioner.getPartition(100)).isEqualTo(Integer.hashCode(100) & 7);
    }

    @Test
    void shouldHandleNegativeHashCodes() {
        // Given
        PowerOfTwoPartitioner partitioner = new PowerOfTwoPartitioner(16);

        // When / Then
        Object keyWithNegativeHash = new Object() {
            @Override
            public int hashCode() {
                return -12345;
            }
        };
        assertThat(partitioner.getPartition(keyWithNegativeHash)).isEqualTo(-12345 & 15);
    }
}
