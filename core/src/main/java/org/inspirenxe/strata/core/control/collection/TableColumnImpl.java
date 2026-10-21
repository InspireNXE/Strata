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
package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.text.Text;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The core {@link TableColumn}: weighted 1, showing each item's appearance name as text, which is
 * {@link String#valueOf(Object)} for an item with no appearance, and not sortable. Text cells are
 * {@linkplain ComponentFactory#cellLabel(Text) cell labels}. A table that holds it is told of every change.
 *
 * @param <T> the type of the table's items
 */
public final class TableColumnImpl<T> implements TableColumn<T> {
    private final ComponentFactory ui;
    private Text title;
    private GridLength width;
    private Function<? super T, ? extends Component<?>> cellFactory;
    private Comparator<? super T> comparator;
    private TableViewImpl<T> table;

    /**
     * Creates a column.
     *
     * @param ui the factory that makes text cells
     * @param title the header title
     */
    public TableColumnImpl(final ComponentFactory ui, final Text title) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.title = Objects.requireNonNull(title, "title");
        this.width = GridLength.weighted(1f);
        this.cellFactory = this.textCells(Function.identity());
    }

    @Override
    public Text title() {
        return this.title;
    }

    @Override
    public TableColumn<T> setTitle(final Text title) {
        this.title = Objects.requireNonNull(title, "title");
        this.layoutChanged();
        return this;
    }

    @Override
    public GridLength width() {
        return this.width;
    }

    @Override
    public TableColumn<T> setWidth(final GridLength width) {
        this.width = Objects.requireNonNull(width, "width");
        this.layoutChanged();
        return this;
    }

    @Override
    public TableColumn<T> setCellText(final Function<? super T, ?> text) {
        Objects.requireNonNull(text, "text");
        this.setCellFactory(this.textCells(text));
        return this.setComparator(Comparator.comparing(item -> this.ui.appearances().nameOf(text.apply(item)).plain(),
                String.CASE_INSENSITIVE_ORDER));
    }

    private Function<T, Component<?>> textCells(final Function<? super T, ?> text) {
        return item -> this.ui.cellLabel(this.ui.appearances().nameOf(text.apply(item)));
    }

    @Override
    public TableColumn<T> setCellFactory(final Function<? super T, ? extends Component<?>> cellFactory) {
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        if (this.table != null) {
            this.table.cellsChanged();
        }
        return this;
    }

    Function<? super T, ? extends Component<?>> cellFactory() {
        return this.cellFactory;
    }

    @Override
    public Optional<Comparator<? super T>> comparator() {
        return Optional.ofNullable(this.comparator);
    }

    @Override
    public TableColumn<T> setComparator(final Comparator<? super T> comparator) {
        this.comparator = comparator;
        if (this.table != null) {
            this.table.comparatorChanged(this);
        }
        return this;
    }

    TableViewImpl<T> table() {
        return this.table;
    }

    /**
     * Gives the column to a table, or takes it back with null.
     *
     * @throws IllegalArgumentException if the column is given while a table already has it
     */
    void setTable(final TableViewImpl<T> table) {
        if (table != null && this.table != null) {
            throw new IllegalArgumentException(this + " is already in a table");
        }
        this.table = table;
    }

    private void layoutChanged() {
        if (this.table != null) {
            this.table.columnLaidOutChanged();
        }
    }

    @Override
    public String toString() {
        return "TableColumn[" + this.title.plain() + "]";
    }
}
