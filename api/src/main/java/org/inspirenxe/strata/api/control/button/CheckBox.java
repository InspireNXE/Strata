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
import org.inspirenxe.strata.api.control.attribute.value.Toggle;

import java.util.Objects;

/**
 * A box that is checked or not, with a caption to its right.
 *
 * <p>A left click anywhere on the check box toggles it, and so does Enter, keypad Enter or Space while it has focus
 * (not repeated). It consumes those inputs. Each toggle by the user fires {@link Toggle#TOGGLE}, and every change
 * fires {@link Toggle#TOGGLED}. A disabled check box does neither and draws its disabled look.</p>
 *
 * <p>It is focusable and tabbable and shows the hand cursor. The box is square, {@linkplain #setBoxSize sized} to the
 * caption's line height plus 8 units by default, which is 17 with a 9-unit font.
 * The caption sits 4 units right of the box, centered on it vertically. The desired size is the box, the gap and the
 * caption's width wide (just the box when the caption is empty), and the taller of the box and the caption.</p>
 *
 * <h2>Theme keys</h2>
 * <p>The box paints cover the whole box. Themes built from images draw the check mark into
 * the checked paints; themes built from colors draw it with {@value #STYLE_MARK} instead.</p>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the box when unchecked.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, required): the box when unchecked and hovered or focused.</li>
 *     <li>{@value #STYLE_CHECKED} (paint, required): the box when checked.</li>
 *     <li>{@value #STYLE_CHECKED_HOVERED} (paint, required): the box when checked and hovered or focused.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): the box when unchecked and disabled. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_CHECKED_DISABLED} (paint, optional): the box when checked and disabled. Falls back to
 *     {@value #STYLE_CHECKED}.</li>
 *     <li>{@value #STYLE_MARK} (paint, optional): drawn over the box while checked, inset by a fifth of the box size.
 *     Nothing if missing.</li>
 *     <li>{@value #STYLE_MARK_DISABLED} (paint, optional): the mark when disabled. Falls back to
 *     {@value #STYLE_MARK}.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring drawn inside the box's edge while focused. No ring
 *     if missing.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the caption when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * CheckBox music = factory.checkBox("Music")
 *         .setSelected(settings.music())
 *         .onEvent(Toggle.TOGGLED, event -> settings.setMusic(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#checkBox(String)}. Do not implement this
 * interface.</p>
 */
public interface CheckBox extends Component<CheckBox>, Focusable<CheckBox>, HasCursor<CheckBox>, HasTooltip<CheckBox>,
        HasText<CheckBox>, Toggle<CheckBox> {
    /**
     * Style key for the unchecked box (paint, required).
     */
    String STYLE_NORMAL = "checkbox.normal";
    /**
     * Style key for the unchecked box when hovered or focused (paint, required).
     */
    String STYLE_HOVERED = "checkbox.hovered";
    /**
     * Style key for the checked box (paint, required).
     */
    String STYLE_CHECKED = "checkbox.checked";
    /**
     * Style key for the checked box when hovered or focused (paint, required).
     */
    String STYLE_CHECKED_HOVERED = "checkbox.checked.hovered";
    /**
     * Style key for the unchecked box when disabled (paint, optional).
     */
    String STYLE_DISABLED = "checkbox.disabled";
    /**
     * Style key for the checked box when disabled (paint, optional).
     */
    String STYLE_CHECKED_DISABLED = "checkbox.checked.disabled";
    /**
     * Style key for the check mark drawn over a checked box (paint, optional).
     */
    String STYLE_MARK = "checkbox.mark";
    /**
     * Style key for the check mark when disabled (paint, optional).
     */
    String STYLE_MARK_DISABLED = "checkbox.mark.disabled";
    /**
     * Style key for the focus ring (paint, optional).
     */
    String STYLE_FOCUS = "checkbox.focus";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "checkbox.text";
    /**
     * Style key for the caption when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "checkbox.text.disabled";

    /**
     * Returns the caption.
     *
     * @return the caption text
     */
    String text();

    /**
     * Replaces the caption. A new layout runs.
     *
     * @param text the caption, on one line; may be empty for a box alone
     * @return this check box
     */
    CheckBox setText(String text);

    /**
     * Returns the size of the square box.
     *
     * @return the box size in UI units: the one set with {@link #setBoxSize}, otherwise the caption's line height
     *         plus 8 as of the last layout
     */
    float boxSize();

    /**
     * Fixes the size of the square box. A new layout runs.
     *
     * @param size the box size in UI units, or {@link Float#NaN} for the caption's line height plus 8
     * @return this check box
     * @throws IllegalArgumentException if {@code size} is negative or infinite
     */
    CheckBox setBoxSize(float size);

    /**
     * A check box's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param selected whether the box was {@linkplain #isSelected() checked}
     * @param text the caption; never null
     */
    record State(boolean selected, String text) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
