package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.core.node.Node;

/**
 * Delivers an event to its target and, for bubbling types, to each ancestor in turn. At each node:
 * {@link Phase#BEFORE} handlers, then the node's default behavior unless the event is canceled, then
 * {@link Phase#AFTER} handlers.
 */
public final class EventRouter {

    private EventRouter() {
    }

    public static <E extends EventImpl> E dispatch(final EventType<? super E> type, final E event, final Node target) {
        Node node = target;
        while (node != null) {
            event.setCurrent(node.facade());

            final EventDispatcher dispatcher = node.dispatcherOrNull();
            if (dispatcher != null) {
                dispatcher.fire(type, Phase.BEFORE, event);
            }
            if (!event.isCancelled()) {
                node.defaultAction(type, event);
            }
            if (dispatcher != null) {
                dispatcher.fire(type, Phase.AFTER, event);
            }

            if (!type.bubbles() || event.isPropagationStopped()) {
                break;
            }
            node = node.parentNode();
        }
        return event;
    }
}
