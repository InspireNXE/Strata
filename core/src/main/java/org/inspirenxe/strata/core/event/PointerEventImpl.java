package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.input.Modifiers;

public class PointerEventImpl extends EventImpl implements PointerEvent {
    private final float x;
    private final float y;
    private final Modifiers modifiers;

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
