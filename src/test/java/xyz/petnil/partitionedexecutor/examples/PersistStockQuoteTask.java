package xyz.petnil.partitionedexecutor.examples;

import xyz.petnil.partitionedexecutor.PartitionedTask;

import java.math.BigDecimal;

record PersistStockQuoteTask(String ticker, BigDecimal lastPrice) implements PartitionedTask {
    @Override
    public Object getPartitionKey() {
        return ticker;
    }

    @Override
    public Runnable getDelegate() {
        return () -> System.out.println(Thread.currentThread().getName() + "|" + ticker + "|" + lastPrice);
    }
}
