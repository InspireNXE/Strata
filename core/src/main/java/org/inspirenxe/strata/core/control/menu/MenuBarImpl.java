package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link MenuBar}. It draws its titles itself, left to right from its left edge, and opens each menu as a
 * popup anchored to its title.
 */
public final class MenuBarImpl extends CustomComponent<MenuBar> implements MenuBar {
    /**
     * The space on each side of a title, in UI units.
     */
    public static final float TITLE_PADDING = 8f;

    private final List<String> titles;
    private final List<MenuImpl> menus;
    private int openIndex;
    private float pointerX;

    // Theme
    private Paint background;
    private Paint hovered;
    private TextStyle textStyle;

    // Computed during measure
    private float[] titleX;
    private float[] titleWidth;
    private TextSpan[] spans;
    private float lineHeight;

    /**
     * Creates a bar with no menus.
     *
     * @param ui the factory that owns the bar
     */
    public MenuBarImpl(final ComponentFactory ui) {
        super(ui);
        this.titles = new ArrayList<>();
        this.menus = new ArrayList<>();
        this.openIndex = -1;
        this.background = Paint.NONE;
        this.hovered = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.titleX = new float[0];
        this.titleWidth = new float[0];
        this.spans = new TextSpan[0];
    }

    @Override
    public MenuBar addMenu(final String title, final Menu menu) {
        Objects.requireNonNull(title, "title");
        if (!(menu instanceof MenuImpl impl)) {
            throw new IllegalArgumentException(menu + " is not a Strata menu");
        }
        if (!impl.isFree()) {
            throw new IllegalArgumentException(menu + " is already a submenu or in a menu bar");
        }
        impl.setBar(this);
        this.titles.add(title);
        this.menus.add(impl);
        this.invalidateLayout();
        return this;
    }

    @Override
    public int menuCount() {
        return this.menus.size();
    }

    @Override
    public Menu menu(final int index) {
        return this.menus.get(index);
    }

    @Override
    public Optional<Integer> openIndex() {
        return this.openIndex < 0 ? Optional.empty() : Optional.of(this.openIndex);
    }

    /**
     * Called by a menu of this bar once it has closed, however it closed.
     */
    void menuClosed(final MenuImpl menu) {
        if (this.openIndex >= 0 && this.menus.get(this.openIndex) == menu) {
            this.openIndex = -1;
        }
    }

    /**
     * Opens a menu for the user through an {@link #OPEN} request.
     */
    private void userOpen(final int index, final boolean fromKeyboard) {
        this.request(OPEN, index, chosen -> this.openMenu(chosen, fromKeyboard));
    }

    /**
     * Closes the open menu and opens another, then reports {@link #OPENED} if it opened. Ignores an index that is
     * out of range or names the menu already open, as a handler may have rewritten it.
     */
    private void openMenu(final int index, final boolean fromKeyboard) {
        if (index < 0 || index >= this.titleX.length || index == this.openIndex) {
            return;
        }
        final int previous = this.openIndex;
        if (previous >= 0) {
            this.menus.get(previous).close();
        }
        final Rect bounds = this.bounds();
        final MenuImpl menu = this.menus.get(index);
        menu.open(new Rect(bounds.x() + this.titleX[index], bounds.y(), this.titleWidth[index], bounds.height()));
        if (menu.isOpen()) {
            this.openIndex = index;
            if (fromKeyboard) {
                menu.highlightFirst();
            }
            this.report(OPENED, index, previous);
        }
    }

    /**
     * Moves from the open menu to its neighbor, wrapping at the ends; called by a menu on Left or Right.
     */
    void step(final int direction) {
        if (this.openIndex < 0 || this.menus.size() < 2) {
            return;
        }
        this.userOpen(Math.floorMod(this.openIndex + direction, this.menus.size()), true);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.background = theme.paintOr(STYLE_BACKGROUND, Paint.NONE);
        this.hovered = theme.paintOr(STYLE_ITEM_HOVERED, theme.paintOr(Menu.STYLE_ITEM_HOVERED, Paint.NONE));
        this.textStyle = theme.textStyleOr(STYLE_TEXT, theme.textStyleOr(Menu.STYLE_TEXT, TextStyle.DEFAULT));
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        final int count = this.titles.size();
        if (this.titleX.length != count) {
            this.titleX = new float[count];
            this.titleWidth = new float[count];
            this.spans = new TextSpan[count];
        }
        float x = 0f;
        for (int i = 0; i < count; i++) {
            this.spans[i] = new TextSpan(this.titles.get(i), this.textStyle);
            this.titleX[i] = x;
            this.titleWidth[i] = metrics.width(this.spans[i]) + TITLE_PADDING * 2f;
            x += this.titleWidth[i];
        }
        this.lineHeight = metrics.lineHeight(this.textStyle);
        return new Size(x, this.lineHeight + 6f);
    }

    private int titleAt(final float x) {
        final float local = x - this.bounds().x();
        for (int i = 0; i < this.titleX.length; i++) {
            if (local >= this.titleX[i] && local < this.titleX[i] + this.titleWidth[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.background.paint(painter, bounds);
        final int hover = this.isHovered() ? this.titleAt(this.pointerX) : -1;
        final float textY = bounds.y() + (bounds.height() - this.lineHeight) * 0.5f;
        for (int i = 0; i < this.spans.length; i++) {
            if (i == hover || i == this.openIndex) {
                this.hovered.paint(painter, new Rect(bounds.x() + this.titleX[i], bounds.y(), this.titleWidth[i], bounds.height()));
            }
            painter.text(this.spans[i], bounds.x() + this.titleX[i] + TITLE_PADDING, textY);
        }
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.pointerX = event.x();
        if (this.openIndex >= 0) {
            final int index = this.titleAt(event.x());
            if (index >= 0 && index != this.openIndex) {
                this.userOpen(index, false);
            }
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() != MouseButton.LEFT) {
            return;
        }
        final int index = this.titleAt(event.x());
        if (index < 0) {
            return;
        }
        if (index == this.openIndex) {
            this.menus.get(index).close();
        } else {
            this.userOpen(index, false);
        }
        event.consume();
    }
}
