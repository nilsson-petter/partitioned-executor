package xyz.petnil.partitionedexecutor;

import java.time.Duration;

/**
 * The SamplingFunction interface defines a method to determine
 * the sampling time for a given partition key. It is commonly used
 * in systems where sampling rates are customized based on specific
 * partitioned data or keys.
 */
@FunctionalInterface
public interface SamplingFunction {

    /**
     * Returns the sampling time for the given partition key.
     * The sampling time is used to control how frequently data is sampled
     * for a specific partition key.
     * <p>
     * Constraint:
     * <ul>
     *   <li>partitionKey must not be null</li>
     * </ul>
     *
     * @param partitionKey the key used to determine the sampling time, must not be null
     * @return the sampling time for the given key as a {@link Duration}
     * @throws NullPointerException if partitionKey is null
     */
    Duration getSamplingTime(Object partitionKey);
}

