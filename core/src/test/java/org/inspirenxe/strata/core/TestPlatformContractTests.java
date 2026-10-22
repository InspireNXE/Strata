/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.core.event.CauseStackImpl;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.Translator;
import org.inspirenxe.strata.spi.testing.CanvasContract;
import org.inspirenxe.strata.spi.testing.PlatformContract;
import org.inspirenxe.strata.spi.testing.TextMetricsContract;
import org.junit.jupiter.api.Nested;

import java.util.Locale;

/**
 * Runs the SPI contracts against the test doubles core's tests use, so a core test that passes with these doubles
 * says something about real adapters too. It also exercises the contract kit's path for a canvas that cannot be read
 * back.
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

        @Override
        protected Translator translator() {
            return new LocaleTranslator(Locale.ROOT);
        }
    }

    /**
     * The platform again with a cause stack of its own, core's, so the cause stack checks run too.
     */
    @Nested
    class PlatformWithCausesTest extends PlatformContract {
        @Override
        protected Platform platform() {
            final TestPlatform platform = new TestPlatform();
            platform.setCauses(CauseStackImpl.forThread());
            return platform;
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

    /**
     * A canvas that draws to readable pixels and stands for an engine that draws shapes by drawing them through core's
     * fallback, so the shape checks run on it.
     */
    @Nested
    class RasterCanvasTest extends CanvasContract {
        @Override
        protected Surface surface(final int width, final int height) {
            final RasterCanvas canvas = new RasterCanvas(width, height);
            return new Surface() {
                @Override
                public org.inspirenxe.strata.spi.Canvas canvas() {
                    return canvas;
                }

                @Override
                public void finish() {
                    if (!canvas.balanced()) {
                        throw new AssertionError("stacks not balanced");
                    }
                }

                @Override
                public boolean readable() {
                    return true;
                }

                @Override
                public int argb(final int x, final int y) {
                    return canvas.argb(x, y);
                }
            };
        }

        @Override
        protected boolean drawsShapes() {
            return true;
        }

        @Override
        protected boolean drawsPixels() {
            return true;
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
