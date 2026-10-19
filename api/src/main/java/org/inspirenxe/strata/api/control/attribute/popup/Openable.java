package org.inspirenxe.strata.api.control.attribute.popup;

import org.inspirenxe.strata.api.event.ControlEvent;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.RequestEvent;

/**
 * A control that opens a popup and closes it again, such as a combo box or a menu. How it opens is particular to
 * each control.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface Openable<S extends Openable<S>> extends Closable<S> {
    /**
     * Asked before the user opens it. Cancel it to keep it closed. Fires only for the user's actions, so a control
     * opened only from code or by another control never fires it.
     */
    EventType<RequestEvent<Openable<?>>> OPEN = EventType.request("openable.open");
    /**
     * Told after it opened, by the user or from code.
     */
    EventType<ControlEvent<Openable<?>>> OPENED = EventType.report("openable.opened");

    /**
     * Returns whether it is open.
     *
     * @return true while open; false for a new control
     */
    boolean isOpen();
}
