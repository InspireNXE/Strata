package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.core.node.Node;

/**
 * Delivers events through the tree. Every event Strata fires goes through {@link #dispatch}.
 */
public final class EventRouter {

    private EventRouter() {
    }

    /**
     * Delivers an event to its target and, for a bubbling type, to each ancestor in turn. At each node it runs the
     * {@link Phase#BEFORE} handlers, then the node's default behavior unless the event is canceled, then the
     * {@link Phase#AFTER} handlers. Bubbling stops at the root or when a handler stops propagation.
     *
     * <p>Before anything runs, the router walks the path the event will take (the target and, for a bubbling type,
     * its ancestors) and looks for handlers registered as wanting snapshots. If it finds none, it takes no
     * snapshots and allocates nothing. Otherwise it snapshots the target and every node on the path that has such a
     * handler for this type.</p>
     *
     * @param <E> the event class
     * @param type the event type, which decides whether the event bubbles
     * @param event the event; its {@link EventImpl#current()} is updated at each node
     * @param target the node to start at
     * @return the same event, so the caller can check whether it was canceled
     */
    public static <E extends EventImpl> E dispatch(final EventType<? super E> type, final E event, final Node target) {
        EventRouter.takeSnapshots(type, event, target);

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

    private static void takeSnapshots(final EventType<?> type, final EventImpl event, final Node target) {
        final boolean bubbles = type.bubbles();
        int wanting = 0;
        for (Node node = target; node != null; node = bubbles ? node.parentNode() : null) {
            if (EventRouter.wantsSnapshots(node, type)) {
                wanting++;
            }
        }
        if (wanting == 0) {
            return;
        }

        final boolean targetWants = EventRouter.wantsSnapshots(target, type);
        final ComponentSnapshot[] snapshots = new ComponentSnapshot[targetWants ? wanting : wanting + 1];
        snapshots[0] = target.snapshot();
        int index = 1;
        for (Node node = bubbles ? target.parentNode() : null; node != null; node = node.parentNode()) {
            if (EventRouter.wantsSnapshots(node, type)) {
                snapshots[index++] = node.snapshot();
            }
        }
        event.setSnapshots(snapshots);
    }

    private static boolean wantsSnapshots(final Node node, final EventType<?> type) {
        final EventDispatcher dispatcher = node.dispatcherOrNull();
        return dispatcher != null && dispatcher.wantsSnapshots(type);
    }
}
