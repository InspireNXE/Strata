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
import org.inspirenxe.strata.api.control.attribute.item.CellSource;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.layout.GridLength;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * One column of a {@link TableView}: its title, its width, what builds its cells, and how it sorts.
 *
 * <p>A column is made by the {@link org.inspirenxe.strata.api.ComponentFactory}, or by
 * {@link TableView#addColumn(String, GridLength, Function)} and {@link TableView#addTextColumn}. It belongs to at
 * most one table at a time. It is mutable: a change to it lays out its table again, and a new cell factory builds the
 * rows in view again. A column is not a component and has no events of its own; the table's
 * {@link TableView#SORT} and {@link TableView#SORTED} carry the column in their {@link TableView.Sort}.</p>
 *
 * <p>A column with a comparator is sortable: clicking its title sorts the rows by it.</p>
 *
 * <pre>{@code
 * TableColumn<Offer> price = factory.<Offer>tableColumn("Price")
 *         .setWidth(GridLength.fixed(60))
 *         .setCellText(offer -> format(offer.price()))
 *         .setComparator(Comparator.comparingDouble(Offer::price));
 * offers.addColumn(price);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#tableColumn(String)}. Call columns on the UI
 * thread. Do not implement this interface.</p>
 *
 * @param <T> the type of the table's items
 */
public interface TableColumn<T> extends Titled<TableColumn<T>>, CellSource<T, TableColumn<T>> {

    /**
     * Returns the width.
     *
     * @return how wide the column is; {@link GridLength#weighted(float) weighted} 1 by default
     */
    GridLength width();

    /**
     * Sets how wide the column is, as a {@link org.inspirenxe.strata.api.layout.GridPanel} sizes its columns.
     *
     * @param width the width
     * @return this column
     */
    TableColumn<T> setWidth(GridLength width);

    /**
     * Sets the cells to text: each cell is a one-line {@link org.inspirenxe.strata.api.control.display.Label} 4 units
     * in from the column's edges, cut short with {@code ...} when too long. It also makes the column sortable by that
     * text, ignoring case; set another comparator afterwards to sort differently. A new column shows
     * {@link String#valueOf(Object)} of each item this way, without sorting.
     *
     * @param text returns the text for an item
     * @return this column
     */
    TableColumn<T> setCellText(Function<? super T, String> text);

    /**
     * Sets what builds the cell for an item, called when the item's row comes into view.
     *
     * @param cellFactory makes the cell for an item
     * @return this column
     */
    @Override
    TableColumn<T> setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Returns the comparator.
     *
     * @return how the column sorts, or empty when it is not sortable
     */
    Optional<Comparator<? super T>> comparator();

    /**
     * Makes the column sortable by a comparator, or not sortable. If its table's rows are sorted by this column they
     * are sorted again, or shown in their set order when the comparator is null, which reports
     * {@link TableView#SORTED}.
     *
     * @param comparator orders the items ascending, or {@code null} to make the column not sortable
     * @return this column
     */
    TableColumn<T> setComparator(Comparator<? super T> comparator);

    /**
     * Returns whether the column is sortable.
     *
     * @return true when it has a comparator
     */
    default boolean isSortable() {
        return this.comparator().isPresent();
    }
}
