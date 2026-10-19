package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.event.Event;

import java.util.Objects;
import java.util.Optional;

/**
 * The base of every core event. It holds the target, the component currently handling the event, and the
 * canceled and propagation flags, and the snapshots taken when it fired. Only {@link EventRouter} moves
 * {@link #current()} and sets the snapshots.
 */
public abstract class EventImpl implements Event {
    private final Component<?> target;
    private Component<?> current;
    private boolean cancelled;
    private boolean propagationStopped;
    private ComponentSnapshot[] snapshots;

    /**
     * Creates an event aimed at a component. {@link #current()} starts as the target.
     *
     * @param target the component the event is fired at
     */
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
    public final Optional<ComponentSnapshot> snapshotOf(final Component<?> component) {
        Objects.requireNonNull(component, "component");
        // By identity. An event holds only a few snapshots, so a linear search is enough.
        if (this.snapshots != null) {
            for (final ComponentSnapshot snapshot : this.snapshots) {
                if (snapshot.component() == component) {
                    return Optional.of(snapshot);
                }
            }
        }
        return Optional.empty();
    }

    final void setSnapshots(final ComponentSnapshot[] snapshots) {
        this.snapshots = snapshots;
    }

    /**
     * Cancels the event. Public only for the event kinds that are {@code Cancelable}; the others do not expose it.
     */
    public final void cancel() {
        this.cancelled = true;
    }

    /**
     * Returns whether the event was canceled. Always false for event kinds that cannot be canceled.
     *
     * @return true after {@link #cancel()}
     */
    public final boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Returns whether each component the event passes runs its default behavior for it. Input events do; a
     * control's request and report events do not, since only the control that fired them acts.
     *
     * @return true for input events
     */
    protected boolean runsDefaultActions() {
        return true;
    }

    /**
     * Clears the canceled and stopped flags, the snapshots and the current component, for an event object that is
     * fired again.
     */
    protected final void reset() {
        this.current = this.target;
        this.cancelled = false;
        this.propagationStopped = false;
        this.snapshots = null;
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
