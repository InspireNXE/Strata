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
 * The handlers registered on one component. Single-threaded: all calls happen on the UI thread.
 *
 * <p>Handler lists are replaced, not mutated, when they change, so a handler may unsubscribe itself (or others)
 * while an event is being delivered, and firing allocates nothing.</p>
 */
public final class EventDispatcher {
    private final Map<EventType<?>, Entry<?>[]> handlers;

    public EventDispatcher() {
        this.handlers = new HashMap<>();
    }

    /**
     * Registers a handler.
     *
     * @param owner an optional token; {@link #removeOwner(Object)} removes every handler registered with it
     */
    public <E extends Event> Subscription subscribe(final EventType<E> type, final Phase phase,
                                                    final EventHandler<? super E> handler, final Object owner) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(handler, "handler");

        final Entry<E> entry = new Entry<>(phase, handler, owner);
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

    public boolean hasHandlers(final EventType<?> type) {
        return this.handlers.containsKey(type);
    }

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

    private record Entry<E extends Event>(Phase phase, EventHandler<? super E> handler, Object owner) {
    }
}
