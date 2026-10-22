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
import org.inspirenxe.strata.api.control.attribute.item.CellEditor;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.layout.ScrollTrack;
import org.inspirenxe.strata.core.layout.ThumbDrag;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.LayoutPass;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * The core {@link TableView}: a header component above a {@link ListViewImpl} whose cells are rows that place one
 * cell per shown column. Column offsets are worked out once per arrange, from the width the list gives its rows, and
 * both the header and every row read them; when they or the sideways offset change, the table asks each built row to
 * be arranged again. The table routes the list's input and selection changes to itself and fires them as its own
 * events, so the list fires no {@link ListView} events.
 *
 * <p>The header takes the pointer for the columns: within {@link #EDGE} units of a resizable column's right edge a
 * drag resizes it and a double click fits it, elsewhere a drag of a movable title moves it, a click sorts, and a right
 * click opens the menu of columns. The horizontal bar sits in a gutter below the body and is drawn by the table, with
 * the geometry of a list's bar; a horizontal wheel notch scrolls {@link #SCROLL_RATE} units.</p>
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
    /**
     * How near a column's right edge the pointer must be to resize it, in UI units, on either side.
     */
    public static final float EDGE = 3f;
    /**
     * How far a horizontal wheel notch scrolls, in UI units.
     */
    public static final float SCROLL_RATE = 10f;
    private static final Set<String> REQUIRED = Set.of(STYLE_HEADER);
    private static final float GUTTER = ScrollPanel.BAR_WIDTH + ScrollPanel.BAR_GAP;

    private final ComponentFactoryImpl ui;
    private Insets headerPadding = DEFAULT_HEADER_PADDING;
    private final List<TableColumnImpl<T>> columns;
    // The shown columns, in order, which the header, the rows and the layout arrays follow
    private List<TableColumnImpl<T>> visible;
    private final Header header;
    private List<T> items;
    private List<T> shown;
    // The index in items of each shown row
    private int[] order;
    // The sort order, first sort first; each column is one of this table's
    private List<Sort> sorts;
    // What type-ahead matches, or null for the first text column
    private Function<? super T, ?> typeAheadName;

    // Auto columns: the widest cell in view of each, measured when stale
    private final Map<TableColumnImpl<T>, Float> autoCells;
    private boolean autoStale;

    // Sideways scrolling
    private ScrollPanel.ScrollPolicy horizontalPolicy;
    private final ScrollTrack hbar;
    private final ThumbDrag hThumbDrag;
    private boolean hbarShown;
    private float scrollX;
    private float maxScrollX;
    private float bodyWidth;
    private boolean arranged;
    private Rect hThumbRect;

    // Column offsets and widths by shown index, relative to the start of a row, and the values the rows were last
    // arranged with
    private float[] columnX;
    private float[] columnWidth;
    private float[] rowsColumnX;
    private float[] rowsColumnWidth;
    private float rowsScrollX;
    private TextLine[] titles;
    private float rowsX;
    // Made during arrange for painting: each title's cell and the lines between columns
    private Rect[] headerCells;
    private Rect[] gridLines;

    // Theme
    private Paint headerPaint;
    private Paint headerHovered;
    private Paint grid;
    private TextStyle headerText;
    private float headerHeight = DEFAULT_HEADER_HEIGHT;
    private Paint thumb = Paint.NONE;
    private Paint thumbHovered = Paint.NONE;
    private Paint track = Paint.NONE;

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
        this.visible = List.of();
        this.items = List.of();
        this.shown = List.of();
        this.order = new int[0];
        this.sorts = List.of();
        this.autoCells = new IdentityHashMap<>();
        this.autoStale = true;
        this.horizontalPolicy = ScrollPanel.ScrollPolicy.NEVER;
        this.hbar = new ScrollTrack(false);
        this.hThumbDrag = new ThumbDrag(this.hbar, false);
        this.hThumbRect = Rect.ZERO;
        this.columnX = new float[0];
        this.columnWidth = new float[0];
        this.rowsColumnX = new float[0];
        this.rowsColumnWidth = new float[0];
        this.titles = new TextLine[0];
        this.headerCells = new Rect[0];
        this.gridLines = new Rect[0];
        this.headerPaint = Paint.NONE;
        this.headerHovered = Paint.NONE;
        this.grid = Paint.NONE;
        this.headerText = TextStyle.DEFAULT;
        this.header = new Header(ui, this);
        this.addChild(this.header);
        this.addChild(this.list);
        this.list.setTypeAhead(this::typeAheadName);
    }

    // ------------------------------------------------------------------
    // Columns
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
        this.autoCells.remove(impl);
        if (this.sortIndex(this.sorts, impl) >= 0) {
            this.applyOrder(this.without(impl));
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

    /**
     * Works out the shown columns again and builds the rows again, after columns were added, removed, moved, shown
     * or hidden.
     */
    private TableView<T> columnsChanged() {
        final List<TableColumnImpl<T>> next = new ArrayList<>(this.columns.size());
        for (final TableColumnImpl<T> column : this.columns) {
            if (column.isShown()) {
                next.add(column);
            }
        }
        this.visible = List.copyOf(next);
        this.autoStale = true;
        this.list.refresh();
        this.invalidateLayout();
        return this;
    }

    /**
     * Called by a column when it was shown or hidden.
     */
    void shownChanged() {
        this.columnsChanged();
    }

    /**
     * Called by a column when its title, width or width limits changed.
     */
    void columnLaidOutChanged() {
        this.invalidateLayout();
    }

    /**
     * Called by a column when its cell factory changed.
     */
    void cellsChanged() {
        this.autoStale = true;
        this.list.refresh();
    }

    @Override
    public ScrollPanel.ScrollPolicy horizontalPolicy() {
        return this.horizontalPolicy;
    }

    @Override
    public TableView<T> setHorizontalPolicy(final ScrollPanel.ScrollPolicy policy) {
        this.horizontalPolicy = Objects.requireNonNull(policy, "policy");
        this.invalidateLayout();
        return this;
    }

    @Override
    public float scrollX() {
        return this.scrollX;
    }

    /**
     * Moves the header and rows sideways, kept within 0 and, once laid out, the columns' overflow.
     */
    private boolean scrollXTo(final float scrollX) {
        final float max = this.arranged ? this.maxScrollX : Float.MAX_VALUE;
        final float next = Math.max(0f, Math.min(scrollX, max));
        if (next == this.scrollX) {
            return false;
        }
        this.scrollX = next;
        this.invalidateArrangement();
        return true;
    }

    /**
     * Asks to resize a column for the user, to a width kept within its limits. Does nothing for a column that is not
     * shown or not resizable, or when the width would not change.
     */
    void userResize(final TableColumnImpl<T> column, final float width) {
        final int at = this.visible.indexOf(column);
        if (at < 0 || at >= this.columnWidth.length || !column.isResizable()) {
            return;
        }
        final float next = column.clamp(width);
        if (next == this.columnWidth[at]) {
            return;
        }
        this.request(RESIZE_COLUMN, new ColumnWidth(column, next), this::applyResize);
    }

    /**
     * Fixes the width of the column a resize request ended with and reports it. A null value, or one whose column
     * this table does not hold, is ignored.
     */
    private void applyResize(final ColumnWidth resize) {
        if (resize == null || !(resize.column() instanceof TableColumnImpl<?> impl) || !this.columns.contains(impl)) {
            return;
        }
        @SuppressWarnings("unchecked") final TableColumnImpl<T> column = (TableColumnImpl<T>) impl;
        final int at = this.visible.indexOf(column);
        final float previous = at >= 0 && at < this.columnWidth.length ? this.columnWidth[at] : 0f;
        final float width = column.clamp(resize.width());
        column.setWidth(GridLength.fixed(width));
        // Laid out again at once, so the next move of the drag compares against the new width
        if (at >= 0 && at < this.columnWidth.length) {
            this.columnWidth[at] = width;
        }
        this.report(COLUMN_RESIZED, new ColumnWidth(column, width), new ColumnWidth(column, previous));
    }

    /**
     * Returns the width that fits a shown column to its title and its cells in view.
     */
    float fitWidth(final int shownIndex) {
        final TableColumnImpl<T> column = this.visible.get(shownIndex);
        final TextMetrics metrics = this.environment().text();
        float widest = this.titleWidth(metrics, column);
        final LayoutPass pass = new LayoutPass(this.ui, metrics);
        for (final Component<?> row : this.list.builtRows()) {
            final Component<?> cell = ((Row<?>) row).cell(shownIndex);
            if (cell != null) {
                widest = Math.max(widest, pass.measure(cell, Float.POSITIVE_INFINITY, this.list.rowHeight()).width());
            }
        }
        return widest;
    }

    /**
     * Asks to move a column for the user to a slot among the shown columns, 0 for before the first and the number of
     * shown columns for after the last. Does nothing for a column that is not movable or when it would not move.
     */
    void userMove(final TableColumnImpl<T> column, final int slot) {
        final int from = this.visible.indexOf(column);
        if (!column.isMovable() || from < 0) {
            return;
        }
        final List<TableColumnImpl<T>> rest = new ArrayList<>(this.columns);
        rest.remove(column);
        final List<TableColumnImpl<T>> others = new ArrayList<>(this.visible);
        others.remove(column);
        if (others.isEmpty()) {
            return;
        }
        // The slot among the other shown columns once this one is taken out
        final int place = from < slot ? slot - 1 : slot;
        final int target = place >= others.size() ? rest.indexOf(others.getLast()) + 1 : rest.indexOf(others.get(place));
        if (target == this.columns.indexOf(column)) {
            return;
        }
        this.request(MOVE_COLUMN, new ColumnMove(column, target), this::applyMove);
    }

    /**
     * Moves the column a move request ended with and reports it. A null value, a column this table does not hold or
     * an index outside the columns is ignored.
     */
    private void applyMove(final ColumnMove move) {
        if (move == null || !(move.column() instanceof TableColumnImpl<?> impl) || !this.columns.contains(impl)
                || move.index() < 0 || move.index() >= this.columns.size()) {
            return;
        }
        @SuppressWarnings("unchecked") final TableColumnImpl<T> column = (TableColumnImpl<T>) impl;
        final int previous = this.columns.indexOf(column);
        if (previous == move.index()) {
            return;
        }
        this.columns.remove(previous);
        this.columns.add(move.index(), column);
        this.columnsChanged();
        this.report(COLUMN_MOVED, new ColumnMove(column, move.index()), new ColumnMove(column, previous));
    }

    /**
     * Opens the menu of columns at a point: a check item per column that shows or hides it. The item of the last
     * shown column is disabled, so one always shows.
     */
    void openColumnMenu(final float x, final float y) {
        final Menu menu = this.ui.menu();
        for (final TableColumnImpl<T> column : this.columns) {
            final boolean last = column.isShown() && this.visible.size() == 1;
            menu.addItem(this.ui.checkMenuItem(column.title())
                    .setSelected(column.isShown())
                    .setEnabled(!last)
                    .setAction(column::setShown));
        }
        menu.openAt(x, y);
    }

    // ------------------------------------------------------------------
    // Sorting
    // ------------------------------------------------------------------

    @Override
    public TableView<T> sortBy(final TableColumn<T> column, final boolean ascending) {
        if (column != null && (!this.columns.contains(column) || !column.isSortable())) {
            throw new IllegalArgumentException(column + " is not a sortable column of this table");
        }
        this.applyOrder(column == null ? List.of() : List.of(new Sort(this.impl(column), ascending)));
        return this;
    }

    @Override
    public TableView<T> sortBy(final List<Sort> order) {
        final List<Sort> next = new ArrayList<>(order.size());
        for (final Sort sort : order) {
            final TableColumn<?> column = Objects.requireNonNull(sort, "sort").column();
            if (column == null || !this.columns.contains(column) || !column.isSortable()) {
                throw new IllegalArgumentException(column + " is not a sortable column of this table");
            }
            if (this.sortIndex(next, column) >= 0) {
                throw new IllegalArgumentException(column + " is named twice in the sort order");
            }
            next.add(new Sort(column, sort.ascending()));
        }
        this.applyOrder(next);
        return this;
    }

    @Override
    public List<Sort> sortOrder() {
        return this.sorts;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<TableColumn<T>> sortColumn() {
        return this.sorts.isEmpty() ? Optional.empty() : Optional.of((TableColumn<T>) this.sorts.getFirst().column());
    }

    @Override
    public boolean isSortAscending() {
        return this.sorts.isEmpty() || this.sorts.getFirst().ascending();
    }

    private static Sort first(final List<Sort> order) {
        return order.isEmpty() ? new Sort(null, true) : order.getFirst();
    }

    private int sortIndex(final List<Sort> order, final TableColumn<?> column) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).column() == column) {
                return i;
            }
        }
        return -1;
    }

    private List<Sort> without(final TableColumnImpl<T> column) {
        final List<Sort> next = new ArrayList<>(this.sorts);
        next.removeIf(sort -> sort.column() == column);
        return next;
    }

    /**
     * Sorts by an order whose columns were checked, then reports {@code SORTED_MANY} if it changed and
     * {@code SORTED} if its first sort did.
     */
    private void applyOrder(final List<Sort> next) {
        final List<Sort> before = this.sorts;
        this.reorder(List.copyOf(next));
        if (!before.equals(this.sorts)) {
            this.report(SORTED_MANY, this.sorts, before);
        }
        final Sort was = first(before);
        final Sort now = first(this.sorts);
        if (!was.equals(now)) {
            this.report(SORTED, now, was);
        }
    }

    /**
     * Called by a column when its comparator changed: sorts again if the rows are sorted by it, or takes it out of
     * the sort order if it is no longer sortable.
     */
    void comparatorChanged(final TableColumnImpl<T> column) {
        if (this.sortIndex(this.sorts, column) >= 0) {
            if (column.comparator().isEmpty()) {
                this.applyOrder(this.without(column));
            } else {
                this.reorder(this.sorts);
            }
        }
        this.invalidateLayout();
    }

    /**
     * Works out the order rows show in for a sort order, as indexes into some items: the set order for no sort, else a
     * stable sort by each sort's comparator in turn. Runs the comparators and changes nothing.
     */
    @SuppressWarnings("unchecked")
    private int[] orderOf(final List<T> items, final List<Sort> order) {
        final Integer[] rows = new Integer[items.size()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i;
        }
        Comparator<T> chain = null;
        for (final Sort sort : order) {
            final Comparator<T> comparator = (Comparator<T>) sort.column().comparator().orElseThrow();
            final Comparator<T> direction = sort.ascending() ? comparator : comparator.reversed();
            chain = chain == null ? direction : chain.thenComparing(direction);
        }
        if (chain != null) {
            final Comparator<T> by = chain;
            Arrays.sort(rows, (a, b) -> by.compare(items.get(a), items.get(b)));
        }
        final int[] indexes = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            indexes[i] = rows[i];
        }
        return indexes;
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
     * Shows the same items in another sort order. Each selected row follows its own item, so a sort never moves the
     * selection to another item with the same key. The comparators run before anything changes.
     */
    private void reorder(final List<Sort> sorts) {
        final int[] order = this.orderOf(this.items, sorts);
        final List<T> sorted = inOrder(this.items, order, !sorts.isEmpty());
        final int[] rowOf = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            rowOf[order[i]] = i;
        }
        final int[] previous = this.order;
        this.sorts = sorts;
        this.order = order;
        this.shown = sorted;
        this.list.replaceItems(sorted, row -> rowOf[previous[row]]);
    }

    /**
     * Asks to sort after a click on a shown column's title. A plain click on the only sort column steps it from
     * ascending to descending to the set order, and on any other column sorts by it alone, ascending. A click with
     * Shift steps the column through the order the same way, adding it at the end.
     */
    void titleClicked(final int shownIndex, final boolean shift) {
        final TableColumnImpl<T> column = this.visible.get(shownIndex);
        if (!column.isSortable()) {
            return;
        }
        if (shift) {
            final List<Sort> next = new ArrayList<>(this.sorts);
            final int at = this.sortIndex(next, column);
            if (at < 0) {
                next.add(new Sort(column, true));
            } else if (next.get(at).ascending()) {
                next.set(at, new Sort(column, false));
            } else {
                next.remove(at);
            }
            this.request(SORT_MANY, List.copyOf(next), this::applySortMany);
            return;
        }
        final Sort wanted;
        if (this.sorts.size() == 1 && this.sorts.getFirst().column() == column) {
            wanted = this.sorts.getFirst().ascending() ? new Sort(column, false) : new Sort(null, true);
        } else {
            wanted = new Sort(column, true);
        }
        this.request(SORT, wanted, this::applySort);
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

    /**
     * Sorts by the order a request ended with, skipping sorts that are null, name no sortable column of this table,
     * or name one again. A null order is ignored.
     */
    private void applySortMany(final List<Sort> order) {
        if (order == null) {
            return;
        }
        final List<Sort> next = new ArrayList<>(order.size());
        for (final Sort sort : order) {
            if (sort != null && sort.column() != null && this.columns.contains(sort.column())
                    && sort.column().isSortable() && this.sortIndex(next, sort.column()) < 0) {
                next.add(sort);
            }
        }
        this.applyOrder(next);
    }

    boolean isSortable(final int shownIndex) {
        return this.visible.get(shownIndex).isSortable();
    }

    /**
     * Returns the direction a shown column sorts in, or null when the rows are not sorted by it.
     */
    private Boolean sortDirection(final int shownIndex) {
        final int at = this.sortIndex(this.sorts, this.visible.get(shownIndex));
        return at < 0 ? null : this.sorts.get(at).ascending();
    }

    @Override
    Component<?> row(final T item) {
        return new Row<>(this.ui, this, item);
    }

    @Override
    boolean startEdit(final int row, final float x, final int codePoint, final Modifiers modifiers) {
        // The column under the pointer for a double press, else the first shown column with an editor
        int at = -1;
        if (Float.isNaN(x)) {
            for (int i = 0; i < this.visible.size() && at < 0; i++) {
                at = this.visible.get(i).cellEditor() != null ? i : -1;
            }
        } else {
            for (int i = 0; i < this.headerCells.length && i < this.visible.size(); i++) {
                if (x >= this.headerCells[i].x() && x < this.headerCells[i].right()) {
                    at = this.visible.get(i).cellEditor() != null ? i : -1;
                }
            }
        }
        if (at < 0) {
            return false;
        }
        final int column = at;
        final CellEditor<T> editor = this.visible.get(column).cellEditor();
        final T item = this.shown.get(row);
        this.request(EDIT_CELL, item, value -> {
            if (!(this.list.rowFor(row) instanceof Row<?> built) || column >= built.cells.size()) {
                return;
            }
            built.edit(column, this.session.begin(editor, item,
                    edited -> this.report(CELL_EDITED, (Object) edited, (Object) item),
                    () -> this.list.dropRow(row), this::requestFocus));
            this.list.pin(row);
            this.session.focus(codePoint, modifiers);
        });
        return true;
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
        // The comparators and the key function run before anything changes
        final List<T> next = Items.copyOf(items);
        final int[] order = this.orderOf(next, this.sorts);
        final List<T> sorted = inOrder(next, order, !this.sorts.isEmpty());
        final IntUnaryOperator moves = this.list.keyMoves(sorted);
        this.items = next;
        this.order = order;
        this.shown = sorted;
        this.autoStale = true;
        this.list.replaceItems(sorted, moves);
        return this;
    }

    @Override
    public TableView<T> refresh() {
        this.autoStale = true;
        this.list.refresh();
        return this;
    }

    @Override
    public TableView<T> refresh(final T item) {
        this.list.refresh(item);
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
    public TableView<T> setTypeAhead(final Function<? super T, ?> name) {
        this.typeAheadName = name;
        this.list.setTypeAhead(name == null ? null : this::typeAheadName);
        return this;
    }

    /**
     * Returns what type-ahead matches for an item: its type-ahead name if one is set, else the text of the first
     * column whose cells are text, else the item.
     */
    private Object typeAheadName(final T item) {
        if (this.typeAheadName != null) {
            return this.typeAheadName.apply(item);
        }
        for (final TableColumnImpl<T> column : this.columns) {
            final Function<? super T, ?> text = column.cellText();
            if (text != null) {
                return text.apply(item);
            }
        }
        return item;
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
        this.headerHeight = Math.max(0f, theme.metricOr(STYLE_HEADER_HEIGHT, DEFAULT_HEADER_HEIGHT));
        this.thumb = theme.paintOr(ScrollPanel.STYLE_THUMB, Paint.NONE);
        this.thumbHovered = theme.paintOr(ScrollPanel.STYLE_THUMB_HOVERED, this.thumb);
        this.track = theme.paintOr(ScrollPanel.STYLE_TRACK, Paint.NONE);
    }

    /**
     * Returns the width a column's title needs: the title, the header padding and, when sortable, the arrow's room.
     */
    private float titleWidth(final TextMetrics metrics, final TableColumnImpl<T> column) {
        return TextLine.of(column.title(), this.headerText, metrics).width() + this.headerPadding.horizontal()
                + (column.isSortable() ? ARROW_ROOM : 0f);
    }

    /**
     * Returns the width a column takes before weighted columns share what is left: its fixed width, or for an auto
     * column the wider of its title and its widest cell in view, kept within its limits. A weighted column takes its
     * minimum.
     */
    private float baseWidth(final TextMetrics metrics, final TableColumnImpl<T> column) {
        final GridLength length = column.width();
        if (length instanceof GridLength.Fixed fixed) {
            return column.clamp(fixed.size());
        } else if (length instanceof GridLength.Weighted) {
            return column.minWidth();
        }
        return column.clamp(Math.max(this.titleWidth(metrics, column), this.autoCells.getOrDefault(column, 0f)));
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        float columnsWidth = 0f;
        for (final TableColumnImpl<T> column : this.visible) {
            columnsWidth += this.baseWidth(context.text(), column);
        }
        final Size head = context.measure(this.header, availableWidth, this.headerHeight);
        final float bodyHeight = Float.isInfinite(availableHeight) ? availableHeight
                : Math.max(0f, availableHeight - head.height());
        final Size body = context.measure(this.list, availableWidth, bodyHeight);
        final float bar = this.horizontalPolicy == ScrollPanel.ScrollPolicy.ALWAYS ? GUTTER : 0f;
        return new Size(Math.max(columnsWidth, body.width()), head.height() + body.height() + bar);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final float headHeight = this.headerHeight;
        final TextMetrics metrics = context.text();
        final Rect full = new Rect(bounds.x(), bounds.y() + headHeight, bounds.width(),
                Math.max(0f, bounds.height() - headHeight));
        float natural = 0f;
        for (final TableColumnImpl<T> column : this.visible) {
            natural += this.baseWidth(metrics, column);
        }
        this.hbarShown = switch (this.horizontalPolicy) {
            case NEVER -> false;
            case ALWAYS -> true;
            case AUTO -> natural > this.list.rowWidthFor(full);
        };
        final Rect body = this.hbarShown
                ? new Rect(full.x(), full.y(), full.width(), Math.max(0f, full.height() - GUTTER))
                : full;
        this.bodyWidth = this.list.rowWidthFor(body);
        final float total = this.layoutColumns(metrics, this.bodyWidth);
        this.maxScrollX = this.horizontalPolicy == ScrollPanel.ScrollPolicy.NEVER ? 0f
                : Math.max(0f, total - this.bodyWidth);
        this.scrollX = Math.max(0f, Math.min(this.scrollX, this.maxScrollX));
        this.arranged = true;
        this.rowsX = body.x() + this.nodeOf(this.list).border().widths().left();
        final float offset = Math.round(this.scrollX);
        final int count = this.visible.size();
        this.headerCells = new Rect[count];
        this.gridLines = new Rect[Math.max(0, count - 1)];
        for (int i = 0; i < count; i++) {
            final float x = this.rowsX + this.columnX[i] - offset;
            this.headerCells[i] = new Rect(x, bounds.y(), this.columnWidth[i], headHeight);
            if (i > 0) {
                this.gridLines[i - 1] = new Rect(x - 1f, bounds.y(), 1f, headHeight + body.height());
            }
        }
        context.arrange(this.header, new Rect(bounds.x(), bounds.y(), bounds.width(), headHeight));
        context.arrange(this.list, body);
        // Rows that kept their slot are not arranged again by the list, so ask them when the columns moved
        if (!Arrays.equals(this.rowsColumnX, this.columnX) || !Arrays.equals(this.rowsColumnWidth, this.columnWidth)
                || this.rowsScrollX != offset) {
            this.rowsColumnX = this.columnX.clone();
            this.rowsColumnWidth = this.columnWidth.clone();
            this.rowsScrollX = offset;
            for (final Component<?> row : this.list.builtRows()) {
                ((Row<?>) row).relayout();
            }
        }
        this.measureAutoCells(context);
        this.hbar.update(new Rect(this.rowsX, bounds.bottom() - ScrollPanel.BAR_WIDTH, this.bodyWidth,
                ScrollPanel.BAR_WIDTH), this.bodyWidth, Math.max(total, this.bodyWidth));
        this.hThumbRect = this.hbar.thumb(this.scrollX, this.maxScrollX);
    }

    /**
     * Measures the cells in view of each shown auto column once the rows are built, when the table is first laid out
     * and after new items or a refresh, and lays out again if one got wider or narrower.
     */
    private void measureAutoCells(final ArrangeContext context) {
        if (!this.autoStale || !this.items.isEmpty() && this.list.builtRows().isEmpty()) {
            return;
        }
        this.autoStale = false;
        boolean changed = false;
        for (int i = 0; i < this.visible.size(); i++) {
            final TableColumnImpl<T> column = this.visible.get(i);
            if (!(column.width() instanceof GridLength.Auto)) {
                continue;
            }
            float widest = 0f;
            for (final Component<?> row : this.list.builtRows()) {
                final Component<?> cell = ((Row<?>) row).cell(i);
                if (cell != null) {
                    widest = Math.max(widest, context.measure(cell, Float.POSITIVE_INFINITY,
                            this.list.rowHeight()).width());
                }
            }
            final Float before = this.autoCells.put(column, widest);
            changed |= before == null || before != widest;
        }
        if (changed) {
            this.invalidateLayout();
        }
    }

    /**
     * Works out each shown column's offset and width for a row width, and the titles cut to fit.
     *
     * @return the columns' total width
     */
    private float layoutColumns(final TextMetrics metrics, final float width) {
        final int count = this.visible.size();
        if (this.columnX.length != count) {
            this.columnX = new float[count];
            this.columnWidth = new float[count];
            this.titles = new TextLine[count];
        }
        float used = 0f;
        float weights = 0f;
        for (int i = 0; i < count; i++) {
            final TableColumnImpl<T> column = this.visible.get(i);
            if (column.width() instanceof GridLength.Weighted weighted) {
                weights += weighted.weight();
                this.columnWidth[i] = 0f;
            } else {
                this.columnWidth[i] = this.baseWidth(metrics, column);
                used += this.columnWidth[i];
            }
        }
        final float left = Math.max(0f, width - used);
        float x = 0f;
        for (int i = 0; i < count; i++) {
            final TableColumnImpl<T> column = this.visible.get(i);
            if (column.width() instanceof GridLength.Weighted weighted && weights > 0f) {
                this.columnWidth[i] = column.clamp(left * weighted.weight() / weights);
            }
            this.columnX[i] = x;
            x += this.columnWidth[i];
            final float room = this.columnWidth[i] - this.headerPadding.horizontal()
                    - (column.isSortable() ? ARROW_ROOM : 0f);
            this.titles[i] = TextLine.of(column.title(), this.headerText, metrics)
                    .ellipsize(room, this.headerText, metrics);
        }
        return x;
    }

    @Override
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
        if (this.grid != Paint.NONE) {
            for (final Rect line : this.gridLines) {
                this.grid.paint(painter, line);
            }
        }
        if (this.hbarShown) {
            this.track.paint(painter, this.hbar.track());
            (this.hThumbDrag.isDragging() ? this.thumbHovered : this.thumb).paint(painter, this.hThumbRect);
        }
    }

    // ------------------------------------------------------------------
    // The horizontal bar
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.hThumbDrag.release();
        if (event.button() != MouseButton.LEFT || !this.hbarShown) {
            return;
        }
        final float page = Math.max(SCROLL_RATE, this.bodyWidth - SCROLL_RATE);
        final float next = this.hThumbDrag.press(event.x(), event.y(), this.scrollX, this.maxScrollX, page);
        if (!Float.isNaN(next)) {
            this.scrollXTo(next);
            event.consume();
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.hThumbDrag.isDragging()) {
            this.scrollXTo(this.hThumbDrag.drag(event.x(), event.y(), this.maxScrollX));
            event.consume();
        }
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.hThumbDrag.release();
    }

    @Override
    protected void onPointerRelease(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT) {
            this.hThumbDrag.release();
        }
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        if (this.maxScrollX > 0f && event.scrollX() != 0f
                && this.scrollXTo(this.scrollX - event.scrollX() * SCROLL_RATE)) {
            event.consume();
        }
    }

    /**
     * The column titles. Sortable titles show the hand cursor while the pointer is over the header, and the edges of
     * resizable columns the resize cursor.
     */
    private static final class Header extends BuiltInComponent<Header> implements HasCursor<Header> {
        private final TableViewImpl<?> table;
        private final Runnable drawTitles;
        private float pointerX;
        private float lineHeight;
        // Set only while paint runs
        private Painter painter;
        private Rect paintBounds;
        // The sort arrows' lines by shown index, and the cell and direction each was made for
        private Rect[][] arrows;
        private Rect[] arrowCells;
        private boolean[] arrowUp;
        // The shown column a press landed on, and whether on its edge, for the drag that may follow
        private int pressed;
        private boolean resizing;
        private boolean moving;
        private float pressX;
        private float pressWidth;
        // Where a moved column would land, as a slot among the shown columns, and its mark
        private int slot;
        private Rect slotMark;

        Header(final ComponentFactoryImpl ui, final TableViewImpl<?> table) {
            super(ui);
            this.table = table;
            this.drawTitles = this::drawTitles;
            this.arrows = new Rect[0][];
            this.arrowCells = new Rect[0];
            this.arrowUp = new boolean[0];
            this.pressed = -1;
            this.slot = -1;
            this.setAnchor(Anchor.FILL);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            this.lineHeight = context.text().lineHeight(this.table.headerText);
            return new Size(0f, this.table.headerHeight);
        }

        /**
         * Returns the sort arrow's lines at the right of a shown column's cell, made again only when the cell or the
         * direction changed.
         */
        private Rect[] arrow(final int index, final Rect cell, final Insets padding, final boolean up) {
            if (this.arrows.length != this.table.headerCells.length) {
                final int count = this.table.headerCells.length;
                this.arrows = new Rect[count][];
                this.arrowCells = new Rect[count];
                this.arrowUp = new boolean[count];
            }
            if (this.arrows[index] == null || this.arrowCells[index] != cell || this.arrowUp[index] != up) {
                final float arrowX = cell.right() - padding.right() - Rings.ARROW_LONG;
                final float arrowY = cell.y() + (cell.height() - Rings.ARROW_SHORT) * 0.5f;
                this.arrows[index] = up ? Shapes.up(arrowX, arrowY) : Shapes.down(arrowX, arrowY);
                this.arrowCells[index] = cell;
                this.arrowUp[index] = up;
            }
            return this.arrows[index];
        }

        private int columnAt(final float x) {
            final Rect[] cells = this.table.headerCells;
            for (int i = cells.length - 1; i >= 0; i--) {
                if (x >= cells[i].x()) {
                    return x < cells[i].right() ? i : -1;
                }
            }
            return -1;
        }

        /**
         * Returns the shown resizable column whose right edge is within {@link #EDGE} units of a point, or -1.
         */
        private int edgeAt(final float x) {
            final Rect[] cells = this.table.headerCells;
            for (int i = cells.length - 1; i >= 0; i--) {
                if (Math.abs(x - cells[i].right()) <= EDGE && this.table.visible.get(i).isResizable()) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Returns the slot among the shown columns a point falls in, by the middles of the titles.
         */
        private int slotAt(final float x) {
            final Rect[] cells = this.table.headerCells;
            for (int i = 0; i < cells.length; i++) {
                if (x < cells[i].x() + cells[i].width() * 0.5f) {
                    return i;
                }
            }
            return cells.length;
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.table.headerPaint.paint(painter, bounds);
            this.painter = painter;
            this.paintBounds = bounds;
            try {
                painter.clipped(bounds, this.drawTitles);
            } finally {
                this.painter = null;
                this.paintBounds = null;
            }
        }

        private void drawTitles() {
            final Painter painter = this.painter;
            final Rect bounds = this.paintBounds;
            final int hovered = this.isHovered() && this.pressed < 0 ? this.columnAt(this.pointerX) : -1;
            final Insets padding = this.table.headerPadding;
            final float textY = bounds.y() + padding.top() + (bounds.height() - padding.vertical() - this.lineHeight) * 0.5f;
            final int count = Math.min(this.table.titles.length, this.table.headerCells.length);
            for (int i = 0; i < count; i++) {
                final Rect cell = this.table.headerCells[i];
                if (i == hovered && this.table.isSortable(i)) {
                    this.table.headerHovered.paint(painter, cell);
                }
                this.table.titles[i].draw(painter, cell.x() + padding.left(), textY);
                final Boolean direction = this.table.sortDirection(i);
                if (direction != null) {
                    Shapes.fill(painter, this.table.headerText.color(), this.arrow(i, cell, padding, direction));
                }
            }
            if (this.moving && this.slotMark != null) {
                painter.fill(this.slotMark, this.table.headerText.color());
            }
        }

        @Override
        protected void onPointerMove(final PointerEvent event) {
            this.pointerX = event.x();
            if (this.edgeAt(event.x()) >= 0) {
                this.setCursor(CursorKind.RESIZE_HORIZONTAL);
            } else {
                final int column = this.columnAt(event.x());
                this.setCursor(column >= 0 && this.table.isSortable(column) ? CursorKind.HAND : null);
            }
        }

        @Override
        protected void onPointerPress(final PointerButtonEvent event) {
            this.pressed = -1;
            this.resizing = false;
            this.moving = false;
            if (event.button() != MouseButton.LEFT) {
                return;
            }
            final int edge = this.edgeAt(event.x());
            if (edge >= 0) {
                this.pressed = edge;
                this.resizing = true;
                this.pressX = event.x();
                this.pressWidth = this.table.columnWidth[edge];
                if (event.clicks() == 2) {
                    this.resize(edge, this.table.fitWidth(edge));
                }
                event.consume();
                return;
            }
            this.pressed = this.columnAt(event.x());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void resize(final int index, final float width) {
            ((TableViewImpl) this.table).userResize(this.table.visible.get(index), width);
        }

        @Override
        protected void onDragStart(final DragEvent event) {
            if (this.pressed >= 0 && !this.resizing && this.pressed < this.table.visible.size()
                    && this.table.visible.get(this.pressed).isMovable()) {
                this.moving = true;
            }
            this.onDrag(event);
        }

        @Override
        protected void onDrag(final DragEvent event) {
            if (this.pressed < 0 || this.pressed >= this.table.visible.size()) {
                return;
            }
            if (this.resizing) {
                this.resize(this.pressed, this.pressWidth + event.x() - this.pressX);
                event.consume();
            } else if (this.moving) {
                this.slot = this.slotAt(event.x());
                final Rect[] cells = this.table.headerCells;
                final float x = this.slot < cells.length ? cells[this.slot].x() : cells[cells.length - 1].right();
                this.slotMark = new Rect(x - 1f, this.bounds().y(), 2f, this.bounds().height());
                event.consume();
            }
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        protected void onDragEnd(final DragEvent event) {
            if (this.moving && this.pressed >= 0 && this.pressed < this.table.visible.size() && this.slot >= 0) {
                ((TableViewImpl) this.table).userMove(this.table.visible.get(this.pressed), this.slot);
            }
            this.pressed = -1;
            this.resizing = false;
            this.moving = false;
            this.slot = -1;
            this.slotMark = null;
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() == MouseButton.RIGHT) {
                this.table.openColumnMenu(event.x(), event.y());
                event.consume();
                return;
            }
            final boolean resized = this.resizing;
            this.resizing = false;
            this.pressed = -1;
            if (event.button() != MouseButton.LEFT || resized) {
                return;
            }
            final int column = this.columnAt(event.x());
            if (column >= 0) {
                this.table.titleClicked(column, event.modifiers().shift());
                event.consume();
            }
        }
    }

    /**
     * One body row: a cell per shown column, each in its column's span and the row's height, moved by the table's
     * sideways offset.
     */
    private static final class Row<T> extends BuiltInComposite<Row<T>> {
        private final TableViewImpl<T> table;
        private final List<Component<?>> cells;

        @SuppressWarnings("this-escape")
        Row(final ComponentFactoryImpl ui, final TableViewImpl<T> table, final T item) {
            super(ui);
            this.table = table;
            this.cells = new ArrayList<>();
            for (final TableColumnImpl<T> column : table.visible) {
                final Component<?> cell = column.cellFactory().apply(item);
                this.cells.add(cell);
                this.addContent(cell);
            }
            this.setAnchor(Anchor.FILL);
        }

        /**
         * Returns the cell of a shown column, or null if the row was built with fewer columns.
         */
        Component<?> cell(final int shownIndex) {
            return shownIndex < this.cells.size() ? this.cells.get(shownIndex) : null;
        }

        /**
         * Puts an editor in place of a shown column's cell.
         */
        void edit(final int shownIndex, final Component<?> editor) {
            this.removeChild(this.cells.get(shownIndex));
            this.cells.set(shownIndex, editor);
            this.addContent(editor);
            this.invalidateLayout();
        }

        /**
         * Arranges the cells again in the row's last slot, after the columns or the sideways offset changed.
         */
        void relayout() {
            this.invalidateArrangement();
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
            final float offset = Math.round(this.table.scrollX);
            for (int i = 0; i < this.cells.size(); i++) {
                if (i >= this.table.columnX.length) {
                    context.arrange(this.cells.get(i), new Rect(bounds.right(), bounds.y(), 0f, bounds.height()));
                    continue;
                }
                final Component<?> cell = this.cells.get(i);
                context.measure(cell, this.table.columnWidth[i], bounds.height());
                context.arrange(cell, new Rect(bounds.x() + this.table.columnX[i] - offset, bounds.y(),
                        this.table.columnWidth[i], bounds.height()));
            }
        }
    }
}
