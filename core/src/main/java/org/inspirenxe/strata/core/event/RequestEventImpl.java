package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.RequestEvent;

/**
 * A control's request without a value. It has no default behavior on the components it passes; the control that
 * fired it acts once delivery is over, unless the event was canceled.
 *
 * @param <C> the control type
 */
public class RequestEventImpl<C> extends EventImpl implements RequestEvent<C> {

    /**
     * Creates a request fired by a control.
     *
     * @param target the control
     */
    public RequestEventImpl(final Component<?> target) {
        super(target);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final C control() {
        return (C) this.target();
    }

    @Override
    protected final boolean runsDefaultActions() {
        return false;
    }
}
