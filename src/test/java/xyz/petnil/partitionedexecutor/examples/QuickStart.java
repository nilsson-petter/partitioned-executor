package xyz.petnil.partitionedexecutor.examples;

import xyz.petnil.partitionedexecutor.PartitionedExecutor;
import xyz.petnil.partitionedexecutor.PartitionedExecutors;
import xyz.petnil.partitionedexecutor.PartitionedTask;

import java.math.BigDecimal;

public class QuickStart implements Runnable {

    public static void main(String[] args) {
        QuickStart quickStart = new QuickStart();
        quickStart.run();
    }

    @Override
    public void run() {
        // An executor with FIFO semantics, 32 partitions and a maximum queue size of 10 000.
        try (PartitionedExecutor<PersistStockQuoteTask> executor = PartitionedExecutors.fifo(32, 10_000)) {
            // Persist four stock quotes, two for AAPL and two for MSFT.
            var task1 = new PersistStockQuoteTask("AAPL", () -> persistQuote("AAPL", BigDecimal.valueOf(130.3d)));
            var task2 = new PersistStockQuoteTask("MSFT", () -> persistQuote("MSFT", BigDecimal.valueOf(209.83d)));
            var task3 = new PersistStockQuoteTask("MSFT", () -> persistQuote("MSFT", BigDecimal.valueOf(208.51d)));
            var task4 = new PersistStockQuoteTask("AAPL", () -> persistQuote("AAPL", BigDecimal.valueOf(131.3d)));
            executor.execute(task1);
            executor.execute(task2);
            executor.execute(task3);
            executor.execute(task4);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void persistQuote(String ticker, BigDecimal lastPrice) {
        System.out.println(Thread.currentThread().getName() + "|" + ticker + "|" + lastPrice);
    }

    private record PersistStockQuoteTask(String ticker, Runnable task) implements PartitionedTask {
        @Override
        public Object getPartitionKey() {
            return ticker;
        }

        @Override
        public Runnable getDelegate() {
            return task;
        }
    }
}
