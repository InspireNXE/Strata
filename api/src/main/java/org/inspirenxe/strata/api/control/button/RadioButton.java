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
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * One choice in a {@link RadioGroup}: a box that is selected or not, with a caption to its right.
 *
 * <p>A left click selects it, and so does Enter, keypad Enter or Space while it has focus (not repeated). On a
 * platform whose conventions say radio buttons act on the press, the left press selects it instead of the click.
 * Clicking a selected button leaves it selected. Arrow keys move the selection through the group, as {@link RadioGroup}
 * describes. It consumes those inputs. Each selection by the user fires {@link Toggle#TOGGLE} with true on the
 * button about to be selected. Every change fires {@link Toggle#TOGGLED} on each button whose state changed, then
 * {@link SingleSelection#SELECTED} on the group. A disabled button does none of this and draws its disabled
 * look.</p>
 *
 * <p>It is focusable and shows the hand cursor, and Tab stops at it when it is its group's selected button, or its
 * group's first enabled button while none is selected, as {@link RadioGroup} says. It measures and lays out like a
 * {@link CheckBox}: a square box the caption's line height plus 8 units on a side, rounded to a whole unit, and the
 * caption 4 units to its right.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the box when not selected.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, required): the box when not selected and hovered or focused.</li>
 *     <li>{@value #STYLE_SELECTED} (paint, required): the box when selected.</li>
 *     <li>{@value #STYLE_SELECTED_HOVERED} (paint, required): the box when selected and hovered or focused.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): the box when not selected and disabled. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_SELECTED_DISABLED} (paint, optional): the box when selected and disabled. Falls back to
 *     {@value #STYLE_SELECTED}.</li>
 *     <li>{@value #STYLE_MARK} (paint, optional): drawn over the box while selected, inset by three tenths of the
 *     box size. Nothing if missing.</li>
 *     <li>{@value #STYLE_MARK_DISABLED} (paint, optional): the mark when disabled. Falls back to
 *     {@value #STYLE_MARK}.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring drawn inside the box's edge while focused.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the caption when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <p>Its caption marks a mnemonic with an underscore, as {@link Button} describes: Alt with the letter focuses and
 * selects the button.</p>
 *
 * <p>Get one from {@link RadioGroup#add}. Do not implement this interface.</p>
 */
public interface RadioButton extends Component<RadioButton>, Focusable<RadioButton>, HasCursor<RadioButton>,
        HasTooltip<RadioButton>, HasContextMenu<RadioButton>, HasText<RadioButton>, Toggle<RadioButton> {
    /**
     * Style key for the box when not selected (paint, required).
     */
    String STYLE_NORMAL = "radio.normal";
    /**
     * Style key for the box when not selected and hovered or focused (paint, required).
     */
    String STYLE_HOVERED = "radio.hovered";
    /**
     * Style key for the selected box (paint, required).
     */
    String STYLE_SELECTED = "radio.selected";
    /**
     * Style key for the selected box when hovered or focused (paint, required).
     */
    String STYLE_SELECTED_HOVERED = "radio.selected.hovered";
    /**
     * Style key for the box when not selected and disabled (paint, optional).
     */
    String STYLE_DISABLED = "radio.disabled";
    /**
     * Style key for the selected box when disabled (paint, optional).
     */
    String STYLE_SELECTED_DISABLED = "radio.selected.disabled";
    /**
     * Style key for the mark drawn over a selected box (paint, optional).
     */
    String STYLE_MARK = "radio.mark";
    /**
     * Style key for the mark when disabled (paint, optional).
     */
    String STYLE_MARK_DISABLED = "radio.mark.disabled";
    /**
     * Style key for the focus ring (paint, optional).
     */
    String STYLE_FOCUS = "radio.focus";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "radio.text";
    /**
     * Style key for the caption when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "radio.text.disabled";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    Text text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line; may be empty for a box alone
     * @return this button
     */
    RadioButton setText(Text text);

    /**
     * Returns the group that made this button. A button {@linkplain RadioGroup#remove removed} from it still returns
     * it, is no longer in its {@linkplain RadioGroup#buttons() buttons}, and can no longer be selected.
     *
     * @return the group that made it
     */
    RadioGroup<?> group();

    /**
     * Returns whether this button is the group's selected one.
     *
     * @return true if selected
     */
    @Override
    boolean isSelected();

    /**
     * Selects this button, deselecting the group's other buttons, or deselects it, leaving nothing selected.
     * {@link Toggle#TOGGLED} fires on each button whose state changed, then the group fires
     * {@link SingleSelection#SELECTED}. Works while disabled too.
     *
     * @param selected true to select this button
     * @return this button
     */
    @Override
    RadioButton setSelected(boolean selected);

    /**
     * A radio button's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param selected whether the button was {@linkplain #isSelected() selected}
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
