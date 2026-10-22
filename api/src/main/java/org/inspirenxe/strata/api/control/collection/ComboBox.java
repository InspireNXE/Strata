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
package org.inspirenxe.strata.api.control.collection;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A drop-down: shows the selected option and opens a list of all options to pick from.
 *
 * <p>A left click opens the list as a popup below the box (above it if there is more room there), at least as wide as
 * the box. A click on an option selects it and closes the list; a click anywhere else, Escape, or focus leaving the
 * box, as Tab does, closes it without changing the selection. The list shows at most {@linkplain #setMaxVisibleOptions the maximum} rows and scrolls
 * the rest.</p>
 *
 * <p>While focused and closed: Up and Down select the previous and next option, Home and End the first and last,
 * and Enter, keypad Enter, Space or Alt+Down open the list. The open list takes no focus, so the keys stay with the
 * box: Up and Down move the highlight without wrapping, Enter, keypad Enter or Space selects it, and Escape closes.
 * Held arrows repeat; Enter, Space and Alt+Down do not. A box with no options takes no keys, and a disabled box
 * ignores input.</p>
 *
 * <p>Options are drawn with the {@linkplain #setLabelFunction label function}. The one that
 * {@link org.inspirenxe.strata.api.ComponentFactory#comboBox()} sets shows each option by its appearance's name, or
 * by {@link String#valueOf(Object)} when it has no appearance. When options are set, the selection moves to the
 * option with the selected option's {@linkplain #setKeyFunction key}, or to the first option if there is none.
 * Code can clear the selection with {@link #clearSelection()}; the box then shows no text until something is
 * selected.</p>
 *
 * <p>The selection events carry option indexes, so read the option from the box:
 * {@code box.items().get(e.value())}. The open list is private to the box and fires no events of its own. The box
 * reports {@link Closable#CLOSED} each time the list closes, however it closed, and never fires
 * {@link Closable#CLOSE}.</p>
 *
 * <p>The box is focusable and tabbable and shows the hand cursor. It asks for its widest option plus padding and
 * the arrow, and one line plus 3 units above and below.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Openable#OPEN}: Asked before the user opens the list: a click on the closed box, or Enter, Space or
 *     Alt+Down while it has focus. It fires only when the list can open, which needs the box attached, enabled and
 *     with options. A handler may change the options before the list opens; the box checks again afterwards and stays
 *     closed if it has no options left. Fires only for the user's actions.</li>
 *     <li>{@link Openable#OPENED}: Told after the list opened. Fires for {@link #open()} called from code too.</li>
 *     <li>{@link SingleSelection#SELECT}: Asked before the user changes the selection: a click on a row of the open
 *     list, Enter or Space on the highlighted row of the open list, or the Up, Down, Home and End keys on the closed
 *     box. A choice from the open list closes it before this fires. Moving the highlight in the open list does not fire
 *     it, and neither does a choice of the option already selected. The value is the index of the option about to be
 *     selected. A handler may rewrite it to another option's index; an index outside the options is ignored. Fires only
 *     for the user's actions.</li>
 *     <li>{@link SingleSelection#SELECTED}: Told after the selection changed. The value is the selected index, -1 once
 *     nothing is selected, and the previous value is the index before. It also fires when {@link #setItems} keeps
 *     the index but the option there is no longer equal to the one before, so the value can equal the previous value.
 *     Fires for changes made from code too, including {@link #setItems}.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the box.</li>
 *     <li>{@value #STYLE_HOVERED} (paint, optional): the box while hovered, focused or open. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, optional): the box when disabled. Falls back to
 *     {@value #STYLE_NORMAL}.</li>
 *     <li>{@value #STYLE_POPUP} (paint, required): behind the open list.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the selected option and the options in the list. Falls back
 *     to the default style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the selected option when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>The row keys of {@link ListView} and the scroll bar keys of
 *     {@link org.inspirenxe.strata.api.layout.ScrollPanel}, for the open list.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): the space between the box's edges and its shown value on the
 *     left and its arrow on the right. Defaults to 4 at the sides and 3 at the top and bottom.</li>
 * </ul>
 *
 * <pre>{@code
 * ComboBox<SortType> sort = factory.comboBox(SortType::label)
 *         .setItems(List.of(SortType.values()))
 *         .onEvent(SingleSelection.SELECTED, e -> search());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#comboBox(Function)}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the options
 */
public interface ComboBox<T> extends Component<ComboBox<T>>, Focusable<ComboBox<T>>, HasCursor<ComboBox<T>>,
        HasTooltip<ComboBox<T>>, ItemSource<T, ComboBox<T>>,
        SingleSelection<T, ComboBox<T>>, Openable<ComboBox<T>> {
    /**
     * Style key for the box (paint, required).
     */
    String STYLE_NORMAL = "combobox.normal";
    /**
     * Style key for the box while hovered, focused or open (paint, optional).
     */
    String STYLE_HOVERED = "combobox.hovered";
    /**
     * Style key for the box when disabled (paint, optional).
     */
    String STYLE_DISABLED = "combobox.disabled";
    /**
     * Style key for the background of the open list (paint, required).
     */
    String STYLE_POPUP = "combobox.popup";
    /**
     * Style key for the option text (text style, optional).
     */
    String STYLE_TEXT = "combobox.text";
    /**
     * Style key for the selected option when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "combobox.text.disabled";
    /**
     * Style key for the space between the box's edges and its shown value on the left and its arrow on the
     * right (insets, optional). Defaults to 4 at the sides and 3 at the top and bottom.
     */
    String STYLE_PADDING = "combobox.padding";
    /**
     * How many rows the open list shows by default before it scrolls.
     */
    int DEFAULT_MAX_VISIBLE_OPTIONS = 8;

    /**
     * Returns the options.
     *
     * @return an unmodifiable copy of the options
     */
    @Override
    List<T> items();

    /**
     * Closes the list, then replaces the options, so a {@link Closable#CLOSED} handler sees the options and the
     * selection from before. The selection moves to the option with the selected option's key, or to the first one. {@link SingleSelection#SELECTED} fires if the selected option or its index changed.
     *
     * @param items the options; the box keeps a copy
     * @return this box
     */
    @Override
    ComboBox<T> setItems(List<? extends T> items);

    /**
     * Sets how options are drawn.
     *
     * @param labelFunction returns what to show for an option: a {@link org.inspirenxe.strata.api.text.Text}, or any
     *     other value, shown by its appearance's name; the option itself by default
     * @return this box
     */
    ComboBox<T> setLabelFunction(Function<? super T, ?> labelFunction);

    /**
     * Returns the selected option.
     *
     * @return the selected option, or empty while nothing is selected
     */
    @Override
    Optional<T> selected();

    /**
     * Returns the selected index.
     *
     * @return the index of the selected option, or -1 while nothing is selected
     */
    @Override
    int selectedIndex();

    /**
     * Selects the option at an index, or nothing for -1.
     *
     * @param index the index, or -1
     * @return this box
     * @throws IndexOutOfBoundsException if the index is not -1 and not an option's index
     */
    @Override
    ComboBox<T> select(int index);

    /**
     * Selects the first option with the same key as the given one, or nothing if there is none.
     *
     * @param item the option
     * @return this box
     */
    @Override
    ComboBox<T> selectItem(T item);

    /**
     * Returns how many rows the open list shows before it scrolls.
     *
     * @return the maximum; {@value #DEFAULT_MAX_VISIBLE_OPTIONS} by default
     */
    int maxVisibleOptions();

    /**
     * Sets how many rows the open list shows before it scrolls.
     *
     * @param count the maximum
     * @return this box
     * @throws IllegalArgumentException if the count is less than 1
     */
    ComboBox<T> setMaxVisibleOptions(int count);

    /**
     * Returns whether the list is open.
     *
     * @return true while the popup shows
     */
    @Override
    boolean isOpen();

    /**
     * Opens the list. Does nothing while detached, disabled, without options or already open.
     *
     * @return this box
     */
    ComboBox<T> open();

    /**
     * Closes the list without changing the selection, and reports {@link Closable#CLOSED} if it was open.
     *
     * @return this box
     */
    @Override
    ComboBox<T> close();
}
