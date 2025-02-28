package xyz.petnil.partitionedexecutor.testdata;

import xyz.petnil.partitionedexecutor.PartitionedTask;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestTask implements PartitionedTask {
    public static final TestTask TEST_TASK = newTestTask();

    private final int partitionKey;
    private final Runnable delegate;
    private final Lock lock = new ReentrantLock();

    public TestTask(int partitionKey, Runnable delegate) {
        this.partitionKey = partitionKey;
        this.delegate = delegate;
    }

    public static TestTask newTestTask() {
        return newTestTask(1);
    }

    public static TestTask newTestTask(int partitionKey) {
        return new TestTask(partitionKey, () -> {});
    }

    @Override
    public Object getPartitionKey() {
        return partitionKey;
    }

    public void halt() {
        lock.lock();
    }

    public void proceed() {
        lock.unlock();
    }

    @Override
    public Runnable getDelegate() {
        return () -> {
            halt();
            proceed();
            delegate.run();
        };
    }

    @Override
    public String toString() {
        return "TestTask{" +
                "partitionKey=" + partitionKey +
                ", delegate=" + delegate +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TestTask testTask = (TestTask) object;
        return partitionKey == testTask.partitionKey && Objects.equals(delegate, testTask.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitionKey, delegate);
    }
}
