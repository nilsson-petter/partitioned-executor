package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SampledPartitionQueue implements PartitionQueue {
    private final Lock mainLock = new ReentrantLock();

    private final BlockingQueue<DelayedObject> partitionKeyQueue;
    private final Map<Object, PartitionedRunnable> taskPerPartitionKeyMap;
    private final SamplingFunction samplingFunction;

    private final AtomicReference<OnDroppedCallback> onDroppedCallback = new AtomicReference<>();

    public SampledPartitionQueue(SamplingFunction samplingFunction) {
        this.samplingFunction = samplingFunction;
        this.partitionKeyQueue = new DelayQueue<>();
        this.taskPerPartitionKeyMap = new HashMap<>();
    }

    @Override
    public boolean enqueue(PartitionedRunnable partitionedRunnable) {
        try {
            Object partitionKey = partitionedRunnable.getPartitionKey();
            mainLock.lock();
            PartitionedRunnable previousTask = taskPerPartitionKeyMap.put(partitionKey, partitionedRunnable);
            if (previousTask == null) {
                return partitionKeyQueue.add(new DelayedObject(partitionKey, samplingFunction.getSamplingTime(partitionKey).toMillis()));
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
    public void setOnDroppedCallback(OnDroppedCallback callback) {
        onDroppedCallback.set(callback);
    }

    private void onDropped(PartitionedRunnable task) {
        OnDroppedCallback od = onDroppedCallback.get();
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
