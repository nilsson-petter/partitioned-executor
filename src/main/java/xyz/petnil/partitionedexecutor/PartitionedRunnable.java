package xyz.petnil.partitionedexecutor;

public interface PartitionedRunnable extends Runnable {
    @Override
    default void run() {
        getDelegate().run();
    }

    Object getPartitionKey();
    Runnable getDelegate();
}
