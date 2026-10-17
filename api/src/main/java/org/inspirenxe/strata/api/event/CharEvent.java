package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.Modifiers;

/**
 * A character the user typed, after the keyboard layout and any input method have done their work. Used by
 * {@link EventTypes#CHAR}. Goes only to the focused component, and bubbles.
 *
 * <p>Characters are Unicode code points, so characters outside the Basic Multilingual Plane, such as emoji, arrive
 * as one event. Some engines, SDL-based ones among them, deliver characters only while a component that accepts
 * text input has focus.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface CharEvent extends Event {

    /**
     * Returns the typed character.
     *
     * @return a Unicode code point
     */
    int codePoint();

    /**
     * Returns the typed character as a string of one or two chars.
     *
     * @return the character
     */
    default String text() {
        return Character.toString(this.codePoint());
    }

    /**
     * Returns the modifiers held when the character was typed. Engines that do not report them give
     * {@link Modifiers#NONE}.
     *
     * @return the held modifiers
     */
    Modifiers modifiers();
}
