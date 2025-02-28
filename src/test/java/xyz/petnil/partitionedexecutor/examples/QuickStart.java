package xyz.petnil.partitionedexecutor.examples;

import xyz.petnil.partitionedexecutor.PartitionedExecutor;
import xyz.petnil.partitionedexecutor.PartitionedExecutors;

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
            executor.execute(new PersistStockQuoteTask("AAPL", BigDecimal.valueOf(130.3d)));
            executor.execute(new PersistStockQuoteTask("MSFT", BigDecimal.valueOf(209.83d)));
            executor.execute(new PersistStockQuoteTask("MSFT", BigDecimal.valueOf(208.51d)));
            executor.execute(new PersistStockQuoteTask("AAPL", BigDecimal.valueOf(131.3d)));
        }

    }

}
