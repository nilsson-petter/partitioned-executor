package xyz.petnil.partitionedexecutor;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SingleThreadedPartitionWorkerTest {

    private SingleThreadedPartitionWorker<PartitionedTask> worker;

    @Mock
    private PartitionQueue<PartitionedTask> mockQueue;

    @Mock
    private PartitionedTask mockTask;
    @Mock
    private SingleThreadedPartitionWorker.Callback<PartitionedTask> mockCallback;

    @BeforeEach
    void setUp() throws InterruptedException {
        MockitoAnnotations.openMocks(this);
        ThreadFactory factory = Thread.ofVirtual().factory();
        when(mockQueue.enqueue(any())).thenReturn(true);
        worker = new SingleThreadedPartitionWorker<>(mockQueue, factory);
        worker.addCallback(mockCallback);
    }

    @AfterEach
    void tearDown() throws Exception {
        worker.close();
    }

    @Test
    void testStart() {
        worker.start();
        assertFalse(worker.isShutdown());
        assertFalse(worker.isTerminated());
    }

    @Test
    void testSubmitForExecutionAcceptedTask() {
        worker.submitForExecution(mockTask);

        verify(mockCallback).onSubmitted(mockTask);
    }

    @Test
    void testSubmitForExecutionRejectedTask() {
        when(mockQueue.enqueue(mockTask)).thenReturn(false);
        worker.submitForExecution(mockTask);

        verify(mockCallback).onRejected(mockTask);
    }

    @Test
    void testPollAndProcessSuccess() throws InterruptedException {
        when(mockQueue.getNextTask()).thenReturn(mockTask, (PartitionedTask) null);

        worker.start();

        verify(mockCallback, timeout(1000)).onSuccess(mockTask);
        assertFalse(worker.isShutdown() || worker.isTerminated());
    }

    @Test
    void testShutdown() {
        worker.start();
        worker.shutdown();

        assertTrue(worker.isShutdown());
        Awaitility.await().untilAsserted(() -> assertTrue(worker.isTerminated()));
    }

    @Test
    void testShutdownNow() {
        when(mockQueue.getQueue()).thenReturn(new LinkedList<>(List.of(mockTask)));
        worker.start();

        Queue<PartitionedTask> remainingTasks = worker.shutdownNow();

        assertTrue(worker.isShutdown());
        assertTrue(worker.isTerminated());
        assertThat(remainingTasks).containsExactly(mockTask);
    }

    @Test
    void testAwaitTaskCompletion() throws InterruptedException {
        worker.start();
        worker.shutdown();
        assertTrue(worker.awaitTermination(Duration.ofSeconds(1)));
    }

    @Test
    void testCallbackOnError() throws InterruptedException {
        doThrow(new RuntimeException("Task failed")).when(mockTask).run();
        when(mockQueue.getNextTask()).thenReturn(mockTask, (PartitionedTask) null);
        worker.start();
        verify(mockCallback, timeout(1000)).onError(eq(mockTask), any(RuntimeException.class));
    }

    @Test
    void simulateInterruption() {
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        ThreadFactory tf = r -> {
            Thread start = Thread.ofVirtual().unstarted(r);
            workerThread.set(start);
            return start;
        };

        var partitionWorker = new SingleThreadedPartitionWorker<>(PartitionQueues.fifo(Integer.MAX_VALUE), tf);
        partitionWorker.addCallback(mockCallback);
        partitionWorker.start();
        workerThread.get().interrupt();

        verify(mockCallback, timeout(1000).times(1)).onInterrupted();
        verify(mockCallback, timeout(1000).times(1)).onTerminated();
    }

    @Test
    void interruptPolling() throws Exception {
        var partitionWorker = new SingleThreadedPartitionWorker<>(PartitionQueues.fifo(Integer.MAX_VALUE), Thread.ofPlatform().daemon().name("test").factory());
        Partition.Callback<PartitionedTask> callback = mock(Partition.Callback.class);
        partitionWorker.addCallback(callback);
        partitionWorker.start();
        partitionWorker.close();
        verify(callback).onShutdown();
        verify(callback).onInterrupted();
        verify(callback).onTerminated();
    }

    @Test
    void getPartitionQueue() {
        assertThat(worker.getPartitionQueue()).isEqualTo(mockQueue);
    }

    @Test
    void multipleCallbacks() {
        Partition.Callback<PartitionedTask> cb1 = mock(Partition.Callback.class);
        Partition.Callback<PartitionedTask> cb2 = mock(Partition.Callback.class);
        worker.addCallback(cb1);
        worker.addCallback(cb2);
        worker.start();

        verify(cb1).onStarted();
        verify(cb2).onStarted();

        worker.removeCallback(cb1);
        worker.shutdown();

        verifyNoMoreInteractions(cb1);
        verify(cb2).onShutdown();
    }
}
