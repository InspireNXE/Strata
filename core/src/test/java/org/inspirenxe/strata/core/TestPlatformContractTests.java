package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.testing.CanvasContract;
import org.inspirenxe.strata.spi.testing.PlatformContract;
import org.inspirenxe.strata.spi.testing.TextMetricsContract;
import org.junit.jupiter.api.Nested;

/**
 * Runs the SPI contracts against the test doubles core's tests use, so a test that passes here proves something
 * about real adapters too. It also exercises the contract kit's non-readable canvas path.
 */
class TestPlatformContractTests {

    @Nested
    class PlatformTest extends PlatformContract {
        @Override
        protected Platform platform() {
            return new TestPlatform();
        }

        @Override
        protected boolean clockRunsOnItsOwn() {
            return false;
        }
    }

    @Nested
    class TextMetricsTest extends TextMetricsContract {
        @Override
        protected TextMetrics metrics() {
            return new TestPlatform().textMetrics();
        }
    }

    @Nested
    class NullCanvasTest extends CanvasContract {
        @Override
        protected Surface surface(final int width, final int height) {
            final NullCanvas canvas = new NullCanvas();
            return new Surface() {
                @Override
                public org.inspirenxe.strata.spi.Canvas canvas() {
                    return canvas;
                }

                @Override
                public void finish() {
                    if (canvas.clipDepth() != 0) {
                        throw new AssertionError("clip stack not balanced: " + canvas.clipDepth());
                    }
                }
            };
        }
    }
}
