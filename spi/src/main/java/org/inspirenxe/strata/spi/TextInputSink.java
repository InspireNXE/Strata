package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.geom.Rect;

/**
 * Tells the engine when a text field gains or loses focus. Some engines (Minecraft 26.3, SDL) deliver no
 * character input until told.
 */
public interface TextInputSink {
    TextInputSink NONE = new TextInputSink() {
        @Override
        public void start(final Rect caretArea) {
        }

        @Override
        public void stop() {
        }
    };

    void start(Rect caretArea);

    void stop();
}
