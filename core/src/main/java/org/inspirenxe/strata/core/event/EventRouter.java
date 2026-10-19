package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.core.node.Node;

/**
 * Delivers events through the tree. Every event Strata fires goes through {@link #dispatch}.
 */
public final class EventRouter {

    private EventRouter() {
    }

    /**
     * Delivers an event to its target and, for a bubbling type, to each node {@link #next} gives in turn. At each
     * node it runs the handlers in priority order, then, for an input event, the node's default behavior unless the
     * event is canceled. Bubbling stops at the end of the path or when a handler stops propagation.
     *
     * <p>Before anything runs, the router walks the path the event will take and looks for handlers of its type.
     * If it finds none, it takes no snapshots and allocates nothing. Otherwise it snapshots the target and every node
     * on the path that has a handler for the type. Reports never take snapshots: they fire after their change, so
     * they carry the value from before it instead.</p>
     *
     * @param <E> the event class
     * @param type the event type, which decides whether the event bubbles
     * @param event the event; its {@link EventImpl#current()} is updated at each node
     * @param target the node to start at
     * @return the same event, so the caller can check whether it was canceled
     */
    public static <E extends EventImpl> E dispatch(final EventType<? super E> type, final E event, final Node target) {
        // A report fires after its change, so a snapshot would show the new state; it carries previous() instead
        if (!(event instanceof ControlEventImpl<?>)) {
            EventRouter.takeSnapshots(type, event, target);
        }

        Node node = target;
        while (node != null) {
            event.setCurrent(node.facade());

            final EventDispatcher dispatcher = node.dispatcherOrNull();
            if (dispatcher != null) {
                dispatcher.fire(type, event);
            }
            if (!event.isCancelled() && event.runsDefaultActions()) {
                node.defaultAction(type, event);
            }

            if (!type.bubbles() || event.isPropagationStopped()) {
                break;
            }
            node = EventRouter.next(node, target, event.runsDefaultActions());
        }
        return event;
    }

    /**
     * Returns the next node on an event's path after {@code node}, or {@code null} at the end. Input events climb to
     * the root. A control's request and report events stop at the first composite above the target, so the events
     * of a composite's private parts reach the composite but nothing outside it: a table hears its inner list, and
     * a form around the table does not. A composite does not stop an event that came out of content it holds for its
     * user: when a node between the target and the composite, the target included, was added as content, the event
     * passes on.
     *
     * @param node the node just visited
     * @param target the event's target
     * @param input whether the event is an input event
     * @return the next node, or {@code null}
     */
    public static Node next(final Node node, final Node target, final boolean input) {
        if (!input && node != target && EventRouter.isComposite(node) && !EventRouter.fromContent(target, node)) {
            return null;
        }
        return node.parentNode();
    }

    private static boolean fromContent(final Node target, final Node composite) {
        for (Node node = target; node != null && node != composite; node = node.parentNode()) {
            if (node.isContent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isComposite(final Node node) {
        final CustomComponent<?> facade = node.facade();
        return facade instanceof CustomComposite<?> && !(facade instanceof CustomContainer<?>);
    }

    private static void takeSnapshots(final EventType<?> type, final EventImpl event, final Node target) {
        final boolean bubbles = type.bubbles();
        int wanting = 0;
        final boolean input = event.runsDefaultActions();
        for (Node node = target; node != null; node = bubbles ? EventRouter.next(node, target, input) : null) {
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
        for (Node node = bubbles ? EventRouter.next(target, target, input) : null; node != null;
             node = EventRouter.next(node, target, input)) {
            if (EventRouter.wantsSnapshots(node, type)) {
                snapshots[index++] = node.snapshot();
            }
        }
        event.setSnapshots(snapshots);
    }

    private static boolean wantsSnapshots(final Node node, final EventType<?> type) {
        final EventDispatcher dispatcher = node.dispatcherOrNull();
        return dispatcher != null && dispatcher.hasHandlers(type);
    }
}
