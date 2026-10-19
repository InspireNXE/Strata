package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The core {@link Menu}. It draws its items itself rather than holding a component per item, and opens as a popup
 * of its host's overlays. A submenu opens with its top-left corner at the right edge of its item.
 *
 * <p>Opening a menu lays out its host's overlays at once, so its bounds and items are known before the next frame.
 * The first menu of a chain remembers what had focus when it opened and gives focus back when it closes, if that
 * component can still take it. A submenu that closes gives focus back to the menu it opened from.</p>
 */
public final class MenuImpl extends CustomComponent<Menu> implements Menu {
    /**
     * The space between the menu's edge and its items, in UI units.
     */
    public static final float EDGE = 2f;
    /**
     * The room at each side of an item for the check mark and the submenu arrow, in UI units.
     */
    public static final float GUTTER = 14f;
    /**
     * The least space between an item's text and its shortcut, in UI units.
     */
    public static final float SHORTCUT_GAP = 16f;
    /**
     * The height of a separator, in UI units.
     */
    public static final float SEPARATOR_HEIGHT = 5f;
    /**
     * The narrowest a menu is, in UI units.
     */
    public static final float MIN_WIDTH = 60f;
    private static final float CHECK_SIZE = 4f;
    private static final Set<String> REQUIRED = Set.of(STYLE_BACKGROUND, STYLE_ITEM_HOVERED);

    private final ComponentFactory ui;
    private final List<Item> items;
    private MenuImpl parent;
    private MenuBarImpl bar;
    private Overlay popup;
    private MenuImpl openSubmenu;
    private Component<?> returnFocus;
    private int highlighted;

    // Theme
    private Paint background;
    private Paint itemHovered;
    private Paint separator;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;
    private TextStyle shortcutStyle;

    // Computed during measure
    private float itemHeight;
    private float lineHeight;
    private float[] itemTop;

    /**
     * Creates an empty menu.
     *
     * @param ui the factory that owns the menu
     */
    @SuppressWarnings("this-escape")
    public MenuImpl(final ComponentFactory ui) {
        super(ui);
        this.ui = ui;
        this.items = new ArrayList<>();
        this.highlighted = -1;
        this.background = Paint.NONE;
        this.itemHovered = Paint.NONE;
        this.separator = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.shortcutStyle = TextStyle.DEFAULT;
        this.itemTop = new float[0];
        this.setFocusable(true);
    }

    // ------------------------------------------------------------------
    // Items
    // ------------------------------------------------------------------

    @Override
    public Menu addItem(final String text, final Runnable action) {
        return this.add(new Item(Objects.requireNonNull(text, "text"), "", Objects.requireNonNull(action, "action"), null, null));
    }

    @Override
    public Menu addItem(final String text, final String shortcut, final Runnable action) {
        return this.add(new Item(Objects.requireNonNull(text, "text"), Objects.requireNonNull(shortcut, "shortcut"),
                Objects.requireNonNull(action, "action"), null, null));
    }

    @Override
    public Menu addCheckItem(final String text, final boolean checked, final Consumer<Boolean> action) {
        final Item item = new Item(Objects.requireNonNull(text, "text"), "", null, Objects.requireNonNull(action, "action"), null);
        item.checked = checked;
        return this.add(item);
    }

    @Override
    public Menu addSubmenu(final String text, final Menu submenu) {
        Objects.requireNonNull(text, "text");
        if (!(submenu instanceof MenuImpl menu)) {
            throw new IllegalArgumentException(submenu + " is not a Strata menu");
        }
        for (MenuImpl up = this; up != null; up = up.parent) {
            if (up == menu) {
                throw new IllegalArgumentException("A menu cannot open itself or a menu it opens from");
            }
        }
        if (menu.parent != null || menu.bar != null) {
            throw new IllegalArgumentException(submenu + " is already a submenu or in a menu bar");
        }
        menu.parent = this;
        return this.add(new Item(text, "", null, null, menu));
    }

    @Override
    public Menu addSeparator() {
        final Item item = new Item("", "", null, null, null);
        item.separator = true;
        item.enabled = false;
        return this.add(item);
    }

    private Menu add(final Item item) {
        this.items.add(item);
        this.invalidateLayout();
        return this;
    }

    @Override
    public Menu clearItems() {
        this.close();
        for (final Item item : this.items) {
            if (item.submenu != null) {
                item.submenu.parent = null;
            }
        }
        this.items.clear();
        this.invalidateLayout();
        return this;
    }

    @Override
    public int itemCount() {
        return this.items.size();
    }

    @Override
    public String itemText(final int index) {
        return this.items.get(index).text;
    }

    @Override
    public boolean isItemEnabled(final int index) {
        return this.items.get(index).enabled;
    }

    @Override
    public Menu setItemEnabled(final int index, final boolean enabled) {
        final Item item = this.items.get(index);
        item.enabled = enabled && !item.separator;
        if (!item.enabled && this.highlighted == index) {
            this.highlight(-1);
        }
        return this;
    }

    @Override
    public boolean isItemChecked(final int index) {
        return this.items.get(index).checked;
    }

    @Override
    public Menu setItemChecked(final int index, final boolean checked) {
        final Item item = this.items.get(index);
        if (item.checkAction == null) {
            throw new IllegalArgumentException("Item " + index + " is not a check item");
        }
        item.checked = checked;
        return this;
    }

    // ------------------------------------------------------------------
    // Opening and closing
    // ------------------------------------------------------------------

    @Override
    public boolean isOpen() {
        return this.popup != null && this.popup.isOpen();
    }

    @Override
    public Menu open(final Rect anchor) {
        Objects.requireNonNull(anchor, "anchor");
        if (this.isOpen() || this.items.isEmpty()) {
            return this;
        }
        this.returnFocus = this.parent == null ? this.focusedComponent() : null;
        this.highlighted = -1;
        this.popup = this.ui.overlays().popup(this, anchor);
        this.popup.onEvent(Overlay.CLOSED, event -> this.closed());
        // Lay the popup out now, so keys pressed before the next frame find the items, and a submenu its anchor
        if (this.ui instanceof ComponentFactoryImpl factory && factory.root() != null) {
            factory.root().layout();
        }
        this.requestFocus();
        return this;
    }

    @Override
    public Menu openAt(final float x, final float y) {
        return this.open(new Rect(x, y, 0f, 0f));
    }

    @Override
    public Menu close() {
        if (this.openSubmenu != null) {
            this.openSubmenu.close();
        }
        if (this.popup != null) {
            this.popup.close();
        }
        return this;
    }

    /**
     * Runs once the popup has closed, however it closed.
     */
    private void closed() {
        this.popup = null;
        this.highlighted = -1;
        if (this.openSubmenu != null) {
            this.openSubmenu.close();
        }
        if (this.parent != null && this.parent.openSubmenu == this) {
            this.parent.openSubmenu = null;
        }
        if (this.bar != null) {
            this.bar.menuClosed(this);
        }
        if (this.returnFocus != null) {
            final Component<?> focus = this.returnFocus;
            this.returnFocus = null;
            if (focus.isAttached()) {
                focus.requestFocus();
            }
        }
        this.report(CLOSED);
    }

    private Component<?> focusedComponent() {
        if (this.ui instanceof ComponentFactoryImpl factory && factory.root() != null) {
            final Node node = factory.root().focused();
            return node == null ? null : node.facade();
        }
        return null;
    }

    /**
     * The first menu of this menu's chain.
     */
    private MenuImpl top() {
        MenuImpl menu = this;
        while (menu.parent != null) {
            menu = menu.parent;
        }
        return menu;
    }

    void setBar(final MenuBarImpl bar) {
        this.bar = bar;
    }

    boolean isFree() {
        return this.bar == null && this.parent == null;
    }

    /**
     * Highlights the first item that can be chosen, for menus opened from the keyboard.
     */
    void highlightFirst() {
        this.highlight(this.nextUsable(-1, 1));
    }

    private void highlight(final int index) {
        if (index == this.highlighted) {
            return;
        }
        this.highlighted = index;
        if (this.openSubmenu != null && (index < 0 || this.items.get(index).submenu != this.openSubmenu)) {
            this.openSubmenu.close();
        }
    }

    private int nextUsable(final int from, final int direction) {
        final int count = this.items.size();
        for (int step = 1; step <= count; step++) {
            final int index = Math.floorMod(from + step * direction, count);
            if (this.items.get(index).enabled) {
                return index;
            }
        }
        return -1;
    }

    private void openSubmenu(final int index, final boolean fromKeyboard) {
        final MenuImpl submenu = this.items.get(index).submenu;
        if (submenu == null || submenu.isOpen()) {
            return;
        }
        final Rect bounds = this.bounds();
        submenu.open(new Rect(bounds.right(), bounds.y() + this.itemTop[index] - EDGE, 0f, 0f));
        this.openSubmenu = submenu;
        if (fromKeyboard) {
            submenu.highlightFirst();
        } else {
            this.requestFocus();
        }
    }

    private void choose(final int index) {
        if (index < 0) {
            return;
        }
        final Item item = this.items.get(index);
        if (!item.enabled) {
            return;
        }
        if (item.submenu != null) {
            this.openSubmenu(index, true);
            return;
        }
        this.request(CHOOSE, index, this::run);
    }

    /**
     * Closes the chain, flips a check item, runs the item's action and reports {@link #CHOSEN}: what an accepted
     * {@link #CHOOSE} request does. Ignores an index that is out of range or names a disabled item, a separator or a
     * submenu item, as a handler may have rewritten it.
     */
    private void run(final int index) {
        if (index < 0 || index >= this.items.size()) {
            return;
        }
        final Item item = this.items.get(index);
        if (!item.enabled || item.submenu != null) {
            return;
        }
        this.top().close();
        if (item.checkAction != null) {
            item.checked = !item.checked;
            item.checkAction.accept(item.checked);
        } else {
            item.action.run();
        }
        this.report(CHOSEN, index, index);
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
        this.background = theme.paintOr(STYLE_BACKGROUND, Paint.NONE);
        this.itemHovered = theme.paintOr(STYLE_ITEM_HOVERED, Paint.NONE);
        this.separator = theme.paintOr(STYLE_SEPARATOR, theme.paintOr(Separator.STYLE_LINE, Paint.NONE));
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.shortcutStyle = theme.textStyleOr(STYLE_SHORTCUT, this.disabledTextStyle);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.lineHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        this.itemHeight = this.lineHeight + 6f;
        if (this.itemTop.length != this.items.size()) {
            this.itemTop = new float[this.items.size()];
        }
        float textWidth = 0f;
        float shortcutWidth = 0f;
        float y = EDGE;
        for (int i = 0; i < this.items.size(); i++) {
            final Item item = this.items.get(i);
            this.itemTop[i] = y;
            if (item.separator) {
                y += SEPARATOR_HEIGHT;
                continue;
            }
            item.span = new TextSpan(item.text, this.textStyle);
            item.disabledSpan = new TextSpan(item.text, this.disabledTextStyle);
            item.shortcutSpan = new TextSpan(item.shortcut, this.shortcutStyle);
            textWidth = Math.max(textWidth, Math.max(metrics.width(item.span), metrics.width(item.disabledSpan)));
            item.shortcutWidth = item.shortcut.isEmpty() ? 0f : metrics.width(item.shortcutSpan);
            shortcutWidth = Math.max(shortcutWidth, item.shortcutWidth);
            y += this.itemHeight;
        }
        final float width = EDGE * 2f + GUTTER * 2f + textWidth + (shortcutWidth > 0f ? SHORTCUT_GAP + shortcutWidth : 0f);
        return new Size(Math.max(MIN_WIDTH, width), y + EDGE);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.background.paint(painter, bounds);
        final float left = bounds.x() + EDGE;
        final float width = bounds.width() - EDGE * 2f;
        for (int i = 0; i < this.items.size(); i++) {
            final Item item = this.items.get(i);
            final float top = bounds.y() + this.itemTop[i];
            if (item.separator) {
                this.separator.paint(painter, new Rect(left + 1f, top + (SEPARATOR_HEIGHT - 1f) * 0.5f, width - 2f, 1f));
                continue;
            }
            if (i == this.highlighted) {
                this.itemHovered.paint(painter, new Rect(left, top, width, this.itemHeight));
            }
            final float textY = top + (this.itemHeight - this.lineHeight) * 0.5f;
            painter.text(item.enabled ? item.span : item.disabledSpan, left + GUTTER, textY);
            final int color = (item.enabled ? this.textStyle : this.disabledTextStyle).color();
            if (item.checked) {
                painter.fill(new Rect(left + (GUTTER - CHECK_SIZE) * 0.5f, top + (this.itemHeight - CHECK_SIZE) * 0.5f,
                        CHECK_SIZE, CHECK_SIZE), color);
            }
            if (!item.shortcut.isEmpty()) {
                painter.text(item.shortcutSpan, left + width - GUTTER - item.shortcutWidth, textY);
            }
            if (item.submenu != null) {
                Rings.right(painter, color, left + width - (GUTTER + Rings.ARROW_SHORT) * 0.5f,
                        top + (this.itemHeight - Rings.ARROW_LONG) * 0.5f);
            }
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    private int itemAt(final float y) {
        final float local = y - this.bounds().y();
        for (int i = this.items.size() - 1; i >= 0; i--) {
            if (local >= this.itemTop[i]) {
                final float height = this.items.get(i).separator ? SEPARATOR_HEIGHT : this.itemHeight;
                return local < this.itemTop[i] + height ? i : -1;
            }
        }
        return -1;
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        final int index = this.itemAt(event.y());
        if (index < 0 || !this.items.get(index).enabled) {
            return;
        }
        this.highlight(index);
        if (this.items.get(index).submenu != null) {
            this.openSubmenu(index, false);
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() != MouseButton.LEFT) {
            return;
        }
        final int index = this.itemAt(event.y());
        if (index >= 0 && this.items.get(index).enabled) {
            this.highlight(index);
            this.choose(index);
        }
        event.consume();
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown()) {
            return;
        }
        final Key key = event.key();
        final boolean press = event.action() == KeyAction.PRESS;
        switch (key) {
            case UP -> this.highlight(this.nextUsable(this.highlighted < 0 ? 0 : this.highlighted, -1));
            case DOWN -> this.highlight(this.nextUsable(this.highlighted, 1));
            case RIGHT -> {
                if (this.highlighted >= 0 && this.items.get(this.highlighted).submenu != null) {
                    this.openSubmenu(this.highlighted, true);
                } else if (this.top().bar != null) {
                    this.top().bar.step(1);
                }
            }
            case LEFT -> {
                if (this.parent != null) {
                    final MenuImpl from = this.parent;
                    this.close();
                    from.requestFocus();
                } else if (this.bar != null) {
                    this.bar.step(-1);
                }
            }
            case ESCAPE -> {
                if (!press) {
                    return;
                }
                final MenuImpl from = this.parent;
                this.close();
                if (from != null) {
                    from.requestFocus();
                }
            }
            default -> {
                if (press && (key.isEnter() || key == Key.SPACE)) {
                    this.choose(this.highlighted);
                } else {
                    return;
                }
            }
        }
        event.consume();
    }

    /**
     * One item. A separator has neither action; a submenu item has only its submenu.
     */
    private static final class Item {
        final String text;
        final String shortcut;
        final Runnable action;
        final Consumer<Boolean> checkAction;
        final MenuImpl submenu;
        boolean separator;
        boolean enabled;
        boolean checked;
        TextSpan span;
        TextSpan disabledSpan;
        TextSpan shortcutSpan;
        float shortcutWidth;

        Item(final String text, final String shortcut, final Runnable action, final Consumer<Boolean> checkAction,
             final MenuImpl submenu) {
            this.text = text;
            this.shortcut = shortcut;
            this.action = action;
            this.checkAction = checkAction;
            this.submenu = submenu;
            this.enabled = true;
            this.span = TextSpan.of(text);
            this.disabledSpan = this.span;
            this.shortcutSpan = TextSpan.of(shortcut);
        }
    }
}
