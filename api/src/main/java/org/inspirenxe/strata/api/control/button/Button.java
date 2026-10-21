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
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;

/**
 * A push button with a text caption, an icon, or both.
 *
 * <p>The button is pressed when it is left-clicked, or when it has focus and Enter or Space is pressed (not
 * repeated). It consumes those inputs. Each press fires {@link #PRESS}, then {@link #PRESSED} unless a handler
 * canceled the request. A disabled button does neither and draws its disabled look.</p>
 *
 * <p>It is focusable and tabbable, shows the hand cursor, and has a minimum size of 20 by 20. Its desired size is
 * the caption plus the text padding (8 units left and right, 5 top and bottom by default). The caption is centered
 * in whatever size the button ends up.</p>
 *
 * <p>An {@linkplain #setIcon(SpriteId) icon} is drawn before the caption, 4 units from it, and the two are centered
 * together. A button with an icon and no caption is meant to be square, so it pads by 2 units on every side and a
 * 16 by 16 icon gives 20 by 20. A disabled button draws its icon in half-bright gray.</p>
 *
 * <p>{@linkplain #setLink A link} has no frame: it draws only its caption, which underlines while hovered or
 * focused, and pads by nothing and has no minimum size.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the face when idle.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, required): the face when hovered or focused.</li>
 *     <li>{@value #STYLE_PRESSED} (paint, required): the face while pressed with the pointer still over it.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, required): the face when disabled.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring drawn inside the edge while focused. No ring if
 *     missing.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the caption when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <p>A link reads no face and no focus ring, and these keys instead:</p>
 * <ul>
 *     <li>{@value #STYLE_LINK_TEXT} (text style, optional): the caption. Falls back to {@value #STYLE_TEXT}.</li>
 *     <li>{@value #STYLE_LINK_TEXT_HOVERED} (text style, optional): the caption while hovered or focused. Falls back
 *     to {@value #STYLE_LINK_TEXT} underlined.</li>
 *     <li>{@value #STYLE_LINK_TEXT_DISABLED} (text style, optional): the caption when disabled. Falls back to
 *     {@value #STYLE_TEXT_DISABLED}.</li>
 * </ul>
 *
 * <pre>{@code
 * Button save = factory.button("Save").onEvent(Button.PRESSED, event -> this.save());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#button(String)}. Do not implement this
 * interface.</p>
 */
public interface Button extends Component<Button>, Focusable<Button>, HasCursor<Button>, HasTooltip<Button>,
        HasText<Button> {
    /**
     * Style key for the idle face (paint, required).
     */
    String STYLE_NORMAL = "button.normal";
    /**
     * Style key for the hovered or focused face (paint, required).
     */
    String STYLE_HOVERED = "button.hovered";
    /**
     * Style key for the pressed face (paint, required).
     */
    String STYLE_PRESSED = "button.pressed";
    /**
     * Style key for the disabled face (paint, required).
     */
    String STYLE_DISABLED = "button.disabled";
    /**
     * Style key for the focus ring (paint, optional).
     */
    String STYLE_FOCUS = "button.focus";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "button.text";
    /**
     * Style key for the caption when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "button.text.disabled";
    /**
     * Style key for the caption of a link (text style, optional).
     */
    String STYLE_LINK_TEXT = "button.link.text";
    /**
     * Style key for the caption of a link while hovered or focused (text style, optional).
     */
    String STYLE_LINK_TEXT_HOVERED = "button.link.text.hovered";
    /**
     * Style key for the caption of a link when disabled (text style, optional).
     */
    String STYLE_LINK_TEXT_DISABLED = "button.link.text.disabled";
    /**
     * Style key for the space between a button's edges and its caption, or its icon and caption (insets,
     * optional). Defaults to 8 at the sides and 5 at the top and bottom. A cycle button reads it too.
     */
    String STYLE_PADDING = "button.padding";
    /**
     * Style key for the space around the icon of a button with an icon and no caption (insets, optional).
     * Defaults to 2 on every side.
     */
    String STYLE_PADDING_ICON = "button.padding.icon";

    /**
     * Fired when the user presses the button by click or key, before it is pressed. Canceling it refuses the press,
     * so {@link #PRESSED} does not fire. Fires only for the user's actions.
     */
    EventType<RequestEvent<Button>> PRESS = EventType.request("button.press");
    /**
     * Fired after the button was pressed, by the user or by {@link #press()}.
     */
    EventType<ControlEvent<Button>> PRESSED = EventType.report("button.pressed");

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    Text text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line
     * @return this button
     */
    Button setText(Text text);

    /**
     * Sets the space between the caption and the button's edges, which with the caption gives the desired size.
     * Padding set here stays through theme changes.
     *
     * @param padding the padding, or {@code null} to go back to the default: the theme's {@value #STYLE_PADDING}
     *     (8 left and right and 5 top and bottom without it), its {@value #STYLE_PADDING_ICON} (2 on every side)
     *     for a button with an icon and no caption, and none for a link
     * @return this button
     */
    Button setTextPadding(Insets padding);

    /**
     * Sets the same text padding on all four sides.
     *
     * @param all the text padding on every side
     * @return this button
     */
    default Button setTextPadding(final float all) {
        return this.setTextPadding(Insets.of(all));
    }

    /**
     * Sets one text padding for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the text padding on the left and right
     * @param vertical the text padding on the top and bottom
     * @return this button
     */
    default Button setTextPadding(final float horizontal, final float vertical) {
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
    default Button setTextPadding(final float left, final float top, final float right, final float bottom) {
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
    Button setIcon(SpriteId sprite);

    /**
     * Shows a visual before the caption, replacing any icon, at the visual's natural size. A new layout runs.
     *
     * @param visual the visual
     * @return this button
     */
    Button setIcon(Visual visual);

    /**
     * Removes the icon. A new layout runs.
     *
     * @return this button
     */
    Button clearIcon();

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
    Button setIconSize(float width, float height);

    /**
     * Returns whether the button is a link.
     *
     * @return true if it draws no frame
     */
    boolean isLink();

    /**
     * Makes the button a link or a framed button again. A link draws no face and no focus ring, only its caption in
     * the link text styles, and the caption underlines while the link is hovered or focused. It has no minimum size
     * and, unless {@link #setTextPadding} was called, no padding; turning the link off restores the minimum size of
     * 20 by 20. A new layout runs.
     *
     * @param link true for a link
     * @return this button
     */
    Button setLink(boolean link);

    /**
     * Presses the button as if it had been clicked, firing {@link #PRESSED} but not {@link #PRESS}. Does nothing
     * while the button is disabled.
     */
    void press();

    /**
     * A button's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. Whether it was
     * pressed or hovered is in the snapshot itself.
     *
     * @param text the caption; never null
     */
    record State(Text text) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
