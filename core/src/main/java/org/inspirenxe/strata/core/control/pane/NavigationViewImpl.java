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
package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.core.layout.Flow;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuItem;
import org.inspirenxe.strata.api.control.menu.SubmenuItem;
import org.inspirenxe.strata.api.control.pane.NavigationEntry;
import org.inspirenxe.strata.api.control.pane.NavigationItem;
import org.inspirenxe.strata.api.control.pane.NavigationView;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Badge;
import org.inspirenxe.strata.core.control.Motion;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.node.Root;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The core {@link NavigationView}, a composite of a private pane, a private toggle for the bar of
 * {@link DisplayMode#MINIMAL}, and the page or content it shows, added as content.
 *
 * <p>The pane is a private composite holding its toggle, the pane header and footer as content, a private
 * {@link ScrollPanel} that cannot take focus around a {@link StackPanel} of the items' rows, and a stack of the footer
 * items' rows. Every entry, children included, has a private row component, so each has its own hover and tooltip;
 * rows of hidden children are hidden, not removed. Rows are built for every entry, since navigation panes are short,
 * and rebuilt when the entries change.</p>
 *
 * <p>An item is a line of the item text style plus the item padding's top and bottom tall. Its icon sits in a square
 * one line tall, at the item's icon size in the middle or scaled down to fit, and children are indented by a line and
 * {@link #GAP}, so a child's icon lines up with its parent's text.
 * A separator is a one-unit line with {@link #SEPARATOR_SPACE} above and below. The column of icons is an item's
 * height plus the pane padding's sides wide, and the bar of {@code MINIMAL} an item's height plus the pane padding's
 * top and bottom tall.</p>
 *
 * <p>{@link DisplayMode#AUTO} is worked out from the bounds in each arrange. When the shown mode changes there, the
 * view closes the pane over the content, switches the pane's look, and measures and arranges its children again for
 * the new mode at once, then asks for a layout so its own size follows. The pane over the content is laid out by
 * {@link Root#popupIn}, and its area follows the view's bounds through {@link Root#setPopupArea}. While it is open,
 * the pane is out of the view's children and put back first when it closes, so the tab order stays the view, the pane
 * header and footer, then the content.</p>
 *
 * <p>Over the theme's {@value NavigationView#STYLE_ANIMATION} metric the pane over the content slides in from the
 * view's left edge: a {@link Motion} moves the pane's offset from minus its area's width to 0, and the popup is drawn
 * cut to its area until the pane comes to rest. It closes at once.</p>
 */
public final class NavigationViewImpl extends BuiltInComposite<NavigationView> implements NavigationView {
    /**
     * The item padding when the theme sets no {@value #STYLE_ITEM_PADDING}: 6 at the sides and 5 at the top and
     * bottom.
     */
    public static final Insets DEFAULT_ITEM_PADDING = new Insets(6f, 5f, 6f, 5f);
    /**
     * The pane padding when the theme sets no {@value #STYLE_PANE_PADDING}: 2 on every side.
     */
    public static final Insets DEFAULT_PANE_PADDING = Insets.of(2f);
    /**
     * The space between an item's icon, text, badge and arrow, and between the toggle and the title, in UI units.
     */
    public static final float GAP = 4f;
    /**
     * The space above and below a separator's line, in UI units.
     */
    public static final float SEPARATOR_SPACE = 4f;
    private static final Set<String> REQUIRED = Set.of(STYLE_ITEM_SELECTED);
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DISABLED_ICON_TINT = 0xFF808080;

    private final ComponentFactoryImpl ui;
    private final List<NavigationEntryImpl> entries;
    private final List<NavigationEntryImpl> footerEntries;
    private final Map<NavigationEntryImpl, Row> rows;
    private List<NavigationItemImpl> all;
    private NavigationItemImpl selected;
    private int reportedIndex;
    private NavigationItemImpl cursor;
    private Text title;
    private DisplayMode displayMode;
    private DisplayMode shownMode;
    private float compactBelow;
    private float minimalBelow;
    private float paneWidth;
    private boolean toggleShown;
    private boolean activateOnClick;
    private Component<?> paneHeader;
    private Component<?> paneFooter;
    private Component<?> content;
    private Component<?> shown;
    private boolean paneOpen;
    private Overlay overlay;
    private Menu menu;
    private final Pane pane;
    private long animationMillis;
    private final Motion paneSlide = new Motion(this::slid);
    private final Toggle barToggle;

    // Theme
    private Paint panePaint;
    private Paint paneOverlayPaint;
    private Paint itemHovered;
    private Paint itemSelected;
    private Paint indicator;
    private Paint separatorPaint;
    private Paint togglePaint;
    private Paint toggleHoveredPaint;
    private Paint focusRing;
    private TextStyle itemText;
    private TextStyle itemTextSelected;
    private TextStyle itemTextDisabled;
    private TextStyle headerText;
    private TextStyle titleText;
    private Insets itemPadding;
    private Insets panePadding;

    // Worked out during layout
    private float lineHeight;
    private float rowHeight;
    private TextLine barTitleWhole;
    private TextLine barTitle;
    private Rect barRect;
    private float barTitleX;
    private float barTitleY;

    /**
     * Creates an empty view in {@link DisplayMode#AUTO} that stretches over its slot.
     *
     * @param ui the factory that owns the view
     */
    @SuppressWarnings("this-escape")
    public NavigationViewImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.ui = ui;
        this.entries = new ArrayList<>();
        this.footerEntries = new ArrayList<>();
        this.rows = new IdentityHashMap<>();
        this.reportedIndex = -1;
        this.title = Text.empty();
        this.displayMode = DisplayMode.AUTO;
        this.shownMode = DisplayMode.EXPANDED;
        this.compactBelow = DEFAULT_COMPACT_BELOW;
        this.minimalBelow = DEFAULT_MINIMAL_BELOW;
        this.paneWidth = DEFAULT_PANE_WIDTH;
        this.toggleShown = true;
        this.activateOnClick = true;
        this.panePaint = Paint.NONE;
        this.paneOverlayPaint = Paint.NONE;
        this.itemHovered = Paint.NONE;
        this.itemSelected = Paint.NONE;
        this.indicator = Paint.NONE;
        this.separatorPaint = Paint.NONE;
        this.togglePaint = Paint.NONE;
        this.toggleHoveredPaint = Paint.NONE;
        this.focusRing = Paint.NONE;
        this.itemText = TextStyle.DEFAULT;
        this.itemTextSelected = TextStyle.DEFAULT;
        this.itemTextDisabled = TextStyle.DEFAULT;
        this.headerText = TextStyle.DEFAULT;
        this.titleText = TextStyle.DEFAULT;
        this.itemPadding = DEFAULT_ITEM_PADDING;
        this.panePadding = DEFAULT_PANE_PADDING;
        this.barTitleWhole = TextLine.EMPTY;
        this.barTitle = TextLine.EMPTY;
        this.barRect = Rect.ZERO;
        this.pane = new Pane(ui);
        this.barToggle = new Toggle(ui);
        this.addChild(this.pane);
        this.addChild(this.barToggle);
        this.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        this.syncLook();
    }

    // ------------------------------------------------------------------
    // Entries
    // ------------------------------------------------------------------

    @Override
    public List<NavigationEntry> items() {
        return this.entries.stream().map(NavigationEntryImpl::asEntry).toList();
    }

    @Override
    public NavigationView addItem(final NavigationEntry entry) {
        return this.addItem(this.entries.size(), entry);
    }

    @Override
    public NavigationView addItem(final int index, final NavigationEntry entry) {
        final NavigationEntryImpl impl = NavigationViewImpl.entry(entry);
        Objects.checkIndex(index, this.entries.size() + 1);
        impl.setOwner(this);
        this.entries.add(index, impl);
        this.structureChanged();
        return this;
    }

    @Override
    public NavigationView removeItem(final NavigationEntry entry) {
        if (entry instanceof final NavigationEntryImpl impl && this.entries.remove(impl)) {
            impl.setOwner(null);
            this.entryRemoved(impl);
        }
        return this;
    }

    @Override
    public NavigationView clearItems() {
        for (final NavigationEntryImpl entry : List.copyOf(this.entries)) {
            this.removeItem(entry.asEntry());
        }
        return this;
    }

    @Override
    public NavigationView addItem(final Text text, final Function<ComponentFactory, ? extends Component<?>> page) {
        return this.addItem(new NavigationItemImpl(text).setPage(page));
    }

    @Override
    public NavigationView addHeader(final Text text) {
        return this.addItem(new NavigationHeaderImpl(text));
    }

    @Override
    public NavigationView addSeparator() {
        return this.addItem(new NavigationSeparatorImpl());
    }

    @Override
    public List<NavigationEntry> footerItems() {
        return this.footerEntries.stream().map(NavigationEntryImpl::asEntry).toList();
    }

    @Override
    public NavigationView addFooterItem(final NavigationEntry entry) {
        return this.addFooterItem(this.footerEntries.size(), entry);
    }

    @Override
    public NavigationView addFooterItem(final int index, final NavigationEntry entry) {
        final NavigationEntryImpl impl = NavigationViewImpl.entry(entry);
        Objects.checkIndex(index, this.footerEntries.size() + 1);
        impl.setOwner(this);
        this.footerEntries.add(index, impl);
        this.structureChanged();
        return this;
    }

    @Override
    public NavigationView removeFooterItem(final NavigationEntry entry) {
        if (entry instanceof final NavigationEntryImpl impl && this.footerEntries.remove(impl)) {
            impl.setOwner(null);
            this.entryRemoved(impl);
        }
        return this;
    }

    @Override
    public NavigationView clearFooterItems() {
        for (final NavigationEntryImpl entry : List.copyOf(this.footerEntries)) {
            this.removeFooterItem(entry.asEntry());
        }
        return this;
    }

    @Override
    public List<NavigationItem> allItems() {
        return List.copyOf(this.all());
    }

    private static NavigationEntryImpl entry(final NavigationEntry entry) {
        if (!(entry instanceof final NavigationEntryImpl impl)) {
            throw new IllegalArgumentException(entry + " is not a Strata navigation entry");
        }
        return impl;
    }

    /**
     * Returns every item, depth first, then the footer's, worked out again after the entries changed.
     */
    private List<NavigationItemImpl> all() {
        if (this.all == null) {
            final List<NavigationItemImpl> items = new ArrayList<>();
            NavigationViewImpl.collect(this.entries, items);
            NavigationViewImpl.collect(this.footerEntries, items);
            this.all = Collections.unmodifiableList(items);
        }
        return this.all;
    }

    private static void collect(final List<? extends NavigationEntryImpl> entries, final List<NavigationItemImpl> out) {
        for (final NavigationEntryImpl entry : entries) {
            if (entry instanceof final NavigationItemImpl item) {
                out.add(item);
                NavigationViewImpl.collect(item.children(), out);
            }
        }
    }

    private int indexOf(final NavigationItemImpl item) {
        return item == null ? -1 : this.all().indexOf(item);
    }

    private NavigationItemImpl itemAt(final Integer index) {
        return index == null || index < 0 || index >= this.all().size() ? null : this.all().get(index);
    }

    /**
     * Called after entries were added or removed anywhere in the view: rebuilds the rows, and reports a selection that
     * left the view or whose index moved.
     */
    void structureChanged() {
        this.all = null;
        this.closeMenu();
        this.rebuildRows();
        if (this.selected != null && this.selected.view() != this) {
            this.selected = null;
            this.showPage();
        }
        final int index = this.selectedIndex();
        if (index != this.reportedIndex) {
            final int previous = this.reportedIndex;
            this.reportedIndex = index;
            this.report(SELECTED, index, previous);
        }
        if (this.cursor != null && this.cursor.view() != this) {
            this.cursor = null;
        }
        this.invalidateLayout();
    }

    /**
     * Called after an entry left the view: discards the pages kept for it and its children, then updates the rest.
     */
    void entryRemoved(final NavigationEntryImpl entry) {
        if (entry instanceof final NavigationItemImpl item) {
            this.discardPages(item);
        }
        this.structureChanged();
    }

    private void discardPages(final NavigationItemImpl item) {
        if (item.keptPage() != null && item.keptPage() == this.shown) {
            this.removeChild(this.shown);
            this.shown = null;
        }
        item.setKeptPage(null);
        for (final NavigationItemImpl child : item.children()) {
            this.discardPages(child);
        }
    }

    /**
     * Called after an entry changed how it looks.
     */
    void entryChanged(final NavigationEntryImpl entry) {
        final Row row = this.rows.get(entry);
        if (row != null) {
            row.sync();
        }
    }

    /**
     * Called after an item of the view was expanded or collapsed, by the user or from code.
     */
    void itemExpanded(final NavigationItemImpl item, final boolean expanded) {
        this.updateRows();
        if (this.cursor != null && !this.isShowing(this.cursor)) {
            this.cursor = item;
        }
        this.invalidateLayout();
        this.report(ITEM_EXPANDED, new Expansion(item, expanded), new Expansion(item, !expanded));
    }

    /**
     * Called after an item of the view was given a new page: discards the one kept, and builds the new one at once
     * if the item is selected.
     */
    void pageChanged(final NavigationItemImpl item) {
        final Component<?> old = item.keptPage();
        item.setKeptPage(null);
        if (old != null && old == this.shown) {
            this.removeChild(old);
            this.shown = null;
        }
        if (item == this.selected) {
            this.showPage();
        }
    }

    // ------------------------------------------------------------------
    // Rows
    // ------------------------------------------------------------------

    private void rebuildRows() {
        final Map<NavigationEntryImpl, Row> kept = new IdentityHashMap<>(this.rows);
        this.rows.clear();
        this.pane.items.clear();
        this.pane.footer.clear();
        this.addRows(this.pane.items, this.entries, 0, kept);
        this.addRows(this.pane.footer, this.footerEntries, 0, kept);
        this.updateRows();
    }

    private void addRows(final StackPanel stack, final List<? extends NavigationEntryImpl> list, final int depth,
                         final Map<NavigationEntryImpl, Row> kept) {
        for (final NavigationEntryImpl entry : list) {
            Row row = kept.get(entry);
            if (row == null) {
                row = new Row(this.ui, entry);
            }
            row.depth = depth;
            this.rows.put(entry, row);
            stack.add(row);
            if (entry instanceof final NavigationItemImpl item) {
                this.addRows(stack, item.children(), depth + 1, kept);
            }
        }
    }

    /**
     * Shows and hides rows for the look and the items' expansion, and tells each to sync.
     */
    private void updateRows() {
        final boolean compact = this.compactLook();
        for (final Row row : this.rows.values()) {
            final boolean visible;
            if (row.entry instanceof NavigationHeaderImpl) {
                visible = !compact;
            } else if (row.entry instanceof final NavigationItemImpl item && row.depth > 0) {
                visible = !compact && NavigationViewImpl.ancestorsExpanded(item);
            } else {
                visible = true;
            }
            row.setVisible(visible);
            row.sync();
        }
    }

    private static boolean ancestorsExpanded(final NavigationItemImpl item) {
        for (Object at = item.owner(); at instanceof final NavigationItemImpl parent; at = parent.owner()) {
            if (!parent.isExpanded()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether an item's row shows in a pane that shows.
     */
    private boolean isShowing(final NavigationItemImpl item) {
        final Row row = this.rows.get(item);
        return row != null && row.isVisible() && (this.paneOpen || this.shownMode != DisplayMode.MINIMAL);
    }

    /**
     * Returns the item drawn as selected: the selected item if its row shows, else its nearest ancestor whose row
     * shows.
     */
    private NavigationItemImpl shownSelection() {
        Object at = this.selected;
        while (at instanceof final NavigationItemImpl item) {
            final Row row = this.rows.get(item);
            if (row != null && row.isVisible()) {
                return item;
            }
            at = item.owner();
        }
        return null;
    }

    /**
     * Returns the items the keyboard cursor can stop on, in the order the pane shows them.
     */
    private List<NavigationItemImpl> cursorStops() {
        final List<NavigationItemImpl> stops = new ArrayList<>();
        for (final NavigationItemImpl item : this.all()) {
            if (item.isEnabled() && this.isShowing(item)) {
                stops.add(item);
            }
        }
        return stops;
    }

    private boolean compactLook() {
        return this.shownMode == DisplayMode.COMPACT && !this.paneOpen;
    }

    /**
     * Shows, hides and fills the parts for the shown mode and whether the pane is open over the content.
     */
    private void syncLook() {
        this.pane.setVisible(this.paneOpen || this.shownMode != DisplayMode.MINIMAL);
        this.pane.toggle.setVisible(this.toggleShown && (this.paneOpen || this.shownMode == DisplayMode.COMPACT));
        this.barToggle.setVisible(this.toggleShown && this.shownMode == DisplayMode.MINIMAL);
        this.pane.syncExtras();
        this.updateRows();
        this.pane.relayout();
        this.invalidateLayout();
    }

    // ------------------------------------------------------------------
    // Selection and activation
    // ------------------------------------------------------------------

    @Override
    public int selectedIndex() {
        return this.indexOf(this.selected);
    }

    @Override
    public Optional<NavigationItem> selected() {
        return Optional.ofNullable(this.selected);
    }

    @Override
    public NavigationView select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.all().size());
        }
        this.applySelection(index == -1 ? null : this.all().get(index));
        return this;
    }

    @Override
    public NavigationView selectItem(final NavigationItem item) {
        return this.select(item instanceof final NavigationItemImpl impl ? this.indexOf(impl) : -1);
    }

    @Override
    public NavigationView setActivateOnClick(final boolean onClick) {
        this.activateOnClick = onClick;
        return this;
    }

    private void applySelection(final NavigationItemImpl item) {
        if (item == this.selected) {
            return;
        }
        // The page is built first, so a page factory that throws leaves the selection as it was
        this.showPage(item);
        this.selected = item;
        final int index = this.selectedIndex();
        final int previous = this.reportedIndex;
        this.reportedIndex = index;
        this.report(SELECTED, index, previous);
    }

    /**
     * Shows the selected item's page, building it the first time, or else the content.
     */
    private void showPage() {
        this.showPage(this.selected);
    }

    /**
     * Shows an item's page, building it the first time, or else the content. A page factory that throws, or returns
     * a component already in a tree, changes nothing and is asked again next time.
     */
    private void showPage(final NavigationItemImpl item) {
        Component<?> next = this.content;
        if (item != null && item.page() != null) {
            if (item.keptPage() == null) {
                final Component<?> page = Objects.requireNonNull(item.page().apply(this.ui), "page");
                this.checkFree(page);
                item.setKeptPage(page);
            }
            next = item.keptPage();
        }
        if (next != this.shown) {
            if (this.shown != null) {
                this.removeChild(this.shown);
            }
            this.shown = next;
            if (next != null) {
                this.addContent(next);
            }
            this.invalidateLayout();
        }
    }

    private void checkFree(final Component<?> component) {
        final Node node = this.nodeOf(component);
        if (node.parentNode() != null || node.root() != null) {
            throw new IllegalArgumentException(component + " is already part of a tree");
        }
    }

    /**
     * Picks an item for the user, as a click or Enter does: asks {@code ACTIVATE}, then selects, expands or opens
     * the menu of children, reports {@code ACTIVATED}, and closes the pane over the content after a pick.
     */
    private void pick(final NavigationItemImpl item) {
        if (!item.isEnabled() || !this.isEnabled()) {
            return;
        }
        this.request(ACTIVATE, this.indexOf(item), index -> {
            final NavigationItemImpl target = this.itemAt(index);
            if (target == null || !target.isEnabled()) {
                return;
            }
            final Outcome outcome = this.choose(target);
            final int at = this.indexOf(target);
            this.report(ACTIVATED, at, at);
            this.finish(target, outcome);
        });
    }

    /**
     * Selects, expands or opens the menu of children for a single click while activation needs a double click.
     */
    private void pickWithoutActivating(final NavigationItemImpl item) {
        if (item.isEnabled() && this.isEnabled()) {
            this.finish(item, this.choose(item));
        }
    }

    /**
     * Activates an item for a double click while activation needs one, without selecting or expanding again.
     */
    private void activateOnly(final NavigationItemImpl item) {
        if (!item.isEnabled() || !this.isEnabled()) {
            return;
        }
        this.request(ACTIVATE, this.indexOf(item), index -> {
            final NavigationItemImpl target = this.itemAt(index);
            if (target != null && target.isEnabled()) {
                final int at = this.indexOf(target);
                this.report(ACTIVATED, at, at);
            }
        });
    }

    /**
     * What a pick of an item does besides activating it.
     */
    private enum Outcome {
        /** A selection or an action: the pane over the content closes. */
        PICKED,
        /** The item expanded or collapsed. */
        EXPANDED,
        /** The item's children open in a menu. */
        MENU
    }

    private Outcome choose(final NavigationItemImpl item) {
        if (this.compactLook() && !item.children().isEmpty()) {
            if (item.isSelectable() && item != this.selected) {
                this.userSelect(item);
            }
            return Outcome.MENU;
        }
        if (item.isSelectable()) {
            if (item != this.selected) {
                this.userSelect(item);
            }
            return Outcome.PICKED;
        }
        if (!item.children().isEmpty()) {
            this.userExpand(item, !item.isExpanded());
            return Outcome.EXPANDED;
        }
        return Outcome.PICKED;
    }

    private void finish(final NavigationItemImpl item, final Outcome outcome) {
        if (outcome == Outcome.MENU) {
            this.openMenu(item);
        } else if (outcome == Outcome.PICKED && this.paneOpen) {
            this.setExpanded(false);
        }
    }

    private void userSelect(final NavigationItemImpl item) {
        this.request(SELECT, this.indexOf(item), index -> {
            if (index != null && index == -1) {
                this.applySelection(null);
                return;
            }
            final NavigationItemImpl target = this.itemAt(index);
            if (target != null && target.isEnabled()) {
                this.applySelection(target);
            }
        });
    }

    private void userExpand(final NavigationItemImpl item, final boolean expanded) {
        this.request(EXPAND_ITEM, new Expansion(item, expanded), expansion -> {
            if (expansion != null && expansion.item() instanceof final NavigationItemImpl target
                    && target.view() == this) {
                target.setExpanded(expansion.expanded());
            }
        });
    }

    // ------------------------------------------------------------------
    // The menu of children in the column of icons
    // ------------------------------------------------------------------

    private void openMenu(final NavigationItemImpl item) {
        this.closeMenu();
        final Row row = this.rows.get(item);
        if (row == null || item.children().isEmpty() || this.hostRoot() == null) {
            return;
        }
        final Menu built = this.ui.menu();
        this.fill(built, item);
        this.menu = built;
        // Beside the column, level with the item
        built.openAt(this.isRightToLeft() ? this.pane.bounds().x() : this.pane.bounds().right(), row.bounds().y());
    }

    private void fill(final Menu target, final NavigationItemImpl parent) {
        for (final NavigationItemImpl child : parent.children()) {
            if (child.children().isEmpty()) {
                final MenuItem entry = this.ui.menuItem(child.text())
                        .setAction(() -> this.menuPicked(child))
                        .setEnabled(child.isEnabled());
                target.addItem(entry);
            } else {
                final Menu inner = this.ui.menu();
                this.fill(inner, child);
                final SubmenuItem entry = this.ui.submenuItem(child.text()).setMenu(inner).setEnabled(child.isEnabled());
                target.addItem(entry);
            }
        }
    }

    private void menuPicked(final NavigationItemImpl child) {
        if (child.view() == this) {
            this.pick(child);
        }
    }

    private void closeMenu() {
        if (this.menu != null) {
            final Menu open = this.menu;
            this.menu = null;
            open.close();
        }
    }

    // ------------------------------------------------------------------
    // Display modes and the pane over the content
    // ------------------------------------------------------------------

    @Override
    public DisplayMode displayMode() {
        return this.displayMode;
    }

    @Override
    public NavigationView setDisplayMode(final DisplayMode mode) {
        this.displayMode = Objects.requireNonNull(mode, "mode");
        if (mode != DisplayMode.AUTO && mode != this.shownMode) {
            this.applyShownMode(mode);
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public DisplayMode shownMode() {
        return this.shownMode;
    }

    @Override
    public float compactBelow() {
        return this.compactBelow;
    }

    @Override
    public float minimalBelow() {
        return this.minimalBelow;
    }

    @Override
    public NavigationView setAutoWidths(final float compactBelow, final float minimalBelow) {
        if (!(compactBelow >= 0f) || !(minimalBelow >= 0f) || Float.isInfinite(compactBelow)
                || Float.isInfinite(minimalBelow) || minimalBelow > compactBelow) {
            throw new IllegalArgumentException("Auto widths must be finite, not negative, and the minimal one no "
                    + "greater than the compact one: " + compactBelow + ", " + minimalBelow);
        }
        this.compactBelow = compactBelow;
        this.minimalBelow = minimalBelow;
        this.invalidateLayout();
        return this;
    }

    private DisplayMode autoMode(final float width) {
        if (width >= this.compactBelow) {
            return DisplayMode.EXPANDED;
        }
        return width >= this.minimalBelow ? DisplayMode.COMPACT : DisplayMode.MINIMAL;
    }

    /**
     * Switches to another shown mode: closes the pane over the content and the menu, and changes the pane's look.
     */
    private void applyShownMode(final DisplayMode mode) {
        this.closeMenu();
        this.setExpanded(false);
        this.shownMode = mode;
        this.syncLook();
    }

    @Override
    public boolean isExpanded() {
        return this.paneOpen;
    }

    @Override
    public NavigationView setExpanded(final boolean expanded) {
        if (expanded) {
            this.openPane();
        } else if (this.overlay != null) {
            this.overlay.close();
        }
        return this;
    }

    private boolean canOpenPane() {
        return !this.paneOpen && this.shownMode != DisplayMode.EXPANDED && this.isEnabled() && this.isVisible()
                && this.isAttached() && this.hostRoot() != null;
    }

    private void openPane() {
        if (!this.canOpenPane()) {
            return;
        }
        this.closeMenu();
        final Root root = this.hostRoot();
        this.removeChild(this.pane);
        this.paneOpen = true;
        this.syncLook();
        final Rect area = this.overlayArea();
        this.overlay = root.popupIn(this.pane, area, true);
        this.overlay.onEvent(Overlay.CLOSED, event -> this.paneClosed());
        if (this.animationMillis > 0L && area.width() > 0f) {
            root.setPopupClipped(this.overlay, true);
            this.paneSlide.move(this.motionAnimator(), this.animationMillis,
                    this.isRightToLeft() ? area.width() : -area.width(), 0f);
        }
        // Lay the pane out now, so the cursor and clicks find its rows before the next frame
        root.layout();
        this.report(EXPANDED, true, false);
    }

    /**
     * Runs once the pane over the content closed, however it closed: puts the pane back first among the children.
     */
    private void paneClosed() {
        this.paneSlide.stop();
        this.pane.slideTo(0f);
        this.overlay = null;
        this.paneOpen = false;
        this.insertChild(0, this.pane);
        this.syncLook();
        this.report(EXPANDED, false, true);
    }

    /**
     * Applies one step of the pane's slide, and stops cutting the popup once the pane comes to rest.
     */
    private void slid(final float offset) {
        this.pane.slideTo(offset);
        final Overlay open = this.overlay;
        final Root root = this.hostRoot();
        if (offset == 0f && !this.paneSlide.isMoving() && open != null && open.isOpen() && root != null) {
            root.setPopupClipped(open, false);
        }
    }

    private Rect overlayArea() {
        final Rect bounds = this.bounds();
        // Right to left the pane opens over the content from the right
        return this.flow(new Rect(bounds.x(), bounds.y(), Math.min(this.paneWidth, bounds.width()), bounds.height()));
    }

    /**
     * Opens or closes the pane over the content for the user through {@code EXPAND}.
     */
    void userToggle() {
        final boolean open = !this.paneOpen;
        if (!this.isEnabled() || open && !this.canOpenPane()) {
            return;
        }
        this.request(EXPAND, open, value -> {
            if (value != null) {
                this.setExpanded(value);
            }
        });
    }

    @Override
    public float paneWidth() {
        return this.paneWidth;
    }

    @Override
    public NavigationView setPaneWidth(final float width) {
        if (!(width >= 0f) || Float.isInfinite(width)) {
            throw new IllegalArgumentException("Pane width must be finite and not negative: " + width);
        }
        this.paneWidth = width;
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isToggleShown() {
        return this.toggleShown;
    }

    @Override
    public NavigationView setToggleShown(final boolean shown) {
        this.toggleShown = shown;
        this.syncLook();
        return this;
    }

    // ------------------------------------------------------------------
    // Title, pane header and footer, content
    // ------------------------------------------------------------------

    @Override
    public Text title() {
        return this.title;
    }

    @Override
    public NavigationView setTitle(final Text title) {
        this.title = Objects.requireNonNull(title, "title");
        this.pane.relayout();
        this.invalidateLayout();
        return this;
    }

    @Override
    public Optional<Component<?>> paneHeader() {
        return Optional.ofNullable(this.paneHeader);
    }

    @Override
    public NavigationView setPaneHeader(final Component<?> header) {
        if (header != this.paneHeader) {
            if (header != null) {
                this.checkFree(header);
            }
            this.paneHeader = header;
            this.pane.syncExtras();
        }
        return this;
    }

    @Override
    public Optional<Component<?>> paneFooter() {
        return Optional.ofNullable(this.paneFooter);
    }

    @Override
    public NavigationView setPaneFooter(final Component<?> footer) {
        if (footer != this.paneFooter) {
            if (footer != null) {
                this.checkFree(footer);
            }
            this.paneFooter = footer;
            this.pane.syncExtras();
        }
        return this;
    }

    @Override
    public Optional<Component<?>> content() {
        return Optional.ofNullable(this.content);
    }

    @Override
    public NavigationView setContent(final Component<?> content) {
        if (content != this.content) {
            if (content != null) {
                this.checkFree(content);
            }
            this.content = content;
            this.showPage();
        }
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
        this.panePaint = theme.paintOr(STYLE_PANE, Paint.NONE);
        this.paneOverlayPaint = theme.paintOr(STYLE_PANE_OVERLAY,
                theme.paint(STYLE_PANE).orElseGet(() -> theme.paintOr(ComboBox.STYLE_POPUP, Paint.NONE)));
        this.itemHovered = theme.paintOr(STYLE_ITEM_HOVERED, theme.paintOr(ListView.STYLE_ROW_HOVERED, Paint.NONE));
        this.itemSelected = theme.paintOr(STYLE_ITEM_SELECTED, Paint.NONE);
        this.indicator = theme.paintOr(STYLE_INDICATOR, Paint.NONE);
        this.separatorPaint = theme.paintOr(STYLE_SEPARATOR, theme.paintOr(Separator.STYLE_LINE, Paint.NONE));
        this.togglePaint = theme.paintOr(STYLE_TOGGLE, Paint.NONE);
        this.toggleHoveredPaint = theme.paintOr(STYLE_TOGGLE_HOVERED, this.itemHovered);
        this.focusRing = theme.paintOr(STYLE_FOCUS, theme.paintOr(Button.STYLE_FOCUS, Paint.NONE));
        this.itemText = theme.textStyleOr(STYLE_ITEM_TEXT, TextStyle.DEFAULT);
        this.itemTextSelected = theme.textStyleOr(STYLE_ITEM_TEXT_SELECTED, this.itemText);
        this.itemTextDisabled = theme.textStyleOr(STYLE_ITEM_TEXT_DISABLED,
                theme.textStyleOr(TextBox.STYLE_TEXT_DISABLED, this.itemText));
        this.headerText = theme.textStyleOr(STYLE_HEADER_TEXT, this.itemTextDisabled);
        this.titleText = theme.textStyleOr(STYLE_TITLE_TEXT, TextStyle.DEFAULT);
        this.itemPadding = theme.insetsOr(STYLE_ITEM_PADDING, DEFAULT_ITEM_PADDING);
        this.panePadding = theme.insetsOr(STYLE_PANE_PADDING, DEFAULT_PANE_PADDING);
        this.animationMillis = Motion.millis(theme, STYLE_ANIMATION);
    }

    /**
     * Works out the line and item heights from the item text style.
     */
    private void measureLines(final TextMetrics metrics) {
        this.lineHeight = metrics.lineHeight(this.itemText);
        this.rowHeight = this.lineHeight + this.itemPadding.vertical();
    }

    private float compactWidth() {
        return this.rowHeight + this.panePadding.horizontal();
    }

    private float barHeight() {
        return this.rowHeight + this.panePadding.vertical();
    }

    private float paneSide() {
        return switch (this.shownMode) {
            case EXPANDED, AUTO -> this.paneWidth;
            case COMPACT -> this.compactWidth();
            case MINIMAL -> 0f;
        };
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.measureLines(metrics);
        final boolean minimal = this.shownMode == DisplayMode.MINIMAL;
        final float side = this.paneSide();
        final float top = minimal ? this.barHeight() : 0f;
        Size paneSize = Size.ZERO;
        if (!this.paneOpen) {
            paneSize = context.measure(this.pane, side, availableHeight);
        }
        context.measure(this.barToggle, this.rowHeight, this.rowHeight);
        this.barTitleWhole = minimal ? TextLine.of(this.title, this.titleText, metrics) : TextLine.EMPTY;
        Size contentSize = Size.ZERO;
        if (this.shown != null) {
            contentSize = context.measure(this.shown, Math.max(0f, availableWidth - side),
                    Math.max(0f, availableHeight - top));
        }
        if (minimal) {
            final float bar = this.panePadding.horizontal() + (this.toggleShown ? this.rowHeight + GAP : 0f)
                    + this.barTitleWhole.width();
            return new Size(Math.max(bar, contentSize.width()), top + contentSize.height());
        }
        return new Size(side + contentSize.width(), Math.max(paneSize.height(), contentSize.height()));
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        if (this.displayMode == DisplayMode.AUTO) {
            final DisplayMode mode = this.autoMode(bounds.width());
            if (mode != this.shownMode) {
                this.applyShownMode(mode);
                this.barTitleWhole = this.shownMode == DisplayMode.MINIMAL
                        ? TextLine.of(this.title, this.titleText, context.text()) : TextLine.EMPTY;
            }
        }
        final boolean minimal = this.shownMode == DisplayMode.MINIMAL;
        final float side = Math.min(this.paneSide(), bounds.width());
        final float top = minimal ? Math.min(this.barHeight(), bounds.height()) : 0f;
        if (!this.paneOpen) {
            final Rect paneRect = new Rect(bounds.x(), bounds.y(), side, bounds.height());
            context.measure(this.pane, paneRect.width(), paneRect.height());
            context.arrange(this.pane, paneRect);
        }
        final Insets padding = this.panePadding;
        context.measure(this.barToggle, this.rowHeight, this.rowHeight);
        context.arrange(this.barToggle, new Rect(bounds.x() + padding.left(), bounds.y() + padding.top(),
                this.rowHeight, this.rowHeight));
        this.barRect = new Rect(bounds.x(), bounds.y(), bounds.width(), top);
        final float titleLeft = bounds.x() + padding.left() + (this.toggleShown ? this.rowHeight + GAP
                : this.itemPadding.left());
        this.barTitle = this.barTitleWhole.ellipsize(Math.max(0f, bounds.right() - padding.right() - titleLeft),
                this.titleText, context.text());
        this.barTitleX = titleLeft;
        this.barTitleY = bounds.y() + padding.top() + (this.rowHeight - this.barTitle.height()) * 0.5f;
        if (this.shown != null) {
            final Rect area = minimal
                    ? new Rect(bounds.x(), bounds.y() + top, bounds.width(), Math.max(0f, bounds.height() - top))
                    : new Rect(bounds.x() + side, bounds.y(), Math.max(0f, bounds.width() - side), bounds.height());
            context.measure(this.shown, area.width(), area.height());
            context.arrange(this.shown, area);
        }
        if (this.overlay != null && this.hostRoot() != null) {
            this.hostRoot().setPopupArea(this.overlay, this.overlayArea());
        }
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.shownMode == DisplayMode.MINIMAL) {
            this.panePaint.paint(painter, this.barRect);
            this.barTitle.draw(painter, this.isRightToLeft() ? Flow.mirrorX(this.barTitleX, bounds)
                    - this.barTitle.width() : this.barTitleX, this.barTitleY);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onDisabled() {
        this.closeMenu();
        this.setExpanded(false);
    }

    @Override
    protected void onFocusGained(final FocusEvent event) {
        final List<NavigationItemImpl> stops = this.cursorStops();
        if (!stops.contains(this.cursor)) {
            final NavigationItemImpl shownSelection = this.shownSelection();
            this.cursor = stops.contains(shownSelection) ? shownSelection : stops.isEmpty() ? null : stops.getFirst();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        // Keys bubble up from the pane header, the footer and the page; only the view's own keys move the cursor
        if (event.target() != this || !event.action().isDown()) {
            return;
        }
        // Right to left, Left expands and Right collapses
        final Key key = Flow.key(event.key(), this.isRightToLeft());
        final boolean press = event.action() == KeyAction.PRESS;
        switch (key) {
            case UP, DOWN, HOME, END -> {
                this.moveCursor(key);
                event.consume();
            }
            case RIGHT -> {
                if (this.cursor != null && !this.cursor.children().isEmpty() && this.isShowing(this.cursor)) {
                    if (this.compactLook()) {
                        this.openMenu(this.cursor);
                    } else if (!this.cursor.isExpanded()) {
                        this.userExpand(this.cursor, true);
                    }
                    event.consume();
                }
            }
            case LEFT -> {
                if (this.cursor != null && this.isShowing(this.cursor) && !this.compactLook()) {
                    if (!this.cursor.children().isEmpty() && this.cursor.isExpanded()) {
                        this.userExpand(this.cursor, false);
                    } else if (this.cursor.owner() instanceof final NavigationItemImpl parent && this.isShowing(parent)) {
                        this.cursor = parent;
                        this.scrollTo(parent);
                    }
                    event.consume();
                }
            }
            default -> {
                if (press && (key.isEnter() || key == Key.SPACE)) {
                    if (this.cursor != null && this.cursorStops().contains(this.cursor)) {
                        this.pick(this.cursor);
                    } else if (this.shownMode == DisplayMode.MINIMAL && !this.paneOpen) {
                        this.userToggle();
                    }
                    event.consume();
                }
            }
        }
    }

    private void moveCursor(final Key key) {
        final List<NavigationItemImpl> stops = this.cursorStops();
        if (stops.isEmpty()) {
            return;
        }
        final int at = stops.indexOf(this.cursor);
        final NavigationItemImpl next = switch (key) {
            case HOME -> stops.getFirst();
            case END -> stops.getLast();
            case UP -> at < 0 ? stops.getLast() : stops.get(Math.max(0, at - 1));
            default -> at < 0 ? stops.getFirst() : stops.get(Math.min(stops.size() - 1, at + 1));
        };
        this.cursor = next;
        this.scrollTo(next);
    }

    private void scrollTo(final NavigationItemImpl item) {
        final Row row = this.rows.get(item);
        if (row != null && row.parent().orElse(null) == this.pane.items) {
            this.pane.scroll.scrollToVisible(row);
        }
    }

    /**
     * Called when a row of the pane over the content is pressed: the press found nothing focusable there, so the
     * view takes focus back, as a press on a row beside the content gives it.
     */
    private void refocus() {
        if (this.isEnabled()) {
            this.requestFocus();
        }
    }

    private void rowClicked(final Row row, final PointerButtonEvent event) {
        if (!(row.entry instanceof final NavigationItemImpl item) || !item.isEnabled() || !this.isEnabled()) {
            return;
        }
        event.consume();
        // The keyboard ring follows the pointer, so Tab or an arrow key carries on from the clicked item
        this.cursor = item;
        final float arrowZone = row.bounds().right() - this.itemPadding.right() - Rings.ARROW_LONG - GAP;
        // Right to left the arrow is at the left of the row
        final float pressed = this.isRightToLeft() ? Flow.mirrorX(event.x(), row.bounds()) : event.x();
        if (!this.compactLook() && !item.children().isEmpty() && pressed >= arrowZone) {
            this.userExpand(item, !item.isExpanded());
        } else if (this.activateOnClick) {
            this.pick(item);
        } else if (event.clicks() >= 2) {
            this.activateOnly(item);
        } else {
            this.pickWithoutActivating(item);
        }
    }

    @Override
    protected Object snapshotState() {
        return new NavigationView.State(this.selectedIndex(), this.shownMode, this.paneOpen);
    }

    /**
     * Returns the item the keyboard cursor is on, for core's tests.
     *
     * @return the item, or {@code null} for none
     */
    public NavigationItem cursorItem() {
        return this.cursor;
    }

    // ------------------------------------------------------------------
    // Parts
    // ------------------------------------------------------------------

    /**
     * The pane: the toggle and the title, the pane header, the items in a scroll panel, the footer items and the
     * pane footer. It fills the slot or the area of the popup it is laid out in.
     */
    private final class Pane extends BuiltInComposite<Pane> {
        /**
         * Moves the pane sideways from where it is laid out, for its slide.
         */
        void slideTo(final float offset) {
            this.setOffset(offset, 0f);
        }

        private final Toggle toggle;
        private final ScrollPanel scroll;
        private final StackPanel items;
        private final StackPanel footer;
        private Component<?> headerShown;
        private Component<?> footerShown;
        private TextLine titleWhole;
        private TextLine titleLine;
        private float topHeight;
        private float titleX;
        private float titleY;

        @SuppressWarnings("this-escape")
        Pane(final ComponentFactoryImpl ui) {
            super(ui);
            this.toggle = new Toggle(ui);
            this.items = ui.stackPanel();
            this.items.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            this.scroll = ui.scrollPanel().add(this.items);
            this.scroll.setFocusable(false);
            this.scroll.setAnchor(Anchor.FILL);
            this.footer = ui.stackPanel();
            this.footer.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            this.titleWhole = TextLine.EMPTY;
            this.titleLine = TextLine.EMPTY;
            this.addChild(this.toggle);
            this.addChild(this.scroll);
            this.addChild(this.footer);
            this.setAnchor(Anchor.FILL);
        }

        /**
         * Lays the pane out again before the next frame.
         */
        void relayout() {
            this.invalidateLayout();
        }

        /**
         * Puts the pane header and footer in the tree, or takes them out in the column of icons.
         */
        void syncExtras() {
            final boolean show = !NavigationViewImpl.this.compactLook();
            final Component<?> header = show ? NavigationViewImpl.this.paneHeader : null;
            if (header != this.headerShown) {
                if (this.headerShown != null) {
                    this.removeChild(this.headerShown);
                }
                this.headerShown = header;
                if (header != null) {
                    this.insertContent(1, header);
                }
            }
            final Component<?> foot = show ? NavigationViewImpl.this.paneFooter : null;
            if (foot != this.footerShown) {
                if (this.footerShown != null) {
                    this.removeChild(this.footerShown);
                }
                this.footerShown = foot;
                if (foot != null) {
                    this.addContent(foot);
                }
            }
            this.invalidateLayout();
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final NavigationViewImpl view = NavigationViewImpl.this;
            view.measureLines(context.text());
            final Insets padding = view.panePadding;
            final float inner = Math.max(0f, availableWidth - padding.horizontal());
            final boolean full = !view.compactLook();
            this.titleWhole = full ? TextLine.of(view.title, view.titleText, context.text()) : TextLine.EMPTY;
            this.topHeight = this.toggle.isVisible() || !this.titleWhole.isEmpty() ? view.rowHeight : 0f;
            context.measure(this.toggle, view.rowHeight, view.rowHeight);
            float height = padding.vertical() + this.topHeight;
            float width = 0f;
            for (final Component<?> part : new Component<?>[] {this.headerShown, this.scroll, this.footer,
                    this.footerShown}) {
                if (part != null) {
                    final Size size = context.measure(part, inner, Float.POSITIVE_INFINITY);
                    height += size.height();
                    width = Math.max(width, size.width());
                }
            }
            return new Size(Float.isInfinite(availableWidth) ? width + padding.horizontal() : availableWidth, height);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            final NavigationViewImpl view = NavigationViewImpl.this;
            final Insets padding = view.panePadding;
            final float x = bounds.x() + padding.left();
            final float width = Math.max(0f, bounds.width() - padding.horizontal());
            final float bottom = bounds.bottom() - padding.bottom();
            float y = bounds.y() + padding.top();
            context.arrange(this.toggle, new Rect(x, y, view.rowHeight, view.rowHeight));
            this.titleX = this.toggle.isVisible() ? x + view.rowHeight + GAP : x + view.itemPadding.left();
            this.titleLine = this.titleWhole.ellipsize(Math.max(0f, bounds.right() - padding.right() - this.titleX),
                    view.titleText, context.text());
            this.titleY = y + (this.topHeight - this.titleLine.height()) * 0.5f;
            y += this.topHeight;
            if (this.headerShown != null) {
                final float height = context.outerSize(this.headerShown).height();
                context.arrange(this.headerShown, new Rect(x, y, width, height));
                y += height;
            }
            final float footerHeight = this.footerShown == null ? 0f : context.outerSize(this.footerShown).height();
            final float footerItems = context.outerSize(this.footer).height();
            final float scrollHeight = Math.max(0f, bottom - y - footerItems - footerHeight);
            context.measure(this.scroll, width, scrollHeight);
            context.arrange(this.scroll, new Rect(x, y, width, scrollHeight));
            y += scrollHeight;
            context.arrange(this.footer, new Rect(x, y, width, footerItems));
            y += footerItems;
            if (this.footerShown != null) {
                context.arrange(this.footerShown, new Rect(x, y, width, footerHeight));
            }
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            final NavigationViewImpl view = NavigationViewImpl.this;
            (view.paneOpen ? view.paneOverlayPaint : view.panePaint).paint(painter, bounds);
            this.titleLine.draw(painter, this.isRightToLeft() ? Flow.mirrorX(this.titleX, bounds)
                    - this.titleLine.width() : this.titleX, this.titleY);
        }
    }

    /**
     * The toggle: three lines in the item text's color on the toggle paint, an item's height square, that opens or
     * closes the pane over the content on a left click.
     */
    private final class Toggle extends BuiltInComponent<Toggle> implements HasCursor<Toggle> {
        private Rect placedFor;
        private final Rect[] lines;

        @SuppressWarnings("this-escape")
        Toggle(final ComponentFactoryImpl ui) {
            super(ui);
            this.lines = new Rect[3];
            this.setCursor(CursorKind.HAND);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            NavigationViewImpl.this.measureLines(context.text());
            final float side = NavigationViewImpl.this.rowHeight;
            this.placedFor = null;
            return new Size(side, side);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            final NavigationViewImpl view = NavigationViewImpl.this;
            (this.isHovered() ? view.toggleHoveredPaint : view.togglePaint).paint(painter, bounds);
            if (!bounds.equals(this.placedFor)) {
                final float x = bounds.x() + (bounds.width() - Rings.ARROW_LONG) * 0.5f;
                final float y = bounds.y() + (bounds.height() - Rings.ARROW_LONG) * 0.5f;
                for (int i = 0; i < 3; i++) {
                    this.lines[i] = new Rect(x, y + i * 3f, Rings.ARROW_LONG, 1f);
                }
                this.placedFor = bounds;
            }
            final int color = (view.isEnabled() ? view.itemText : view.itemTextDisabled).color();
            for (final Rect line : this.lines) {
                painter.fill(line, color);
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT) {
                NavigationViewImpl.this.userToggle();
                event.consume();
            }
        }
    }

    /**
     * One entry of the pane: an item's icon, text, badge and arrow, a header's caption or a separator's line. An item
     * row in the column of icons is a square with the icon in the middle and the badge at its corner.
     */
    private final class Row extends BuiltInComponent<Row> implements HasCursor<Row>, HasTooltip<Row> {
        private final NavigationEntryImpl entry;
        private int depth;
        private final Badge badge;
        private TextLine normal;
        private TextLine selectedLine;
        private TextLine disabled;

        // Worked out for the last bounds painted
        private Rect placedFor;
        private boolean placedCompact;
        private boolean placedRightToLeft;
        private TextLine shownNormal;
        private TextLine shownSelected;
        private TextLine shownDisabled;
        private Rect iconRect;
        private Rect indicatorRect;
        private Rect badgeRect;
        private Rect lineRect;
        private float textX;
        private float textY;
        private float arrowX;
        private float arrowY;
        private final Rings.Arrow arrow = new Rings.Arrow();
        private final Rings.Ring ring = new Rings.Ring();

        @SuppressWarnings("this-escape")
        Row(final ComponentFactoryImpl ui, final NavigationEntryImpl entry) {
            super(ui);
            this.entry = entry;
            this.badge = new Badge();
            this.normal = TextLine.EMPTY;
            this.selectedLine = TextLine.EMPTY;
            this.disabled = TextLine.EMPTY;
            this.shownNormal = TextLine.EMPTY;
            this.shownSelected = TextLine.EMPTY;
            this.shownDisabled = TextLine.EMPTY;
            this.iconRect = Rect.ZERO;
            this.indicatorRect = Rect.ZERO;
            this.badgeRect = Rect.ZERO;
            this.lineRect = Rect.ZERO;
            this.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        }

        /**
         * Takes the entry's cursor and tooltip, and lays out again, after the entry or the look changed.
         */
        void sync() {
            if (this.entry instanceof final NavigationItemImpl item) {
                this.setCursor(item.isEnabled() ? CursorKind.HAND : null);
                final Tooltip own = item.tooltip().orElse(null);
                final Tooltip shown = own == null && NavigationViewImpl.this.compactLook()
                        ? Tooltip.text(item.text()) : own;
                if (shown == null) {
                    this.clearTooltip();
                } else {
                    this.setTooltip(shown);
                }
            }
            this.invalidateLayout();
        }

        @Override
        protected void onApplyTheme(final Theme theme) {
            this.badge.applyTheme(theme);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final NavigationViewImpl view = NavigationViewImpl.this;
            final TextMetrics metrics = context.text();
            view.measureLines(metrics);
            this.placedFor = null;
            final Insets padding = view.itemPadding;
            if (this.entry instanceof NavigationSeparatorImpl) {
                return new Size(padding.horizontal(), 1f + SEPARATOR_SPACE * 2f);
            }
            if (this.entry instanceof final NavigationHeaderImpl header) {
                this.normal = TextLine.of(header.text(), view.headerText, metrics);
                return new Size(padding.horizontal() + this.normal.width(), view.rowHeight);
            }
            final NavigationItemImpl item = (NavigationItemImpl) this.entry;
            this.normal = TextLine.of(item.text(), view.itemText, metrics);
            this.selectedLine = TextLine.of(item.text(), view.itemTextSelected, metrics);
            this.disabled = TextLine.of(item.text(), view.itemTextDisabled, metrics);
            this.badge.setCount(item.badge());
            this.badge.measure(metrics);
            if (view.compactLook()) {
                return new Size(view.rowHeight, view.rowHeight);
            }
            float width = padding.horizontal() + this.depth * (view.lineHeight + GAP) + view.lineHeight + GAP
                    + Math.max(this.normal.width(), Math.max(this.selectedLine.width(), this.disabled.width()));
            if (this.badge.isShown()) {
                width += GAP + this.badge.width();
            }
            if (!item.children().isEmpty()) {
                width += GAP + Rings.ARROW_LONG;
            }
            return new Size(width, view.rowHeight);
        }

        /**
         * Works out where the parts go in the bounds, and cuts the text short to fit. Right to left each part is
         * worked out as for left to right and mirrored inside the bounds; the text's x stays as for left to right,
         * since each shown line is mirrored by its own width when it is drawn.
         */
        private void place(final Rect bounds, final boolean compact) {
            final NavigationViewImpl view = NavigationViewImpl.this;
            final Insets padding = view.itemPadding;
            final float line = view.lineHeight;
            this.placedFor = bounds;
            this.placedCompact = compact;
            this.placedRightToLeft = this.isRightToLeft();
            this.indicatorRect = this.flow(new Rect(bounds.x(), bounds.y(), INDICATOR_WIDTH, bounds.height()));
            if (this.entry instanceof NavigationSeparatorImpl) {
                final float inset = compact ? 2f : padding.left();
                this.lineRect = this.flow(new Rect(bounds.x() + inset, bounds.y() + SEPARATOR_SPACE,
                        Math.max(0f, bounds.width() - inset * 2f), 1f));
                return;
            }
            final TextMetrics metrics = this.environment().text();
            if (this.entry instanceof NavigationHeaderImpl) {
                this.textX = bounds.x() + padding.left();
                this.textY = bounds.y() + (bounds.height() - this.normal.height()) * 0.5f;
                this.shownNormal = this.normal.ellipsize(Math.max(0f, bounds.right() - padding.right() - this.textX),
                        view.headerText, metrics);
                return;
            }
            final NavigationItemImpl item = (NavigationItemImpl) this.entry;
            if (compact) {
                this.iconRect = this.flow(fitIcon(item, bounds.x() + (bounds.width() - line) * 0.5f,
                        bounds.y() + (bounds.height() - line) * 0.5f, line));
                this.badgeRect = this.flow(this.badge.cornerAt(bounds.right(), bounds.bottom()));
                return;
            }
            final float left = bounds.x() + padding.left() + this.depth * (line + GAP);
            this.iconRect = this.flow(fitIcon(item, left, bounds.y() + (bounds.height() - line) * 0.5f, line));
            float right = bounds.right() - padding.right();
            if (!item.children().isEmpty()) {
                this.arrowX = right - Rings.ARROW_LONG;
                this.arrowY = bounds.y() + (bounds.height() - Rings.ARROW_SHORT) * 0.5f;
                right = this.arrowX - GAP;
            } else {
                this.arrowX = bounds.right();
            }
            if (this.badge.isShown()) {
                final Rect badgeRect = new Rect(right - this.badge.width(),
                        bounds.y() + (bounds.height() - this.badge.height()) * 0.5f, this.badge.width(),
                        this.badge.height());
                this.badgeRect = this.flow(badgeRect);
                right = badgeRect.x() - GAP;
            }
            if (this.placedRightToLeft && !item.children().isEmpty()) {
                this.arrowX = Flow.mirrorX(this.arrowX, bounds) - Rings.ARROW_LONG;
            }
            this.textX = left + line + GAP;
            this.textY = bounds.y() + (bounds.height() - this.normal.height()) * 0.5f;
            final float room = Math.max(0f, right - this.textX);
            this.shownNormal = this.normal.ellipsize(room, view.itemText, metrics);
            this.shownSelected = this.selectedLine.ellipsize(room, view.itemTextSelected, metrics);
            this.shownDisabled = this.disabled.ellipsize(room, view.itemTextDisabled, metrics);
        }

        /**
         * Returns where an item's icon draws in its square slot one line tall: at its size in the middle, or scaled
         * down to fit, keeping its shape.
         */
        private static Rect fitIcon(final NavigationItemImpl item, final float x, final float y, final float line) {
            final float width = item.iconWidth();
            final float height = item.iconHeight();
            final float scale = Math.min(1f, Math.min(width > line ? line / width : 1f,
                    height > line ? line / height : 1f));
            final float drawnWidth = width * scale;
            final float drawnHeight = height * scale;
            return new Rect(x + (line - drawnWidth) * 0.5f, y + (line - drawnHeight) * 0.5f, drawnWidth, drawnHeight);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            final NavigationViewImpl view = NavigationViewImpl.this;
            final boolean compact = view.compactLook();
            if (!bounds.equals(this.placedFor) || compact != this.placedCompact
                    || this.isRightToLeft() != this.placedRightToLeft) {
                this.place(bounds, compact);
            }
            if (this.entry instanceof NavigationSeparatorImpl) {
                view.separatorPaint.paint(painter, this.lineRect);
                return;
            }
            if (this.entry instanceof NavigationHeaderImpl) {
                this.shownNormal.draw(painter, this.textAt(this.shownNormal, bounds), this.textY);
                return;
            }
            final NavigationItemImpl item = (NavigationItemImpl) this.entry;
            final boolean enabled = item.isEnabled() && view.isEnabled();
            final boolean isSelected = view.shownSelection() == item;
            if (isSelected) {
                view.itemSelected.paint(painter, bounds);
                view.indicator.paint(painter, this.indicatorRect);
            } else if (enabled && this.isHovered()) {
                view.itemHovered.paint(painter, bounds);
            }
            final int tint = enabled ? WHITE : DISABLED_ICON_TINT;
            if (item.visual() != null) {
                painter.visual(item.visual(), this.iconRect, tint);
            } else if (item.sprite() != null) {
                painter.sprite(item.sprite(), this.iconRect, tint);
            }
            if (!compact) {
                final TextLine text = isSelected && enabled ? this.shownSelected
                        : enabled ? this.shownNormal : this.shownDisabled;
                text.draw(painter, this.textAt(text, bounds), this.textY);
                if (!item.children().isEmpty()) {
                    final int color = (enabled ? view.itemText : view.itemTextDisabled).color();
                    if (item.isExpanded()) {
                        this.arrow.up(painter, color, this.arrowX, this.arrowY);
                    } else {
                        this.arrow.down(painter, color, this.arrowX, this.arrowY);
                    }
                }
            }
            this.badge.paint(painter, this.badgeRect);
            if (view.isFocused() && view.cursor == item) {
                this.ring.paint(painter, view.focusRing, bounds);
            }
        }

        /**
         * Returns where a shown line starts: at the text's x, or mirrored by the line's width right to left.
         */
        private float textAt(final TextLine text, final Rect bounds) {
            return this.placedRightToLeft ? Flow.mirrorX(this.textX, bounds) - text.width() : this.textX;
        }

        @Override
        protected void onPointerPress(final PointerButtonEvent event) {
            if (NavigationViewImpl.this.paneOpen && event.button() == MouseButton.LEFT) {
                NavigationViewImpl.this.refocus();
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT) {
                NavigationViewImpl.this.rowClicked(this, event);
            }
        }
    }
}
