package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class TrailingThrottledPartitionQueue implements PartitionQueue {
    private final Lock mainLock = new ReentrantLock();

    private final BlockingQueue<DelayedObject> partitionKeyQueue;
    private final Map<Object, PartitionedRunnable> taskPerPartitionKeyMap;
    private final ThrottlingFunction throttlingFunction;

    private final AtomicReference<Callback> onDroppedCallback = new AtomicReference<>();

    public TrailingThrottledPartitionQueue(ThrottlingFunction throttlingFunction) {
        this.throttlingFunction = Objects.requireNonNull(throttlingFunction);
        this.partitionKeyQueue = new DelayQueue<>();
        this.taskPerPartitionKeyMap = new HashMap<>();
    }

    public ThrottlingFunction getThrottlingFunction() {
        return throttlingFunction;
    }

    public Map<Object, PartitionedRunnable> getState() {
        return new HashMap<>(taskPerPartitionKeyMap);
    }

    @Override
    public boolean enqueue(PartitionedRunnable task) {
        Objects.requireNonNull(task);
        try {
            Object partitionKey = task.getPartitionKey();
            mainLock.lock();
            PartitionedRunnable previousTask = taskPerPartitionKeyMap.put(partitionKey, task);
            if (previousTask == null) {
                return partitionKeyQueue.add(new DelayedObject(partitionKey, throttlingFunction.getThrottlingInterval(partitionKey).toMillis()));
            } else {
                onDropped(previousTask);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public PartitionedRunnable getNextTask(Duration duration) throws InterruptedException {
        Objects.requireNonNull(duration);
        try {
            mainLock.lock();
            DelayedObject delayedPartitionKey = partitionKeyQueue.poll(duration.toMillis(), TimeUnit.MILLISECONDS);
            if (delayedPartitionKey != null) {
                return taskPerPartitionKeyMap.remove(delayedPartitionKey.getObject());
            }
            return null;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void setCallback(Callback callback) {
        onDroppedCallback.set(callback);
    }

    private void onDropped(PartitionedRunnable task) {
        Callback od = onDroppedCallback.get();
        if (od != null) {
            od.onDropped(task);
        }
    }

    @Override
    public Queue<PartitionedRunnable> getQueue() {
        mainLock.lock();
        try {
            Queue<PartitionedRunnable> queue = new LinkedList<>();
            List<DelayedObject> delayedObjects = new ArrayList<>(partitionKeyQueue);
            delayedObjects.forEach(d -> queue.add(taskPerPartitionKeyMap.get(d.object)));
            return queue;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public int getQueueSize() {
        return partitionKeyQueue.size();
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
