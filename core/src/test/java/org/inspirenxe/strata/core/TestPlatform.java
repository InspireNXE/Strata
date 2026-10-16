package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;

import java.util.Optional;

/**
 * A platform with monospaced text (6 units per code point, 9 units per line) and a clock tests move by hand.
 */
public final class TestPlatform implements Platform {
    public static final float CHAR_WIDTH = 6f;
    public static final float LINE_HEIGHT = 9f;

    private final boolean sendsKeyRepeat;
    private long nanos;
    private String clipboard;
    private Rect textInputArea;

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
                return span.text().codePointCount(0, span.text().length()) * CHAR_WIDTH;
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
        return new Capabilities(this.sendsKeyRepeat);
    }
}
