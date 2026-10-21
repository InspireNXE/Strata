/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.api.overlay;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.event.control.ControlEvent;

/**
 * One open popup or dialog; see {@link Overlays}.
 *
 * <p>An overlay is not a component, but it reports {@link Closable#CLOSED} and takes handlers of its own. Its events
 * reach only the handlers registered on the overlay itself. It never fires {@link Closable#CLOSE}; a window inside it
 * asks before the user closes it.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Closable#CLOSED}: Reported once the overlay has closed, however it closed: from {@link #close()}, a
 *     press outside a popup, an Escape press or a click on a toast, for example. The event's
 *     {@linkplain ControlEvent#control() control} is this overlay and its {@linkplain ControlEvent#target() target} is
 *     the overlay's {@linkplain #content() content}, already detached. It fires once per overlay, so a handler
 *     registered after the overlay closed never runs.</li>
 * </ul>
 *
 * <pre>{@code
 * Overlay popup = factory.overlays().popup(list, button.bounds());
 * popup.onEvent(Closable.CLOSED, event -> button.setText("More"));
 * }</pre>
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface Overlay extends Closable<Overlay> {
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
     * Brings a popup or a toast above the other popups and toasts, so it is drawn over them and the pointer hits it
     * first. Dialogs keep their place: an overlay is raised only as far as the first dialog opened after it, so a
     * dialog stays above everything opened before it. Does nothing for a dialog or a closed overlay.
     *
     * @return this overlay
     */
    Overlay raise();

    /**
     * Closes the overlay and detaches its content, then reports {@link #CLOSED}. Closing an overlay that is already
     * closed does nothing.
     *
     * @return this overlay
     */
    @Override
    Overlay close();
}
