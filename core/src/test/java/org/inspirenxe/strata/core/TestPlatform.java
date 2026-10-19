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
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.Optional;

/**
 * A platform with monospaced text (6 units per code point, 9 units per line) and a clock tests move by hand.
 */
public final class TestPlatform implements Platform {
    public static final float CHAR_WIDTH = 6f;
    public static final float LINE_HEIGHT = 9f;
    // Monospaced text measures wider, so tests can tell which style a span was measured in
    public static final float MONO_CHAR_WIDTH = 7f;

    private final boolean sendsKeyRepeat;
    private long nanos;
    private String clipboard;
    private Rect textInputArea;
    private ThemeSource themes = ThemeSource.NONE;

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

    @Override
    public Clock clock() {
        return () -> this.nanos;
    }

    @Override
    public TextInputSink textInput() {
        return new TextInputSink() {
            @Override
            public void start(final Rect caretArea) {
                TestPlatform.this.textInputArea = caretArea;
            }

            @Override
            public void stop() {
                TestPlatform.this.textInputArea = null;
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
