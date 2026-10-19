package org.inspirenxe.strata.core.control.collection;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The core {@link TreeView}. It flattens the shown part of the hierarchy into entries of item, depth and whether the
 * item has children, and shows them in a {@link ListViewImpl} whose cells are rows of indent, arrow and the user's
 * cell. The list keeps focus; Left and Right bubble up from it to the tree.
 *
 * @param <T> the type of the items
 */
public final class TreeViewImpl<T> extends CustomComposite<TreeView<T>> implements TreeView<T> {
    private static final Paint DEFAULT_ARROW = Paint.solid(0xFFA0A0A0);

    private final ComponentFactory ui;
    private final ListViewImpl<Entry<T>> list;
    private final Set<Object> expanded;
    private final List<Consumer<Optional<T>>> selectionListeners;
    private final List<Consumer<T>> activateListeners;
    private List<T> roots;
    private List<Entry<T>> entries;
    private List<T> shown;
    private Function<? super T, ? extends List<? extends T>> children;
    private Function<? super T, ? extends Component<?>> cellFactory;
    private Function<? super T, ?> keyFunction;
    private float indent;
    private Paint arrow;
    private boolean quiet;

    /**
     * Creates a tree with no roots.
     *
     * @param ui the factory that owns the tree
     * @param cellFactory makes the component for an item's row
     */
    @SuppressWarnings("this-escape")
    public TreeViewImpl(final ComponentFactory ui, final Function<? super T, ? extends Component<?>> cellFactory) {
        super(ui);
        this.ui = ui;
        this.expanded = new HashSet<>();
        this.selectionListeners = new ArrayList<>();
        this.activateListeners = new ArrayList<>();
        this.roots = List.of();
        this.entries = List.of();
        this.shown = List.of();
        this.children = item -> List.of();
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.keyFunction = Function.identity();
        this.indent = DEFAULT_INDENT;
        this.arrow = DEFAULT_ARROW;
        this.list = new ListViewImpl<>(ui, entry -> new Row<>(this.ui, this, entry));
        this.list.setAnchor(Anchor.FILL);
        this.list.setKeyFunction(entry -> this.keyFunction.apply(entry.item));
        this.list.onSelectionChanged(entry -> {
            if (!this.quiet) {
                this.fireSelection();
            }
        });
        this.list.onActivate(entry -> {
            for (final Consumer<T> listener : List.copyOf(this.activateListeners)) {
                listener.accept(entry.item);
            }
        });
        this.addChild(this.list);
    }

    // ------------------------------------------------------------------
    // Hierarchy
    // ------------------------------------------------------------------

    @Override
    public List<T> roots() {
        return this.roots;
    }

    @Override
    public TreeView<T> setRoots(final List<? extends T> roots) {
        this.roots = List.copyOf(roots);
        this.rebuild();
        return this;
    }

    @Override
    public TreeView<T> setChildren(final Function<? super T, ? extends List<? extends T>> children) {
        this.children = Objects.requireNonNull(children, "children");
        this.rebuild();
        return this;
    }

    @Override
    public TreeView<T> setCellFactory(final Function<? super T, ? extends Component<?>> cellFactory) {
        this.cellFactory = Objects.requireNonNull(cellFactory, "cellFactory");
        this.list.refresh();
        return this;
    }

    @Override
    public TreeView<T> setKeyFunction(final Function<? super T, ?> keyFunction) {
        this.keyFunction = Objects.requireNonNull(keyFunction, "keyFunction");
        return this;
    }

    @Override
    public TreeView<T> refresh() {
        this.rebuild();
        this.list.refresh();
        return this;
    }

    private Object key(final T item) {
        return this.keyFunction.apply(item);
    }

    private List<? extends T> childrenOf(final T item) {
        final List<? extends T> found = this.children.apply(item);
        return found == null ? List.of() : found;
    }

    /**
     * Flattens the shown hierarchy and hands it to the list, which keeps the selection on the same key. Listeners
     * run only if the selected item changed.
     */
    private void rebuild() {
        final Optional<T> before = this.selected();
        final List<Entry<T>> flat = new ArrayList<>();
        for (final T root : this.roots) {
            this.flatten(root, 0, flat);
        }
        this.entries = List.copyOf(flat);
        final List<T> items = new ArrayList<>(flat.size());
        for (final Entry<T> entry : flat) {
            items.add(entry.item);
        }
        this.shown = List.copyOf(items);
        this.quiet = true;
        try {
            this.list.setItems(this.entries);
        } finally {
            this.quiet = false;
        }
        if (!this.selected().equals(before)) {
            this.fireSelection();
        }
    }

    private void flatten(final T item, final int depth, final List<Entry<T>> into) {
        final List<? extends T> kids = this.childrenOf(item);
        into.add(new Entry<>(item, depth, !kids.isEmpty()));
        if (!kids.isEmpty() && this.expanded.contains(this.key(item))) {
            for (final T child : kids) {
                this.flatten(child, depth + 1, into);
            }
        }
    }

    @Override
    public boolean isExpanded(final T item) {
        return this.expanded.contains(this.key(item));
    }

    @Override
    public TreeView<T> setExpanded(final T item, final boolean expanded) {
        final Object key = this.key(item);
        if (expanded == this.expanded.contains(key)) {
            return this;
        }
        if (expanded) {
            this.expanded.add(key);
            this.rebuild();
            return this;
        }
        // A selection inside the collapsing subtree moves up to the collapsing item
        final int at = this.indexOf(key);
        final int selected = this.list.selectedIndex();
        boolean inside = false;
        if (at >= 0 && selected > at) {
            inside = true;
            for (int i = at + 1; i <= selected; i++) {
                if (this.entries.get(i).depth <= this.entries.get(at).depth) {
                    inside = false;
                    break;
                }
            }
        }
        this.expanded.remove(key);
        if (inside) {
            this.quiet = true;
            try {
                this.list.select(at);
            } finally {
                this.quiet = false;
            }
        }
        this.rebuild();
        if (inside) {
            this.fireSelection();
        }
        return this;
    }

    private int indexOf(final Object key) {
        for (int i = 0; i < this.entries.size(); i++) {
            if (Objects.equals(key, this.key(this.entries.get(i).item))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public TreeView<T> expandAll() {
        final List<T> pending = new ArrayList<>(this.roots);
        final Set<Object> seen = new HashSet<>();
        while (!pending.isEmpty()) {
            final T item = pending.removeLast();
            final List<? extends T> kids = this.childrenOf(item);
            if (!kids.isEmpty() && seen.add(this.key(item))) {
                this.expanded.add(this.key(item));
                pending.addAll(kids);
            }
        }
        this.rebuild();
        return this;
    }

    @Override
    public TreeView<T> collapseAll() {
        int selected = this.list.selectedIndex();
        while (selected > 0 && this.entries.get(selected).depth > 0) {
            selected--;
        }
        final boolean moved = selected != this.list.selectedIndex();
        if (moved) {
            this.quiet = true;
            try {
                this.list.select(selected);
            } finally {
                this.quiet = false;
            }
        }
        this.expanded.clear();
        this.rebuild();
        if (moved) {
            this.fireSelection();
        }
        return this;
    }

    @Override
    public List<T> shownItems() {
        return this.shown;
    }

    @Override
    public int depth(final int index) {
        return this.entries.get(index).depth;
    }

    @Override
    public float indent() {
        return this.indent;
    }

    @Override
    public TreeView<T> setIndent(final float indent) {
        if (indent < 0f || Float.isInfinite(indent)) {
            throw new IllegalArgumentException("Indent must be finite and not negative: " + indent);
        }
        this.indent = indent;
        this.list.refresh();
        return this;
    }

    @Override
    public TreeView<T> setRowHeight(final float height) {
        this.list.setRowHeight(height);
        return this;
    }

    @Override
    public TreeView<T> setPlaceholder(final String placeholder) {
        this.list.setPlaceholder(placeholder);
        return this;
    }

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    @Override
    public Optional<T> selected() {
        return this.list.selected().map(entry -> entry.item);
    }

    @Override
    public TreeView<T> select(final T item) {
        if (item == null) {
            this.list.select(-1);
            return this;
        }
        final List<T> path = new ArrayList<>();
        if (!this.findPath(this.roots, this.key(item), path, new HashSet<>())) {
            this.list.select(-1);
            return this;
        }
        boolean opened = false;
        for (int i = 0; i < path.size() - 1; i++) {
            opened |= this.expanded.add(this.key(path.get(i)));
        }
        if (opened) {
            this.rebuild();
        }
        final int index = this.indexOf(this.key(item));
        this.list.select(index);
        return this;
    }

    /**
     * Depth-first search for an item by key, filling {@code path} from a root down to the item.
     */
    private boolean findPath(final List<? extends T> level, final Object key, final List<T> path, final Set<Object> seen) {
        for (final T candidate : level) {
            final Object candidateKey = this.key(candidate);
            path.add(candidate);
            if (Objects.equals(candidateKey, key)) {
                return true;
            }
            if (seen.add(candidateKey) && this.findPath(this.childrenOf(candidate), key, path, seen)) {
                return true;
            }
            path.removeLast();
        }
        return false;
    }

    @Override
    public TreeView<T> onSelectionChanged(final Consumer<Optional<T>> listener) {
        this.selectionListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @Override
    public TreeView<T> onActivate(final Consumer<T> listener) {
        this.activateListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    private void fireSelection() {
        final Optional<T> selected = this.selected();
        for (final Consumer<Optional<T>> listener : List.copyOf(this.selectionListeners)) {
            listener.accept(selected);
        }
    }

    void toggle(final Entry<T> entry) {
        this.setExpanded(entry.item, !this.isExpanded(entry.item));
    }

    // ------------------------------------------------------------------
    // Theme, layout and input
    // ------------------------------------------------------------------

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.arrow = theme.paintOr(STYLE_ARROW, DEFAULT_ARROW);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return context.measure(this.list, availableWidth, availableHeight);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        context.arrange(this.list, bounds);
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (event.target() != this.list || !event.action().isDown() || event.modifiers().control() || event.modifiers().alt()) {
            return;
        }
        final int index = this.list.selectedIndex();
        if (index < 0) {
            return;
        }
        final Entry<T> entry = this.entries.get(index);
        switch (event.key()) {
            case RIGHT -> {
                if (!entry.parent) {
                    return;
                }
                if (!this.isExpanded(entry.item)) {
                    this.setExpanded(entry.item, true);
                } else if (index + 1 < this.entries.size()) {
                    this.list.select(index + 1);
                }
            }
            case LEFT -> {
                if (entry.parent && this.isExpanded(entry.item)) {
                    this.setExpanded(entry.item, false);
                } else {
                    for (int i = index - 1; i >= 0; i--) {
                        if (this.entries.get(i).depth < entry.depth) {
                            this.list.select(i);
                            break;
                        }
                    }
                }
            }
            default -> {
                return;
            }
        }
        event.consume();
    }

    /**
     * One shown item.
     *
     * @param item the item
     * @param depth 0 for a root
     * @param parent whether the item has children
     */
    record Entry<T>(T item, int depth, boolean parent) {
    }

    /**
     * One row: the indent, the arrow column and the user's cell, which fills the rest.
     */
    private static final class Row<T> extends CustomComposite<Row<T>> {
        private final TreeViewImpl<T> tree;
        private final Entry<T> entry;
        private final Component<?> cell;

        @SuppressWarnings("this-escape")
        Row(final ComponentFactory ui, final TreeViewImpl<T> tree, final Entry<T> entry) {
            super(ui);
            this.tree = tree;
            this.entry = entry;
            this.cell = tree.cellFactory.apply(entry.item);
            this.addChild(this.cell);
            this.setAnchor(Anchor.FILL);
        }

        private float arrowX(final Rect bounds) {
            return bounds.x() + this.entry.depth * this.tree.indent;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final float lead = this.entry.depth * this.tree.indent + ARROW_WIDTH;
            final Size inner = context.measure(this.cell, Math.max(0f, availableWidth - lead), availableHeight);
            return new Size(lead + inner.width(), availableHeight);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            final float x = this.arrowX(bounds) + ARROW_WIDTH;
            context.arrange(this.cell, new Rect(x, bounds.y(), Math.max(0f, bounds.right() - x), bounds.height()));
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            if (!this.entry.parent) {
                return;
            }
            final float x = this.arrowX(bounds);
            if (this.tree.isExpanded(this.entry.item)) {
                Rings.down(painter, this.tree.arrow, x + (ARROW_WIDTH - Rings.ARROW_LONG) * 0.5f,
                        bounds.y() + (bounds.height() - Rings.ARROW_SHORT) * 0.5f);
            } else {
                Rings.right(painter, this.tree.arrow, x + (ARROW_WIDTH - Rings.ARROW_SHORT) * 0.5f,
                        bounds.y() + (bounds.height() - Rings.ARROW_LONG) * 0.5f);
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() != MouseButton.LEFT || !this.entry.parent) {
                return;
            }
            final float x = this.arrowX(this.bounds());
            if (event.x() >= x && event.x() < x + ARROW_WIDTH) {
                this.tree.toggle(this.entry);
                event.consume();
            }
        }
    }
}
