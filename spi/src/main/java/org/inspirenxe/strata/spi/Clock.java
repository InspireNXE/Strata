package org.inspirenxe.strata.spi;

/**
 * Monotonic time source. Core never reads the wall clock itself, so tests can drive time.
 */
@FunctionalInterface
public interface Clock {
    Clock SYSTEM = System::nanoTime;

    long nanoTime();

    default long millis() {
        return this.nanoTime() / 1_000_000L;
    }
}
