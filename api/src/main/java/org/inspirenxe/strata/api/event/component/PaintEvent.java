package org.inspirenxe.strata.api.event.component;

import org.inspirenxe.strata.api.event.Cancelable;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;

/**
 * Drawing a component: {@link org.inspirenxe.strata.api.component.Component#PAINTED} after the component and its
 * children have drawn, so handlers draw on top, and {@link org.inspirenxe.strata.api.component.Component#PAINT}
 * before, as a {@link Pre} event that can skip the component's own drawing.
 *
 * <p>Paint events reach only their component and never take snapshots. A component fires them only when it has a
 * handler for them, and reuses one event object for every frame, so keep handlers free of allocation where you can
 * and never keep the event.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface PaintEvent extends Event {

    /**
     * Returns what to draw with, valid only during this frame.
     *
     * @return the painter
     */
    Painter painter();

    /**
     * Returns the component's bounds, in the host's coordinates.
     *
     * @return the bounds
     */
    Rect bounds();

    /**
     * The paint event fired before a component draws its background. Canceling it skips the component's own
     * background, border and content; its children still draw.
     *
     * <p>Only Strata creates events. Do not implement this interface.</p>
     */
    interface Pre extends PaintEvent, Cancelable {
    }
}
