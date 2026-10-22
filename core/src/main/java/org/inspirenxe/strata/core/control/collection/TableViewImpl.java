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
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * The core {@link TableView}: a header component above a {@link ListViewImpl} whose cells are rows that place one
 * cell per column. Column offsets are worked out once per arrange, from the width the list gives its rows, and both
 * the header and every row read them. The table routes the list's input and selection changes to itself and fires
 * them as its own events, so the list fires no {@link ListView} events.
 *
 * @param <T> the type of the items
 */
public final class TableViewImpl<T> extends ListBackedControl<T, T, TableView<T>> implements TableView<T> {
    /**
     * The space between a title and its column's edges when the theme sets no {@value #STYLE_HEADER_PADDING}:
     * {@value ComponentFactory#CELL_PADDING} at the sides.
     */
    public static final Insets DEFAULT_HEADER_PADDING = new Insets(ComponentFactory.CELL_PADDING, 0f,
            ComponentFactory.CELL_PADDING, 0f);
    /**
     * The room a sort arrow takes at the right of a sortable title, in UI units: the arrow and 4 units before it.
     */
    public static final float ARROW_ROOM = Rings.ARROW_LONG + 4f;
    private static final Set<String> REQUIRED = Set.of(STYLE_HEADER);

    private final ComponentFactoryImpl ui;
    private Insets headerPadding = DEFAULT_HEADER_PADDING;
    private final List<TableColumnImpl<T>> columns;
    private final Header header;
    private List<T> items;
    private List<T> shown;
    // The index in items of each shown row
    private int[] order;
    private TableColumnImpl<T> sortColumn;
    private boolean ascending;

    // Column offsets and widths, relative to the start of a row
    private float[] columnX;
    private float[] columnWidth;
    private TextLine[] titles;
    private float rowsX;

    // Theme
    private Paint headerPaint;
    private Paint headerHovered;
    private Paint grid;
    private TextStyle headerText;

    /**
     * Creates a table with no columns.
     *
     * @param ui the factory that owns the table
     */
    @SuppressWarnings("this-escape")
    public TableViewImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.ui = ui;
        this.columns = new ArrayList<>();
        this.items = List.of();
        this.shown = List.of();
        this.order = new int[0];
        this.ascending = true;
        this.columnX = new float[0];
        this.columnWidth = new float[0];
        this.titles = new TextLine[0];
        this.headerPaint = Paint.NONE;
        this.headerHovered = Paint.NONE;
        this.grid = Paint.NONE;
        this.headerText = TextStyle.DEFAULT;
        this.header = new Header(ui, this);
        this.addChild(this.header);
        this.addChild(this.list);
    }

    // ------------------------------------------------------------------
    // Columns and sorting
    // ------------------------------------------------------------------

    @Override
    public List<TableColumn<T>> columns() {
        return List.copyOf(this.columns);
    }

    @Override
    public TableView<T> addColumn(final TableColumn<T> column) {
        return this.addColumn(this.columns.size(), column);
    }

    @Override
    public TableView<T> addColumn(final int index, final TableColumn<T> column) {
        final TableColumnImpl<T> impl = this.impl(column);
        Objects.checkIndex(index, this.columns.size() + 1);
        impl.setTable(this);
        this.columns.add(index, impl);
        return this.columnsChanged();
    }

    @Override
    public TableView<T> removeColumn(final TableColumn<T> column) {
        final int index = this.columns.indexOf(column);
        if (index < 0) {
            return this;
        }
        final TableColumnImpl<T> impl = this.columns.remove(index);
        impl.setTable(null);
        if (impl == this.sortColumn) {
            this.clearSort(impl);
        }
        return this.columnsChanged();
    }

    @Override
    public TableView<T> addColumn(final Text title, final GridLength width,
                                  final Function<? super T, ? extends Component<?>> cellFactory) {
        return this.addColumn(new TableColumnImpl<T>(this.ui, title).setWidth(width).setCellFactory(cellFactory));
    }

    @Override
    public TableView<T> addTextColumn(final Text title, final GridLength width, final Function<? super T, ?> text) {
        return this.addColumn(new TableColumnImpl<T>(this.ui, title).setWidth(width).setCellText(text));
    }

    @SuppressWarnings("unchecked")
    private TableColumnImpl<T> impl(final TableColumn<T> column) {
        if (!(column instanceof TableColumnImpl<?> impl)) {
            throw new IllegalArgumentException(column + " is not a Strata table column");
        }
        return (TableColumnImpl<T>) impl;
    }

    private TableView<T> columnsChanged() {
        this.list.refresh();
        this.invalidateLayout();
        return this;
    }

    /**
     * Called by a column when its title or width changed.
     */
    void columnLaidOutChanged() {
        this.invalidateLayout();
    }

    /**
     * Called by a column when its cell factory changed.
     */
    void cellsChanged() {
        this.list.refresh();
    }

    /**
     * Called by a column when its comparator changed: sorts again if the rows are sorted by it, or goes back to the
     * set order if it is no longer sortable.
     */
    void comparatorChanged(final TableColumnImpl<T> column) {
        if (column == this.sortColumn) {
            if (column.comparator().isEmpty()) {
                this.clearSort(column);
            } else {
                this.reorder(column, this.ascending);
            }
        }
        this.invalidateLayout();
    }

    /**
     * Goes back to the set order after the sort column stopped being one, and reports it.
     */
    private void clearSort(final TableColumnImpl<T> previous) {
        final boolean wasAscending = this.ascending;
        this.reorder(null, true);
        this.report(SORTED, new Sort(null, true), new Sort(previous, wasAscending));
    }

    @Override
    public TableView<T> sortBy(final TableColumn<T> column, final boolean ascending) {
        if (column != null && (!this.columns.contains(column) || !column.isSortable())) {
            throw new IllegalArgumentException(column + " is not a sortable column of this table");
        }
        final TableColumnImpl<T> next = column == null ? null : this.impl(column);
        final TableColumnImpl<T> previousColumn = this.sortColumn;
        final boolean previousAscending = this.ascending;
        this.reorder(next, next == null || ascending);
        if (next != previousColumn || this.ascending != previousAscending) {
            this.report(SORTED, new Sort(next, this.ascending), new Sort(previousColumn, previousAscending));
        }
        return this;
    }

    @Override
    public Optional<TableColumn<T>> sortColumn() {
        return Optional.ofNullable(this.sortColumn);
    }

    @Override
    public boolean isSortAscending() {
        return this.ascending;
    }

    /**
     * Works out the order rows show in for a sort, as indexes into some items: the set order for no column, else a
     * stable sort by the column's comparator. Runs the comparator and changes nothing.
     */
    @SuppressWarnings("unchecked")
    private int[] orderOf(final List<T> items, final TableColumnImpl<T> column, final boolean ascending) {
        final Integer[] rows = new Integer[items.size()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i;
        }
        if (column != null) {
            final Comparator<T> comparator = (Comparator<T>) column.comparator().orElseThrow();
            final Comparator<T> direction = ascending ? comparator : comparator.reversed();
            Arrays.sort(rows, (a, b) -> direction.compare(items.get(a), items.get(b)));
        }
        final int[] order = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            order[i] = rows[i];
        }
        return order;
    }

    /**
     * Returns the items in an order, or the items themselves when the rows are in the set order.
     */
    private static <T> List<T> inOrder(final List<T> items, final int[] order, final boolean sorted) {
        if (!sorted) {
            return items;
        }
        final List<T> shown = new ArrayList<>(order.length);
        for (final int index : order) {
            shown.add(items.get(index));
        }
        return List.copyOf(shown);
    }

    /**
     * Shows the same items in another sort. Each selected row follows its own item, so a sort never moves the
     * selection to another item with the same key. The comparator runs before anything changes.
     */
    private void reorder(final TableColumnImpl<T> column, final boolean ascending) {
        final int[] order = this.orderOf(this.items, column, ascending);
        final List<T> sorted = inOrder(this.items, order, column != null);
        final int[] rowOf = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            rowOf[order[i]] = i;
        }
        final int[] previous = this.order;
        this.sortColumn = column;
        this.ascending = ascending;
        this.order = order;
        this.shown = sorted;
        this.list.replaceItems(sorted, row -> rowOf[previous[row]]);
    }

    /**
     * Asks to sort by a column after a click on its title: ascending first, then flipping.
     */
    void titleClicked(final int index) {
        final TableColumnImpl<T> column = this.columns.get(index);
        if (!column.isSortable()) {
            return;
        }
        final boolean ascending = column != this.sortColumn || !this.ascending;
        this.request(SORT, new Sort(column, ascending), this::applySort);
    }

    /**
     * Sorts as a request ended, or goes back to the set order when the sort names no column. A null sort, or one that
     * names a column this table does not hold or cannot sort by, is ignored; a handler can cause either by rewriting
     * the sort or removing the comparator.
     */
    @SuppressWarnings("unchecked")
    private void applySort(final Sort sort) {
        if (sort == null) {
            return;
        }
        final TableColumn<?> column = sort.column();
        if (column == null) {
            this.sortBy(null, true);
        } else if (this.columns.contains(column) && column.isSortable()) {
            this.sortBy((TableColumn<T>) column, sort.ascending());
        }
    }

    boolean isSortable(final int column) {
        return this.columns.get(column).isSortable();
    }

    @Override
    Component<?> row(final T item) {
        return new Row<>(this.ui, this, item);
    }

    // ------------------------------------------------------------------
    // Items and selection, mostly the list's
    // ------------------------------------------------------------------

    @Override
    public List<T> items() {
        return this.items;
    }

    @Override
    public List<T> shownItems() {
        return this.shown;
    }

    @Override
    public TableView<T> setItems(final List<? extends T> items) {
        // The comparator and the key function run before anything changes
        final List<T> next = Items.copyOf(items);
        final int[] order = this.orderOf(next, this.sortColumn, this.ascending);
        final List<T> sorted = inOrder(next, order, this.sortColumn != null);
        final IntUnaryOperator moves = this.list.keyMoves(sorted);
        this.items = next;
        this.order = order;
        this.shown = sorted;
        this.list.replaceItems(sorted, moves);
        return this;
    }

    @Override
    public TableView<T> refresh() {
        this.list.refresh();
        return this;
    }

    @Override
    public TableView<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.list.setKeyFunction(keyFunction);
        return this;
    }

    @Override
    public Optional<T> selected() {
        return this.list.selected();
    }

    @Override
    public TableView<T> selectItem(final T item) {
        this.list.selectItem(item);
        return this;
    }

    @Override
    public List<T> selectedItems() {
        return this.list.selectedItems();
    }

    @Override
    public TableView<T> selectItems(final Collection<? extends T> items) {
        this.list.selectItems(items);
        return this;
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.headerPaint = theme.paintOr(STYLE_HEADER, Paint.NONE);
        this.headerHovered = theme.paintOr(STYLE_HEADER_HOVERED, this.headerPaint);
        this.grid = theme.paintOr(STYLE_GRID, Paint.NONE);
        this.headerText = theme.textStyleOr(STYLE_HEADER_TEXT, TextStyle.DEFAULT);
        this.headerPadding = theme.insetsOr(STYLE_HEADER_PADDING, DEFAULT_HEADER_PADDING);
    }

    private float autoWidth(final TextMetrics metrics, final int column) {
        final TableColumnImpl<T> definition = this.columns.get(column);
        return TextLine.of(definition.title(), this.headerText, metrics).width() + this.headerPadding.horizontal()
                + (definition.isSortable() ? ARROW_ROOM : 0f);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        float columnsWidth = 0f;
        for (int i = 0; i < this.columns.size(); i++) {
            final GridLength width = this.columns.get(i).width();
            if (width instanceof GridLength.Fixed fixed) {
                columnsWidth += fixed.size();
            } else {
                columnsWidth += this.autoWidth(context.text(), i);
            }
        }
        final Size head = context.measure(this.header, availableWidth, ListView.DEFAULT_ROW_HEIGHT);
        final float bodyHeight = Float.isInfinite(availableHeight) ? availableHeight : Math.max(0f, availableHeight - head.height());
        final Size body = context.measure(this.list, availableWidth, bodyHeight);
        return new Size(Math.max(columnsWidth, body.width()), head.height() + body.height());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final float headHeight = ListView.DEFAULT_ROW_HEIGHT;
        final Rect body = new Rect(bounds.x(), bounds.y() + headHeight, bounds.width(), Math.max(0f, bounds.height() - headHeight));
        this.layoutColumns(context.text(), this.list.rowWidthFor(body));
        this.rowsX = body.x() + this.nodeOf(this.list).border().widths().left();
        context.arrange(this.header, new Rect(bounds.x(), bounds.y(), bounds.width(), headHeight));
        context.arrange(this.list, body);
    }

    private void layoutColumns(final TextMetrics metrics, final float width) {
        final int count = this.columns.size();
        if (this.columnX.length != count) {
            this.columnX = new float[count];
            this.columnWidth = new float[count];
            this.titles = new TextLine[count];
        }
        float used = 0f;
        float weights = 0f;
        for (int i = 0; i < count; i++) {
            final GridLength length = this.columns.get(i).width();
            if (length instanceof GridLength.Fixed fixed) {
                this.columnWidth[i] = fixed.size();
            } else if (length instanceof GridLength.Weighted weighted) {
                this.columnWidth[i] = 0f;
                weights += weighted.weight();
            } else {
                this.columnWidth[i] = this.autoWidth(metrics, i);
            }
            used += this.columnWidth[i];
        }
        final float left = Math.max(0f, width - used);
        float x = 0f;
        for (int i = 0; i < count; i++) {
            if (this.columns.get(i).width() instanceof GridLength.Weighted weighted && weights > 0f) {
                this.columnWidth[i] = left * weighted.weight() / weights;
            }
            this.columnX[i] = x;
            x += this.columnWidth[i];
            final float room = this.columnWidth[i] - this.headerPadding.horizontal() - (this.isSortable(i) ? ARROW_ROOM : 0f);
            this.titles[i] = TextLine.of(this.columns.get(i).title(), this.headerText, metrics)
                    .ellipsize(room, this.headerText, metrics);
        }
    }

    @Override
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
        if (this.grid == Paint.NONE) {
            return;
        }
        for (int i = 1; i < this.columns.size(); i++) {
            this.grid.paint(painter, new Rect(this.rowsX + this.columnX[i] - 1f, bounds.y(), 1f, bounds.height()));
        }
    }

    /**
     * The column titles. Sortable titles show the hand cursor while the pointer is over the header.
     */
    private static final class Header extends BuiltInComponent<Header> implements HasCursor<Header> {
        private final TableViewImpl<?> table;
        private float pointerX;
        private float lineHeight;

        Header(final ComponentFactoryImpl ui, final TableViewImpl<?> table) {
            super(ui);
            this.table = table;
            this.setAnchor(Anchor.FILL);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            this.lineHeight = context.text().lineHeight(this.table.headerText);
            return new Size(0f, ListView.DEFAULT_ROW_HEIGHT);
        }

        private int columnAt(final float x) {
            for (int i = this.table.columnX.length - 1; i >= 0; i--) {
                if (x >= this.table.rowsX + this.table.columnX[i]) {
                    return x < this.table.rowsX + this.table.columnX[i] + this.table.columnWidth[i] ? i : -1;
                }
            }
            return -1;
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.table.headerPaint.paint(painter, bounds);
            final int hovered = this.isHovered() ? this.columnAt(this.pointerX) : -1;
            final Insets padding = this.table.headerPadding;
            final float textY = bounds.y() + padding.top() + (bounds.height() - padding.vertical() - this.lineHeight) * 0.5f;
            for (int i = 0; i < this.table.titles.length; i++) {
                final Rect cell = new Rect(this.table.rowsX + this.table.columnX[i], bounds.y(), this.table.columnWidth[i],
                        bounds.height());
                if (i == hovered && this.table.isSortable(i)) {
                    this.table.headerHovered.paint(painter, cell);
                }
                this.table.titles[i].draw(painter, cell.x() + padding.left(), textY);
                if (this.table.sortColumn != null && this.table.columns.get(i) == this.table.sortColumn) {
                    final float arrowX = cell.right() - padding.right() - Rings.ARROW_LONG;
                    final float arrowY = cell.y() + (cell.height() - Rings.ARROW_SHORT) * 0.5f;
                    if (this.table.ascending) {
                        Rings.up(painter, this.table.headerText.color(), arrowX, arrowY);
                    } else {
                        Rings.down(painter, this.table.headerText.color(), arrowX, arrowY);
                    }
                }
            }
        }

        @Override
        protected void onPointerMove(final PointerEvent event) {
            this.pointerX = event.x();
            final int column = this.columnAt(event.x());
            this.setCursor(column >= 0 && this.table.isSortable(column) ? CursorKind.HAND : null);
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() != MouseButton.LEFT) {
                return;
            }
            final int column = this.columnAt(event.x());
            if (column >= 0) {
                this.table.titleClicked(column);
                event.consume();
            }
        }
    }

    /**
     * One body row: a cell per column, each in its column's span and the row's height.
     */
    private static final class Row<T> extends BuiltInComposite<Row<T>> {
        private final TableViewImpl<T> table;
        private final List<Component<?>> cells;

        @SuppressWarnings("this-escape")
        Row(final ComponentFactoryImpl ui, final TableViewImpl<T> table, final T item) {
            super(ui);
            this.table = table;
            this.cells = new ArrayList<>();
            for (final TableColumnImpl<T> column : table.columns) {
                final Component<?> cell = column.cellFactory().apply(item);
                this.cells.add(cell);
                this.addContent(cell);
            }
            this.setAnchor(Anchor.FILL);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            float width = 0f;
            for (int i = 0; i < this.cells.size(); i++) {
                final float columnWidth = i < this.table.columnWidth.length ? this.table.columnWidth[i] : availableWidth;
                width += context.measure(this.cells.get(i), columnWidth, availableHeight).width();
            }
            return new Size(width, availableHeight);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            for (int i = 0; i < this.cells.size(); i++) {
                if (i >= this.table.columnX.length) {
                    context.arrange(this.cells.get(i), new Rect(bounds.right(), bounds.y(), 0f, bounds.height()));
                    continue;
                }
                final Component<?> cell = this.cells.get(i);
                context.measure(cell, this.table.columnWidth[i], bounds.height());
                context.arrange(cell, new Rect(bounds.x() + this.table.columnX[i], bounds.y(), this.table.columnWidth[i],
                        bounds.height()));
            }
        }
    }
}
