package org.inspirenxe.strata.api.event.input;

import org.inspirenxe.strata.api.event.Cancelable;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * An event about the mouse pointer. Positions are in the host's coordinates, in UI units; {@link #localX()} and
 * {@link #localY()} give the position relative to the current component.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface PointerEvent extends Cancelable {

    /**
     * Returns the pointer's x in the host's coordinates.
     *
     * @return the x position
     */
    float x();

    /**
     * Returns the pointer's y in the host's coordinates.
     *
     * @return the y position
     */
    float y();

    /**
     * Returns the pointer's x relative to the {@linkplain #current() current} component's left edge. It changes as
     * the event bubbles.
     *
     * @return the x position inside the current component
     */
    default float localX() {
        return this.x() - this.current().bounds().x();
    }

    /**
     * Returns the pointer's y relative to the {@linkplain #current() current} component's top edge. It changes as
     * the event bubbles.
     *
     * @return the y position inside the current component
     */
    default float localY() {
        return this.y() - this.current().bounds().y();
    }

    /**
     * Returns the modifier keys held at the time of the event. Engines that do not report modifiers with pointer
     * input give {@link Modifiers#NONE}.
     *
     * @return the held modifiers
     */
    Modifiers modifiers();
}
