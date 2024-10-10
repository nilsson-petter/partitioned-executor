package xyz.petnil.partitionedexecutor;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;

class VirtualThreadPartitionThreadFactoryCreator implements PartitionThreadFactoryCreator {
    private final String name;

    public VirtualThreadPartitionThreadFactoryCreator(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public ThreadFactory createThreadFactory(int partitionNumber) {
        return Thread.ofVirtual().name(name + "-" + partitionNumber).factory();
    }
}
