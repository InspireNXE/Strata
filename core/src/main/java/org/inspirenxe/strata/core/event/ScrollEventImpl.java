package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;

/**
 * The core {@link ScrollEvent}, sent to the component under the pointer.
 */
public final class ScrollEventImpl extends PointerEventImpl implements ScrollEvent {
    private final float scrollX;
    private final float scrollY;
    private final ScrollKind kind;

    /**
     * Creates the event.
     *
     * @param target the component under the pointer
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @param modifiers the modifiers held, or {@link Modifiers#NONE} when the engine does not report them
     * @param scrollX the horizontal amount in notches, positive toward the start of a row (the left)
     * @param scrollY the vertical amount in notches, positive when the wheel turns away from the user
     * @param kind the kind of device the scroll came from
     */
    public ScrollEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers,
                           final float scrollX, final float scrollY, final ScrollKind kind) {
        super(target, x, y, modifiers);
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.kind = kind;
    }

    @Override
    public float scrollX() {
        return this.scrollX;
    }

    @Override
    public float scrollY() {
        return this.scrollY;
    }

    @Override
    public ScrollKind kind() {
        return this.kind;
    }
}
