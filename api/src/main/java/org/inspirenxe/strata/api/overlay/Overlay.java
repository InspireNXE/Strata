package org.inspirenxe.strata.api.overlay;

import org.inspirenxe.strata.api.component.Component;

/**
 * One open popup or dialog; see {@link Overlays}.
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface Overlay {

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
     * Closes the overlay and detaches its content. Closing an overlay that is already closed does nothing.
     */
    void close();

    /**
     * Adds a listener that runs once the overlay has closed, however it closed. A listener added after it closed
     * never runs.
     *
     * @param listener the listener
     * @return this overlay
     */
    Overlay onClosed(Runnable listener);
}
