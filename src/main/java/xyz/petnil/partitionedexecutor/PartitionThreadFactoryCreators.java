package xyz.petnil.partitionedexecutor;

import java.util.Objects;

public class PartitionThreadFactoryCreators {
    private PartitionThreadFactoryCreators() {
    }

    public static PartitionThreadFactoryCreator virtualThread(String name) {
        Objects.requireNonNull(name);
        return i -> Thread.ofVirtual().name(name + "-" + i).factory();
    }
}
