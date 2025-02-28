package xyz.petnil.partitionedexecutor;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.petnil.partitionedexecutor.testdata.TestTask;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static xyz.petnil.partitionedexecutor.testdata.TestTask.newTestTask;

class SingleThreadedPartitionWorkerTest {
    private static final TestTask TEST_TASK = new TestTask(1, () -> {
    });

    private SingleThreadedPartitionWorker<TestTask> worker;
    private PartitionQueue<TestTask> queue;
    private Partition.Callback<TestTask> callback;

    @BeforeEach
    void setUp() throws InterruptedException {
        queue = PartitionQueue.fifo(Integer.MAX_VALUE);
        worker = new SingleThreadedPartitionWorker<>(queue, Thread.ofPlatform().factory());
        callback = mock(Partition.Callback.class);
        worker.addCallback(callback);
    }

    @AfterEach
    void tearDown() throws Exception {
        //worker.close();
    }

    @Test
    void testStart() {
        assertFalse(worker.isShutdown());
        assertFalse(worker.isTerminated());
    }

    @Test
    void testSubmitForExecutionAcceptedTask() {
        worker.submitForExecution(TEST_TASK);
        verify(callback).onSubmitted(TEST_TASK);
    }

    @Test
    void testSubmitForExecutionRejectedTask() {
        worker = new SingleThreadedPartitionWorker<>(PartitionQueue.fifo(1), Thread.ofPlatform().factory());
        worker.addCallback(callback);
        var task1 = newTestTask(1);
        task1.halt();

        var task2 = newTestTask(2);
        var task3 = newTestTask(3);

        worker.submitForExecution(task1);
        Awaitility.await().until(() -> worker.getPartitionQueue().getQueueSize() == 0);

        worker.submitForExecution(task2);
        worker.submitForExecution(task3);
        verify(callback).onSubmitted(task1);
        verify(callback).onSubmitted(task2);
        verify(callback).onRejected(task3);
        task1.proceed();
    }

    @Test
    void testPollAndProcessSuccess() throws InterruptedException {
        worker.submitForExecution(TEST_TASK);
        verify(callback, timeout(1000)).onSuccess(TEST_TASK);
        assertFalse(worker.isShutdown() || worker.isTerminated());
    }

    @Test
    void testShutdown() {
        worker.shutdown();
        assertTrue(worker.isShutdown());
        Awaitility.await().untilAsserted(() -> assertTrue(worker.isTerminated()));
    }

    @Test
    void testAwaitTaskCompletion() throws InterruptedException {
        worker.submitForExecution(TEST_TASK);
        worker.shutdown();
        assertTrue(worker.awaitTermination(Duration.ofSeconds(5)));
    }

    @Test
    void testCallbackOnError() throws InterruptedException {
        TestTask taskFailed = new TestTask(1, () -> {
            throw new RuntimeException("Task failed");
        });
        worker.submitForExecution(taskFailed);
        verify(callback, timeout(1000)).onError(eq(taskFailed), any(RuntimeException.class));
    }

    @Test
    void interruptPolling() throws Exception {
        var partitionWorker = new SingleThreadedPartitionWorker<>(PartitionQueue.fifo(Integer.MAX_VALUE), Thread.ofPlatform().daemon().name("test").factory());
        Partition.Callback<PartitionedTask> callback = mock(Partition.Callback.class);
        partitionWorker.addCallback(callback);
        partitionWorker.close();
        verify(callback).onShutdown();
        verify(callback).onTerminated();
    }

    @Test
    void getPartitionQueue() {
        assertThat(worker.getPartitionQueue()).isEqualTo(queue);
    }

    @Test
    void multipleCallbacks() {
        Partition.Callback<TestTask> cb1 = mock(Partition.Callback.class);
        Partition.Callback<TestTask> cb2 = mock(Partition.Callback.class);
        worker.addCallback(cb1);
        worker.addCallback(cb2);
        worker.submitForExecution(TEST_TASK);
        verify(cb1).onSubmitted(TEST_TASK);
        verify(cb2).onSubmitted(TEST_TASK);

        worker.removeCallback(cb1);
        reset(cb1);
        worker.shutdown();

        verifyNoMoreInteractions(cb1);
        verify(cb2).onShutdown();
    }

    @Test
    void autoCloseable() {
        try (SingleThreadedPartitionWorker<TestTask> worker = new SingleThreadedPartitionWorker<>(PartitionQueue.fifo(Integer.MAX_VALUE), Thread.ofPlatform().daemon().name("partition").factory())) {
        }
    }

    @Test
    void autoCloseable_sampledQueue() {
        try (SingleThreadedPartitionWorker<TestTask> worker = new SingleThreadedPartitionWorker<>(PartitionQueue.throttled(i -> Duration.ofMillis(200)), Thread.ofPlatform().name("partition").factory())) {
            worker.addCallback(callback);
            worker.submitForExecution(TEST_TASK);
            worker.submitForExecution(TEST_TASK);
            worker.submitForExecution(TEST_TASK);
        }
        verify(callback, times(3)).onSubmitted(TEST_TASK);
        verify(callback, times(2)).onDropped(TEST_TASK);
        verify(callback, times(1)).onSuccess(TEST_TASK);
        verify(callback, times(1)).onShutdown();
        verify(callback, times(1)).onTerminated();
        verifyNoMoreInteractions(callback);
    }

    @Test
    void close() {
        TestTask testTask = newTestTask();
        testTask.halt();
        worker.submitForExecution(testTask);
        new Thread(worker::close).start();
        testTask.proceed();
        Awaitility.await().until(worker::isTerminated);
    }
}
