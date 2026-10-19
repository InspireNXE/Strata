package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.text.RichText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The core {@link TableView}: a header component above a {@link ListViewImpl} whose cells are rows that place one
 * cell per column. Column offsets are worked out once per arrange, from the width the list gives its rows, and both
 * the header and every row read them.
 *
 * @param <T> the type of the items
 */
public final class TableViewImpl<T> extends CustomComposite<TableView<T>> implements TableView<T> {
    /**
     * The space between a title or a text cell and its column's edges, in UI units.
     */
    public static final float CELL_PADDING = 4f;
    /**
     * The room a sort arrow takes at the right of a sortable title, in UI units.
     */
    public static final float ARROW_ROOM = Rings.ARROW_LONG + CELL_PADDING;
    private static final Set<String> REQUIRED = Set.of(STYLE_HEADER);

    private final ComponentFactory ui;
    private final List<Column<T>> columns;
    private final ListViewImpl<T> list;
    private final Header header;
    private List<T> items;
    private List<T> shown;
    private int sortColumn;
    private boolean ascending;

    // Column offsets and widths, relative to the start of a row
    private float[] columnX;
    private float[] columnWidth;
    private TextSpan[] titles;
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
    public TableViewImpl(final ComponentFactory ui) {
        super(ui);
        this.ui = ui;
        this.columns = new ArrayList<>();
        this.items = List.of();
        this.shown = List.of();
        this.sortColumn = -1;
        this.ascending = true;
        this.columnX = new float[0];
        this.columnWidth = new float[0];
        this.titles = new TextSpan[0];
        this.headerPaint = Paint.NONE;
        this.headerHovered = Paint.NONE;
        this.grid = Paint.NONE;
        this.headerText = TextStyle.DEFAULT;
        this.header = new Header(ui, this);
        this.list = new ListViewImpl<>(ui, item -> new Row<>(this.ui, this, item));
        this.list.setAnchor(Anchor.FILL);
        this.addChild(this.header);
        this.addChild(this.list);
    }

    // ------------------------------------------------------------------
    // Columns and sorting
    // ------------------------------------------------------------------

    @Override
    public TableView<T> addColumn(final String title, final GridLength width,
                                  final Function<? super T, ? extends Component<?>> cellFactory) {
        this.columns.add(new Column<>(Objects.requireNonNull(title, "title"), Objects.requireNonNull(width, "width"),
                Objects.requireNonNull(cellFactory, "cellFactory"), null));
        return this.columnsChanged();
    }

    @Override
    public TableView<T> addTextColumn(final String title, final GridLength width, final Function<? super T, String> text) {
        Objects.requireNonNull(text, "text");
        final Function<T, Component<?>> cell = item -> {
            final var label = this.ui.label(text.apply(item));
            label.setOverflow(TextOverflow.ELLIPSIS);
            label.setMargin(Insets.of(CELL_PADDING, 0f));
            label.setAnchor(Anchor.MIDDLE_LEFT);
            return label;
        };
        this.columns.add(new Column<>(Objects.requireNonNull(title, "title"), Objects.requireNonNull(width, "width"), cell,
                Comparator.comparing(text, String.CASE_INSENSITIVE_ORDER)));
        return this.columnsChanged();
    }

    private TableView<T> columnsChanged() {
        this.list.refresh();
        this.invalidateLayout();
        return this;
    }

    @Override
    public int columnCount() {
        return this.columns.size();
    }

    @Override
    public String columnTitle(final int column) {
        return this.columns.get(column).title;
    }

    @Override
    public TableView<T> setColumnComparator(final int column, final Comparator<? super T> comparator) {
        this.columns.get(column).comparator = comparator;
        if (column == this.sortColumn) {
            if (comparator == null) {
                this.sortColumn = -1;
                this.ascending = true;
            }
            this.resort();
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public TableView<T> sortBy(final int column, final boolean ascending) {
        if (column != -1) {
            Objects.checkIndex(column, this.columns.size());
            if (this.columns.get(column).comparator == null) {
                throw new IllegalArgumentException("Column " + column + " is not sortable");
            }
        }
        this.sortColumn = column;
        this.ascending = column == -1 || ascending;
        this.resort();
        return this;
    }

    @Override
    public int sortColumn() {
        return this.sortColumn;
    }

    @Override
    public boolean isSortAscending() {
        return this.ascending;
    }

    @SuppressWarnings("unchecked")
    private void resort() {
        if (this.sortColumn < 0) {
            this.shown = this.items;
        } else {
            final Comparator<T> comparator = (Comparator<T>) this.columns.get(this.sortColumn).comparator;
            final List<T> sorted = new ArrayList<>(this.items);
            sorted.sort(this.ascending ? comparator : comparator.reversed());
            this.shown = List.copyOf(sorted);
        }
        this.list.setItems(this.shown);
    }

    /**
     * Sorts by a column after a click on its title: ascending first, then flipping.
     */
    void titleClicked(final int column) {
        if (this.columns.get(column).comparator == null) {
            return;
        }
        this.sortBy(column, column != this.sortColumn || !this.ascending);
    }

    boolean isSortable(final int column) {
        return this.columns.get(column).comparator != null;
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
        this.items = List.copyOf(items);
        this.resort();
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
    public TableView<T> setRowHeight(final float height) {
        this.list.setRowHeight(height);
        return this;
    }

    @Override
    public TableView<T> setPlaceholder(final String placeholder) {
        this.list.setPlaceholder(placeholder);
        return this;
    }

    @Override
    public int selectedIndex() {
        return this.list.selectedIndex();
    }

    @Override
    public Optional<T> selected() {
        return this.list.selected();
    }

    @Override
    public TableView<T> select(final int index) {
        this.list.select(index);
        return this;
    }

    @Override
    public TableView<T> selectItem(final T item) {
        this.list.selectItem(item);
        return this;
    }

    @Override
    public TableView<T> onSelectionChanged(final Consumer<Optional<T>> listener) {
        this.list.onSelectionChanged(listener);
        return this;
    }

    @Override
    public TableView<T> onActivate(final Consumer<T> listener) {
        this.list.onActivate(listener);
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
    }

    private float autoWidth(final TextMetrics metrics, final int column) {
        final Column<T> definition = this.columns.get(column);
        return metrics.width(new TextSpan(definition.title, this.headerText)) + CELL_PADDING * 2f
                + (definition.comparator != null ? ARROW_ROOM : 0f);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        float columnsWidth = 0f;
        for (int i = 0; i < this.columns.size(); i++) {
            final GridLength width = this.columns.get(i).width;
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
        this.rowsX = body.x() + this.list.border().widths().left();
        context.arrange(this.header, new Rect(bounds.x(), bounds.y(), bounds.width(), headHeight));
        context.arrange(this.list, body);
    }

    private void layoutColumns(final TextMetrics metrics, final float width) {
        final int count = this.columns.size();
        if (this.columnX.length != count) {
            this.columnX = new float[count];
            this.columnWidth = new float[count];
            this.titles = new TextSpan[count];
        }
        float used = 0f;
        float weights = 0f;
        for (int i = 0; i < count; i++) {
            final GridLength length = this.columns.get(i).width;
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
            if (this.columns.get(i).width instanceof GridLength.Weighted weighted && weights > 0f) {
                this.columnWidth[i] = left * weighted.weight() / weights;
            }
            this.columnX[i] = x;
            x += this.columnWidth[i];
            final float room = this.columnWidth[i] - CELL_PADDING * 2f - (this.isSortable(i) ? ARROW_ROOM : 0f);
            final List<TextSpan> title = RichText.ellipsize(List.of(new TextSpan(this.columns.get(i).title, this.headerText)),
                    room, metrics);
            this.titles[i] = title.isEmpty() ? new TextSpan("", this.headerText) : title.getFirst();
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
     * One column's definition.
     */
    private static final class Column<T> {
        final String title;
        final GridLength width;
        final Function<? super T, ? extends Component<?>> cellFactory;
        Comparator<? super T> comparator;

        Column(final String title, final GridLength width, final Function<? super T, ? extends Component<?>> cellFactory,
               final Comparator<? super T> comparator) {
            this.title = title;
            this.width = width;
            this.cellFactory = cellFactory;
            this.comparator = comparator;
        }
    }

    /**
     * The column titles. Sortable titles show the hand cursor while the pointer is over the header.
     */
    private static final class Header extends CustomComponent<Header> {
        private final TableViewImpl<?> table;
        private float pointerX;
        private float lineHeight;

        Header(final ComponentFactory ui, final TableViewImpl<?> table) {
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
            final float textY = bounds.y() + (bounds.height() - this.lineHeight) * 0.5f;
            for (int i = 0; i < this.table.titles.length; i++) {
                final Rect cell = new Rect(this.table.rowsX + this.table.columnX[i], bounds.y(), this.table.columnWidth[i],
                        bounds.height());
                if (i == hovered && this.table.isSortable(i)) {
                    this.table.headerHovered.paint(painter, cell);
                }
                painter.text(this.table.titles[i], cell.x() + CELL_PADDING, textY);
                if (i == this.table.sortColumn) {
                    final float arrowX = cell.right() - CELL_PADDING - Rings.ARROW_LONG;
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
    private static final class Row<T> extends CustomComposite<Row<T>> {
        private final TableViewImpl<T> table;
        private final List<Component<?>> cells;

        @SuppressWarnings("this-escape")
        Row(final ComponentFactory ui, final TableViewImpl<T> table, final T item) {
            super(ui);
            this.table = table;
            this.cells = new ArrayList<>();
            for (final Column<T> column : table.columns) {
                final Component<?> cell = column.cellFactory.apply(item);
                this.cells.add(cell);
                this.addChild(cell);
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
