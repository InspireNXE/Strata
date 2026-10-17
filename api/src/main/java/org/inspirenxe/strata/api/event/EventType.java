package org.inspirenxe.strata.api.event;

import java.util.Objects;

/**
 * A typed key for one kind of event, used to register handlers with
 * {@link org.inspirenxe.strata.api.component.Component#on}. Strata's own types are in {@link EventTypes}. Types are
 * compared by identity in dispatch, so always use the constants rather than creating equal copies.
 *
 * @param <E> the event interface handlers receive
 * @param name a readable name, for debugging
 * @param eventClass the event interface handlers receive
 * @param bubbles whether the event travels up through the target's ancestors
 */
public record EventType<E extends Event>(String name, Class<E> eventClass, boolean bubbles) {
    /**
     * Checks the arguments.
     */
    public EventType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(eventClass, "eventClass");
    }

    /**
     * Creates a type whose events travel from the target up to the screen's root.
     *
     * @param <E> the event interface
     * @param name a readable name
     * @param eventClass the event interface
     * @return the new type
     */
    public static <E extends Event> EventType<E> bubbling(final String name, final Class<E> eventClass) {
        return new EventType<>(name, eventClass, true);
    }

    /**
     * Creates a type whose events reach only their target.
     *
     * @param <E> the event interface
     * @param name a readable name
     * @param eventClass the event interface
     * @return the new type
     */
    public static <E extends Event> EventType<E> direct(final String name, final Class<E> eventClass) {
        return new EventType<>(name, eventClass, false);
    }

    /**
     * Returns the type's name, for debugging.
     *
     * @return for example {@code EventType[click]}
     */
    @Override
    public String toString() {
        return "EventType[" + this.name + "]";
    }
}
