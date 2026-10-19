package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.control.ControlEvent;

/**
 * A control's report without a value. It has no default behavior on the components it passes.
 *
 * @param <C> the control type
 */
public class ControlEventImpl<C> extends EventImpl implements ControlEvent<C> {
    private final C control;

    /**
     * Creates a report fired by a control, which is also the target.
     *
     * @param target the control
     */
    @SuppressWarnings("unchecked")
    public ControlEventImpl(final Component<?> target) {
        this(target, (C) target);
    }

    /**
     * Creates a report fired for something that is not a component, such as a radio group or an overlay, aimed at
     * a component that stands for it.
     *
     * @param target the component the event is aimed at
     * @param control what {@link #control()} returns
     */
    public ControlEventImpl(final Component<?> target, final C control) {
        super(target);
        this.control = control;
    }

    @Override
    public final C control() {
        return this.control;
    }

    @Override
    protected final boolean runsDefaultActions() {
        return false;
    }
}
