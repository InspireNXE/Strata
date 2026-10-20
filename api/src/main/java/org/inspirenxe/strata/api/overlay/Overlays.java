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
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;

/**
 * Shows components above a host's screen: popups such as a drop-down list, and dialogs such as a confirmation
 * box. Each host has one; get it from {@link org.inspirenxe.strata.api.ComponentFactory#overlays()}.
 *
 * <p>Overlays stack in the order they open, each drawn above the screen and the overlays before it, outside every
 * clip. While any overlay is open:</p>
 * <ul>
 *     <li>The pointer reaches the topmost overlay under it, and the screen only where no overlay is.</li>
 *     <li>A press outside a popup closes it and goes no further. A press on a popup below the top one closes the
 *     popups above it.</li>
 *     <li>A dialog is modal: nothing below it gets pointer input, focus, or Tab, and keys go to it when nothing in it
 *     has focus.</li>
 *     <li>An Escape press that nothing consumed closes the topmost overlay that is not a toast.</li>
 * </ul>
 *
 * <p>Toasts are short notices that close themselves. They stack down the top-right corner, {@value #TOAST_MARGIN}
 * units in from the edges and {@value #TOAST_MARGIN} apart, in the order they opened, above the overlays opened
 * before them. A press elsewhere does not close them, and a click on one closes it. They take no focus.</p>
 *
 * <p>Opening a dialog moves focus to the first tabbable component in it, and closing it puts focus back where it
 * was if that component can still take it. Closing the screen closes every overlay.</p>
 *
 * <p>Strata provides this object. Do not implement this interface. Call it on the UI thread only.</p>
 */
public interface Overlays {
    /**
     * Style key for the shade drawn over everything below an open dialog (paint, optional). Nothing is drawn if it
     * is missing.
     */
    String STYLE_BACKDROP = "overlay.backdrop";
    /**
     * Style key for behind a text toast (paint, optional). Without it a text toast draws on the tooltip
     * background.
     */
    String STYLE_TOAST = "toast.background";
    /**
     * Style key for a text toast's text (text style, optional). Falls back to the tooltip text style.
     */
    String STYLE_TOAST_TEXT = "toast.text";
    /**
     * How long a toast shows unless told otherwise, in milliseconds.
     */
    long DEFAULT_TOAST_MILLIS = 3000L;
    /**
     * The space around toasts, in UI units.
     */
    float TOAST_MARGIN = 4f;

    /**
     * Opens a popup next to an area, usually the bounds of the component that opened it. The popup takes its
     * desired size. It sits below the area, left edges lined up, or above it when there is more room above. It is
     * moved left as needed to stay inside the host.
     *
     * @param content what to show; it must not be in a tree already
     * @param anchor the area to show it next to, in the host's coordinates
     * @return the open popup
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory
     */
    Overlay popup(Component<?> content, Rect anchor);

    /**
     * Opens a modal dialog. The content is laid out in the whole host, where its own alignment places it: a
     * centered window stays centered when the host resizes.
     *
     * @param content what to show; it must not be in a tree already
     * @return the open dialog
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory
     */
    Overlay dialog(Component<?> content);

    /**
     * Shows a line of text as a toast for {@value #DEFAULT_TOAST_MILLIS} milliseconds. The text wraps at a third of
     * the host's width and sits 6 units in from the toast's edges; {@code 
} starts a new line.
     *
     * @param message the text
     * @return the open toast
     */
    Overlay toast(Text message);

    /**
     * Does the same as {@link #toast(Text)} with literal text.
     *
     * @param message the text
     * @return the open toast
     */
    default Overlay toast(final String message) {
        return this.toast(Text.of(message));
    }

    /**
     * Shows a component as a toast for a time. The content takes its desired size and draws its own background.
     * The time is measured by the platform's clock, and the toast closes on the first frame drawn after it runs out.
     *
     * @param content what to show; it must not be in a tree already
     * @param millis how long it shows, in milliseconds
     * @return the open toast
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory, or
     *     {@code millis} is not greater than 0
     */
    Overlay toast(Component<?> content, long millis);

    /**
     * Finds the overlay a component is shown in.
     *
     * @param component a component, or anything inside an overlay's content
     * @return the overlay whose content is the component or one of its ancestors; empty if the component is not
     *     in an open overlay
     */
    Optional<Overlay> of(Component<?> component);

    /**
     * Returns the open overlays.
     *
     * @return a copy, bottom first
     */
    List<Overlay> open();

    /**
     * Closes every open overlay, top first.
     */
    void closeAll();
}
