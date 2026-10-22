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
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.CellSource;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.RowView;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A scrolling list of items, each shown as a row built by a cell factory, that selects one item or several.
 *
 * <h2>Rows</h2>
 * <p>Every row is {@linkplain #setRowHeight the row height} tall, with {@linkplain #setRowSpacing the row
 * spacing} between rows, and as wide as the list less its scroll bar gutter. The list builds a component only for
 * the rows in view, from the {@linkplain #setCellFactory cell factory}, and drops it when the row scrolls out, so a
 * list of thousands of items costs no more than a screenful. A cell is laid out in its row like a child of an
 * {@code AnchorPanel}. Keep nothing in a cell that must survive scrolling; build it from the item.</p>
 *
 * <p>The list draws each row's background under its cell: the selected face, the hovered face while the pointer is
 * over the row or anything in it, or the plain face.</p>
 *
 * <h2>Selection and input</h2>
 * <ul>
 *     <li>In the default {@link SelectionMode#SINGLE} mode at most one item is selected. A left press on a row
 *     selects it; a press on the selected row deselects it only when the list {@linkplain #setDeselectable is deselectable}.
 *     A cell that consumes the press, such as a button, keeps the row from being selected.</li>
 *     <li>In {@link SelectionMode#MULTIPLE} mode Control, Shift and Control+A select several rows, as that mode
 *     describes. Those changes ask {@link MultiSelection#SELECT_MANY} in place of
 *     {@link Selection#SELECT}, and every change of the selected rows reports
 *     {@link MultiSelection#SELECTED_MANY}.</li>
 *     <li>A double press on a row {@linkplain Activatable#ACTIVATE activates} it, and so does Enter or keypad Enter
 *     on the selected row while the list has focus (not repeated). With {@link #setActivateOnClick} a single left
 *     click does instead of a double press, as in a drop-down.</li>
 *     <li>While focused: Up and Down move the selection, Page Up and Page Down by a viewport, Home and End to the
 *     ends; the list scrolls to keep the selection in view. With nothing selected, Up starts from the last row.
 *     Held keys repeat. Keys with Alt are left alone, and so are keys with Control other than Control+A in
 *     {@link SelectionMode#MULTIPLE} mode. An empty list takes no keys.</li>
 *     <li>The wheel and the scroll bar scroll as in {@link org.inspirenxe.strata.api.layout.ScrollPanel}, and the
 *     list uses its theme keys for the bar.</li>
 * </ul>
 *
 * <p>{@link #setItems} keeps each selected item selected if an item with the same {@linkplain #setKeyFunction key}
 * is still there, so a list refreshed from the server keeps the row the user picked. Items with equal keys are
 * matched in order: the second item with a key stays on the second one, or on the last when there are fewer. If no
 * item has that key, nothing is selected.</p>
 *
 * <p>The events carry row indexes, so read the item from the list: {@code list.items().get(e.value())}. Handle
 * {@link Selection#SELECT} or {@link Activatable#ACTIVATE} to refuse or change what the user does, and
 * {@link Selection#SELECTED} or {@link Activatable#ACTIVATED} to react.</p>
 *
 * <p>The list is focusable and tabbable. It asks for the width of its widest built row, at least 40 units, and the
 * height of all its rows, but no more height than it is offered; give it a height or stretch it to scroll.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Selection#SELECT}: Asked before the user changes the selection: a press on a row, a press on the
 *     selected row of a {@linkplain #setDeselectable deselectable} list, which clears it, or the Up, Down, Page Up,
 *     Page Down, Home and End keys while the list has focus. It fires only when the selection would change. The value
 *     is the index of the row about to be selected, or -1 when the user clears the selection. A handler may rewrite it
 *     to another index or to -1; the list then selects that row and scrolls it into view, as {@link #select} does, and
 *     ignores any other index outside the items. Fires only for the user's actions.</li>
 *     <li>{@link Selection#SELECTED}: Told after the selection changed. The value is the selected index, or -1
 *     when nothing is selected, and the previous value is the index before. It also fires when {@link #setItems} keeps
 *     the selection at the same index but on an item that is no longer equal to the one before, so the value can equal
 *     the previous value. Fires for changes made from code too, including {@link #setItems}.</li>
 *     <li>{@link Activatable#ACTIVATE}: Asked before the user activates a row: a double press on a row, or Enter while
 *     the list has focus and a row is selected. With {@link #setActivateOnClick} a click on a row activates it and a
 *     double press does not. The value is the row's index. A handler may rewrite it to another row; an index outside
 *     the items is ignored. Fires only for the user's actions.</li>
 *     <li>{@link Activatable#ACTIVATED}: Told after the user activated a row. The value is the row's index, and the
 *     previous value is the same index. The list has no method that activates a row from code.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, optional): the list's whole area, behind the rows, so an empty list or
 *     the space below the last row is not see-through. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_ROW_SELECTED} (paint, required): the selected row.</li>
 *     <li>{@value #STYLE_ROW} (paint, optional): every other row. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_ROW_HOVERED} (paint, optional): a row under the pointer. Falls back to
 *     {@value #STYLE_ROW}.</li>
 *     <li>{@value #STYLE_PLACEHOLDER} (text style, optional): the {@linkplain #setPlaceholder placeholder}. Falls
 *     back to the default style.</li>
 *     <li>{@value #STYLE_CELL_PADDING} (insets, optional): the margin of the cell labels that
 *     {@link org.inspirenxe.strata.api.ComponentFactory#cellLabel(String)} makes. Defaults to
 *     {@value org.inspirenxe.strata.api.ComponentFactory#CELL_PADDING} at the sides and none at the top and
 *     bottom.</li>
 *     <li>The scroll bar keys of {@link org.inspirenxe.strata.api.layout.ScrollPanel}.</li>
 * </ul>
 *
 * <pre>{@code
 * ListView<Listing> listings = factory.listView((Listing listing) -> factory.label(listing.name()))
 *         .setRowHeight(24)
 *         .setKeyFunction(Listing::record);
 * listings.onEvent(Activatable.ACTIVATED, e -> buy(listings.items().get(e.value()), 1));
 * listings.setItems(fromServer);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#listView(Function)}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface ListView<T> extends Component<ListView<T>>, Focusable<ListView<T>>, HasCursor<ListView<T>>,
        HasTooltip<ListView<T>>, ItemSource<T, ListView<T>>, CellSource<T, ListView<T>>,
        MultiSelection<T, ListView<T>>, Activatable<ListView<T>>, RowView<ListView<T>> {
    /**
     * Style key for the list's area behind its rows (paint, optional).
     */
    String STYLE_BACKGROUND = "list.background";
    /**
     * Style key for an unselected row (paint, optional).
     */
    String STYLE_ROW = "list.row";
    /**
     * Style key for an unselected row under the pointer (paint, optional).
     */
    String STYLE_ROW_HOVERED = "list.row.hovered";
    /**
     * Style key for the selected row (paint, required).
     */
    String STYLE_ROW_SELECTED = "list.row.selected";
    /**
     * Style key for the selected row while the list does not have focus (paint, optional). Defaults to
     * {@value #STYLE_ROW_SELECTED}. Trees and tables read it too.
     */
    String STYLE_ROW_SELECTED_UNFOCUSED = "list.row.selected.unfocused";
    /**
     * Style key for a one-unit ring just inside the selected row while the list has focus (paint, optional).
     * Defaults to none. Trees and tables read it too.
     */
    String STYLE_ROW_RING = "list.row.ring";
    /**
     * Style key for a one-unit ring just inside the selected row while the list does not have focus (paint,
     * optional). Defaults to none. Trees and tables read it too.
     */
    String STYLE_ROW_RING_UNFOCUSED = "list.row.ring.unfocused";
    /**
     * Style key for the space above the first row and below the last (insets, optional), in UI units, which
     * scrolls with the rows. Only its top and bottom are read. Defaults to none. Lists inside other controls ignore it.
     */
    String STYLE_PADDING = "list.padding";
    /**
     * Style key for the widest a row gets (metric, optional), in UI units. A list wider than this centers a column
     * of rows this wide and puts its scroll bar beside the column, {@value #STYLE_BAR_GAP} units right of it, even
     * where that reaches past the list's right edge. Defaults
     * to 0, which makes rows as wide as the list. Lists inside other controls, such as a table's or a combo box's,
     * ignore it.
     */
    String STYLE_ROW_WIDTH = "list.row.width";
    /**
     * Style key for the space between a centered column of rows and the scroll bar (metric, optional), in UI units.
     * Read only when {@value #STYLE_ROW_WIDTH} narrows the rows. Defaults to 2.
     */
    String STYLE_BAR_GAP = "list.bar.gap";
    /**
     * Style key for how far one wheel notch scrolls (metric, optional), in rows. Defaults to 1. Trees and tables
     * read it too.
     */
    String STYLE_SCROLL_ROWS = "list.scroll.rows";
    /**
     * Style key for a one-unit line just outside the list's top and bottom edges (paint, optional), next to the
     * list. Defaults to none. Lists inside other controls do not draw it.
     */
    String STYLE_EDGE = "list.edge";
    /**
     * Style key for a one-unit line outside {@value #STYLE_EDGE}, away from the list (paint, optional). Defaults to
     * none.
     */
    String STYLE_EDGE_OUTER = "list.edge.outer";
    /**
     * Style key for the placeholder text (text style, optional).
     */
    String STYLE_PLACEHOLDER = "list.placeholder";
    /**
     * Style key for the margin around the default cell of lists, trees and tables, the cells
     * {@link org.inspirenxe.strata.api.ComponentFactory#cell(Object)} and
     * {@link org.inspirenxe.strata.api.ComponentFactory#cellLabel(String)} make, and the rows of a combo box's
     * or a text box's list (insets, optional). Defaults to
     * {@value org.inspirenxe.strata.api.ComponentFactory#CELL_PADDING} at the sides and none at the top and
     * bottom.
     */
    String STYLE_CELL_PADDING = "list.cell.padding";

    /**
     * Returns the items.
     *
     * @return an unmodifiable copy of the items
     */
    List<T> items();

    /**
     * Replaces the items and rebuilds the rows in view. Each selected item moves to the item with its key, as
     * {@link ItemSource} describes, or leaves the selection; {@link #SELECTED} fires if the selected item or its
     * index changed. The scroll offset is kept, clamped to the new length.
     *
     * @param items the items; the list keeps a copy
     * @return this list
     * @throws NullPointerException if an item is null
     */
    ListView<T> setItems(List<? extends T> items);

    /**
     * Rebuilds the rows in view from the same items, for when the items changed inside.
     *
     * @return this list
     */
    ListView<T> refresh();

    /**
     * Sets what builds the component for an item's row, and rebuilds the rows in view.
     *
     * @param cellFactory makes a new component for an item each time its row comes into view
     * @return this list
     */
    ListView<T> setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Sets how items are told apart when {@link #setItems} keeps the selection. By default an item is its own key,
     * compared with {@code equals}.
     *
     * @param keyFunction returns the key of an item; keys are compared with {@code equals}
     * @return this list
     */
    ListView<T> setKeyFunction(Function<? super T, ?> keyFunction);

    /**
     * Returns the row height.
     *
     * @return the height of every row; {@value #DEFAULT_ROW_HEIGHT} by default
     */
    float rowHeight();

    /**
     * Sets the height of every row.
     *
     * @param height the height in UI units
     * @return this list
     * @throws IllegalArgumentException if the height is not greater than 0 or not finite
     */
    ListView<T> setRowHeight(float height);

    /**
     * Returns the row spacing.
     *
     * @return the space between rows; 0 by default
     */
    float rowSpacing();

    /**
     * Sets the space between rows. No background is drawn in it.
     *
     * @param spacing the space in UI units
     * @return this list
     * @throws IllegalArgumentException if the spacing is negative or not finite
     */
    ListView<T> setRowSpacing(float spacing);

    /**
     * Returns the selected index.
     *
     * @return the index of the selected item, or -1 when nothing is selected
     */
    int selectedIndex();

    /**
     * Returns the selected item.
     *
     * @return the selected item, or empty when nothing is selected
     */
    Optional<T> selected();

    /**
     * Selects the item at an index and scrolls it into view.
     *
     * @param index the index, or -1 to clear the selection
     * @return this list
     * @throws IndexOutOfBoundsException if the index is not -1 and not an item's index
     */
    ListView<T> select(int index);

    /**
     * Selects the first item with the same key as the given one, or clears the selection if there is none.
     *
     * @param item the item to find by its key, or null to clear the selection
     * @return this list
     */
    ListView<T> selectItem(T item);

    /**
     * Returns whether a press on the selected row deselects it.
     *
     * @return false by default
     */
    boolean isDeselectable();

    /**
     * Sets whether a press on the selected row deselects it.
     *
     * @param deselectable true to allow deselecting by pressing
     * @return this list
     */
    ListView<T> setDeselectable(boolean deselectable);

    /**
     * Sets whether a single click activates a row, instead of a double click.
     *
     * @param onClick true to activate on every click
     * @return this list
     */
    ListView<T> setActivateOnClick(boolean onClick);

    /**
     * Sets the text shown in the middle of the list while it has no items.
     *
     * @param placeholder the text, or {@code null} for none
     * @return this list
     */
    ListView<T> setPlaceholder(Text placeholder);
}
