package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.Modifiers;

/**
 * A pointer event at a point in root coordinates.
 */
public interface PointerEvent extends Event {

    float x();

    float y();

    /**
     * X relative to the current component's top-left corner.
     */
    default float localX() {
        return this.x() - this.current().bounds().x();
    }

    /**
     * Y relative to the current component's top-left corner.
     */
    default float localY() {
        return this.y() - this.current().bounds().y();
    }

    Modifiers modifiers();
}
