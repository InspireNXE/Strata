package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.ScrollKind;

/**
 * A scroll from a mouse wheel or trackpad, fired at the component under the pointer. Used by
 * {@link EventTypes#SCROLL}.
 *
 * <p>Amounts are in wheel notches: one notch of a notched wheel is 1, and a trackpad reports fractions. Horizontal
 * scrolling comes from tilting wheels, trackpads, and on some engines from Shift plus a vertical wheel.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface ScrollEvent extends PointerEvent {

    /**
     * Returns the horizontal scroll amount.
     *
     * @return positive to scroll toward the left (the start of a row), negative toward the right
     */
    float scrollX();

    /**
     * Returns the vertical scroll amount.
     *
     * @return positive when the wheel turns away from the user, which scrolls toward the top of the content;
     *         negative toward the user, which scrolls toward the bottom
     */
    float scrollY();

    /**
     * Returns the kind of device the scroll came from.
     *
     * @return the device kind
     */
    ScrollKind kind();
}
