package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ThrottledPartitionQueue<T extends PartitionedTask> implements PartitionQueue<T> {
    private final Lock mapLock = new ReentrantLock();

    private final BlockingQueue<DelayedObject> partitionKeyQueue = new DelayQueue<>();
    private final Map<Object, T> taskPerPartitionKeyMap = new HashMap<>();
    private final ThrottlingFunction throttlingFunction;

    private final Set<Callback<T>> callbacks = ConcurrentHashMap.newKeySet();

    public ThrottledPartitionQueue(ThrottlingFunction throttlingFunction) {
        this.throttlingFunction = Objects.requireNonNull(throttlingFunction);
    }

    public ThrottlingFunction getThrottlingFunction() {
        return throttlingFunction;
    }

    public Map<Object, T> getState() {
        return new HashMap<>(taskPerPartitionKeyMap);
    }

    @Override
    public boolean enqueue(T task) {
        Objects.requireNonNull(task);
        Object partitionKey = task.getPartitionKey();

        mapLock.lock();
        try {
            T previousTask = taskPerPartitionKeyMap.put(partitionKey, task);
            if (previousTask != null) {
                onDropped(previousTask);
                return true;
            }
        } finally {
            mapLock.unlock();
        }

        return partitionKeyQueue.add(
                new DelayedObject(partitionKey, throttlingFunction.getThrottlingInterval(partitionKey).toMillis())
        );

    }

    @Override
    public T getNextTask(Duration duration) throws InterruptedException {
        DelayedObject delayedPartitionKey = partitionKeyQueue.poll(duration.toMillis(), TimeUnit.MILLISECONDS);
        mapLock.lock();
        try {
            return delayedPartitionKey == null ? null : taskPerPartitionKeyMap.remove(delayedPartitionKey.getObject());
        } finally {
            mapLock.unlock();
        }
    }

    private void onDropped(T task) {
        callbacks.forEach(c -> c.onDropped(task));
    }

    @Override
    public Queue<T> getQueue() {
        Queue<T> snapshotQueue = new LinkedList<>();

        mapLock.lock();
        try {
            for (DelayedObject d : partitionKeyQueue) {
                T task = taskPerPartitionKeyMap.get(d.getObject());
                if (task != null) {
                    snapshotQueue.add(task);
                }
            }
        } finally {
            mapLock.unlock();
        }

        return snapshotQueue;
    }

    @Override
    public int getQueueSize() {
        return partitionKeyQueue.size();
    }

    @Override
    public void removeCallback(Callback<T> callback) {
        Objects.requireNonNull(callback);
        callbacks.remove(callback);
    }

    @Override
    public void addCallback(Callback<T> callback) {
        Objects.requireNonNull(callback);
        callbacks.add(callback);
    }

    private static class DelayedObject implements Delayed {

        private final Object object;
        private final long startTime;

        public DelayedObject(Object object, long delayInMs) {
            this.startTime = System.currentTimeMillis() + delayInMs;
            this.object = object;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = startTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.startTime, ((DelayedObject) o).startTime);
        }

        public Object getObject() {
            return object;
        }
    }
}
