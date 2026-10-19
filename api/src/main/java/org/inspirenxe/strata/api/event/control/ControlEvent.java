package org.inspirenxe.strata.api.event.control;

import org.inspirenxe.strata.api.event.Event;

/**
 * A control's Post event: something happened, such as a press or a close. It fires after the fact, whether the user
 * or code caused it, and cannot be canceled.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 *
 * @param <C> the control type
 */
public interface ControlEvent<C> extends Event {

    /**
     * Returns the control that fired the event, which is also its {@linkplain #target() target}.
     *
     * @return the control
     */
    C control();
}
