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

import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.command.HasCommand;
import org.inspirenxe.strata.api.control.attribute.text.HasIcon;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.Objects;

/**
 * A push button with a text caption, an icon, or both.
 *
 * <p>The button is pressed when it is left-clicked, or when it has focus and Enter or Space is pressed (not
 * repeated). On a platform whose conventions say buttons act on the press, the left press presses it instead of the
 * click. It consumes those inputs. Each press fires {@link #PRESS}, then {@link #PRESSED} unless a handler
 * canceled the request, and then runs the button's {@linkplain #setCommand command} if it has one. A disabled button
 * does none of it and draws its disabled look.</p>
 *
 * <p>It is focusable and tabbable, shows the hand cursor, and has a minimum width of 20. Its desired size is the
 * caption plus the text padding (8 units left and right, 5 top and bottom by default), and at least 20 tall; a
 * smaller height set on it applies. The caption is centered in whatever size the button ends up.</p>
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
 *     <li>{@value #STYLE_DEFAULT} (paint, optional): the face of a default button when idle, in place of
 *     {@value #STYLE_NORMAL}. Falls back to {@value #STYLE_NORMAL}.</li>
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
        HasText<Button>, HasIcon<Button>, HasCommand<Command.Action, Button> {
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
     * Style key for the idle face of a {@linkplain #setDefault default button} (paint, optional).
     */
    String STYLE_DEFAULT = "button.default";
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
     * Style key for where a caption too wide for its button is cut, as insets from the button's edges (insets,
     * optional). Defaults to the padding's left and right, and none at the top and bottom. A caption that fits is
     * kept inside this area too. A cycle button reads it too.
     */
    String STYLE_CLIP = "button.clip";
    /**
     * Style key for how fast a caption too wide for its button scrolls (metric, optional), in milliseconds per unit
     * of overflow for one sweep there and back. Defaults to 0, which cuts the caption at the clip area's right
     * instead. A cycle button reads it too.
     */
    String STYLE_TEXT_SCROLL = "button.text.scroll";
    /**
     * Style key for the shortest sweep of a scrolling caption (metric, optional), in milliseconds. Defaults to 0.
     * A cycle button reads it too.
     */
    String STYLE_TEXT_SCROLL_MINIMUM = "button.text.scroll.minimum";

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
     * @param padding the padding; {@link #clearTextPadding()} goes back to the default
     * @return this button
     */
    Button setTextPadding(Insets padding);

    /**
     * Goes back to the default text padding: the theme's {@value #STYLE_PADDING} (8 left and right and 5 top and
     * bottom without it), its {@value #STYLE_PADDING_ICON} (2 on every side) for a button with an icon and no
     * caption, and none for a link. A new layout runs.
     *
     * @return this button
     */
    Button clearTextPadding();

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
     * Returns whether the button is a link.
     *
     * @return true if it draws no frame
     */
    boolean isLink();

    /**
     * Makes the button a link or a framed button again. A link draws no face and no focus ring, only its caption in
     * the link text styles, and the caption underlines while the link is hovered or focused. It has no minimum size
     * and, unless {@link #setTextPadding} was called, no padding; turning the link off restores the minimum width of
     * 20 and the desired height of at least 20. A minimum size set with {@code setMinSize} is kept either way, before
     * or after this call. A new layout runs.
     *
     * @param link true for a link
     * @return this button
     */
    Button setLink(boolean link);

    /**
     * Returns whether the button is its scope's default button.
     *
     * @return true after {@code setDefault(true)}; false for a new button
     */
    boolean isDefault();

    /**
     * Makes the button the default button of its scope, or an ordinary one again. A default button is pressed for
     * the user, with {@link #PRESS} first, by an Enter press with no modifiers that nothing on the focus path
     * consumed, while focus is inside the nearest window that holds the button, or inside the screen's content or the
     * overlay that holds it when no window does. It must be visible and enabled, with every ancestor. A text box
     * consumes Enter only while a handler for its submit events is on its path, so the fields of a form submit
     * through the default button without code. When a scope holds several default buttons, the first in tree order
     * is pressed. A default button draws the {@value #STYLE_DEFAULT} face when idle.
     *
     * @param isDefault true to make it the default button
     * @return this button
     */
    Button setDefault(boolean isDefault);

    /**
     * Returns whether the button is its scope's cancel button.
     *
     * @return true after {@code setCancel(true)}; false for a new button
     */
    boolean isCancel();

    /**
     * Makes the button the cancel button of its scope, or an ordinary one again. A cancel button is pressed for the
     * user by an Escape press with no modifiers that nothing on the focus path consumed, in the same scope as a
     * {@linkplain #setDefault default button}, before the host closes the top overlay with it. A window that would
     * close on Escape presses its cancel button instead.
     *
     * @param cancel true to make it the cancel button
     * @return this button
     */
    Button setCancel(boolean cancel);

    /**
     * Presses the button as if it had been clicked, firing {@link #PRESSED} but not {@link #PRESS}, then running its
     * {@linkplain #command() command} if it has one and the command is enabled. Does nothing while the button is
     * disabled.
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
