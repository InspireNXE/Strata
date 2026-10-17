package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextMetrics;

import java.util.Optional;

/**
 * What a component may ask of the engine outside layout: text measurement, time, the clipboard and the held
 * modifiers. Get it with {@link CustomComponent#environment()}.
 *
 * <p>It comes from the host, so tests can drive time and the clipboard, and a component written against it works
 * on any engine. Do not read the system clock or AWT clipboard directly in a component.</p>
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface Environment {

    /**
     * Returns the engine's text measurement, the same one layout uses.
     *
     * @return the text metrics
     */
    TextMetrics text();

    /**
     * Returns the host's clock, for blinking carets and other timing. Only differences between two readings mean
     * anything; it is not the time of day.
     *
     * @return milliseconds on a clock that never goes backward
     */
    long millis();

    /**
     * Reads text from the clipboard.
     *
     * @return the clipboard text, or empty if it holds none or the engine has no clipboard
     */
    Optional<String> clipboardText();

    /**
     * Puts text on the clipboard. Engines without a clipboard ignore it.
     *
     * @param text the text
     */
    void setClipboardText(String text);

    /**
     * Returns the modifier keys held, as of the latest input the host received.
     *
     * @return the held modifiers; {@link Modifiers#NONE} before any input
     */
    Modifiers modifiers();
}
