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

import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * A platform with monospaced text (6 units per code point, 9 units per line), a clock tests move by hand, and a
 * fixed wall clock.
 */
public final class TestPlatform implements Platform {
    public static final float CHAR_WIDTH = 6f;
    public static final float LINE_HEIGHT = 9f;
    // Monospaced text measures wider, so tests can tell which style a span was measured in
    public static final float MONO_CHAR_WIDTH = 7f;
    // What the wall clock reads until a test sets another time
    public static final LocalDateTime TODAY = LocalDateTime.of(2026, 10, 20, 12, 0);

    private final boolean sendsKeyRepeat;
    private long nanos;
    private String clipboard;
    private Rect textInputArea;
    private TextInputKind textInputKind;
    private ThemeSource themes = ThemeSource.NONE;
    private java.time.Clock wall = java.time.Clock.fixed(TODAY.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    public TestPlatform() {
        this(true);
    }

    public TestPlatform(final boolean sendsKeyRepeat) {
        this.sendsKeyRepeat = sendsKeyRepeat;
    }

    public void advanceMillis(final long millis) {
        this.nanos += millis * 1_000_000L;
    }

    /**
     * The area passed to the last text input start, or {@code null} while text input is stopped.
     */
    public Rect textInputArea() {
        return this.textInputArea;
    }

    /**
     * The kind passed to the last text input start, or {@code null} while text input is stopped.
     */
    public TextInputKind textInputKind() {
        return this.textInputKind;
    }

    @Override
    public TextMetrics textMetrics() {
        return new TextMetrics() {
            @Override
            public float width(final TextSpan span) {
                return span.text().codePointCount(0, span.text().length())
                        * (span.style().monospace() ? MONO_CHAR_WIDTH : CHAR_WIDTH);
            }

            @Override
            public float lineHeight(final TextStyle style) {
                return LINE_HEIGHT;
            }
        };
    }

    @Override
    public Clipboard clipboard() {
        return new Clipboard() {
            @Override
            public Optional<String> get() {
                return Optional.ofNullable(TestPlatform.this.clipboard);
            }

            @Override
            public void set(final String text) {
                TestPlatform.this.clipboard = text;
            }
        };
    }

    /**
     * Sets the wall clock the platform's clock reports, which is noon UTC on 20 October 2026 until set.
     */
    public void setWall(final java.time.Clock wall) {
        this.wall = wall;
    }

    /**
     * Sets the wall clock to a fixed instant in UTC.
     */
    public void setToday(final LocalDateTime now) {
        this.wall = java.time.Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @Override
    public Clock clock() {
        return new Clock() {
            @Override
            public long nanoTime() {
                return TestPlatform.this.nanos;
            }

            @Override
            public java.time.Clock wall() {
                return TestPlatform.this.wall;
            }
        };
    }

    @Override
    public TextInputSink textInput() {
        return new TextInputSink() {
            @Override
            public void start(final Rect caretArea) {
                this.start(caretArea, TextInputKind.TEXT);
            }

            @Override
            public void start(final Rect caretArea, final TextInputKind kind) {
                TestPlatform.this.textInputArea = caretArea;
                TestPlatform.this.textInputKind = kind;
            }

            @Override
            public void stop() {
                TestPlatform.this.textInputArea = null;
                TestPlatform.this.textInputKind = null;
            }
        };
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities(this.sendsKeyRepeat, false);
    }

    public void setThemes(final ThemeSource themes) {
        this.themes = themes;
    }

    @Override
    public ThemeSource themes() {
        return this.themes;
    }
}
