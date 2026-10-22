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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The core {@link TreeView}. It flattens the shown part of the hierarchy into entries of item, depth and whether the
 * item has children, and shows them in a {@link ListViewImpl} whose cells are rows of indent, arrow and the user's
 * cell. The list keeps focus; Left and Right bubble up from it to the tree. The tree routes the list's input and
 * selection changes to itself and fires them as its own events, so the list fires no {@link ListView} events. A
 * change from code that moves the selection more than once, such as a collapse that rebuilds the rows, reports
 * {@code SELECTED_MANY} and {@link TreeView#SELECTED} at most once each, at the end.
 *
 * @param <T> the type of the items
 */
public final class TreeViewImpl<T> extends BuiltInComposite<TreeView<T>> implements TreeView<T> {
    private static final Paint DEFAULT_ARROW = Paint.solid(0xFFA0A0A0);

    private final ComponentFactoryImpl ui;
    private final ListViewImpl<Entry<T>> list;
    private final Set<Object> expanded;
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
    public TreeViewImpl(final ComponentFactoryImpl ui, final Function<? super T, ? extends Component<?>> cellFactory) {
        super(ui);
        this.ui = ui;
        this.expanded = new HashSet<>();
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
        this.list.route(this::userSelect, this::userActivate, (index, previous) -> {
            if (!this.quiet) {
                this.report(SELECTED, index, previous);
            }
        });
        this.list.routeMany((indexes, current) -> this.request(SELECT_MANY, indexes, set -> this.list.applyMany(set, current)),
                (indexes, previous) -> {
                    if (!this.quiet) {
                        this.report(SELECTED_MANY, indexes, previous);
                    }
                });
        this.addChild(this.list);
    }

    // ------------------------------------------------------------------
    // Hierarchy
    // ------------------------------------------------------------------

    @Override
    public List<T> items() {
        return this.roots;
    }

    @Override
    public TreeView<T> setItems(final List<? extends T> items) {
        this.roots = List.copyOf(items);
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
     * Rebuilds the rows with {@link #flattenShown} as one change of selection, through {@link #changeSelection}.
     */
    private void rebuild() {
        this.changeSelection(this::flattenShown);
    }

    /**
     * Runs a change that may move the selection several times, then reports {@code SELECTED_MANY} once if the
     * selected rows or items changed and {@link TreeView#SELECTED} once if the selected index or item changed. Inside
     * another such change it only runs the change, and the outer one reports.
     */
    private void changeSelection(final Runnable change) {
        if (this.quiet) {
            change.run();
            return;
        }
        final int beforeIndex = this.list.selectedIndex();
        final Optional<T> before = this.selected();
        final List<Integer> beforeSet = this.list.selectedIndexes();
        final List<T> beforeItems = this.selectedItems();
        this.quiet = true;
        try {
            change.run();
        } finally {
            this.quiet = false;
        }
        final List<Integer> set = this.list.selectedIndexes();
        if (!set.equals(beforeSet) || !this.selectedItems().equals(beforeItems)) {
            this.report(SELECTED_MANY, set, beforeSet);
        }
        final int index = this.list.selectedIndex();
        if (index != beforeIndex || !this.selected().equals(before)) {
            this.report(SELECTED, index, beforeIndex);
        }
    }

    /**
     * Reports {@link TreeView#EXPANDED} for each item, or nothing for null.
     */
    private void reportExpansions(final List<T> items, final boolean expanded) {
        if (items == null) {
            return;
        }
        for (final T item : items) {
            this.report(EXPANDED, new Expansion<>(item, expanded), new Expansion<>(item, !expanded));
        }
    }

    /**
     * Flattens the shown hierarchy and hands it to the list, which keeps the selection on the same key.
     */
    private void flattenShown() {
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
        this.list.setItems(this.entries);
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
        this.changeSelection(() -> {
            if (expanded) {
                this.expanded.add(key);
            } else {
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
                // With several selected, hidden rows just leave the selection when the rows are rebuilt
                if (inside && this.list.selectionMode() == SelectionMode.SINGLE) {
                    this.list.select(at);
                }
            }
            this.flattenShown();
            this.report(EXPANDED, new Expansion<>(item, expanded), new Expansion<>(item, !expanded));
        });
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
        final List<T> opened = this.isHandled(EXPANDED) ? new ArrayList<>() : null;
        final List<T> pending = new ArrayList<>(this.roots);
        final Set<Object> seen = new HashSet<>();
        while (!pending.isEmpty()) {
            final T item = pending.removeLast();
            final List<? extends T> kids = this.childrenOf(item);
            if (!kids.isEmpty() && seen.add(this.key(item))) {
                if (this.expanded.add(this.key(item)) && opened != null) {
                    opened.add(item);
                }
                pending.addAll(kids);
            }
        }
        this.changeSelection(() -> {
            this.flattenShown();
            this.reportExpansions(opened, true);
        });
        return this;
    }

    @Override
    public TreeView<T> collapseAll() {
        final List<T> closed;
        if (this.isHandled(EXPANDED) && !this.expanded.isEmpty()) {
            closed = new ArrayList<>();
            this.collectExpanded(this.roots, new HashSet<>(), closed);
        } else {
            closed = null;
        }
        this.changeSelection(() -> {
            int selected = this.list.selectedIndex();
            while (selected > 0 && this.entries.get(selected).depth > 0) {
                selected--;
            }
            if (selected != this.list.selectedIndex() && this.list.selectionMode() == SelectionMode.SINGLE) {
                this.list.select(selected);
            }
            this.expanded.clear();
            this.flattenShown();
            this.reportExpansions(closed, false);
        });
        return this;
    }

    /**
     * Collects, depth first, every item reachable from {@code level} whose key is expanded, each key once.
     */
    private void collectExpanded(final List<? extends T> level, final Set<Object> seen, final List<T> into) {
        for (final T item : level) {
            final Object key = this.key(item);
            if (!seen.add(key)) {
                continue;
            }
            if (this.expanded.contains(key)) {
                into.add(item);
            }
            this.collectExpanded(this.childrenOf(item), seen, into);
        }
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
    public TreeView<T> setPlaceholder(final Text placeholder) {
        this.list.setPlaceholder(placeholder);
        return this;
    }

    @Override
    public float rowHeight() {
        return this.list.rowHeight();
    }

    @Override
    public float rowSpacing() {
        return this.list.rowSpacing();
    }

    @Override
    public TreeView<T> setRowSpacing(final float spacing) {
        this.list.setRowSpacing(spacing);
        return this;
    }

    @Override
    public float scrollY() {
        return this.list.scrollY();
    }

    @Override
    public TreeView<T> setScrollY(final float scrollY) {
        this.list.setScrollY(scrollY);
        return this;
    }

    @Override
    public TreeView<T> scrollToIndex(final int index) {
        this.list.scrollToIndex(index);
        return this;
    }

    @Override
    public boolean isDeselectable() {
        return this.list.isDeselectable();
    }

    @Override
    public TreeView<T> setDeselectable(final boolean deselectable) {
        this.list.setDeselectable(deselectable);
        return this;
    }

    @Override
    public TreeView<T> setActivateOnClick(final boolean onClick) {
        this.list.setActivateOnClick(onClick);
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
    public int selectedIndex() {
        return this.list.selectedIndex();
    }

    @Override
    public List<T> selectedItems() {
        final List<T> items = new ArrayList<>();
        for (final Entry<T> entry : this.list.selectedItems()) {
            items.add(entry.item);
        }
        return items;
    }

    @Override
    public SelectionMode selectionMode() {
        return this.list.selectionMode();
    }

    @Override
    public TreeView<T> setSelectionMode(final SelectionMode mode) {
        this.list.setSelectionMode(mode);
        return this;
    }

    @Override
    public List<Integer> selectedIndexes() {
        return this.list.selectedIndexes();
    }

    @Override
    public TreeView<T> selectIndexes(final Collection<Integer> indexes) {
        this.list.selectIndexes(indexes);
        return this;
    }

    @Override
    public TreeView<T> selectItems(final Collection<? extends T> items) {
        final List<List<T>> paths = new ArrayList<>(items.size());
        final Set<Object> found = new HashSet<>();
        for (final T item : items) {
            final List<T> path = new ArrayList<>();
            if (item != null && this.findPath(this.roots, this.key(item), path, new HashSet<>())) {
                paths.add(path);
                found.add(this.key(item));
            }
        }
        if (found.size() > 1 && this.list.selectionMode() == SelectionMode.SINGLE) {
            throw new IllegalArgumentException("A tree in SINGLE mode selects at most one item: " + found.size()
                    + " found");
        }
        this.changeSelection(() -> {
            final List<T> opened = new ArrayList<>();
            for (final List<T> path : paths) {
                for (int i = 0; i < path.size() - 1; i++) {
                    if (this.expanded.add(this.key(path.get(i)))) {
                        opened.add(path.get(i));
                    }
                }
            }
            if (!opened.isEmpty()) {
                this.flattenShown();
                this.reportExpansions(opened, true);
            }
            final List<Integer> indexes = new ArrayList<>(paths.size());
            for (final List<T> path : paths) {
                final int index = this.indexOf(this.key(path.getLast()));
                if (index >= 0) {
                    indexes.add(index);
                }
            }
            this.list.selectIndexes(indexes);
        });
        return this;
    }

    @Override
    public TreeView<T> selectAll() {
        this.list.selectAll();
        return this;
    }

    @Override
    public TreeView<T> select(final int index) {
        this.list.select(index);
        return this;
    }

    @Override
    public TreeView<T> selectItem(final T item) {
        if (item == null) {
            this.list.select(-1);
            return this;
        }
        final List<T> path = new ArrayList<>();
        if (!this.findPath(this.roots, this.key(item), path, new HashSet<>())) {
            this.list.select(-1);
            return this;
        }
        this.changeSelection(() -> {
            final List<T> opened = new ArrayList<>();
            for (int i = 0; i < path.size() - 1; i++) {
                if (this.expanded.add(this.key(path.get(i)))) {
                    opened.add(path.get(i));
                }
            }
            if (!opened.isEmpty()) {
                this.flattenShown();
                this.reportExpansions(opened, true);
            }
            this.list.select(this.indexOf(this.key(item)));
        });
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

    /**
     * Asks to expand or collapse an item after a click on its arrow.
     */
    void toggle(final Entry<T> entry) {
        this.userExpand(entry.item, !this.isExpanded(entry.item));
    }

    /**
     * Asks to expand or collapse an item for the user.
     */
    private void userExpand(final T item, final boolean expanded) {
        this.request(EXPAND, new Expansion<>(item, expanded), this::applyExpansion);
    }

    /**
     * Expands or collapses the item a request ended with, as {@link #setExpanded} does. A null value or item is
     * ignored.
     */
    @SuppressWarnings("unchecked")
    private void applyExpansion(final Expansion<?> expansion) {
        if (expansion != null && expansion.item() != null) {
            this.setExpanded((T) expansion.item(), expansion.expanded());
        }
    }

    /**
     * Asks to select a row for the user. The list routes its own selections here, and Left and Right move through
     * the list as Up and Down do.
     */
    private void userSelect(final int index) {
        this.request(SELECT, index, this.list::applySelect);
    }

    /**
     * Asks to activate a row the user activated in the list.
     */
    private void userActivate(final int index) {
        this.request(ACTIVATE, index, this::applyActivate);
    }

    /**
     * Reports the row a request ended with as activated. A null or an index outside the rows is ignored.
     */
    private void applyActivate(final Integer index) {
        if (index != null && index >= 0 && index < this.entries.size()) {
            this.report(ACTIVATED, index, index);
        }
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
                    this.userExpand(entry.item, true);
                } else if (index + 1 < this.entries.size()) {
                    this.list.keyMove(index + 1, event.modifiers().shift());
                }
            }
            case LEFT -> {
                if (entry.parent && this.isExpanded(entry.item)) {
                    this.userExpand(entry.item, false);
                } else {
                    for (int i = index - 1; i >= 0; i--) {
                        if (this.entries.get(i).depth < entry.depth) {
                            this.list.keyMove(i, event.modifiers().shift());
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
    private static final class Row<T> extends BuiltInComposite<Row<T>> {
        private final TreeViewImpl<T> tree;
        private final Entry<T> entry;
        private final Component<?> cell;

        @SuppressWarnings("this-escape")
        Row(final ComponentFactoryImpl ui, final TreeViewImpl<T> tree, final Entry<T> entry) {
            super(ui);
            this.tree = tree;
            this.entry = entry;
            this.cell = tree.cellFactory.apply(entry.item);
            this.addContent(this.cell);
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
