package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.DragEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;

public final class DragEventImpl extends PointerEventImpl implements DragEvent {
    private final MouseButton button;
    private final int clicks;
    private final float startX;
    private final float startY;
    private final float deltaX;
    private final float deltaY;

    public DragEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers,
                         final MouseButton button, final int clicks, final float startX, final float startY,
                         final float deltaX, final float deltaY) {
        super(target, x, y, modifiers);
        this.button = button;
        this.clicks = clicks;
        this.startX = startX;
        this.startY = startY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Override
    public MouseButton button() {
        return this.button;
    }

    @Override
    public int clicks() {
        return this.clicks;
    }

    @Override
    public float startX() {
        return this.startX;
    }

    @Override
    public float startY() {
        return this.startY;
    }

    @Override
    public float deltaX() {
        return this.deltaX;
    }

    @Override
    public float deltaY() {
        return this.deltaY;
    }
}
