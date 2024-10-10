package xyz.petnil.partitionedexecutor;

public class PartitionThreadFactoryCreators {
    private PartitionThreadFactoryCreators() {
    }

    public static PartitionThreadFactoryCreator virtualThread(String name) {
        return new VirtualThreadPartitionThreadFactoryCreator(name);
    }
}
