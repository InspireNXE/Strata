package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;

/**
 * The core {@link DragEvent}. The host sends one when a drag starts, one for each pointer move during it, and one
 * when the button that started it is released. All of them go to the component the button was pressed on.
 */
public final class DragEventImpl extends PointerEventImpl implements DragEvent {
    private final MouseButton button;
    private final int clicks;
    private final float startX;
    private final float startY;
    private final float deltaX;
    private final float deltaY;

    /**
     * Creates the event.
     *
     * @param target the component the button was pressed on
     * @param x the pointer's x, in the host's coordinates; the press position for the start event
     * @param y the pointer's y, in the host's coordinates; the press position for the start event
     * @param modifiers the modifiers held, or {@link Modifiers#NONE} when the engine does not report them
     * @param button the button that started the drag
     * @param clicks the click count of the press that started the drag
     * @param startX the x of that press, in the host's coordinates
     * @param startY the y of that press, in the host's coordinates
     * @param deltaX the horizontal movement since the previous event of this drag, 0 for the start event
     * @param deltaY the vertical movement since the previous event of this drag, 0 for the start event
     */
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
