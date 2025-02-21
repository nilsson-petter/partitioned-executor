package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartitionersTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 3, 5, 6})
    void powerOfTwo_illegalArgument(int val) {
        assertThatThrownBy(() -> Partitioners.powerOfTwo(val)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void powerOfTwo() {
        Partitioner partitioner = Partitioners.powerOfTwo(1);
        assertThat(partitioner).isInstanceOf(PowerOfTwoPartitioner.class);
        assertThat(partitioner.getMaxNumberOfPartitions()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void generalPurpose_illegalArgument(int val) {
        assertThatThrownBy(() -> Partitioners.generalPurpose(val)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generalPurpose() {
        Partitioner partitioner = Partitioners.generalPurpose(1);
        assertThat(partitioner).isInstanceOf(GeneralPurposePartitioner.class);
        assertThat(partitioner.getMaxNumberOfPartitions()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16, 32})
    void mostSuitableFor_powerOfTwo(int val) {
        assertThat(Partitioners.mostSuitableFor(val)).isInstanceOf(PowerOfTwoPartitioner.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 6, 7, 9, 10})
    void mostSuitableFor_generalPurpose(int val) {
        assertThat(Partitioners.mostSuitableFor(val)).isInstanceOf(GeneralPurposePartitioner.class);
    }
}