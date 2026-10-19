package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.event.DragEvent;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.event.ScrollEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.layout.ScrollBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The core {@link ListView}. Rows are built during arrange, once the viewport is known: rows that scrolled out are
 * removed, rows that scrolled in are built, measured and placed, and the change schedules one more layout. Rows sit
 * inside the list's border widths. A wheel notch scrolls one row and its spacing.
 *
 * @param <T> the type of the items
 */
public final class ListViewImpl<T> extends CustomComposite<ListView<T>> implements ListView<T> {
    /**
     * The narrowest a list asks to be, in UI units.
     */
    public static final float MIN_WIDTH = 40f;
    private static final Set<String> REQUIRED = Set.of(STYLE_ROW_SELECTED, ScrollPanel.STYLE_THUMB);
    private static final float GUTTER = ScrollPanel.BAR_WIDTH + ScrollPanel.BAR_GAP;

    private final List<Consumer<Optional<T>>> selectionListeners;
    private final List<Consumer<T>> activateListeners;
    private final Map<Integer, Component<?>> rows;
    private final ScrollBar bar;
    private final Runnable drawRows;
    private List<T> items;
    private Function<? super T, ? extends Component<?>> cellFactory;
    private Function<? super T, ?> keyFunction;
    private float rowHeight;
    private float rowSpacing;
    private int selectedIndex;
    private boolean deselectable;
    private boolean activateOnClick;
    private String placeholder;
    private boolean rowsStale;
    private int reveal;

    // Layout results
    private float scrollY;
    private float maxScrollY;
    private boolean barShown;
    private Rect viewport;
    private float rowWidth;

    // Pointer
    private float pointerY;
    private boolean dragging;
    private float dragGrab;

    // Theme
    private Paint row;
    private Paint rowHovered;
    private Paint rowSelected;
    private Paint thumb;
    private Paint thumbHovered;
    private Paint track;
    private TextStyle placeholderStyle;

    // Set only while paint runs
    private Painter painter;

    /**
     * Creates an empty list.
     *
     * @param ui the factory that owns the list
     * @param cellFactory makes the component for an item's row
     */
    @SuppressWarnings("this-escape")
    public ListViewImpl(final ComponentFactory ui, final Function<? super T, ? extends Component<?>> cellFactory) {
        super(ui);
        this.selectionListeners = new ArrayList<>();
        this.activateListeners = new ArrayList<>();
        this.rows = new HashMap<>();
        this.bar = new ScrollBar(true);
        this.drawRows = this::drawRows;
        this.items = List.of();
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.keyFunction = Function.identity();
        this.rowHeight = DEFAULT_ROW_HEIGHT;
        this.selectedIndex = -1;
        this.reveal = -1;
        this.viewport = Rect.ZERO;
        this.row = Paint.NONE;
        this.rowHovered = Paint.NONE;
        this.rowSelected = Paint.NONE;
        this.thumb = Paint.NONE;
        this.thumbHovered = Paint.NONE;
        this.track = Paint.NONE;
        this.placeholderStyle = TextStyle.DEFAULT;
        this.setFocusable(true);
        this.setTabbable(true);
    }

    // ------------------------------------------------------------------
    // ListView
    // ------------------------------------------------------------------

    @Override
    public List<T> items() {
        return this.items;
    }

    @Override
    public ListView<T> setItems(final List<? extends T> items) {
        final Optional<T> before = this.selected();
        final int beforeIndex = this.selectedIndex;
        this.items = List.copyOf(items);
        int index = -1;
        if (before.isPresent()) {
            final Object key = this.keyFunction.apply(before.get());
            for (int i = 0; i < this.items.size(); i++) {
                if (Objects.equals(key, this.keyFunction.apply(this.items.get(i)))) {
                    index = i;
                    break;
                }
            }
        }
        this.selectedIndex = index;
        this.rowsStale = true;
        this.invalidateLayout();
        if (index != beforeIndex || !this.selected().equals(before)) {
            this.fireSelection();
        }
        return this;
    }

    @Override
    public ListView<T> refresh() {
        this.rowsStale = true;
        this.invalidateLayout();
        return this;
    }

    @Override
    public ListView<T> setCellFactory(final Function<? super T, ? extends Component<?>> cellFactory) {
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        return this.refresh();
    }

    @Override
    public ListView<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    @Override
    public float rowHeight() {
        return this.rowHeight;
    }

    @Override
    public ListView<T> setRowHeight(final float height) {
        if (!(height > 0f) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("Row height must be finite and greater than 0: " + height);
        }
        this.rowHeight = height;
        return this.refresh();
    }

    @Override
    public float rowSpacing() {
        return this.rowSpacing;
    }

    @Override
    public ListView<T> setRowSpacing(final float spacing) {
        if (!(spacing >= 0f) || Float.isInfinite(spacing)) {
            throw new IllegalArgumentException("Row spacing must be finite and not negative: " + spacing);
        }
        this.rowSpacing = spacing;
        return this.refresh();
    }

    @Override
    public int selectedIndex() {
        return this.selectedIndex;
    }

    @Override
    public Optional<T> selected() {
        return this.selectedIndex < 0 ? Optional.empty() : Optional.ofNullable(this.items.get(this.selectedIndex));
    }

    @Override
    public ListView<T> select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.items.size());
        }
        if (index != this.selectedIndex) {
            this.selectedIndex = index;
            this.fireSelection();
        }
        if (index >= 0) {
            this.scrollToIndex(index);
        }
        return this;
    }

    @Override
    public ListView<T> selectItem(final T item) {
        final Object key = this.keyFunction.apply(item);
        for (int i = 0; i < this.items.size(); i++) {
            if (Objects.equals(key, this.keyFunction.apply(this.items.get(i)))) {
                return this.select(i);
            }
        }
        return this.select(-1);
    }

    @Override
    public boolean isDeselectable() {
        return this.deselectable;
    }

    @Override
    public ListView<T> setDeselectable(final boolean deselectable) {
        this.deselectable = deselectable;
        return this;
    }

    @Override
    public ListView<T> setActivateOnClick(final boolean onClick) {
        this.activateOnClick = onClick;
        return this;
    }

    @Override
    public ListView<T> setPlaceholder(final String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public float scrollY() {
        return this.scrollY;
    }

    @Override
    public ListView<T> setScrollY(final float scrollY) {
        this.scrollTo(scrollY);
        return this;
    }

    @Override
    public ListView<T> scrollToIndex(final int index) {
        Objects.checkIndex(index, this.items.size());
        this.reveal = index;
        this.invalidateArrangement();
        return this;
    }

    @Override
    public ListView<T> onSelectionChanged(final Consumer<Optional<T>> listener) {
        this.selectionListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @Override
    public ListView<T> onActivate(final Consumer<T> listener) {
        this.activateListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    private void fireSelection() {
        final Optional<T> selected = this.selected();
        for (final Consumer<Optional<T>> listener : List.copyOf(this.selectionListeners)) {
            listener.accept(selected);
        }
    }

    private void activate(final int index) {
        if (index < 0 || index >= this.items.size()) {
            return;
        }
        final T item = this.items.get(index);
        for (final Consumer<T> listener : List.copyOf(this.activateListeners)) {
            listener.accept(item);
        }
    }

    private boolean scrollTo(final float scrollY) {
        final float next = Math.max(0f, Math.min(scrollY, this.maxScrollY));
        if (next == this.scrollY) {
            return false;
        }
        this.scrollY = next;
        this.invalidateArrangement();
        return true;
    }

    private float step() {
        return this.rowHeight + this.rowSpacing;
    }

    private float totalHeight() {
        final int count = this.items.size();
        return count == 0 ? 0f : count * this.rowHeight + (count - 1) * this.rowSpacing;
    }

    /**
     * The width rows get when the list is arranged in a rectangle: the inside of the border, less the scroll bar's
     * gutter when the rows overflow. A table lays its header out with this before the list is arranged.
     */
    float rowWidthFor(final Rect bounds) {
        final Rect inner = bounds.shrink(this.border().widths());
        return Math.max(0f, inner.width() - (this.totalHeight() > inner.height() ? GUTTER : 0f));
    }

    private int rowAt(final float y) {
        if (!this.viewport.contains(this.viewport.x(), y)) {
            return -1;
        }
        final float along = y - this.viewport.y() + Math.round(this.scrollY);
        final int index = (int) Math.floor(along / this.step());
        if (index < 0 || index >= this.items.size() || along - index * this.step() >= this.rowHeight) {
            return -1;
        }
        return index;
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
        this.row = theme.paintOr(STYLE_ROW, Paint.NONE);
        this.rowHovered = theme.paintOr(STYLE_ROW_HOVERED, this.row);
        this.rowSelected = theme.paintOr(STYLE_ROW_SELECTED, Paint.NONE);
        this.thumb = theme.paintOr(ScrollPanel.STYLE_THUMB, Paint.NONE);
        this.thumbHovered = theme.paintOr(ScrollPanel.STYLE_THUMB_HOVERED, this.thumb);
        this.track = theme.paintOr(ScrollPanel.STYLE_TRACK, Paint.NONE);
        this.placeholderStyle = theme.textStyleOr(STYLE_PLACEHOLDER, TextStyle.DEFAULT);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final float insetWidth = this.border().widths().horizontal();
        final float insetHeight = this.border().widths().vertical();
        final float total = this.totalHeight();
        final boolean overflow = !Float.isInfinite(availableHeight) && total > availableHeight - insetHeight;
        final float gutter = overflow ? GUTTER : 0f;
        final float cellWidth = availableWidth - insetWidth - gutter;
        float widest = 0f;
        for (final Component<?> cell : this.childList()) {
            widest = Math.max(widest, context.measure(cell, cellWidth, this.rowHeight).width());
        }
        final float width = Math.max(MIN_WIDTH, widest + insetWidth + gutter);
        final float height = total + insetHeight;
        return new Size(width, Float.isInfinite(availableHeight) ? height : Math.min(height, availableHeight));
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect inner = bounds.shrink(this.border().widths());
        final float total = this.totalHeight();
        this.barShown = total > inner.height();
        this.viewport = new Rect(inner.x(), inner.y(), Math.max(0f, inner.width() - (this.barShown ? GUTTER : 0f)),
                inner.height());
        this.maxScrollY = Math.max(0f, total - this.viewport.height());
        if (this.reveal >= 0 && this.reveal < this.items.size()) {
            final float top = this.reveal * this.step();
            if (top < this.scrollY) {
                this.scrollY = top;
            } else if (top + this.rowHeight > this.scrollY + this.viewport.height()) {
                this.scrollY = top + this.rowHeight - this.viewport.height();
            }
        }
        this.reveal = -1;
        this.scrollY = Math.max(0f, Math.min(this.scrollY, this.maxScrollY));
        this.rowWidth = this.viewport.width();

        if (this.rowsStale) {
            this.rowsStale = false;
            this.rows.clear();
            this.clearChildren();
        }
        final int first;
        final int last;
        if (this.items.isEmpty() || this.viewport.height() <= 0f) {
            first = 0;
            last = -1;
        } else {
            first = Math.max(0, (int) Math.floor(this.scrollY / this.step()));
            last = Math.min(this.items.size() - 1, (int) Math.floor((this.scrollY + this.viewport.height()) / this.step()));
        }
        for (final Iterator<Map.Entry<Integer, Component<?>>> it = this.rows.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Integer, Component<?>> entry = it.next();
            if (entry.getKey() < first || entry.getKey() > last) {
                it.remove();
                this.removeChild(entry.getValue());
            }
        }
        final float offset = Math.round(this.scrollY);
        for (int i = first; i <= last; i++) {
            Component<?> cell = this.rows.get(i);
            if (cell == null) {
                cell = this.cellFactory.apply(this.items.get(i));
                this.rows.put(i, cell);
                this.addChild(cell);
                context.measure(cell, this.rowWidth, this.rowHeight);
            }
            context.arrange(cell, new Rect(this.viewport.x(), this.viewport.y() + i * this.step() - offset,
                    this.rowWidth, this.rowHeight));
        }
        this.bar.update(new Rect(inner.right() - ScrollPanel.BAR_WIDTH, inner.y(), ScrollPanel.BAR_WIDTH, inner.height()),
                this.viewport.height(), Math.max(total, this.viewport.height()));
    }

    @Override
    protected Rect childClip() {
        return this.viewport;
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.items.isEmpty()) {
            if (this.placeholder != null && !this.placeholder.isEmpty()) {
                final TextSpan span = new TextSpan(this.placeholder, this.placeholderStyle);
                final float width = this.environment().text().width(span);
                final float height = this.environment().text().lineHeight(this.placeholderStyle);
                painter.text(span, bounds.x() + (bounds.width() - width) * 0.5f, bounds.y() + (bounds.height() - height) * 0.5f);
            }
            return;
        }
        this.painter = painter;
        try {
            painter.clipped(this.viewport, this.drawRows);
        } finally {
            this.painter = null;
        }
    }

    /**
     * Draws each built row's background, inside the viewport clip.
     */
    private void drawRows() {
        final int hoveredRow = this.isHoverWithin() ? this.rowAt(this.pointerY) : -1;
        final float offset = Math.round(this.scrollY);
        for (final Integer index : this.rows.keySet()) {
            final Paint paint;
            if (index == this.selectedIndex) {
                paint = this.rowSelected;
            } else if (index == hoveredRow) {
                paint = this.rowHovered;
            } else {
                paint = this.row;
            }
            paint.paint(this.painter, new Rect(this.viewport.x(), this.viewport.y() + index * this.step() - offset,
                    this.rowWidth, this.rowHeight));
        }
    }

    @Override
    protected void paintOverChildren(final Painter painter, final Rect bounds) {
        if (this.barShown) {
            this.bar.paint(painter, this.scrollY, this.maxScrollY, this.track, this.dragging ? this.thumbHovered : this.thumb);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.pointerY = event.y();
    }

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.pointerY = event.y();
        this.dragging = false;
        if (event.button() != MouseButton.LEFT) {
            return;
        }
        if (this.barShown && this.bar.track().contains(event.x(), event.y())) {
            final Rect thumbRect = this.bar.thumb(this.scrollY, this.maxScrollY);
            if (thumbRect.contains(event.x(), event.y())) {
                this.dragging = true;
                this.dragGrab = event.y() - thumbRect.y();
            } else {
                final float page = Math.max(this.step(), this.viewport.height() - this.step());
                this.scrollTo(this.scrollY + (event.y() < thumbRect.y() ? -page : page));
            }
            event.consume();
            return;
        }
        final int index = this.rowAt(event.y());
        if (index < 0) {
            return;
        }
        if (this.deselectable && index == this.selectedIndex && event.clicks() == 1) {
            this.select(-1);
        } else if (index != this.selectedIndex) {
            this.select(index);
        }
        if (event.clicks() == 2 && !this.activateOnClick) {
            this.activate(index);
        }
        event.consume();
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (this.activateOnClick && event.button() == MouseButton.LEFT) {
            final int index = this.rowAt(event.y());
            if (index >= 0) {
                this.activate(index);
                event.consume();
            }
        }
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (this.dragging) {
            this.scrollTo(this.bar.offsetForThumbAt(event.y() - this.dragGrab, this.maxScrollY));
            event.consume();
        }
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.dragging = false;
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        if (this.scrollTo(this.scrollY - event.scrollY() * this.step())) {
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || event.modifiers().control() || event.modifiers().alt() || this.items.isEmpty()) {
            return;
        }
        final int page = Math.max(1, (int) Math.floor(this.viewport.height() / this.step()));
        final int last = this.items.size() - 1;
        final int current = this.selectedIndex;
        final int next = switch (event.key()) {
            case UP -> current < 0 ? last : Math.max(0, current - 1);
            case DOWN -> current < 0 ? 0 : Math.min(last, current + 1);
            case PAGE_UP -> Math.max(0, (current < 0 ? 0 : current) - page);
            case PAGE_DOWN -> Math.min(last, (current < 0 ? 0 : current) + page);
            case HOME -> 0;
            case END -> last;
            default -> -2;
        };
        if (next >= 0) {
            this.select(next);
            event.consume();
        } else if (event.key().isEnter() && event.action() == KeyAction.PRESS && current >= 0) {
            this.activate(current);
            event.consume();
        }
    }
}
