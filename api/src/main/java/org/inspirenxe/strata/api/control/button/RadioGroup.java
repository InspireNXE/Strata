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

import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;

/**
 * A set of {@link RadioButton}s of which at most one is selected. Each button stands for a value, and the group
 * reports the value of the selected button. The group is not a component: place its buttons anywhere in the tree,
 * in any layout.
 *
 * <p>A new group has no buttons and nothing selected. {@link #add} makes a button in it, and {@link #remove} and
 * {@link #clear} take buttons out again, so a group rebuilt from new choices drops its old buttons. Selecting a
 * button deselects the one before it. While a button
 * has focus, Up and Left move the selection and focus to the previous button in the group that is in a tree, enabled
 * and visible, and Down and Right to the next one, wrapping at the ends. Held arrows repeat, and arrows with Control
 * or Alt are left alone. The focused button consumes the arrows it acts on, even when no other button can take the
 * selection.</p>
 *
 * <p>The group is one stop for Tab: only the selected button is {@linkplain RadioButton#isTabbable() tabbable}, or
 * the first enabled button while none is selected, so Tab moves into the group and out of it in one step and the
 * arrows move inside it. The group sets this on each new button, each change of the selection and each button
 * enabled or disabled. A button whose tabbable flag code changed with {@code setTabbable} keeps that setting.</p>
 *
 * <p>The group is an {@link EventTarget} of its own for {@link SingleSelection#SELECTED}, which carries the index
 * of the selected button in {@link #buttons()}. The event's {@linkplain org.inspirenxe.strata.api.event.Event#target()
 * target} is the newly selected button, or the deselected one when the selection was cleared. Its events reach only
 * the handlers registered on the group, and do not bubble through the tree its buttons are in. The group never fires
 * {@link SingleSelection#SELECT}; to refuse a selection, handle {@link Toggle#TOGGLE} on the buttons or on a
 * container around them.</p>
 *
 * <pre>{@code
 * RadioGroup<Difficulty> difficulty = factory.radioGroup();
 * column.add(difficulty.add("Easy", Difficulty.EASY))
 *         .add(difficulty.add("Hard", Difficulty.HARD));
 * difficulty.selectItem(settings.difficulty())
 *         .onEvent(SingleSelection.SELECTED, event -> difficulty.selected().ifPresent(settings::setDifficulty));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#radioGroup()}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the values the buttons stand for
 */
public interface RadioGroup<T> extends SingleSelection<T, RadioGroup<T>> {
    /**
     * Makes a new, unselected button in this group. The button is not added to any container.
     *
     * @param text the button's caption
     * @param value the value the button stands for; may be null
     * @return the new button
     */
    RadioButton add(Text text, T value);

    /**
     * Does the same as {@link #add(Text, T)} with literal text.
     *
     * @param text the button's caption
     * @param value the value the button stands for; may be null
     * @return the new button
     */
    default RadioButton add(final String text, final T value) {
        return this.add(Text.of(text), value);
    }

    /**
     * Takes a button out of the group. Removing the selected button deselects it, which reports {@link Toggle#TOGGLED}
     * on it, and the group fires {@link SingleSelection#SELECTED} with -1. Removing a button before the selected one
     * fires {@code SELECTED} with the selected button's new index and its old one as the previous value. The group
     * leaves the button in whatever container holds it; removed, it can no longer be selected, and code is expected
     * to take it out of its container or throw it away. A button that is not in the group is ignored.
     *
     * @param button the button to remove
     * @return this group
     */
    RadioGroup<T> remove(RadioButton button);

    /**
     * Takes every button out of the group, as {@link #remove} does for each, so a selected button reports
     * {@link Toggle#TOGGLED} and the group fires {@link SingleSelection#SELECTED} with -1 once. The group leaves the
     * buttons in whatever containers hold them. Does nothing for an empty group.
     *
     * @return this group
     */
    RadioGroup<T> clear();

    /**
     * Returns the group's buttons in the order they were made.
     *
     * @return an unmodifiable list of the buttons
     */
    List<RadioButton> buttons();

    /**
     * Returns the value of the button at an index.
     *
     * @param index the button's index in {@link #buttons()}
     * @return the value, which may be null
     * @throws IndexOutOfBoundsException if there is no button at {@code index}
     */
    T value(int index);

    /**
     * Returns the selected button.
     *
     * @return the selected button, or empty if none is
     */
    Optional<RadioButton> selectedButton();

    /**
     * Returns the index of the selected button.
     *
     * @return the selected button's index in {@link #buttons()}, or -1 if none is
     */
    @Override
    int selectedIndex();

    /**
     * Returns the value of the selected button.
     *
     * @return the selected value, or empty if no button is selected or the selected button stands for null
     */
    @Override
    Optional<T> selected();

    /**
     * Selects the button at an index, or deselects every button for -1. Fires {@link Toggle#TOGGLED} on each button
     * whose state changed, then {@link SingleSelection#SELECTED} if the selection changed. Works on disabled
     * buttons too.
     *
     * @param index the button's index in {@link #buttons()}, or -1
     * @return this group
     * @throws IndexOutOfBoundsException if the index is not -1 and not a button's index
     */
    @Override
    RadioGroup<T> select(int index);

    /**
     * Selects the first button that stands for a value, compared with {@link Object#equals}, or deselects every
     * button if none does. Fires the events {@link #select(int)} does. Works on disabled buttons too.
     *
     * @param value the value to select; may be null
     * @return this group
     */
    @Override
    RadioGroup<T> selectItem(T value);
}
