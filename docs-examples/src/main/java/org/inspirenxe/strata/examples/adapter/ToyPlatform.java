package org.inspirenxe.strata.examples.adapter;

// --8<-- [start:platform]
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.Optional;

/**
 * The platform for the imaginary engine. It has an in-memory clipboard, needs no text input signal, and sends no
 * key repeats, so Strata makes them.
 */
public final class ToyPlatform implements Platform {
    // Constant for the platform's life, as the SPI requires
    private static final Capabilities CAPABILITIES = new Capabilities(false);

    private final TextMetrics metrics = new MonospaceTextMetrics();
    private final Clipboard clipboard = new Clipboard() {
        private String text;

        @Override
        public Optional<String> get() {
            return Optional.ofNullable(this.text);
        }

        @Override
        public void set(final String text) {
            this.text = text;
        }
    };
    private final TextInputSink textInput = new TextInputSink() {
        @Override
        public void start(final Rect caretArea) {
            // An engine with an on-screen keyboard would open it here, near caretArea
        }

        @Override
        public void stop() {
        }
    };

    @Override
    public TextMetrics textMetrics() {
        return this.metrics;
    }

    @Override
    public Clipboard clipboard() {
        return this.clipboard;
    }

    @Override
    public Clock clock() {
        return Clock.SYSTEM;
    }

    @Override
    public TextInputSink textInput() {
        return this.textInput;
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public ThemeSource themes() {
        return ThemeSource.NONE;
    }
}
// --8<-- [end:platform]
