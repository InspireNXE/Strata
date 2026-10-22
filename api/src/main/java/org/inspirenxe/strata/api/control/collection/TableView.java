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
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.CellEditing;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.RowView;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Rows of items under a header of columns, such as a list of sale offers with a name, a price and a seller.
 *
 * <p>The body is a {@link ListView}: only the rows in view are built, it scrolls the same way, and it selects and
 * activates rows the same way. Each row lays out one cell per {@linkplain TableColumn#isShown shown} column, made by
 * that column's cell factory. The header sits above the body, as tall as the theme's {@value #STYLE_HEADER_HEIGHT}
 * says and 20 units by default, and lines its columns up with the cells even when the scroll bar shows.</p>
 *
 * <p>Column widths work like a {@link org.inspirenxe.strata.api.layout.GridPanel}'s: fixed widths first, then
 * {@link GridLength#auto() auto} columns, then weighted columns share what is left by weight. An auto column is as
 * wide as the widest of its title, plus 8 units and 11 more for the arrow when sortable, and its cells in view,
 * measured when the table is first laid out and again after {@link #setItems} and {@link #refresh()}. Each column
 * stays within its {@linkplain TableColumn#setWidthLimits width limits}. Titles sit 4 units in from the left of
 * their column, and a sort arrow 4 units in from the right.</p>
 *
 * <p>The table does not scroll sideways by default, so columns wider than it are cut off. With
 * {@link #setHorizontalPolicy} set to {@code AUTO} or {@code ALWAYS}, columns keep their widths, weighted columns
 * their share of the body or their minimum width, and the header and the rows scroll sideways together, with a bar
 * along the bottom, the wheel's horizontal scrolling and Shift with the wheel where the engine sends it so.</p>
 *
 * <p>The user changes the columns from the header: dragging the right edge of a {@linkplain TableColumn#isResizable
 * resizable} column's title resizes it, through {@link #RESIZE_COLUMN}, and fixes its width; a double click there
 * fits it to its title and its cells in view. Dragging a {@linkplain TableColumn#isMovable movable} title moves the
 * column, through {@link #MOVE_COLUMN}; columns are the table's own entries, so the table reorders
 * {@link #columns()} itself. A right click on the header opens a menu of check items, one per column, that show and
 * hide them; the last shown column cannot be hidden.</p>
 *
 * <p>A column with a comparator is sortable. Clicking its title sorts the rows by it ascending, then descending,
 * then back to the order the items were set in, and the title shows an arrow. Shift and a click adds the column to
 * the sort order, or moves it on through the same three steps if it is there, so rows that tie on the first column
 * are sorted by the next, through {@link #SORT_MANY}. Text columns sort by their text, ignoring case, unless given
 * another comparator. Sorting changes only the order the rows are shown in: {@link #items()} keeps the order they
 * were set in, while {@link #shownItems()} and every row index follow the order on screen.</p>
 *
 * <p>Columns with a {@linkplain TableColumn#setCellEditor cell editor} are edited in place, as {@link CellEditing}
 * describes: a double press edits the cell under the pointer when its column has an editor, and F2 and a typed
 * character edit the current row's first shown column that has one.</p>
 *
 * <p>The table is focusable and tabbable, and keeps focus on its body: {@link #requestFocus()} focuses the body,
 * {@link #isFocused()} is true while the body has focus, a press on the header focuses it too, and the tab order
 * holds the table once. {@link Focusable#FOCUS_GAINED} and {@link Focusable#FOCUS_LOST} fire on the table.</p>
 *
 * <p>The table fires its own events. The selection and activation events carry row indexes in the shown order, so
 * read the item with {@code table.shownItems().get(e.value())}. The body list is private to the table: its
 * {@link ListView} events never reach anything outside the table.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Selection#SELECT}: Asked before the user changes the selection: a press on a row, or the Up,
 *     Down, Page Up, Page Down, Home and End keys while focus is in the table. It fires only when the selection would
 *     change. The value is the row of {@link #shownItems()} about to be selected, or -1 when the user clears the
 *     selection of a {@linkplain #setDeselectable deselectable} table. A handler may rewrite it to another row or to
 *     -1; the table then selects as {@link #select} does and ignores any other index outside the rows. In
 *     {@link SelectionMode#MULTIPLE} mode the user's changes ask {@link MultiSelection#SELECT_MANY} instead, as in a
 *     {@link ListView}. Fires only for the user's actions.</li>
 *     <li>{@link Selection#SELECTED}: Told after the selected row changed. The value is the selected row of
 *     {@link #shownItems()}, or -1 when nothing is selected, and the previous value is the row before. A sort or
 *     {@link #setItems} that moves the selected item to another row fires it too, and so does {@link #setItems} when
 *     the item kept selected is no longer equal to the one before, in which case the value can equal the previous
 *     value. Fires for changes made from code too.</li>
 *     <li>{@link Activatable#ACTIVATE}: Asked before the user activates a row: a double press on a row, or Enter while
 *     focus is in the table and a row is selected. The value is the row of {@link #shownItems()}. A handler may rewrite
 *     it to another row; an index outside the rows is ignored. Fires only for the user's actions.</li>
 *     <li>{@link Activatable#ACTIVATED}: Told after the user activated a row. The value is the row of
 *     {@link #shownItems()}, and the previous value is the same row. The table has no method that activates a row from
 *     code.</li>
 *     <li>{@link #SORT} and {@link #SORTED}: the first sort, as described on each.</li>
 *     <li>{@link #SORT_MANY} and {@link #SORTED_MANY}: the whole sort order, as described on each.</li>
 *     <li>{@link #RESIZE_COLUMN} and {@link #COLUMN_RESIZED}: a column's width, as described on each.</li>
 *     <li>{@link #MOVE_COLUMN} and {@link #COLUMN_MOVED}: a column's place, as described on each.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>The body reads the {@link ListView} keys. The header reads these.</p>
 * <ul>
 *     <li>{@value #STYLE_HEADER} (paint, required): behind each column title.</li>
 *     <li>{@value #STYLE_HEADER_HOVERED} (paint, optional): behind a sortable title under the pointer. Falls back
 *     to {@value #STYLE_HEADER}.</li>
 *     <li>{@value #STYLE_HEADER_TEXT} (text style, optional): the titles and sort arrows. Falls back to the default
 *     text style.</li>
 *     <li>{@value #STYLE_GRID} (paint, optional): a one-unit line between columns, in the header and the body.
 *     Nothing if missing.</li>
 *     <li>{@value #STYLE_HEADER_PADDING} (insets, optional): the space between a column's edges and its title and
 *     sort arrow. Defaults to 4 at the sides and none at the top and bottom.</li>
 *     <li>{@value #STYLE_HEADER_HEIGHT} (metric, optional): the header's height in UI units. Defaults to
 *     {@value #DEFAULT_HEADER_HEIGHT}.</li>
 *     <li>The scroll bar keys of {@link ScrollPanel}, for the horizontal bar.</li>
 * </ul>
 *
 * <pre>{@code
 * TableView<Offer> offers = factory.<Offer>tableView()
 *         .addTextColumn("Item", GridLength.weighted(1), Offer::name)
 *         .addTextColumn("Price", GridLength.fixed(60), offer -> format(offer.price()))
 *         .addColumn(factory.<Offer>tableColumn("Seller").setCellText(Offer::seller))
 *         .setItems(market.offers());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#tableView()}. Do not implement this
 * interface.</p>
 *
 * @param <T> the type of the items
 */
public interface TableView<T> extends Component<TableView<T>>, Focusable<TableView<T>>, HasCursor<TableView<T>>,
        HasTooltip<TableView<T>>, HasContextMenu<TableView<T>>, ItemSource<T, TableView<T>>,
        MultiSelection<T, TableView<T>>, Activatable<TableView<T>>, RowView<TableView<T>>,
        CellEditing<T, TableView<T>> {
    /**
     * Style key for the header behind each title (paint, required).
     */
    String STYLE_HEADER = "table.header";
    /**
     * Style key for a sortable header title under the pointer (paint, optional).
     */
    String STYLE_HEADER_HOVERED = "table.header.hovered";
    /**
     * Style key for the header titles (text style, optional).
     */
    String STYLE_HEADER_TEXT = "table.header.text";
    /**
     * Style key for the lines between columns (paint, optional).
     */
    String STYLE_GRID = "table.grid";
    /**
     * Style key for the space between a column's edges and its title and sort arrow (insets, optional).
     * Defaults to 4 at the sides and none at the top and bottom.
     */
    String STYLE_HEADER_PADDING = "table.header.padding";
    /**
     * Style key for the header's height (metric, optional), in UI units. Defaults to {@value #DEFAULT_HEADER_HEIGHT}.
     */
    String STYLE_HEADER_HEIGHT = "table.header.height";
    /**
     * The header's height when the theme sets no {@value #STYLE_HEADER_HEIGHT}, in UI units.
     */
    float DEFAULT_HEADER_HEIGHT = 20f;
    /**
     * Asked before the user sorts the rows by one column: a click on the title of a sortable column. A click on a
     * column the rows are not sorted by alone sorts ascending, the next descending, and the next goes back to the set
     * order, which the value names with a null column. A click with Shift asks {@link #SORT_MANY} instead. The value
     * is the sort about to apply. A handler may rewrite it to another sort; the table then sorts by that column alone,
     * as {@link #sortBy(TableColumn, boolean)} does, and ignores a sort whose column is neither null nor a sortable
     * column of this table. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<TableView<?>, Sort>> SORT = EventType.valueRequest("table_view.sort");
    /**
     * Told after the first sort changed. The value is the new first sort and the previous value the one before. It
     * fires only when the column or the direction changed: after the user's click, {@link #sortBy},
     * {@link TableColumn#setComparator} removing the comparator of the sort column, or {@link #removeColumn} removing
     * the sort column. It fires after {@link #SORTED_MANY}.
     */
    EventType<ValueEvent<TableView<?>, Sort>> SORTED = EventType.valueReport("table_view.sorted");
    /**
     * The sort, as {@link #sortColumn()}, {@link #isSortAscending()} and {@link #sortBy} say, null while the rows are
     * in their set order. Binds two ways through {@link #SORTED}. A sort whose column is not a sortable column of this
     * table throws as {@code sortBy} does. It is not named {@code SORT} because that is the request's name.
     */
    PropertyKey<TableView<?>, Sort> SORT_ORDER = PropertyKey.of("table_view.sort_order", TableView::currentSort,
            TableView::applySort, SORTED);
    /**
     * Asked before the user changes a sort order of several columns: a click with Shift on the title of a sortable
     * column, which adds it to the order ascending, makes it descending, or takes it out, in that order. The value is
     * the order about to apply, first sort first, and is empty for the set order. A handler may rewrite it; the table
     * then sorts as {@link #sortBy(List)} does, skipping sorts whose column is null, not a sortable column of this
     * table or named before. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<TableView<?>, List<Sort>>> SORT_MANY = EventType.valueRequest("table_view.sort_many");
    /**
     * Told after the sort order changed, whoever changed it, before {@link #SORTED} if the first sort changed too.
     * The value is the new order and the previous value the order before, each empty for the set order.
     */
    EventType<ValueEvent<TableView<?>, List<Sort>>> SORTED_MANY = EventType.valueReport("table_view.sorted_many");
    /**
     * Asked while the user drags the right edge of a resizable column's title, once for each move, and for a double
     * click there, which fits the column to its title and its cells in view. The value is the column and the width
     * it is about to get, already kept within its {@linkplain TableColumn#setWidthLimits limits}. A handler may
     * rewrite the width, which the table keeps within the limits again, or cancel the move. The table then fixes the
     * column's width. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<TableView<?>, ColumnWidth>> RESIZE_COLUMN = EventType.valueRequest("table_view.resize_column");
    /**
     * Told after the user resized a column. The value is the column and its new width, and the previous value the
     * same column and the width it had before. A width set from code with {@link TableColumn#setWidth} does not fire
     * it.
     */
    EventType<ValueEvent<TableView<?>, ColumnWidth>> COLUMN_RESIZED = EventType.valueReport("table_view.column_resized");
    /**
     * Asked when the user drops a movable column's title in another place among the shown titles. The value is the
     * column and the index in {@link #columns()} it is about to have. A handler may rewrite the index; the table
     * ignores an index outside the columns. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<TableView<?>, ColumnMove>> MOVE_COLUMN = EventType.valueRequest("table_view.move_column");
    /**
     * Told after the user moved a column. The value is the column and its new index in {@link #columns()}, and the
     * previous value the same column and its index before. Adding and removing columns from code does not fire it.
     */
    EventType<ValueEvent<TableView<?>, ColumnMove>> COLUMN_MOVED = EventType.valueReport("table_view.column_moved");

    /**
     * Returns the columns, left to right.
     *
     * @return an unmodifiable copy of the columns
     */
    List<TableColumn<T>> columns();

    /**
     * Adds a column at the right. The rows are built again.
     *
     * @param column the column
     * @return this table
     * @throws IllegalArgumentException if the column is already in a table
     */
    TableView<T> addColumn(TableColumn<T> column);

    /**
     * Adds a column at an index, moving the columns from there to the right. The rows are built again.
     *
     * @param index where the column goes, from 0 to the number of columns
     * @param column the column
     * @return this table
     * @throws IndexOutOfBoundsException if the index is outside 0 to the number of columns
     * @throws IllegalArgumentException if the column is already in a table
     */
    TableView<T> addColumn(int index, TableColumn<T> column);

    /**
     * Removes a column. If the rows are sorted by it, they go back to their set order and {@link #SORTED} fires. The
     * rows are built again. Does nothing if the column is not in this table.
     *
     * @param column the column
     * @return this table
     */
    TableView<T> removeColumn(TableColumn<T> column);

    /**
     * Adds a column whose cells are components, as
     * {@code addColumn(factory.tableColumn(title).setWidth(width).setCellFactory(cellFactory))} would.
     *
     * @param title the header title
     * @param width how wide the column is
     * @param cellFactory makes the cell for an item; called when the row comes into view
     * @return this table
     */
    TableView<T> addColumn(Text title, GridLength width, Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Does the same as {@link #addColumn(Text, GridLength, Function)} with literal text.
     *
     * @param title the header title
     * @param width how wide the column is
     * @param cellFactory makes the cell for an item; called when the row comes into view
     * @return this table
     */
    default TableView<T> addColumn(final String title, final GridLength width, final Function<? super T, ? extends Component<?>> cellFactory) {
        return this.addColumn(Text.of(title), width, cellFactory);
    }

    /**
     * Adds a column of text, sortable by that text ignoring case, as
     * {@code addColumn(factory.tableColumn(title).setWidth(width).setCellText(text))} would.
     *
     * @param title the header title
     * @param width how wide the column is
     * @param text returns what to show for an item: a {@link Text}, or any other value, shown by its appearance's
     *     name
     * @return this table
     */
    TableView<T> addTextColumn(Text title, GridLength width, Function<? super T, ?> text);

    /**
     * Does the same as {@link #addTextColumn(Text, GridLength, Function)} with literal text.
     *
     * @param title the header title
     * @param width how wide the column is
     * @param text returns what to show for an item: a {@link Text}, or any other value, shown by its appearance's
     *     name
     * @return this table
     */
    default TableView<T> addTextColumn(final String title, final GridLength width, final Function<? super T, ?> text) {
        return this.addTextColumn(Text.of(title), width, text);
    }

    /**
     * Sorts the rows by a column, or shows them in their set order. The selection stays on the same item.
     * {@link #SORTED} fires if the column or the direction changed.
     *
     * @param column the column, or {@code null} for the set order
     * @param ascending true for ascending, false for descending; ignored for the set order
     * @return this table
     * @throws IllegalArgumentException if the column is not in this table or not sortable
     */
    TableView<T> sortBy(TableColumn<T> column, boolean ascending);

    /**
     * Sorts the rows by several columns: by the first sort, then rows that tie by the next, and so on, or shows them
     * in their set order for an empty list. The selection stays on the same items. {@link #SORTED_MANY} fires if the
     * order changed, and then {@link #SORTED} if the first sort did.
     *
     * @param order the sorts, first sort first
     * @return this table
     * @throws IllegalArgumentException if a sort's column is null, not in this table, not sortable, or named twice
     */
    TableView<T> sortBy(List<Sort> order);

    /**
     * Returns the sort order.
     *
     * @return the sorts, first sort first; empty when the rows are in their set order
     */
    List<Sort> sortOrder();

    /**
     * Returns how the table scrolls sideways.
     *
     * @return the policy; {@link ScrollPanel.ScrollPolicy#NEVER} by default
     */
    ScrollPanel.ScrollPolicy horizontalPolicy();

    /**
     * Sets how the table scrolls sideways. {@code NEVER}, the default, fits the columns to the body and cuts off what
     * does not fit; {@code AUTO} scrolls when the columns are wider than the body, and {@code ALWAYS} shows the bar
     * whether or not they are.
     *
     * @param policy the policy
     * @return this table
     */
    TableView<T> setHorizontalPolicy(ScrollPanel.ScrollPolicy policy);

    /**
     * Returns how far the header and the rows are scrolled sideways.
     *
     * @return the horizontal offset in UI units; 0 while the table does not scroll sideways
     */
    float scrollX();

    /**
     * Returns the column the rows are sorted by.
     *
     * @return the column, or empty when the rows are in their set order
     */
    Optional<TableColumn<T>> sortColumn();

    /**
     * Returns whether the sort is ascending.
     *
     * @return true for ascending; also true when the rows are not sorted
     */
    boolean isSortAscending();

    /**
     * Returns the items in the order they were set.
     *
     * @return an unmodifiable list of the items
     */
    List<T> items();

    /**
     * Returns the items in the order they are shown.
     *
     * @return an unmodifiable list of the items, sorted if a sort column is set
     */
    List<T> shownItems();

    /**
     * Replaces the items, sorting them by the sort column. The selection stays on an item with the same key, as
     * {@link ListView#setItems} does.
     *
     * @param items the new items, copied
     * @return this table
     * @throws NullPointerException if an item is null
     */
    TableView<T> setItems(List<? extends T> items);

    /**
     * Builds every row again, for items whose content changed.
     *
     * @return this table
     */
    TableView<T> refresh();

    /**
     * Sets the names that type-ahead matches while focus is in the table, or turns it off. It works as
     * {@link ListView#setTypeAhead} describes, over the rows in the shown order.
     *
     * @param name returns what an item is called: a {@link Text}, or any other value, named by its appearance's
     *     name. By default an item is named by the first column whose cells are text, the column a new column or
     *     {@link TableColumn#setCellText} makes, and by its own appearance's name when no column is. Null turns
     *     type-ahead off.
     * @return this table
     */
    TableView<T> setTypeAhead(Function<? super T, ?> name);

    /**
     * Returns the index of the selected row in the shown order.
     *
     * @return the index, or -1 when nothing is selected
     */
    int selectedIndex();

    /**
     * Selects the row at an index in the shown order and scrolls it into view, as {@link ListView#select} does.
     *
     * @param index the index, or -1 to select nothing
     * @return this table
     * @throws IndexOutOfBoundsException if {@code index} is neither -1 nor a row index
     */
    TableView<T> select(int index);

    /**
     * Selects the row of an item with the same key, or nothing if there is none.
     *
     * @param item the item, or null to select nothing
     * @return this table
     */
    TableView<T> selectItem(T item);

    private static Sort currentSort(final TableView<?> table) {
        return table.sortColumn().<Sort>map(column -> new Sort(column, table.isSortAscending())).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> void applySort(final TableView<T> table, final Sort sort) {
        if (sort == null || sort.column() == null) {
            table.sortBy(null, true);
        } else {
            table.sortBy((TableColumn<T>) sort.column(), sort.ascending());
        }
    }

    /**
     * A column's width, as the user resizes it.
     *
     * @param column the column
     * @param width the width in UI units
     */
    record ColumnWidth(TableColumn<?> column, float width) {
    }

    /**
     * A column's place among the columns, as the user moves it.
     *
     * @param column the column
     * @param index its index in {@link TableView#columns()}
     */
    record ColumnMove(TableColumn<?> column, int index) {
    }

    /**
     * How the rows of a table are sorted.
     *
     * @param column the column the rows are sorted by, or {@code null} for the order the items were set in
     * @param ascending true for ascending, false for descending; always true for the set order when the table
     *     reports it
     */
    record Sort(TableColumn<?> column, boolean ascending) {
    }
}
