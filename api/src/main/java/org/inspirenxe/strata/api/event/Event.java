package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.ComponentSnapshot;

import java.util.Optional;

/**
 * Base of every UI event. See the {@linkplain org.inspirenxe.strata.api.event package description} for the order
 * in which handlers and default behavior run.
 *
 * <p>An event object is valid only while it is being delivered. Do not keep it and read it later: Strata may reuse
 * objects or make {@link #current()} change. To keep the state a component had when the event fired, take its
 * {@linkplain #snapshotOf(Component) snapshot}, which stays valid.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface Event {

    /**
     * Returns the component the event was fired at.
     *
     * @return the target; never null
     */
    Component<?> target();

    /**
     * Returns the component whose handlers or default behavior are running now. Equal to {@link #target()} at
     * first; while a bubbling event travels up, it is each ancestor in turn.
     *
     * @return the current component; never null
     */
    Component<?> current();

    /**
     * Returns the snapshot taken of a component when the event fired, before any handler or default behavior ran.
     *
     * <p>Snapshots are lazy: Strata takes them only when a handler that asked for one, registered with
     * {@link Component#onWithSnapshot(EventType, Phase, EventHandler, Object)}, is on the event's path. The path is
     * the target and, for a bubbling type, every ancestor the target had when the event fired. When at least one
     * such handler is on the path, Strata snapshots the target and each component on the path that has such a
     * handler for this event type. Nothing else is captured, and an event with no such handler on its path has no
     * snapshots at all.</p>
     *
     * <p>Each snapshot covers only its own component, not the component's children. A component that joins the
     * path while the event is being delivered, for example because a handler moved the target to another
     * container, has no snapshot.</p>
     *
     * @param component the component to look up
     * @return the component's snapshot, or empty if none was taken of it for this event
     */
    Optional<ComponentSnapshot> snapshotOf(Component<?> component);

    /**
     * Returns the snapshot of the {@linkplain #target() target}. Present whenever a handler registered with
     * {@link Component#onWithSnapshot(EventType, Phase, EventHandler, Object)} is on the event's path, so such a
     * handler can always call this.
     *
     * @return the target's snapshot; never null
     * @throws IllegalStateException if no snapshot was taken for this event; see {@link #snapshotOf(Component)}
     */
    default ComponentSnapshot snapshot() {
        return this.snapshotOf(this.target()).orElseThrow(() -> new IllegalStateException(
                "No snapshot was taken for this event; register the handler with onWithSnapshot"));
    }

    /**
     * Returns the snapshot of the {@linkplain #current() current} component. Present when the current component
     * has a handler for this event type registered with
     * {@link Component#onWithSnapshot(EventType, Phase, EventHandler, Object)}, or is the target of an event that
     * has snapshots.
     *
     * @return the current component's snapshot; never null
     * @throws IllegalStateException if no snapshot was taken of the current component; see
     *                               {@link #snapshotOf(Component)}
     */
    default ComponentSnapshot currentSnapshot() {
        return this.snapshotOf(this.current()).orElseThrow(() -> new IllegalStateException(
                "No snapshot was taken of " + this.current() + "; register its handler with onWithSnapshot"));
    }

    /**
     * Skips the default behavior of the current component and of every ancestor the event still reaches. Handlers
     * keep running, and the event keeps bubbling unless stopped too. Canceling during the default behavior or an
     * {@link Phase#AFTER} handler only affects ancestors.
     */
    void cancel();

    /**
     * Returns whether the event was canceled.
     *
     * @return true after {@link #cancel()} or {@link #consume()}
     */
    boolean isCancelled();

    /**
     * Keeps the event from reaching any further ancestor. The current component's remaining handlers and default
     * behavior still run.
     */
    void stopPropagation();

    /**
     * Returns whether propagation was stopped.
     *
     * @return true after {@link #stopPropagation()} or {@link #consume()}
     */
    boolean isPropagationStopped();

    /**
     * Cancels the event and stops it: "handled here". Built-in controls consume the input they act on.
     */
    default void consume() {
        this.cancel();
        this.stopPropagation();
    }

    /**
     * Returns whether something canceled or stopped the event. The host reports such input to the engine as
     * consumed.
     *
     * @return true if canceled or stopped
     */
    default boolean isHandled() {
        return this.isCancelled() || this.isPropagationStopped();
    }
}
