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
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.command.HasCommand;
import org.inspirenxe.strata.api.control.attribute.text.HasIcon;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * A button that stays down while it is on, such as Bold on a formatting toolbar or List in a view switcher.
 *
 * <p>It looks, measures and lays out as a {@link Button} does, with the same caption, icon and padding, and reads
 * the same {@code button.*} keys. While it is on it draws the selected faces in place of the idle and hovered ones.
 * A left click flips it, and so does Enter, keypad Enter or Space while it has focus (not repeated). On a platform
 * whose conventions say buttons act on the press, the left press flips it instead of the click. It consumes those
 * inputs. Each flip by the user asks {@link Toggle#TOGGLE}, and every change reports {@link Toggle#TOGGLED}. A
 * disabled toggle button does neither and draws its disabled look.</p>
 *
 * <p>It can show a {@link Command.Toggle}: it takes the command's text, follows its enabled source, and its state
 * and the command's stay the same both ways, as a check box's do.</p>
 *
 * <h2>Theme keys</h2>
 * <p>Every key {@link Button} reads for a framed button, and these:</p>
 * <ul>
 *     <li>{@value #STYLE_SELECTED} (paint, optional): the face while on. Falls back to
 *     {@value Button#STYLE_PRESSED}.</li>
 *     <li>{@value #STYLE_SELECTED_HOVERED} (paint, optional): the face while on and hovered or focused. Falls back
 *     to {@value #STYLE_SELECTED}.</li>
 *     <li>{@value #STYLE_SELECTED_DISABLED} (paint, optional): the face while on and disabled. Falls back to
 *     {@value Button#STYLE_DISABLED}.</li>
 * </ul>
 *
 * <pre>{@code
 * ToggleButton bold = factory.toggleButton().setIcon(BOLD).setTooltip("Bold")
 *         .onEvent(Toggle.TOGGLED, event -> editor.setBold(event.value()));
 * }</pre>
 *
 * <p>Its caption marks a mnemonic with an underscore, as {@link Button} describes: Alt with the letter focuses and
 * flips it.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#toggleButton(String)}. Do not implement this
 * interface.</p>
 */
public interface ToggleButton extends Component<ToggleButton>, Focusable<ToggleButton>, HasCursor<ToggleButton>,
        HasTooltip<ToggleButton>, HasContextMenu<ToggleButton>, HasText<ToggleButton>, HasIcon<ToggleButton>,
        Toggle<ToggleButton>, HasCommand<Command.Toggle, ToggleButton> {
    /**
     * Style key for the face while on (paint, optional).
     */
    String STYLE_SELECTED = "button.selected";
    /**
     * Style key for the face while on and hovered or focused (paint, optional).
     */
    String STYLE_SELECTED_HOVERED = "button.selected.hovered";
    /**
     * Style key for the face while on and disabled (paint, optional).
     */
    String STYLE_SELECTED_DISABLED = "button.selected.disabled";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    Text text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line; may be empty for an icon alone
     * @return this button
     */
    ToggleButton setText(Text text);

    /**
     * Sets the space between the caption and the button's edges, as {@link Button#setTextPadding(Insets)} does.
     *
     * @param padding the padding; {@link #clearTextPadding()} goes back to the default
     * @return this button
     */
    ToggleButton setTextPadding(Insets padding);

    /**
     * Goes back to the default text padding, as {@link Button#clearTextPadding()} does. A new layout runs.
     *
     * @return this button
     */
    ToggleButton clearTextPadding();

    /**
     * Sets the same text padding on all four sides.
     *
     * @param all the text padding on every side
     * @return this button
     */
    default ToggleButton setTextPadding(final float all) {
        return this.setTextPadding(Insets.of(all));
    }

    /**
     * Sets one text padding for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the text padding on the left and right
     * @param vertical the text padding on the top and bottom
     * @return this button
     */
    default ToggleButton setTextPadding(final float horizontal, final float vertical) {
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
    default ToggleButton setTextPadding(final float left, final float top, final float right, final float bottom) {
        return this.setTextPadding(new Insets(left, top, right, bottom));
    }

    /**
     * Returns where the icon sits beside the caption.
     *
     * @return the placement; {@link IconPlacement#START} for a new button
     */
    IconPlacement iconPlacement();

    /**
     * Sets where the icon sits beside the caption. A new layout runs.
     *
     * @param placement the placement
     * @return this button
     */
    ToggleButton setIconPlacement(IconPlacement placement);

    /**
     * A toggle button's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. Whether it was
     * pressed or hovered is in the snapshot itself.
     *
     * @param selected whether the button was {@linkplain #isSelected() on}
     * @param text the caption; never null
     */
    record State(boolean selected, Text text) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
