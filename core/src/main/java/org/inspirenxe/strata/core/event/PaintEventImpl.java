package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.PaintEvent;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;

/**
 * The paint event of one component, serving as both {@link Component#PAINT} and {@link Component#PAINTED}. The
 * component keeps one and resets it before each firing, so painting allocates nothing for it.
 */
public final class PaintEventImpl extends EventImpl implements PaintEvent.Pre {
    private Painter painter;
    private Rect bounds;

    /**
     * Creates the paint event of a component.
     *
     * @param target the component
     */
    public PaintEventImpl(final Component<?> target) {
        super(target);
    }

    /**
     * Readies the event for one firing: clears the canceled and stopped flags and sets what to draw with.
     *
     * @param painter the painter for this frame
     * @param bounds the component's bounds
     * @return this event
     */
    public PaintEventImpl begin(final Painter painter, final Rect bounds) {
        this.reset();
        this.painter = painter;
        this.bounds = bounds;
        return this;
    }

    @Override
    public Painter painter() {
        return this.painter;
    }

    @Override
    public Rect bounds() {
        return this.bounds;
    }
}
