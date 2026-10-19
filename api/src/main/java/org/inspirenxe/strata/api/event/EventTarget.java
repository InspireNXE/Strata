package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.event.input.InputEvents;

/**
 * Something handlers can be registered on. Every {@link org.inspirenxe.strata.api.component.Component} is one, and
 * so are the few things with events that are not components, such as a radio group.
 *
 * <p>On one target, handlers for a type run in ascending priority, and in registration order when priorities are
 * equal: the highest number runs last. A handler registered without a priority has priority 0. Use a lower number
 * to run before others, for example to veto first, and a higher one to run after them, for example to see the
 * final value of a request. A handler may register or remove handlers while it runs; the change applies from the
 * next event.</p>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the type registration returns, for chaining
 */
public interface EventTarget<S> {

    /**
     * Registers a handler with priority 0.
     *
     * @param <E> the event interface
     * @param type the event type, such as {@link InputEvents#KEY} or a control's constant
     * @param handler the handler
     * @return this target, for chaining
     */
    default <E extends Event> S onEvent(final EventType<E> type, final EventHandler<? super E> handler) {
        return this.onEvent(type, 0, handler);
    }

    /**
     * Registers a handler with a priority. The handler stays until the target is discarded; use
     * {@link #subscribe} for one that must be removed earlier.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param priority where the handler runs among this target's handlers for the type; the highest runs last
     * @param handler the handler
     * @return this target, for chaining
     */
    <E extends Event> S onEvent(EventType<E> type, int priority, EventHandler<? super E> handler);

    /**
     * Registers a handler like {@link #onEvent(EventType, int, EventHandler)} and returns a way to remove it, for
     * handlers that live shorter than the target, such as one that follows a drag.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param priority where the handler runs among this target's handlers for the type; the highest runs last
     * @param handler the handler
     * @return a subscription that removes the handler when closed; closing it twice does nothing
     */
    <E extends Event> Subscription subscribe(EventType<E> type, int priority, EventHandler<? super E> handler);
}
