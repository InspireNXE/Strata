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
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.attribute.value.Validatable;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * A box that is checked, empty or mixed, with a caption to its right.
 *
 * <p>A left click anywhere on the checkbox changes it, and so does Enter, keypad Enter or Space while it has focus
 * (not repeated). On a platform whose conventions say checkboxes act on the press, the left press changes it instead
 * of the click. It consumes those inputs. A change checks an empty or mixed box and empties a checked one. With
 * {@link #setAllowsMixed(boolean) mixed allowed}, the user's changes cycle empty, checked, mixed instead. Code sets
 * the mixed state with {@link #setState}, for example on a box that stands for several others while only some of them
 * are checked. A disabled checkbox takes no changes from the user and draws its disabled look.</p>
 *
 * <p>Each change by the user fires {@link #CHANGE}, and then {@link Toggle#TOGGLE} if it would change
 * {@link #isSelected()}, which is true only while checked. Every change fires {@link #CHANGED}, and
 * {@link Toggle#TOGGLED} when {@code isSelected()} changed, so a handler on {@code TOGGLED} hears a box go between
 * checked and anything else.</p>
 *
 * <p>It is focusable and tabbable and shows the hand cursor. The box is square, {@linkplain #setBoxSize sized} to the
 * caption's line height plus 8 units by default, rounded to whole units so the mark centers exactly, which is 17
 * with a 9-unit font. The caption sits 4 units right of the box, centered on it vertically. The desired size is the
 * box, the gap and the caption's width wide (just the box when the caption is empty), and the taller of the box and
 * the caption.</p>
 *
 * <h2>Theme keys</h2>
 * <p>The box paints cover the whole box. Themes built from images draw the check mark into the checked paints;
 * themes built from colors draw it with {@value #STYLE_MARK} instead. A mixed box uses the unchecked paints with the
 * {@value #STYLE_MARK_MIXED} square over them.</p>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the box when unchecked.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, required): the box when unchecked and hovered or focused.</li>
 *     <li>{@value #STYLE_CHECKED} (paint, required): the box when checked.</li>
 *     <li>{@value #STYLE_CHECKED_HOVERED} (paint, required): the box when checked and hovered or focused.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): the box when unchecked and disabled. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_CHECKED_DISABLED} (paint, optional): the box when checked and disabled. Falls back to
 *     {@value #STYLE_CHECKED}.</li>
 *     <li>{@value #STYLE_MARK} (paint, optional): a check mark drawn over the box while checked, inside a fifth of
 *     the box size from each edge. Nothing if missing.</li>
 *     <li>{@value #STYLE_MARK_DISABLED} (paint, optional): the mark when disabled. Falls back to
 *     {@value #STYLE_MARK}.</li>
 *     <li>{@value #STYLE_MARK_MIXED} (paint, optional): a square over the box while mixed, inset by a fifth of the box
 *     size. Falls back to {@value #STYLE_MARK}; nothing if both are missing.</li>
 *     <li>{@value #STYLE_MARK_MIXED_DISABLED} (paint, optional): the mixed square when disabled. Falls back to
 *     {@value #STYLE_MARK_DISABLED}, then to the mixed square.</li>
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
        HasText<CheckBox>, Toggle<CheckBox>, Validatable<CheckBox>, HasCommand<Command.Toggle, CheckBox> {
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
     * Style key for the box while the check box is enabled and its validation is an error, in place of the four
     * enabled faces, checked or not (paint, optional). The check and mixed marks still draw on it. Without it the box
     * draws its usual face with a one-unit outline in {@value Validatable#STYLE_ERROR}.
     */
    String STYLE_INVALID = "checkbox.invalid";
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
     * Style key for the square drawn over a mixed box (paint, optional).
     */
    String STYLE_MARK_MIXED = "checkbox.mark.mixed";
    /**
     * Style key for the mixed square when disabled (paint, optional).
     */
    String STYLE_MARK_MIXED_DISABLED = "checkbox.mark.mixed.disabled";
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
     * Asked before the user changes the box. The value is the state the change would leave; cancel to keep the box as
     * it is, or set another state. Fires only for the user's actions, before {@link Toggle#TOGGLE}.
     */
    EventType<ValueRequestEvent<CheckBox, CheckState>> CHANGE = EventType.valueRequest("checkbox.change");
    /**
     * Told after the state changed, by the user or from code. The value is the new state and the previous value the
     * state before.
     */
    EventType<ValueEvent<CheckBox, CheckState>> CHANGED = EventType.valueReport("checkbox.changed");
    /**
     * The state, as {@link #state()} and {@link #setState} say. Binds two ways through {@link #CHANGED}. Bind
     * {@link Toggle#SELECTED} instead where the box never shows {@link CheckState#MIXED}.
     */
    PropertyKey<CheckBox, CheckState> STATE = PropertyKey.of("checkbox.state", CheckBox::state, CheckBox::setState,
            CHANGED);

    /**
     * Returns the state.
     *
     * @return the state; {@link CheckState#UNCHECKED} for a new box
     */
    CheckState state();

    /**
     * Sets the state. {@link #CHANGED} fires if it changed, and {@link Toggle#TOGGLED} if {@link #isSelected()}
     * changed with it. {@code setSelected} sets {@link CheckState#CHECKED} or {@link CheckState#UNCHECKED}.
     *
     * @param state the state
     * @return this checkbox
     */
    CheckBox setState(CheckState state);

    /**
     * Returns whether the user's changes include the mixed state.
     *
     * @return whether the user can choose mixed; false by default
     */
    boolean allowsMixed();

    /**
     * Sets whether the user's changes cycle empty, checked, mixed, for a box where mixed is a choice of its own. By
     * default a change checks an empty or mixed box and empties a checked one, and only code sets mixed.
     *
     * @param allowsMixed whether the user can choose mixed
     * @return this checkbox
     */
    CheckBox setAllowsMixed(boolean allowsMixed);

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
     * @return this checkbox
     */
    CheckBox setText(Text text);

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
     * @return this checkbox
     * @throws IllegalArgumentException if {@code size} is negative or infinite
     */
    CheckBox setBoxSize(float size);

    /**
     * A checkbox's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param state the box's {@linkplain #state() state}; never null
     * @param text the caption; never null
     */
    record State(CheckState state, Text text) {

        /**
         * Checks the fields.
         */
        public State {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(text, "text");
        }

        /**
         * Returns whether the box was checked.
         *
         * @return whether the state is {@link CheckState#CHECKED}
         */
        public boolean selected() {
            return this.state.isChecked();
        }
    }
}
