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
package org.inspirenxe.strata.api.control.menu;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.geometry.Rect;

import java.util.Optional;

/**
 * A popup that shows any component, such as a color picker opened from a button or a small form opened on a right
 * click.
 *
 * <p>The flyout draws its background and shows its content inside its padding, at the content's desired size plus
 * the padding, but no larger than the host. It opens below or above its anchor like any popup, or with
 * {@link #openIn} inside an area, such as the space beside a component.</p>
 *
 * <p>Opening it moves focus to the first component in the content that Tab would stop at, or else to the flyout
 * itself, which is focusable for that reason but not tabbable. While focus is inside it, Tab and Shift+Tab move only
 * among the content's components, wrapping at the ends. Escape closes it, as it closes the top overlay, and when it
 * closes focus goes back to where it was before it opened. With {@linkplain #setLightDismiss light dismiss}, the
 * default, a press outside closes it and goes no further. Without it, a press outside goes to whatever is under the
 * pointer and the flyout stays open.</p>
 *
 * <p>The content is the developer's, so its control events, such as a button's {@code PRESSED}, reach handlers
 * registered on the flyout. They go no further, since the flyout has no parent.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Openable#OPENED}: Reported each time the flyout opens.</li>
 *     <li>{@link Closable#CLOSED}: Reported each time it closes, for any reason: Escape, a press outside,
 *     {@link #close()}, or clearing its content while it is open.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, optional): behind the whole flyout. Falls back to
 *     {@value ComboBox#STYLE_POPUP}.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): between the flyout's edges and its content. Defaults to 4 on
 *     every side.</li>
 * </ul>
 *
 * <pre>{@code
 * ColorPicker picker = factory.colorPicker();
 * Flyout flyout = factory.flyout(picker);
 * swatch.onEvent(Button.PRESSED, event -> flyout.open(swatch.bounds()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#flyout()}. Do not implement this interface.</p>
 */
public interface Flyout extends Component<Flyout>, Focusable<Flyout>, HasTooltip<Flyout>, Popup<Flyout> {
    /**
     * Style key for the flyout's background (paint, optional).
     */
    String STYLE_BACKGROUND = "flyout.background";
    /**
     * Style key for the space between the flyout's edges and its content (insets, optional). Defaults to 4 on every
     * side.
     */
    String STYLE_PADDING = "flyout.padding";

    /**
     * Returns what the flyout shows.
     *
     * @return the content, or empty until one is set
     */
    Optional<Component<?>> content();

    /**
     * Sets what the flyout shows. While the flyout is open, new content takes the old content's place at once, and
     * if focus was in the flyout it moves to the new content as on opening. Clearing the content of an open flyout
     * closes it.
     *
     * @param content the content, or {@code null} for none; a flyout with no content does nothing on open
     * @return this flyout
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory
     */
    Flyout setContent(Component<?> content);

    /**
     * Returns whether a press outside the open flyout closes it.
     *
     * @return true by default
     */
    boolean isLightDismiss();

    /**
     * Sets whether a press outside the open flyout closes it. Applies at once to an open flyout.
     *
     * @param dismiss true to close on a press outside, which then goes no further; false to keep the flyout open
     *     and let the press reach what is under the pointer
     * @return this flyout
     */
    Flyout setLightDismiss(boolean dismiss);

    /**
     * Returns whether the flyout is open.
     *
     * @return true while it shows in a popup
     */
    @Override
    boolean isOpen();

    /**
     * Opens the flyout below a rectangle, or above it when there is more room there, as
     * {@link org.inspirenxe.strata.api.overlay.Overlays#popup} places popups. Does nothing if it is already open or
     * has no content.
     *
     * @param anchor the rectangle in root coordinates, such as a button's bounds
     * @return this flyout
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    @Override
    Flyout open(Rect anchor);

    /**
     * Opens the flyout with its top-left corner at a point, moved to stay inside the host. Does nothing if it is
     * already open or has no content.
     *
     * @param x the point's x in root coordinates
     * @param y the point's y in root coordinates
     * @return this flyout
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    @Override
    Flyout openAt(float x, float y);

    /**
     * Opens the flyout laid out in an area, as {@link org.inspirenxe.strata.api.overlay.Overlays#popupIn} lays
     * popups out: at its desired size, no larger than the area, where its alignment places it in the area, which is
     * the area's top-left corner by default. The area is cut to the host. To open it beside a button, pass the space
     * from the button's right edge to the host's. Does nothing if it is already open or has no content.
     *
     * @param area the area in root coordinates
     * @return this flyout
     * @throws IllegalStateException if the factory's host has no screen yet
     */
    Flyout openIn(Rect area);

    /**
     * Closes the flyout and reports {@link Closable#CLOSED}. Does nothing if it is not open.
     *
     * @return this flyout
     */
    @Override
    Flyout close();

    /**
     * A flyout's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param open whether the flyout was open
     */
    record State(boolean open) {
    }
}
