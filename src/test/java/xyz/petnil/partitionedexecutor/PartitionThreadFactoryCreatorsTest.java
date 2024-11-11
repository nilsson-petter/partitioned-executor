package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartitionThreadFactoryCreatorsTest {

    @Test
    void virtualThread() {
        var prefix = "test";
        var number = 1;
        PartitionThreadFactoryCreator partitionThreadFactoryCreator = PartitionThreadFactoryCreators.virtualThread(prefix);
        ThreadFactory threadFactory = partitionThreadFactoryCreator.createThreadFactory(1);

        Thread thread = threadFactory.newThread(() -> {
        });

        assertThat(thread.isVirtual()).isTrue();
        assertThat(thread.getName()).isEqualTo(prefix + "-" + number);
    }

    @Test
    void virtualThread_nullCheck() {
        assertThatThrownBy(() -> PartitionThreadFactoryCreators.virtualThread(null)).isInstanceOf(NullPointerException.class);
    }

}