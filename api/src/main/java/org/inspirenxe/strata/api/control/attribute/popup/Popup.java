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
package org.inspirenxe.strata.api.control.attribute.popup;

import org.inspirenxe.strata.api.geometry.Rect;

/**
 * A component that shows itself in an overlay, opened at an anchor and closed by the user or from code: a
 * {@link org.inspirenxe.strata.api.control.menu.Menu} or a {@link org.inspirenxe.strata.api.control.menu.Flyout}. A
 * {@link org.inspirenxe.strata.api.control.button.DropDownButton} opens any popup below itself.
 *
 * <p>A popup is opened with {@link #open(Rect)}, {@link #openAt(float, float)} or {@link #open()}, never added to a
 * container. While
 * open it is the whole content of a popup overlay (see {@link org.inspirenxe.strata.api.overlay.Overlays#popup}),
 * so it has no parent and its events reach only the handlers registered on it, not the screen behind it.</p>
 *
 * <p>An open popup takes focus. When it closes it gives focus back to the component that had it when the popup
 * opened, if that component can still take it.</p>
 *
 * <p>A popup opened from code never asks {@link Openable#OPEN}. Whatever opens it for the user, such as a drop-down
 * button or a menu bar, asks on its own behalf. A popup reports {@link Openable#OPENED} each time it opens and
 * {@link Closable#CLOSED} each time it closes, for any reason.</p>
 *
 * <p>When the user closes it with Escape or a press outside it, the popup asks {@link Closable#CLOSE} first, with the
 * user as the origin, so a handler that cancels keeps it open, as a flyout holding an unsaved form may; the key or
 * press is used up either way. A press outside that a refusing popup keeps also keeps the popups under it open.
 * {@link #close()} from code, and a popup closed because the one it opened from closed, ask nothing.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the methods return
 */
public interface Popup<S extends Popup<S>> extends Openable<S> {
    /**
     * Opens the popup below a rectangle, or above it when there is more room there, as
     * {@link org.inspirenxe.strata.api.overlay.Overlays#popup} places popups. Does nothing if it is already open or
     * has nothing to show, which each implementation defines.
     *
     * @param anchor the rectangle in root coordinates, such as a button's bounds
     * @return this popup
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    S open(Rect anchor);

    /**
     * Opens the popup with its top-left corner at a point, moved to stay inside the host. Does nothing if it is
     * already open or has nothing to show.
     *
     * @param x the point's x in root coordinates
     * @param y the point's y in root coordinates
     * @return this popup
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    S openAt(float x, float y);

    /**
     * Opens the popup where it last opened: at the rectangle or point of its last {@link #open(Rect)} or
     * {@link #openAt(float, float)}, or wherever else its kind last opened it, such as a flyout's area. A popup that
     * never opened opens at the pointer, as {@code openAt} would at the pointer's position. Does nothing if it is
     * already open or has nothing to show.
     *
     * @return this popup
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    @Override
    S open();

    /**
     * Closes the popup and reports {@link Closable#CLOSED}. Does nothing if it is not open.
     *
     * @return this popup
     */
    @Override
    S close();
}
