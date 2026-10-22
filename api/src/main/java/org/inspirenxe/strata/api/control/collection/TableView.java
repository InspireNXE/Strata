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
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.RowView;
import org.inspirenxe.strata.api.control.attribute.item.MultiSelection;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Rows of items under a header of columns, such as a list of sale offers with a name, a price and a seller.
 *
 * <p>The body is a {@link ListView}: only the rows in view are built, it scrolls the same way, and it selects and
 * activates rows the same way. Each row lays out one cell per column, made by that column's cell factory. The header
 * sits above the body, {@link ListView#DEFAULT_ROW_HEIGHT} units tall, and lines its columns up with the cells even
 * when the scroll bar shows.</p>
 *
 * <p>Column widths work like a {@link org.inspirenxe.strata.api.layout.GridPanel}'s: fixed widths first, then
 * {@link GridLength#auto() auto} columns at their title's width plus 8 units, and 11 more for the arrow when
 * sortable, and weighted columns share what is left by weight. Titles sit 4 units in from the left of their column,
 * and a sort arrow 4 units in from the right.</p>
 *
 * <p>A column with a comparator is sortable. Clicking its title sorts the rows by it, ascending first and then
 * flipping on each click, and the title shows an arrow. Text columns sort by their text, ignoring case, unless
 * given another comparator. Sorting changes only the order the rows are shown in: {@link #items()} keeps the order
 * they were set in, while {@link #shownItems()} and every row index follow the order on screen.</p>
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
public interface TableView<T> extends Component<TableView<T>>, HasCursor<TableView<T>>, HasTooltip<TableView<T>>,
        ItemSource<T, TableView<T>>,
        MultiSelection<T, TableView<T>>, Activatable<TableView<T>>, RowView<TableView<T>> {
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
     * Asked before the user sorts the rows: a click on the title of a sortable column. The first click on a column
     * sorts ascending and each further click flips the direction. The value is the sort about to apply. A handler
     * may rewrite it to another sort; the table then sorts as {@link #sortBy} does, and ignores a sort whose column
     * is neither null nor a sortable column of this table. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<TableView<?>, Sort>> SORT = EventType.valueRequest("table_view.sort");
    /**
     * Told after the sort changed. The value is the new sort and the previous value the sort before. It fires only when
     * the column or the direction changed: after the user's click, {@link #sortBy}, {@link TableColumn#setComparator}
     * removing the comparator of the sort column, or {@link #removeColumn} removing the sort column.
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
     * Sets what identifies an item across {@link #setItems} calls, as {@link ListView#setKeyFunction} does.
     *
     * @param keyFunction returns an item's key
     * @return this table
     */
    TableView<T> setKeyFunction(Function<? super T, ?> keyFunction);

    /**
     * Sets the height of every body row.
     *
     * @param height the row height in UI units
     * @return this table
     * @throws IllegalArgumentException if {@code height} is not greater than 0 or is infinite
     */
    TableView<T> setRowHeight(float height);

    /**
     * Sets the text shown in the body while there are no items.
     *
     * @param placeholder the text, or null for none
     * @return this table
     */
    TableView<T> setPlaceholder(Text placeholder);

    /**
     * Returns the index of the selected row in the shown order.
     *
     * @return the index, or -1 when nothing is selected
     */
    int selectedIndex();

    /**
     * Returns the selected item.
     *
     * @return the selected item, or empty when nothing is selected
     */
    Optional<T> selected();

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
     * How the rows of a table are sorted.
     *
     * @param column the column the rows are sorted by, or {@code null} for the order the items were set in
     * @param ascending true for ascending, false for descending; always true for the set order when the table
     *     reports it
     */
    record Sort(TableColumn<?> column, boolean ascending) {
    }
}
