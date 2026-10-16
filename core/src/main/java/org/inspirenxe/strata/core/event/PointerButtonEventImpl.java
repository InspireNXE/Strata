package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;

public final class PointerButtonEventImpl extends PointerEventImpl implements PointerButtonEvent {
    private final MouseButton button;
    private final int clicks;

    public PointerButtonEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers,
                                  final MouseButton button, final int clicks) {
        super(target, x, y, modifiers);
        this.button = button;
        this.clicks = clicks;
    }

    @Override
    public MouseButton button() {
        return this.button;
    }

    @Override
    public int clicks() {
        return this.clicks;
    }
}
