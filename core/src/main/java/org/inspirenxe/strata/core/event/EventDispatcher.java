package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The handlers registered on one component. A node creates its dispatcher on the first subscription. Every call
 * must come from the UI thread.
 *
 * <p>Each change swaps in a new handler array for the event type. A handler can therefore unsubscribe itself or
 * others while an event is being delivered (the delivery in progress still reaches every handler it started with),
 * and firing allocates nothing.</p>
 */
public final class EventDispatcher {
    private final Map<EventType<?>, Entry<?>[]> handlers;

    /**
     * Creates a dispatcher with no handlers.
     */
    public EventDispatcher() {
        this.handlers = new HashMap<>();
    }

    /**
     * Registers a handler. Handlers for one type and phase run in the order they were registered.
     *
     * @param <E> the event class
     * @param type the event type to handle
     * @param phase whether the handler runs before or after the component's default behavior
     * @param handler the handler
     * @param owner a token for {@link #removeOwner(Object)}, or {@code null} for none
     * @param snapshots whether events of this type should take snapshots while this handler is registered
     * @return a subscription that removes this handler; unsubscribing twice does nothing
     */
    public <E extends Event> Subscription subscribe(final EventType<E> type, final Phase phase,
                                                    final EventHandler<? super E> handler, final Object owner,
                                                    final boolean snapshots) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(handler, "handler");

        final Entry<E> entry = new Entry<>(phase, handler, owner, snapshots);
        final Entry<?>[] current = this.handlers.get(type);
        final Entry<?>[] next;
        if (current == null) {
            next = new Entry<?>[] {entry};
        } else {
            next = new Entry<?>[current.length + 1];
            System.arraycopy(current, 0, next, 0, current.length);
            next[current.length] = entry;
        }
        this.handlers.put(type, next);
        return () -> this.remove(type, entry);
    }

    /**
     * Calls every handler registered for the type and phase, in registration order. Exceptions from a handler
     * propagate and stop the remaining handlers.
     *
     * @param <E> the event class
     * @param type the event type
     * @param phase the phase to run
     * @param event the event to pass
     */
    @SuppressWarnings("unchecked")
    public <E extends Event> void fire(final EventType<E> type, final Phase phase, final E event) {
        final Entry<?>[] entries = this.handlers.get(type);
        if (entries == null) {
            return;
        }
        for (final Entry<?> entry : entries) {
            if (entry.phase == phase) {
                ((EventHandler<? super E>) entry.handler).handle(event);
            }
        }
    }

    /**
     * Returns whether any handler, in either phase, is registered for a type.
     *
     * @param type the event type
     * @return true if at least one handler is registered
     */
    public boolean hasHandlers(final EventType<?> type) {
        return this.handlers.containsKey(type);
    }

    /**
     * Returns whether any handler for a type, in either phase, was registered as wanting snapshots. Allocates
     * nothing.
     *
     * @param type the event type
     * @return true if at least one such handler is registered
     */
    public boolean wantsSnapshots(final EventType<?> type) {
        final Entry<?>[] entries = this.handlers.get(type);
        if (entries == null) {
            return false;
        }
        for (final Entry<?> entry : entries) {
            if (entry.snapshots) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes every handler registered with an owner token. Owners compare by identity.
     *
     * @param owner the token passed to {@link #subscribe}
     */
    public void removeOwner(final Object owner) {
        Objects.requireNonNull(owner, "owner");
        final List<EventType<?>> types = new ArrayList<>(this.handlers.keySet());
        for (final EventType<?> type : types) {
            final Entry<?>[] current = this.handlers.get(type);
            final List<Entry<?>> kept = new ArrayList<>(current.length);
            for (final Entry<?> entry : current) {
                if (entry.owner != owner) {
                    kept.add(entry);
                }
            }
            this.replace(type, kept);
        }
    }

    /**
     * Removes every handler. Subscriptions handed out earlier become no-ops.
     */
    public void clear() {
        this.handlers.clear();
    }

    private void remove(final EventType<?> type, final Entry<?> target) {
        final Entry<?>[] current = this.handlers.get(type);
        if (current == null) {
            return;
        }
        final List<Entry<?>> kept = new ArrayList<>(current.length);
        for (final Entry<?> entry : current) {
            if (entry != target) {
                kept.add(entry);
            }
        }
        this.replace(type, kept);
    }

    private void replace(final EventType<?> type, final List<Entry<?>> entries) {
        if (entries.isEmpty()) {
            this.handlers.remove(type);
        } else {
            this.handlers.put(type, entries.toArray(new Entry<?>[0]));
        }
    }

    private record Entry<E extends Event>(Phase phase, EventHandler<? super E> handler, Object owner,
                                         boolean snapshots) {
    }
}
