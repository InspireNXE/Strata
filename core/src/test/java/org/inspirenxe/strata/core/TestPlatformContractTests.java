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
