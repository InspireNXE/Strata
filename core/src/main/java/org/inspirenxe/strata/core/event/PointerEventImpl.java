package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The core {@link PointerEvent}. The host uses it directly for pointer enter, leave and move; the button, drag and
 * scroll events extend it.
 */
public class PointerEventImpl extends EventImpl implements PointerEvent {
    private final float x;
    private final float y;
    private final Modifiers modifiers;

    /**
     * Creates the event.
     *
     * @param target the component the event is for
     * @param x the pointer's x, in the host's coordinates
     * @param y the pointer's y, in the host's coordinates
     * @param modifiers the modifiers held, or {@link Modifiers#NONE} when the engine does not report them
     */
    public PointerEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers) {
        super(target);
        this.x = x;
        this.y = y;
        this.modifiers = modifiers;
    }

    @Override
    public final float x() {
        return this.x;
    }

    @Override
    public final float y() {
        return this.y;
    }

    @Override
    public final Modifiers modifiers() {
        return this.modifiers;
    }
}
