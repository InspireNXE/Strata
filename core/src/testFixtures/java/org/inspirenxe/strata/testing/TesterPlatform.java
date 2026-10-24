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
package org.inspirenxe.strata.testing;

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.Scheduler;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.Optional;

/**
 * The platform behind a {@link ScreenTester}: text measured as if monospaced, a clock the tester moves by hand, a
 * scheduler that runs as that clock moves, a wall clock that never moves, a clipboard held in memory and no engine
 * text input. The engine sends key repeats
 * itself, so holding a key repeats nothing unless the test presses it again.
 */
final class TesterPlatform implements Platform {
    private final java.time.Clock wall;
    private final TesterScheduler scheduler = new TesterScheduler();
    private long nanos;
    private String clipboard;

    TesterPlatform(final java.time.Clock wall) {
        this.wall = wall;
    }

    /**
     * Moves the clock forward.
     *
     * @param millis how far, in milliseconds; not negative
     */
    void advance(final long millis) {
        this.nanos += millis * 1_000_000L;
        this.scheduler.advanceTo(this.nanos / 1_000_000L);
    }

    @Override
    public TextMetrics textMetrics() {
        return new TextMetrics() {
            @Override
            public float width(final TextSpan span) {
                return span.text().codePointCount(0, span.text().length())
                        * (span.style().monospace() ? ScreenTester.MONO_CHAR_WIDTH : ScreenTester.CHAR_WIDTH)
                        * scale(span.style());
            }

            @Override
            public float lineHeight(final TextStyle style) {
                return ScreenTester.LINE_HEIGHT * scale(style);
            }
        };
    }

    /**
     * How much larger a style's size draws than the tester's default size.
     */
    private static float scale(final TextStyle style) {
        return style.size() == 0f ? 1f : style.size() / ScreenTester.TEXT_SIZE;
    }

    @Override
    public Scheduler scheduler() {
        return this.scheduler;
    }

    @Override
    public Clipboard clipboard() {
        return new Clipboard() {
            @Override
            public Optional<String> get() {
                return Optional.ofNullable(TesterPlatform.this.clipboard);
            }

            @Override
            public void set(final String text) {
                TesterPlatform.this.clipboard = text;
            }
        };
    }

    @Override
    public Clock clock() {
        return new Clock() {
            @Override
            public long nanoTime() {
                return TesterPlatform.this.nanos;
            }

            @Override
            public java.time.Clock wall() {
                return TesterPlatform.this.wall;
            }
        };
    }

    @Override
    public TextInputSink textInput() {
        return TextInputSink.NONE;
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities(true, false);
    }

    @Override
    public ThemeSource themes() {
        return ThemeSource.NONE;
    }
}
