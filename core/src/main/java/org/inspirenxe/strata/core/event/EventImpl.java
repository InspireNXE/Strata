package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.Event;

import java.util.Objects;

public abstract class EventImpl implements Event {
    private final Component<?> target;
    private Component<?> current;
    private boolean cancelled;
    private boolean propagationStopped;

    protected EventImpl(final Component<?> target) {
        this.target = Objects.requireNonNull(target, "target");
        this.current = target;
    }

    @Override
    public final Component<?> target() {
        return this.target;
    }

    @Override
    public final Component<?> current() {
        return this.current;
    }

    final void setCurrent(final Component<?> current) {
        this.current = current;
    }

    @Override
    public final void cancel() {
        this.cancelled = true;
    }

    @Override
    public final boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public final void stopPropagation() {
        this.propagationStopped = true;
    }

    @Override
    public final boolean isPropagationStopped() {
        return this.propagationStopped;
    }
}
