package xyz.petnil.partitionedexecutor;

public class PartitionThreadFactoryCreators {
    private PartitionThreadFactoryCreators() {
    }

    public static PartitionThreadFactoryCreator virtualThread(String name) {
        return i -> Thread.ofVirtual().name(name + "-" + i).factory();
    }
}
