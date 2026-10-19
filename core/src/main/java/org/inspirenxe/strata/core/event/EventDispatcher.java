package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The handlers registered on one event target: a component, or something with events that is not one, such as a
 * radio group. A target creates its dispatcher on the first subscription. Every call must come from the UI thread.
 *
 * <p>Handlers for a type run in ascending priority, and in registration order when priorities are equal. Each change
 * swaps in a new handler array for the type, so a handler can register or remove handlers while an event is being
 * delivered (the delivery in progress still reaches every handler it started with), and firing allocates
 * nothing.</p>
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
     * Registers a handler.
     *
     * @param <E> the event class
     * @param type the event type to handle
     * @param priority where the handler runs among the others for the type; the highest runs last
     * @param handler the handler
     * @return a subscription that removes this handler; unsubscribing twice does nothing
     */
    public <E extends Event> Subscription subscribe(final EventType<E> type, final int priority,
                                                    final EventHandler<? super E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");

        final Entry<E> entry = new Entry<>(priority, handler);
        final Entry<?>[] current = this.handlers.get(type);
        final Entry<?>[] next;
        if (current == null) {
            next = new Entry<?>[] {entry};
        } else {
            // After every entry of the same or a lower priority, so ties keep registration order
            int at = current.length;
            while (at > 0 && current[at - 1].priority > priority) {
                at--;
            }
            next = new Entry<?>[current.length + 1];
            System.arraycopy(current, 0, next, 0, at);
            next[at] = entry;
            System.arraycopy(current, at, next, at + 1, current.length - at);
        }
        this.handlers.put(type, next);
        return () -> this.remove(type, entry);
    }

    /**
     * Calls every handler registered for the type, in priority order. Exceptions from a handler propagate and stop
     * the remaining handlers.
     *
     * @param <E> the event class
     * @param type the event type
     * @param event the event to pass
     */
    @SuppressWarnings("unchecked")
    public <E extends Event> void fire(final EventType<E> type, final E event) {
        final Entry<?>[] entries = this.handlers.get(type);
        if (entries == null) {
            return;
        }
        for (final Entry<?> entry : entries) {
            ((EventHandler<? super E>) entry.handler).handle(event);
        }
    }

    /**
     * Returns whether any handler is registered for a type. Allocates nothing.
     *
     * @param type the event type
     * @return true if at least one handler is registered
     */
    public boolean hasHandlers(final EventType<?> type) {
        return this.handlers.containsKey(type);
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
        if (kept.isEmpty()) {
            this.handlers.remove(type);
        } else {
            this.handlers.put(type, kept.toArray(new Entry<?>[0]));
        }
    }

    private record Entry<E extends Event>(int priority, EventHandler<? super E> handler) {
    }
}
