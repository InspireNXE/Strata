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
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.CellSource;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Two lists side by side with buttons between them that move items across, for picking a subset such as the members
 * of a group.
 *
 * <h2>Moving</h2>
 * <ul>
 *     <li>The {@code >} button moves the selected item of the left list to the end of the right list, and {@code <}
 *     moves the selected item of the right list to the end of the left one. Each is enabled only while its list has
 *     the selection.</li>
 *     <li>The {@code >>} and {@code <<} buttons move every item of the left or right list across, in order. Each is
 *     enabled only while its list has items.</li>
 *     <li>A double click on a row, or Enter while a list has focus and a selected row, moves that row's item
 *     across.</li>
 * </ul>
 * <p>When the selected item moves, the selection goes to the item that took its place on the same side, or to the one
 * before it when it was last, so pressing the same button again moves the next item. {@link #move} and
 * {@link #moveAll} move items from code.</p>
 *
 * <h2>Selection</h2>
 * <p>At most one item is selected across both lists; selecting a row in one list clears the selection in the other.
 * Indexes for {@link SingleSelection} count the left items first and then the right ones, so index
 * {@code items(Side.LEFT).size()} is the first item on the right. The {@linkplain #setKeyFunction key function}
 * finds items for {@link #selectItem}, for moves, and for keeping the selection across {@link #setItems}.</p>
 *
 * <p>Each list is a {@link ListView} with the default row height, built from the same {@linkplain #setCellFactory
 * cell factory}. The lists and the buttons are focusable and tabbable, in the order left list, buttons, right
 * list. Items must not be null.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link #MOVE}: Fired when the user is about to move an item: with a button, a double click or Enter. Moving
 *     every item fires it once per item. The value names the item and where it would go, which is the end of the other
 *     list. Cancel the event to leave the item where it is, or set another move: an item on the other side of the one
 *     the move names, and an index there, which is clamped to the list. A move whose item is not on that side is
 *     ignored. Fires only for the user's actions: {@link #move} and {@link #moveAll} fire none.</li>
 *     <li>{@link #MOVED}: Fired after an item moved, by the user or by {@link #move} or {@link #moveAll}. The value
 *     names the item, its new side and its index there, and {@code previous()} the side and index it left. Fires for
 *     changes made from code too.</li>
 *     <li>{@link SingleSelection#SELECT}: Fired when the user selects a row by a press or the arrow keys, with the
 *     index as described above.</li>
 *     <li>{@link SingleSelection#SELECTED}: Fired after every change of the selected index or item: by the user, by
 *     {@link #select}, by a move or by {@link #setItems}.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_LIST} (paint, optional): behind each list. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_TITLE} (text style, optional): the titles above the lists. Falls back to
 *     {@value org.inspirenxe.strata.api.control.display.Label#STYLE_TEXT}.</li>
 * </ul>
 * <p>The rows use the {@link ListView} keys and the buttons the
 * {@link org.inspirenxe.strata.api.control.button.Button} keys.</p>
 *
 * <h2>Size</h2>
 * <p>Each list asks to be {@value #DEFAULT_LIST_WIDTH} units wide and {@linkplain #setVisibleRows tall enough} for
 * {@value #DEFAULT_VISIBLE_ROWS} rows. The buttons sit in a column between the lists, as wide as the widest of them,
 * {@value #SPACING} units from each list and from each other, centered on the lists' height. When either side has a
 * title, a row of titles one line plus {@value #SPACING} units tall sits above the lists. Given more or less room, the
 * lists share the width equally and take the height.</p>
 *
 * <pre>{@code
 * DualList<String> members = factory.<String>dualList()
 *         .setTitle(DualList.Side.LEFT, "Players")
 *         .setTitle(DualList.Side.RIGHT, "Members")
 *         .setItems(DualList.Side.LEFT, List.of("Ana", "Ben", "Cleo"))
 *         .setItems(DualList.Side.RIGHT, List.of("Dev"))
 *         .onEvent(DualList.MOVED, event -> group.setMembers(members.items(DualList.Side.RIGHT)));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#dualList()}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface DualList<T> extends Component<DualList<T>>, HasCursor<DualList<T>>, HasTooltip<DualList<T>>,
        CellSource<T, DualList<T>>,
        SingleSelection<T, DualList<T>> {
    /**
     * Style key for behind each list (paint, optional).
     */
    String STYLE_LIST = "duallist.list";
    /**
     * Style key for the titles above the lists (text style, optional).
     */
    String STYLE_TITLE = "duallist.title";
    /**
     * How many rows each list is tall enough for unless told otherwise.
     */
    int DEFAULT_VISIBLE_ROWS = 6;
    /**
     * The width each list asks for, in UI units.
     */
    float DEFAULT_LIST_WIDTH = 100f;
    /**
     * The space between the lists and the buttons, between the buttons, and below the titles, in UI units.
     */
    float SPACING = 4f;

    /**
     * Asked before the user moves an item to the other list. The value names the item and where it would go. A
     * handler may cancel the move or set another one, as the class description says; the value must not be set to
     * null. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<DualList<?>, Move<?>>> MOVE = EventType.valueRequest("dual_list.move");
    /**
     * Told after an item moved to the other list. The value names the item, its new side and its index there; the
     * previous value names the same item, the side it left and its index there. Fires for changes made from code too.
     */
    EventType<ValueEvent<DualList<?>, Move<?>>> MOVED = EventType.valueReport("dual_list.moved");

    /**
     * Returns the items of one side.
     *
     * @param side which list
     * @return an unmodifiable list of the items, top to bottom
     */
    List<T> items(Side side);

    /**
     * Replaces the items of one side. If the selection is on that side, it stays on the item with the same key, or
     * clears when there is none. {@link #SELECTED} fires if the selected index or item changed, which includes the
     * indexes of the right side shifting when the left side changes length.
     *
     * @param side which list
     * @param items the new items, copied; must not contain null
     * @return this list
     */
    DualList<T> setItems(Side side, List<? extends T> items);

    /**
     * Builds the rows of both lists again, after the items changed in a way their cells show.
     *
     * @return this list
     */
    DualList<T> refresh();

    /**
     * Sets how items are matched: by {@link #selectItem}, by moves, and when {@link #setItems} keeps the selection.
     *
     * @param keyFunction returns the key of an item; items with equal keys are the same item. The item itself by
     *                    default.
     * @return this list
     */
    DualList<T> setKeyFunction(Function<? super T, ?> keyFunction);

    /**
     * Returns the title of one side.
     *
     * @param side which list
     * @return the title above that list; empty by default
     */
    Text title(Side side);

    /**
     * Sets the title above one list. The row of titles shows while either side has a title that is not empty.
     *
     * @param side which list
     * @param title the title, or an empty string for none
     * @return this list
     */
    DualList<T> setTitle(Side side, Text title);

    /**
     * Does the same as {@link #setTitle(Side, Text)} with literal text.
     *
     * @param side which list
     * @param title the title, or an empty string for none
     * @return this list
     */
    default DualList<T> setTitle(final Side side, final String title) {
        return this.setTitle(side, Text.of(title));
    }

    /**
     * Returns how many rows the lists ask to be tall enough for.
     *
     * @return the number of rows; {@value #DEFAULT_VISIBLE_ROWS} by default
     */
    int visibleRows();

    /**
     * Sets how many rows the lists ask to be tall enough for. A list with more items scrolls.
     *
     * @param rows the number of rows
     * @return this list
     * @throws IllegalArgumentException if {@code rows} is less than 1
     */
    DualList<T> setVisibleRows(int rows);

    /**
     * Returns the side the selection is on.
     *
     * @return the side of the selected item, or empty when nothing is selected
     */
    Optional<Side> selectedSide();

    /**
     * Returns the selected index, counting the left items first and then the right ones.
     *
     * @return the index, or -1 when nothing is selected
     */
    @Override
    int selectedIndex();

    /**
     * Returns the selected item.
     *
     * @return the selected item on either side, or empty when nothing is selected
     */
    @Override
    Optional<T> selected();

    /**
     * Selects the item at an index, counting the left items first and then the right ones, scrolls it into view and
     * clears the selection in the other list. {@link #SELECTED} fires if the selection changed.
     *
     * @param index the index, or -1 to select nothing
     * @return this list
     * @throws IndexOutOfBoundsException if {@code index} is neither -1 nor the index of an item on either side
     */
    @Override
    DualList<T> select(int index);

    /**
     * Selects the item with the same key, looking in the left list first, or nothing if neither list has it.
     *
     * @param item the item
     * @return this list
     */
    @Override
    DualList<T> selectItem(T item);

    /**
     * Moves an item to the end of one list from the other, as the user's moves do but without {@link #MOVE}.
     * {@link #MOVED} fires. Does nothing if the other list has no item with the same key.
     *
     * @param item the item to move
     * @param to the list it goes to
     * @return this list
     */
    DualList<T> move(T item, Side to);

    /**
     * Moves every item of one list to the end of the other, in order, as {@link #move} does for each.
     *
     * @param to the list they go to
     * @return this list
     */
    DualList<T> moveAll(Side to);

    /**
     * One of the two lists.
     */
    enum Side {
        /**
         * The list on the left.
         */
        LEFT,
        /**
         * The list on the right.
         */
        RIGHT;

        /**
         * Returns the other side.
         *
         * @return {@link #RIGHT} for {@link #LEFT}, and {@link #LEFT} for {@link #RIGHT}
         */
        public Side opposite() {
            return this == LEFT ? RIGHT : LEFT;
        }
    }

    /**
     * An item and a place in one of the lists.
     *
     * @param <T> the type of the item
     * @param item the item
     * @param side the list
     * @param index the item's index in that list
     */
    record Move<T>(T item, Side side, int index) {

        /**
         * Checks the values.
         *
         * @throws IllegalArgumentException if {@code index} is negative
         */
        public Move {
            Objects.requireNonNull(item, "item");
            Objects.requireNonNull(side, "side");
            if (index < 0) {
                throw new IllegalArgumentException("Index must not be negative: " + index);
            }
        }
    }

    /**
     * A dual list's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param left the items of the left list
     * @param right the items of the right list
     * @param selectedIndex the {@linkplain #selectedIndex() selected index}, or -1
     */
    record State(List<?> left, List<?> right, int selectedIndex) {

        /**
         * Copies the lists.
         */
        public State {
            left = List.copyOf(left);
            right = List.copyOf(right);
        }
    }
}
