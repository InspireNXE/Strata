package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The core {@link GridView}. Each cell is a private child component, a slot, holding the component its cell factory
 * made, so each can have its own hover and tooltip. The hover and selection paints are drawn over that component.
 *
 * @param <T> the type of the items
 */
public final class GridViewImpl<T> extends CustomComposite<GridView<T>> implements GridView<T> {
    /**
     * How far a slot's content sits in from its edges, in UI units.
     */
    public static final float INSET = 1f;
    private static final Set<String> REQUIRED = Set.of(STYLE_CELL);

    private final ComponentFactory ui;
    private final List<Slot<T>> slots;
    private List<T> items;
    private Function<? super T, ? extends Component<?>> cellFactory;
    private Function<? super T, ?> keyFunction;
    private boolean activateOnClick;
    private Function<? super T, Tooltip> tooltips;
    private int columns;
    private float cellSize;
    private float spacing;
    private boolean selectable;
    private int selectedIndex;

    // Theme
    private Paint cellPaint;
    private Paint hoveredPaint;
    private Paint selectedPaint;

    /**
     * Creates a grid with no slots.
     *
     * @param ui the factory that owns the grid
     * @param cellFactory makes the component shown in a filled slot
     */
    @SuppressWarnings("this-escape")
    public GridViewImpl(final ComponentFactory ui, final Function<? super T, ? extends Component<?>> cellFactory) {
        super(ui);
        this.ui = ui;
        this.slots = new ArrayList<>();
        this.items = List.of();
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.keyFunction = Function.identity();
        this.columns = DEFAULT_COLUMNS;
        this.cellSize = DEFAULT_CELL_SIZE;
        this.selectedIndex = -1;
        this.cellPaint = Paint.NONE;
        this.hoveredPaint = Paint.NONE;
        this.selectedPaint = Paint.NONE;
    }

    // ------------------------------------------------------------------
    // Items and slots
    // ------------------------------------------------------------------

    @Override
    public List<T> items() {
        return this.items;
    }

    @Override
    public GridView<T> setItems(final List<? extends T> items) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.rebuild();
        if (this.selectedIndex >= this.items.size()) {
            this.select(-1);
        }
        return this;
    }

    @Override
    public T item(final int index) {
        return this.items.get(index);
    }

    @Override
    public GridView<T> setItem(final int index, final T item) {
        Objects.checkIndex(index, this.items.size());
        final List<T> next = new ArrayList<>(this.items);
        next.set(index, item);
        this.items = Collections.unmodifiableList(next);
        this.slots.get(index).fill(item);
        return this;
    }

    @Override
    public GridView<T> refresh() {
        this.rebuild();
        return this;
    }

    @Override
    public GridView<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    @Override
    public GridView<T> setCellFactory(final Function<? super T, ? extends Component<?>> cellFactory) {
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.rebuild();
        return this;
    }

    @Override
    public GridView<T> setTooltips(final Function<? super T, Tooltip> tooltips) {
        this.tooltips = tooltips;
        this.rebuild();
        return this;
    }

    private void rebuild() {
        this.clearChildren();
        this.slots.clear();
        for (int i = 0; i < this.items.size(); i++) {
            final Slot<T> slot = new Slot<>(this.ui, this, i);
            this.slots.add(slot);
            this.addChild(slot);
            slot.fill(this.items.get(i));
        }
        this.invalidateLayout();
    }

    @Override
    public int columns() {
        return this.columns;
    }

    @Override
    public GridView<T> setColumns(final int columns) {
        if (columns < 1) {
            throw new IllegalArgumentException("At least one column is needed: " + columns);
        }
        this.columns = columns;
        this.invalidateLayout();
        return this;
    }

    @Override
    public float cellSize() {
        return this.cellSize;
    }

    @Override
    public GridView<T> setCellSize(final float size) {
        if (!(size > 0f) || Float.isInfinite(size)) {
            throw new IllegalArgumentException("Cell size must be finite and greater than 0: " + size);
        }
        this.cellSize = size;
        this.invalidateLayout();
        return this;
    }

    @Override
    public float spacing() {
        return this.spacing;
    }

    @Override
    public GridView<T> setSpacing(final float spacing) {
        if (!(spacing >= 0f) || Float.isInfinite(spacing)) {
            throw new IllegalArgumentException("Spacing must be finite and not negative: " + spacing);
        }
        this.spacing = spacing;
        this.invalidateLayout();
        return this;
    }

    // ------------------------------------------------------------------
    // Selection and clicks
    // ------------------------------------------------------------------

    @Override
    public boolean isSelectable() {
        return this.selectable;
    }

    @Override
    public GridView<T> setSelectable(final boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    @Override
    public int selectedIndex() {
        return this.selectedIndex;
    }

    @Override
    public GridView<T> select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.items.size());
        }
        final int previous = this.selectedIndex;
        if (index != previous) {
            this.selectedIndex = index;
            this.report(SELECTED, index, previous);
        }
        return this;
    }

    @Override
    public Optional<T> selected() {
        return this.selectedIndex < 0 ? Optional.empty() : Optional.ofNullable(this.items.get(this.selectedIndex));
    }

    @Override
    public GridView<T> selectItem(final T item) {
        final Object key = item == null ? null : this.keyFunction.apply(item);
        for (int i = 0; i < this.items.size(); i++) {
            final T candidate = this.items.get(i);
            if (Objects.equals(key, candidate == null ? null : this.keyFunction.apply(candidate))) {
                return this.select(i);
            }
        }
        return this.select(-1);
    }

    @Override
    public GridView<T> setActivateOnClick(final boolean onClick) {
        this.activateOnClick = onClick;
        return this;
    }

    @Override
    public int hoveredIndex() {
        for (final Slot<T> slot : this.slots) {
            if (slot.isHoverWithin()) {
                return slot.index;
            }
        }
        return -1;
    }

    /**
     * Handles a click on a cell: a left click asks to select it when the grid is selectable, then the click itself
     * is asked for and reported, then a left double click, or a single one with activate on click, asks to activate
     * the cell.
     */
    private void clicked(final int index, final PointerButtonEvent event) {
        final boolean left = event.button() == MouseButton.LEFT;
        if (this.selectable && left && index != this.selectedIndex) {
            this.request(SELECT, index, this::applySelect);
        }
        this.request(CLICK, new Click<>(index, this.items.get(index), event.button(), event.modifiers()),
                this::applyClick);
        if (left && event.clicks() == (this.activateOnClick ? 1 : 2)) {
            this.request(ACTIVATE, index, this::applyActivate);
        }
    }

    /**
     * Reports the activation a request ended with. A null or an index outside the cells is ignored.
     */
    private void applyActivate(final Integer index) {
        if (index != null && index >= 0 && index < this.items.size()) {
            this.report(ACTIVATED, index, index);
        }
    }

    /**
     * Selects the slot a request ended with, or clears the selection for -1. A null or any other index outside the
     * slots is ignored.
     */
    private void applySelect(final Integer index) {
        if (index != null && index >= -1 && index < this.items.size()) {
            this.select(index);
        }
    }

    /**
     * Reports the click a request ended with. A null click or one whose index is not a slot's is ignored.
     */
    private void applyClick(final Click<?> click) {
        if (click != null && click.index() >= 0 && click.index() < this.items.size()) {
            this.report(CLICKED, click, click);
        }
    }

    // ------------------------------------------------------------------
    // Theme and layout
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.cellPaint = theme.paintOr(STYLE_CELL, Paint.NONE);
        this.hoveredPaint = theme.paintOr(STYLE_CELL_HOVERED, Paint.NONE);
        this.selectedPaint = theme.paintOr(STYLE_CELL_SELECTED, this.hoveredPaint);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        for (final Slot<T> slot : this.slots) {
            context.measure(slot, this.cellSize, this.cellSize);
        }
        final int count = this.items.size();
        if (count == 0) {
            return Size.ZERO;
        }
        final int rows = (count + this.columns - 1) / this.columns;
        return new Size(this.columns * this.cellSize + (this.columns - 1) * this.spacing,
                rows * this.cellSize + (rows - 1) * this.spacing);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final float step = this.cellSize + this.spacing;
        for (final Slot<T> slot : this.slots) {
            final int column = slot.index % this.columns;
            final int row = slot.index / this.columns;
            context.arrange(slot, new Rect(bounds.x() + column * step, bounds.y() + row * step, this.cellSize, this.cellSize));
        }
    }

    /**
     * One slot and its cell, if it holds an item.
     */
    private static final class Slot<T> extends CustomComposite<Slot<T>> {
        private final GridViewImpl<T> grid;
        private final int index;
        private Component<?> content;

        Slot(final ComponentFactory ui, final GridViewImpl<T> grid, final int index) {
            super(ui);
            this.grid = grid;
            this.index = index;
        }

        void fill(final T item) {
            if (this.content != null) {
                this.removeChild(this.content);
                this.content = null;
            }
            if (item != null) {
                this.content = this.grid.cellFactory.apply(item);
                this.addContent(this.content);
            }
            this.setTooltip(item == null || this.grid.tooltips == null ? null : this.grid.tooltips.apply(item));
            this.invalidateLayout();
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            if (this.content != null) {
                context.measure(this.content, Math.max(0f, availableWidth - INSET * 2f), Math.max(0f, availableHeight - INSET * 2f));
            }
            return new Size(this.grid.cellSize, this.grid.cellSize);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            if (this.content != null) {
                context.arrange(this.content, bounds.shrink(Insets.of(INSET)));
            }
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.grid.cellPaint.paint(painter, bounds);
        }

        @Override
        protected void paintOverChildren(final Painter painter, final Rect bounds) {
            if (this.index == this.grid.selectedIndex) {
                this.grid.selectedPaint.paint(painter, bounds);
            } else if (this.isHoverWithin()) {
                this.grid.hoveredPaint.paint(painter, bounds);
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            this.grid.clicked(this.index, event);
            event.consume();
        }
    }
}
