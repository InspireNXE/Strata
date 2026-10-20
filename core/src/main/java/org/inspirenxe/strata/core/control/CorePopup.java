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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.geometry.Rect;

/**
 * What core's popups, the menu and the flyout, offer a control that holds one and opens it for the user, beyond
 * the public {@link Popup}. The drop-down button is that control. It checks {@link #canOpen()} before it asks
 * anything, opens the popup through {@link #openBelow}, and hears every close through its {@link Holder}, so it
 * needs no handler on the popup and never knows which kind of popup it holds.
 */
public interface CorePopup {
    /**
     * Returns whether {@link Popup#open} would show the popup now.
     *
     * @return true if it is closed and has something to show: items for a menu, content for a flyout
     */
    boolean canOpen();

    /**
     * Returns the control that holds this popup.
     *
     * @return the holder, or {@code null} when none holds it
     */
    Holder holder();

    /**
     * Gives this popup to a control, or takes it back.
     *
     * @param holder the control, or {@code null} to free the popup
     * @throws IllegalArgumentException if another control holds the popup, or it is a menu that opens from a menu
     *     or a menu bar
     */
    void setHolder(Holder holder);

    /**
     * Opens the popup below a rectangle, as {@link Popup#open(Rect)} does. A menu opened this way is at least as
     * wide as the rectangle until it closes, and highlights its first item when a key opened it.
     *
     * @param anchor the rectangle in root coordinates, the holder's bounds
     * @param fromKeyboard true when the user opened it with a key
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    void openBelow(Rect anchor, boolean fromKeyboard);

    /**
     * A control that holds a popup.
     */
    interface Holder {
        /**
         * Called after the held popup closed, however it closed, once the popup has given focus back and reported
         * its own {@link org.inspirenxe.strata.api.control.attribute.popup.Closable#CLOSED}.
         */
        void popupClosed();
    }
}
