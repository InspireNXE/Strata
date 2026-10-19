package org.inspirenxe.strata.api.event;

/**
 * A control's Pre event: the user asked the control to do something, such as press or close, and it has not
 * happened yet. Cancel it to refuse. The control fires it through its whole path first, so an ancestor can refuse
 * too, and acts afterwards unless the event was canceled.
 *
 * <p>Request events fire only for what the user does. Changes made from code, through a control's setters and
 * methods, fire none.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 *
 * @param <C> the control type
 */
public interface RequestEvent<C> extends Cancelable {

    /**
     * Returns the control that fired the event, which is also its {@linkplain #target() target}.
     *
     * @return the control
     */
    C control();
}
