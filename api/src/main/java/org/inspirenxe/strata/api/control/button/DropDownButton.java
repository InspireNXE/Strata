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
package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;
import java.util.Optional;

/**
 * A button with a down arrow after its caption that opens a {@link Popup} below itself, such as a
 * {@link org.inspirenxe.strata.api.control.menu.Menu} of commands or a
 * {@link org.inspirenxe.strata.api.control.menu.Flyout} holding a color picker.
 *
 * <p>A left click, or Enter, keypad Enter, Space or Alt+Down while it has focus (not repeated), asks
 * {@link Openable#OPEN}. Unless a handler cancels it, the button opens its popup below its bounds and reports
 * {@link Openable#OPENED}. The popup takes focus, and when it closes gives focus back to what had it, which is the
 * button when the user opened it. A menu opened by a key highlights its first item. When the popup closes, for any
 * reason, the button reports {@link Closable#CLOSED} after the popup's own. A click on the button while its popup is
 * open closes the popup; with a popup that closes on a press outside, the press that lands on the button closes it.
 * The button never asks {@link Closable#CLOSE}.</p>
 *
 * <p>A menu opens at least as wide as the button, and a flyout at its content's size. With no popup, or one that has
 * nothing to show (a menu with no items, a flyout with no content), the button asks nothing and stays closed. A popup
 * belongs to one button at a time.</p>
 *
 * <p>It looks and measures like a {@link Button}, with an arrow 7 units wide and 4 tall at the right end of the
 * space inside its padding, 4 units after the caption. While open it draws the pressed face. It is focusable and
 * tabbable, shows the hand cursor, and is at least 20 by 20 units. A disabled button takes no focus, does not open,
 * closes its popup, and draws its disabled face with the arrow in the disabled text style.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Openable#OPEN}: Asked when the user opens the popup. Cancel it to keep the popup closed.</li>
 *     <li>{@link Openable#OPENED}: Reported after the button opened its popup, for the user or from
 *     {@link #open()}.</li>
 *     <li>{@link Closable#CLOSED}: Reported after the popup the button opened closed, for any reason.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>Every {@code button.*} key a {@link Button} reads except the link keys, with the same requirements and
 * fallbacks, plus:</p>
 * <ul>
 *     <li>{@value #STYLE_ARROW} (paint, optional): the arrow. Without it the arrow is drawn in the caption's text
 *     color. A disabled button always draws it in the disabled caption's color.</li>
 * </ul>
 *
 * <pre>{@code
 * Menu sort = factory.menu()
 *         .addItem("By name", () -> this.sort(Order.NAME))
 *         .addItem("By price", () -> this.sort(Order.PRICE));
 * DropDownButton button = factory.dropDownButton("Sort", sort);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#dropDownButton(String)}. Do not implement this
 * interface.</p>
 */
public interface DropDownButton extends Component<DropDownButton>, Focusable<DropDownButton>,
        HasCursor<DropDownButton>, HasTooltip<DropDownButton>, HasText<DropDownButton>, Openable<DropDownButton> {
    /**
     * Style key for the arrow (paint, optional).
     */
    String STYLE_ARROW = "dropdown.arrow";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    @Override
    Text text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line
     * @return this button
     */
    @Override
    DropDownButton setText(Text text);

    /**
     * Sets the space between the button's edges and its content, which with the caption, the icon and the arrow
     * gives the desired size. Padding set here stays through theme changes.
     *
     * @param padding the padding, or {@code null} to go back to the default: the theme's
     *     {@value Button#STYLE_PADDING} (8 left and right and 5 top and bottom without it), or its
     *     {@value Button#STYLE_PADDING_ICON} (2 on every side) for a button with an icon and no caption
     * @return this button
     */
    DropDownButton setTextPadding(Insets padding);

    /**
     * Sets the same text padding on all four sides.
     *
     * @param all the text padding on every side
     * @return this button
     */
    default DropDownButton setTextPadding(final float all) {
        return this.setTextPadding(Insets.of(all));
    }

    /**
     * Sets one text padding for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the text padding on the left and right
     * @param vertical the text padding on the top and bottom
     * @return this button
     */
    default DropDownButton setTextPadding(final float horizontal, final float vertical) {
        return this.setTextPadding(Insets.of(horizontal, vertical));
    }

    /**
     * Sets the text padding on each side.
     *
     * @param left the text padding on the left
     * @param top the text padding on the top
     * @param right the text padding on the right
     * @param bottom the text padding on the bottom
     * @return this button
     */
    default DropDownButton setTextPadding(final float left, final float top, final float right, final float bottom) {
        return this.setTextPadding(new Insets(left, top, right, bottom));
    }

    /**
     * Returns the icon sprite.
     *
     * @return the sprite, or null if the icon is a visual or there is none
     */
    SpriteId iconSprite();

    /**
     * Returns the icon visual.
     *
     * @return the visual, or null if the icon is a sprite or there is none
     */
    Visual iconVisual();

    /**
     * Shows a sprite before the caption, replacing any icon. The size stays what it was, 16 by 16 by default. A new
     * layout runs.
     *
     * @param sprite the sprite; see {@link SpriteId} for where engines look for it
     * @return this button
     */
    DropDownButton setIcon(SpriteId sprite);

    /**
     * Shows a visual before the caption, replacing any icon, at the visual's natural size. A new layout runs.
     *
     * @param visual the visual
     * @return this button
     */
    DropDownButton setIcon(Visual visual);

    /**
     * Removes the icon. A new layout runs.
     *
     * @return this button
     */
    DropDownButton clearIcon();

    /**
     * Returns the width the icon is drawn at.
     *
     * @return the width in UI units; 16 by default
     */
    float iconWidth();

    /**
     * Returns the height the icon is drawn at.
     *
     * @return the height in UI units; 16 by default
     */
    float iconHeight();

    /**
     * Sets the size the icon is drawn at. Strata cannot ask an engine for a sprite's size, so the caller gives it.
     * A new layout runs.
     *
     * @param width the width in UI units, finite and not negative
     * @param height the height in UI units, finite and not negative
     * @return this button
     * @throws IllegalArgumentException if either size is negative, infinite or not a number
     */
    DropDownButton setIconSize(float width, float height);

    /**
     * Returns the popup the button opens.
     *
     * @return the popup, or empty until one is set
     */
    Optional<Popup<?>> popup();

    /**
     * Sets the popup the button opens. Replacing a popup that is open closes it first, which reports
     * {@link Closable#CLOSED} if this button opened it. The popup then belongs to this button until another popup
     * replaces it.
     *
     * @param popup a menu or flyout made by this button's factory, or {@code null} for none
     * @return this button
     * @throws IllegalArgumentException if another drop-down button holds the popup, it is a menu that opens from a
     *     menu or a menu bar, or this button's factory did not make it
     */
    DropDownButton setPopup(Popup<?> popup);

    /**
     * Returns whether the popup this button opened is open.
     *
     * @return true from {@link Openable#OPENED} until {@link Closable#CLOSED}; false for a new button, and while
     *     the popup is open from somewhere else
     */
    @Override
    boolean isOpen();

    /**
     * Opens the popup below the button and reports {@link Openable#OPENED}, without asking {@link Openable#OPEN}.
     * Does nothing if it is already open, the button is disabled or not shown, or the popup is missing or has
     * nothing to show.
     *
     * @return this button
     */
    DropDownButton open();

    /**
     * Closes the popup this button opened, which reports {@link Closable#CLOSED}. Does nothing if it is not open.
     *
     * @return this button
     */
    @Override
    DropDownButton close();

    /**
     * A drop-down button's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. Whether
     * it was pressed or hovered is in the snapshot itself.
     *
     * @param text the caption; never null
     * @param open whether the button's popup was open
     */
    record State(Text text, boolean open) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
