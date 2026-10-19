package org.inspirenxe.strata.spi.testing;

import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks a {@link Platform} against the requirements in its documentation. Extend it in an adapter's tests and
 * return the adapter's platform from {@link #platform()}. Override {@link #hasClipboard()} and
 * {@link #clockRunsOnItsOwn()} when the platform under test has no working clipboard (headless) or a clock the test
 * drives by hand.
 */
public abstract class PlatformContract {

    /**
     * Returns the platform under test. Called once per check.
     *
     * @return the adapter's platform
     */
    protected abstract Platform platform();

    /**
     * Returns whether the clipboard should keep what it is given. The default is true; return false for a platform
     * running headless or with {@link Clipboard#NONE}.
     *
     * @return true if text written to the clipboard must read back
     */
    protected boolean hasClipboard() {
        return true;
    }

    /**
     * Returns whether the clock advances in real time. The default is true; return false for a test clock.
     *
     * @return true if the clock should advance while the test sleeps
     */
    protected boolean clockRunsOnItsOwn() {
        return true;
    }

    @Test
    void providesEveryService() {
        final Platform platform = this.platform();
        assertNotNull(platform.textMetrics(), "textMetrics");
        assertNotNull(platform.clipboard(), "clipboard");
        assertNotNull(platform.clock(), "clock");
        assertNotNull(platform.textInput(), "textInput");
        assertNotNull(platform.capabilities(), "capabilities");
    }

    @Test
    void capabilitiesDoNotChange() {
        final Platform platform = this.platform();
        final Capabilities first = platform.capabilities();
        for (int i = 0; i < 10; i++) {
            assertEquals(first, platform.capabilities());
        }
    }

    @Test
    void textMetricsAgreeBetweenCalls() {
        final Platform platform = this.platform();
        final TextSpan span = TextSpan.of("Strata");
        assertEquals(platform.textMetrics().width(span), platform.textMetrics().width(span), 0f);
    }

    @Test
    void clockNeverGoesBackward() {
        final Clock clock = this.platform().clock();
        long previous = clock.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            final long now = clock.nanoTime();
            assertTrue(now >= previous, "clock went backward");
            previous = now;
        }
        assertEquals(clock.nanoTime() / 1_000_000L, clock.millis(), 1L, "millis is nanoTime in milliseconds");
    }

    @Test
    void clockAdvancesInRealTime() throws InterruptedException {
        assumeTrue(this.clockRunsOnItsOwn(), "the platform's clock is driven by the test");
        final Clock clock = this.platform().clock();
        final long start = clock.millis();
        Thread.sleep(20L);
        assertTrue(clock.millis() - start >= 10L, "clock did not advance while sleeping");
    }

    @Test
    void clipboardNeverThrows() {
        final Clipboard clipboard = this.platform().clipboard();
        assertDoesNotThrow(() -> {
            clipboard.set("");
            clipboard.set("line one\nline two");
            clipboard.set("😀");
            clipboard.get();
        });
    }

    @Test
    void clipboardKeepsWhatItIsGiven() {
        assumeTrue(this.hasClipboard(), "the platform has no working clipboard here");
        final Clipboard clipboard = this.platform().clipboard();
        clipboard.set("strata clipboard check 😀");
        assertEquals(Optional.of("strata clipboard check 😀"), clipboard.get());
    }

    @Test
    void textInputAcceptsRepeatedCalls() {
        final Platform platform = this.platform();
        assertDoesNotThrow(() -> {
            platform.textInput().start(new Rect(0, 0, 100, 20));
            platform.textInput().start(new Rect(10, 10, 100, 20));
            platform.textInput().stop();
            platform.textInput().stop();
        });
    }
}
