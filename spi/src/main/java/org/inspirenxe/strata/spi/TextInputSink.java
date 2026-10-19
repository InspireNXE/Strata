package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.geometry.Rect;

/**
 * Where a host tells the engine that a text field gained or lost focus. Some engines deliver no typed characters
 * until told (SDL-based engines among them), and some show an on-screen keyboard or input method
 * window at the given area.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>{@link #start} and {@link #stop} may arrive repeatedly; treat a second start as moving the input
 *     area.</li>
 *     <li>After {@link #start}, the adapter must forward typed characters to {@link InputSink#charTyped}.</li>
 * </ul>
 */
public interface TextInputSink {
    /**
     * A sink that ignores both calls, for engines that always deliver characters.
     */
    TextInputSink NONE = new TextInputSink() {
        @Override
        public void start(final Rect caretArea) {
        }

        @Override
        public void stop() {
        }
    };

    /**
     * Called when a component that accepts text input gains focus.
     *
     * @param caretArea where the text field is, in UI units, for placing input method windows
     */
    void start(Rect caretArea);

    /**
     * Called when focus leaves the last component that accepts text input.
     */
    void stop();
}
