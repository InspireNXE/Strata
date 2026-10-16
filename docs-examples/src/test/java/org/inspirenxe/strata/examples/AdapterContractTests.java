package org.inspirenxe.strata.examples;

// --8<-- [start:contracts]
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.examples.adapter.MonospaceTextMetrics;
import org.inspirenxe.strata.examples.adapter.RecordingCanvas;
import org.inspirenxe.strata.examples.adapter.ToyPlatform;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.testing.CanvasContract;
import org.inspirenxe.strata.spi.testing.PlatformContract;
import org.inspirenxe.strata.spi.testing.TextMetricsContract;
import org.junit.jupiter.api.Nested;

/**
 * Holds the example adapter to the same contracts as Strata's own adapters.
 */
class AdapterContractTests {

    @Nested
    class Metrics extends TextMetricsContract {
        @Override
        protected TextMetrics metrics() {
            return new MonospaceTextMetrics();
        }
    }

    @Nested
    class Platforms extends PlatformContract {
        @Override
        protected Platform platform() {
            return new ToyPlatform();
        }
    }

    @Nested
    class Canvases extends CanvasContract {
        @Override
        protected Surface surface(final int width, final int height) {
            // The recording canvas has no pixels to read back, so only the checks that need none run
            final RecordingCanvas canvas = new RecordingCanvas();
            canvas.begin();
            return new Surface() {
                @Override
                public Canvas canvas() {
                    return canvas;
                }
            };
        }
    }
}
// --8<-- [end:contracts]
