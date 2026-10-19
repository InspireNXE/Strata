package org.inspirenxe.strata.api.overlay;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.ControlEvent;
import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;

/**
 * One open popup or dialog; see {@link Overlays}.
 *
 * <p>An overlay is not a component, but it has an event of its own, {@link #CLOSED}, and takes handlers through
 * {@link EventTarget}. Its events reach only the handlers registered on the overlay itself.</p>
 *
 * <pre>{@code
 * Overlay popup = factory.overlays().popup(list, button.bounds());
 * popup.onEvent(Overlay.CLOSED, event -> button.setText("More"));
 * }</pre>
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface Overlay extends EventTarget<Overlay> {
    /**
     * Reported once the overlay has closed, however it closed: from {@link #close()}, a press outside a popup, an
     * Escape press or a click on a toast, for example. The event's {@linkplain ControlEvent#control() control} is
     * this overlay and its {@linkplain ControlEvent#target() target} is the overlay's {@linkplain #content()
     * content}, already detached. It fires once per overlay, so a handler registered after the overlay closed never
     * runs.
     */
    EventType<ControlEvent<Overlay>> CLOSED = EventType.report("overlay.closed");

    /**
     * Returns what the overlay shows.
     *
     * @return the content passed when it opened
     */
    Component<?> content();

    /**
     * Returns whether the overlay is a modal dialog.
     *
     * @return true for a dialog, false for a popup or a toast
     */
    boolean isModal();

    /**
     * Returns whether the overlay is still open.
     *
     * @return false once closed
     */
    boolean isOpen();

    /**
     * Closes the overlay and detaches its content, then reports {@link #CLOSED}. Closing an overlay that is already
     * closed does nothing.
     */
    void close();
}
