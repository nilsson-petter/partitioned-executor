package xyz.petnil.partitionedexecutor;

import java.time.Duration;

/**
 * The ThrottlingFunction interface defines a method to determine
 * the throttling interval for a given partition key. It is commonly used
 * in systems where throttling rates are customized based on specific
 * partitioned data or keys.
 */
@FunctionalInterface
public interface ThrottlingFunction {

    /**
     * Returns the throttling interval for the given partition key.
     * The throttling interval is used to control how frequently tasks with the same partition key is being
     * processed.
     * <p>
     * Constraint:
     * <ul>
     *   <li>partitionKey must not be null</li>
     * </ul>
     *
     * @param partitionKey the key used to determine the throttling interval, must not be null
     * @return the throttling time for the given key as a {@link Duration}
     * @throws NullPointerException if partitionKey is null
     */
    Duration getThrottlingInterval(Object partitionKey);
}

