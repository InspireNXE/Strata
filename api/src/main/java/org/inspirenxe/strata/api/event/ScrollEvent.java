package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.ScrollKind;

public interface ScrollEvent extends PointerEvent {

    float scrollX();

    /**
     * Positive scrolls up (away from the user).
     */
    float scrollY();

    ScrollKind kind();
}
