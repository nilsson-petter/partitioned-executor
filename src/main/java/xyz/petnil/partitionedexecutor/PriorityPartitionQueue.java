package xyz.petnil.partitionedexecutor;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class PriorityPartitionQueue implements PartitionQueue {
    private final PriorityBlockingQueue<PriorityTask> taskQueue;
    private final Comparator<PartitionedRunnable> comparator;

    public PriorityPartitionQueue(Comparator<PartitionedRunnable> comparator) {
        this.comparator = comparator;
        taskQueue = new PriorityBlockingQueue<>();
    }

    @Override
    public boolean enqueue(PartitionedRunnable partitionedRunnable) {
        return taskQueue.add(new PriorityTask(partitionedRunnable));
    }

    @Override
    public PartitionedRunnable getNextTask(Duration timeout) throws InterruptedException {
        PriorityTask poll = taskQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (poll != null) {
            return poll.delegate;
        }
        return null;
    }

    @Override
    public void setOnDroppedCallback(OnDroppedCallback callback) {
        // Not implemented
    }

    @Override
    public Queue<PartitionedRunnable> getQueue() {
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
        private final PartitionedRunnable delegate;

        public PriorityTask(PartitionedRunnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public int compareTo(PriorityTask o) {
            return comparator.compare(delegate, o.delegate);
        }
    }

}
