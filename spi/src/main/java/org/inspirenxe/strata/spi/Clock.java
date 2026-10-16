package org.inspirenxe.strata.spi;

/**
 * A monotonic time source. Strata never reads the system clock itself, so tests can drive time.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Never go backward. Only differences between readings matter; the zero point is arbitrary.</li>
 *     <li>Advance in real time while the UI runs, or click counting, caret blinking and key repeat stall.</li>
 * </ul>
 */
@FunctionalInterface
public interface Clock {
    /**
     * The JVM's monotonic clock, {@link System#nanoTime()}.
     */
    Clock SYSTEM = System::nanoTime;

    /**
     * Reads the clock.
     *
     * @return nanoseconds since an arbitrary fixed point
     */
    long nanoTime();

    /**
     * Reads the clock in milliseconds.
     *
     * @return {@link #nanoTime()} divided by one million
     */
    default long millis() {
        return this.nanoTime() / 1_000_000L;
    }
}
