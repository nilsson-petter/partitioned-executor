package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartitionersTest {

    @Test
    void powerOfTwo_illegalArgument() {
        assertThatThrownBy(() -> Partitioners.powerOfTwo(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Partitioners.powerOfTwo(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Partitioners.powerOfTwo(3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Partitioners.powerOfTwo(5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Partitioners.powerOfTwo(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void powerOfTwo() {
        Partitioner partitioner = Partitioners.powerOfTwo(1);
        assertThat(partitioner).isInstanceOf(PowerOfTwoPartitioner.class);
        assertThat(partitioner.getMaxNumberOfPartitions()).isEqualTo(1);
    }

    @Test
    void generalPurpose_illegalArgument() {
        assertThatThrownBy(() -> Partitioners.generalPurpose(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Partitioners.generalPurpose(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generalPurpose() {
        Partitioner partitioner = Partitioners.generalPurpose(1);
        assertThat(partitioner).isInstanceOf(GeneralPurposePartitioner.class);
        assertThat(partitioner.getMaxNumberOfPartitions()).isEqualTo(1);
    }
}