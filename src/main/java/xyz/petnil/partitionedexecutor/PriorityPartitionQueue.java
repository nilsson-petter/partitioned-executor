package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class PriorityPartitionQueue implements PartitionQueue {
    private final PriorityBlockingQueue<PriorityTask> taskQueue;

    private final Comparator<PartitionedTask> comparator;

    public PriorityPartitionQueue(Comparator<PartitionedTask> comparator) {
        this.comparator = Objects.requireNonNull(comparator);
        this.taskQueue = new PriorityBlockingQueue<>();
    }

    public Comparator<PartitionedTask> getComparator() {
        return comparator;
    }

    @Override
    public boolean enqueue(PartitionedTask task) {
        Objects.requireNonNull(task);
        return taskQueue.add(new PriorityTask(task));
    }

    @Override
    public PartitionedTask getNextTask(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout);
        PriorityTask poll = taskQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (poll != null) {
            return poll.delegate;
        }
        return null;
    }

    @Override
    public void removeCallback(Callback callback) {
        // Not implemented
    }

    @Override
    public void addCallback(Callback callback) {
        // Not implemented
    }

    @Override
    public Queue<PartitionedTask> getQueue() {
        return new LinkedList<>(taskQueue)
                .stream()
                .map(o -> o.delegate)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public int getQueueSize() {
        return taskQueue.size();
    }

    private final class PriorityTask implements Comparable<PriorityTask> {
        private final PartitionedTask delegate;

        public PriorityTask(PartitionedTask delegate) {
            this.delegate = delegate;
        }

        @Override
        public int compareTo(PriorityTask o) {
            return comparator.compare(delegate, o.delegate);
        }
    }

}
