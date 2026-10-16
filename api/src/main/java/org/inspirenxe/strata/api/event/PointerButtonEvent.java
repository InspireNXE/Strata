package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.MouseButton;

/**
 * A press, release or click.
 */
public interface PointerButtonEvent extends PointerEvent {

    MouseButton button();

    /**
     * 1 for a single click, 2 for a double click, 3 for a triple click.
     */
    int clicks();
}
