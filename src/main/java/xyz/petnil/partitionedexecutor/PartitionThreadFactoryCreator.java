package xyz.petnil.partitionedexecutor;

import java.util.concurrent.ThreadFactory;

public interface PartitionThreadFactoryCreator {
    ThreadFactory createThreadFactory(int partitionNumber);
}
