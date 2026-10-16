package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.ScrollEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;

public final class ScrollEventImpl extends PointerEventImpl implements ScrollEvent {
    private final float scrollX;
    private final float scrollY;
    private final ScrollKind kind;

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
