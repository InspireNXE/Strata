package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.MouseButton;

/**
 * A mouse button press, release or click. Used by {@link EventTypes#POINTER_PRESS},
 * {@link EventTypes#POINTER_RELEASE} and {@link EventTypes#CLICK}.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface PointerButtonEvent extends PointerEvent {

    /**
     * Returns the button involved.
     *
     * @return the button
     */
    MouseButton button();

    /**
     * Returns the position of this press in a run of quick presses. A press counts as the next in a run when it
     * hits the same component with the same button within 500 ms and 4 units of the previous press; the count goes
     * 1, 2, 3 and then starts again at 1. A release and its click carry the count of their press.
     *
     * <p>Strata delivers every press at once with its count, never holding a single click back to wait for a
     * double. React to the count you care about: a text box selects a word on 2.</p>
     *
     * @return 1 for a single click, 2 for a double click, 3 for a triple click
     */
    int clicks();
}
