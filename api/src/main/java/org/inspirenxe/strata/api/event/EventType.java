package org.inspirenxe.strata.api.event;

import java.util.Objects;

/**
 * A typed key for one kind of event.
 *
 * @param name a readable name, for debugging
 * @param eventClass the event interface the handlers receive
 * @param bubbles whether the event travels up through the target's ancestors
 */
public record EventType<E extends Event>(String name, Class<E> eventClass, boolean bubbles) {
    public EventType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(eventClass, "eventClass");
    }

    public static <E extends Event> EventType<E> bubbling(final String name, final Class<E> eventClass) {
        return new EventType<>(name, eventClass, true);
    }

    public static <E extends Event> EventType<E> direct(final String name, final Class<E> eventClass) {
        return new EventType<>(name, eventClass, false);
    }

    @Override
    public String toString() {
        return "EventType[" + this.name + "]";
    }
}
