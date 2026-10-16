package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.Modifiers;

/**
 * A typed character, as a Unicode code point so characters outside the Basic Multilingual Plane work.
 */
public interface CharEvent extends Event {

    int codePoint();

    default String text() {
        return Character.toString(this.codePoint());
    }

    Modifiers modifiers();
}
