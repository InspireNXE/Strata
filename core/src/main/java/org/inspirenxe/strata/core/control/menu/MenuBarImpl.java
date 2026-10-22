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
package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.menu.SubmenuItem;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.RectCache;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.CommandSource;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.KeyboardMenu;
import org.inspirenxe.strata.core.node.MnemonicTarget;
import org.inspirenxe.strata.core.text.MnemonicLine;
import org.inspirenxe.strata.core.text.Mnemonics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link MenuBar}. It draws its titles itself, left to right from its left edge, and opens each menu as a
 * popup anchored to its title. Its titles are {@link SubmenuItemImpl}s; a disabled title, or one without a menu,
 * draws in the menu's disabled text style and opens nothing.
 *
 * <p>Alt with a title's mnemonic opens its menu from the keyboard. Alt pressed and released alone, or F10, makes
 * the root give the bar every key and highlights its first title that opens a menu: Left and Right move the
 * highlight, wrapping, Down, Enter or Space open the highlighted menu, a title's mnemonic opens its menu, and
 * Escape lets the keys go. Any other key lets them go too and goes on as usual. Focus never moves to the bar.</p>
 */
public final class MenuBarImpl extends BuiltInComponent<MenuBar> implements MenuBar, CommandSource,
        MnemonicTarget, KeyboardMenu {
    /**
     * The space around each title when the theme sets no {@value #STYLE_TITLE_PADDING}: 8 at the sides and 3 at
     * the top and bottom.
     */
    public static final Insets DEFAULT_TITLE_PADDING = Insets.of(8f, 3f);

    private final List<SubmenuItemImpl> entries;
    private int openIndex;
    // The title the keyboard highlights while the bar has the keys, or -1
    private int keyboardIndex;
    private boolean switching;
    private float pointerX;
    private final RectCache hoverRect = new RectCache();

    // Theme
    private Paint background;
    private Paint hovered;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private float[] titleX;
    private float[] titleWidth;
    private MnemonicLine[] spans;
    private float lineHeight;
    private Insets titlePadding = DEFAULT_TITLE_PADDING;

    /**
     * Creates a bar with no menus.
     *
     * @param ui the factory that owns the bar
     */
    public MenuBarImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.entries = new ArrayList<>();
        this.openIndex = -1;
        this.keyboardIndex = -1;
        this.background = Paint.NONE;
        this.hovered = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.titleX = new float[0];
        this.titleWidth = new float[0];
        this.spans = new MnemonicLine[0];
    }

    @Override
    public List<SubmenuItem> menus() {
        return List.copyOf(this.entries);
    }

    @Override
    public MenuBar addMenu(final SubmenuItem entry) {
        return this.addMenu(this.entries.size(), entry);
    }

    @Override
    public MenuBar addMenu(final int index, final SubmenuItem entry) {
        if (!(entry instanceof SubmenuItemImpl impl)) {
            throw new IllegalArgumentException(entry + " is not a Strata menu entry");
        }
        Objects.checkIndex(index, this.entries.size() + 1);
        if (impl.owner() != null) {
            throw new IllegalArgumentException(entry + " is already in a menu or a menu bar");
        }
        impl.check(impl.submenu(), this);
        impl.setOwner(this);
        impl.link(this);
        this.entries.add(index, impl);
        if (this.openIndex >= index) {
            this.openIndex++;
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public MenuBar removeMenu(final SubmenuItem entry) {
        final int index = this.entries.indexOf(entry);
        if (index < 0) {
            return this;
        }
        if (index == this.openIndex) {
            this.select(-1);
        }
        final SubmenuItemImpl impl = this.entries.remove(index);
        impl.unlink();
        impl.setOwner(null);
        if (this.openIndex > index) {
            this.openIndex--;
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public MenuBar addMenu(final Text title, final Menu menu) {
        return this.addMenu(new SubmenuItemImpl(title).setMenu(menu));
    }

    /**
     * Called by a title when it changed: lays out again, and closes its menu if it can no longer open.
     */
    void entryChanged(final MenuEntryImpl entry) {
        final int index = this.entries.indexOf(entry);
        if (index >= 0 && index == this.openIndex && (!entry.usable() || entry.submenu() == null)) {
            this.select(-1);
        }
        this.invalidateLayout();
    }

    /**
     * Adds the commands of every usable entry in the menus of the bar's usable titles, and in their submenus, in
     * order, so the root runs them from their shortcuts while the bar is attached, visible and enabled.
     */
    @Override
    public void collectCommands(final List<Command> out) {
        for (final SubmenuItemImpl entry : this.entries) {
            if (entry.usable() && entry.submenu() != null) {
                entry.submenu().collectCommands(out);
            }
        }
    }

    private MenuImpl menuAt(final int index) {
        return this.entries.get(index).submenu();
    }

    // The bar closes the menu, so it is on the cause chain of the menu's reports
    private void closeMenu(final int index) {
        try (CauseFrame _ = this.node().causes().pushFrame().push(this)) {
            this.menuAt(index).close();
        }
    }

    @Override
    public int selectedIndex() {
        return this.openIndex;
    }

    @Override
    public Optional<Menu> selected() {
        return this.openIndex < 0 ? Optional.empty() : Optional.ofNullable(this.menuAt(this.openIndex));
    }

    @Override
    public MenuBar select(final int index) {
        if (index == -1) {
            if (this.openIndex >= 0) {
                this.closeMenu(this.openIndex);
            }
            return this;
        }
        Objects.checkIndex(index, this.entries.size());
        if (this.isAttached()) {
            this.openMenu(index, false);
        }
        return this;
    }

    @Override
    public MenuBar selectItem(final Menu menu) {
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.menuAt(i) == menu) {
                return this.select(i);
            }
        }
        return this.select(-1);
    }

    /**
     * Called by a menu of this bar once it has closed, however it closed. Reports the bar's selection clearing,
     * unless the bar closed the menu to open another.
     */
    void menuClosed(final MenuImpl menu) {
        if (this.openIndex >= 0 && this.menuAt(this.openIndex) == menu) {
            final int previous = this.openIndex;
            this.openIndex = -1;
            if (!this.switching) {
                this.report(SELECTED, -1, previous);
            }
        }
    }

    /**
     * Opens a menu for the user through a {@code SELECT} request.
     */
    private void userOpen(final int index, final boolean fromKeyboard) {
        this.request(SELECT, index, chosen -> {
            if (chosen != null) {
                this.openMenu(chosen, fromKeyboard);
            }
        });
    }

    /**
     * Closes the open menu and opens another, then reports {@code SELECTED} once for the change. Ignores an index
     * that is out of range, names the menu already open, or names a disabled title or one without a menu, as a
     * handler may have rewritten it.
     */
    private void openMenu(final int index, final boolean fromKeyboard) {
        if (index < 0 || index >= this.titleX.length || index >= this.entries.size() || index == this.openIndex
                || !this.entries.get(index).usable() || this.menuAt(index) == null) {
            return;
        }
        final int previous = this.openIndex;
        if (previous >= 0) {
            this.switching = true;
            try {
                this.closeMenu(previous);
            } finally {
                this.switching = false;
            }
        }
        final Rect bounds = this.bounds();
        final MenuImpl menu = this.menuAt(index);
        menu.open(new Rect(bounds.x() + this.titleX[index], bounds.y(), this.titleWidth[index], bounds.height()));
        if (menu.isOpen()) {
            this.openIndex = index;
            if (fromKeyboard) {
                menu.highlightFirst();
            }
            this.report(SELECTED, index, previous);
        } else if (previous >= 0) {
            this.report(SELECTED, -1, previous);
        }
    }

    /**
     * Moves from the open menu to its neighbor, wrapping at the ends; called by a menu on Left or Right.
     */
    void step(final int direction) {
        final int count = this.entries.size();
        if (this.openIndex < 0 || count < 2) {
            return;
        }
        for (int step = 1; step < count; step++) {
            final int index = Math.floorMod(this.openIndex + step * direction, count);
            if (this.entries.get(index).usable() && this.menuAt(index) != null) {
                this.userOpen(index, true);
                return;
            }
        }
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.background = theme.paintOr(STYLE_BACKGROUND, Paint.NONE);
        this.hovered = theme.paintOr(STYLE_ITEM_HOVERED, theme.paintOr(Menu.STYLE_ITEM_HOVERED, Paint.NONE));
        this.textStyle = theme.textStyleOr(STYLE_TEXT, theme.textStyleOr(Menu.STYLE_TEXT, TextStyle.DEFAULT));
        this.disabledTextStyle = theme.textStyleOr(Menu.STYLE_TEXT_DISABLED, this.textStyle);
        this.titlePadding = theme.insetsOr(STYLE_TITLE_PADDING, DEFAULT_TITLE_PADDING);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        final int count = this.entries.size();
        if (this.titleX.length != count) {
            this.titleX = new float[count];
            this.titleWidth = new float[count];
            this.spans = new MnemonicLine[count];
        }
        float x = 0f;
        for (int i = 0; i < count; i++) {
            final SubmenuItemImpl entry = this.entries.get(i);
            this.spans[i] = MnemonicLine.of(entry.text(), entry.usable() ? this.textStyle : this.disabledTextStyle,
                    metrics);
            this.titleX[i] = x;
            this.titleWidth[i] = this.spans[i].plain().width() + this.titlePadding.horizontal();
            x += this.titleWidth[i];
        }
        this.lineHeight = metrics.lineHeight(this.textStyle);
        return new Size(x, this.lineHeight + this.titlePadding.vertical());
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
        final int hover = this.keyboardIndex >= 0 ? this.keyboardIndex
                : this.isHovered() ? this.titleAt(this.pointerX) : -1;
        final boolean marked = this.hostRoot() != null && this.hostRoot().showsMnemonics();
        final Insets padding = this.titlePadding;
        final float textY = bounds.y() + padding.top() + (bounds.height() - padding.vertical() - this.lineHeight) * 0.5f;
        for (int i = 0; i < this.spans.length; i++) {
            if (i == hover || i == this.openIndex) {
                this.hovered.paint(painter, this.hoverRect.at(bounds.x() + this.titleX[i], bounds.y(),
                        this.titleWidth[i], bounds.height()));
            }
            this.spans[i].line(marked).draw(painter, bounds.x() + this.titleX[i] + padding.left(), textY);
        }
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    private boolean opens(final int index) {
        return index >= 0 && index < this.spans.length && index < this.entries.size()
                && this.entries.get(index).usable() && this.menuAt(index) != null;
    }

    private int titleWithMnemonic(final int mnemonic) {
        for (int i = 0; i < this.spans.length; i++) {
            if (this.opens(i) && this.spans[i].mnemonic() == mnemonic) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean hasMnemonic(final int mnemonic) {
        return this.titleWithMnemonic(mnemonic) >= 0;
    }

    /**
     * Opens the menu of the first title with the mnemonic for the user, from the keyboard, as Alt with the
     * mnemonic does.
     */
    @Override
    public void activateMnemonic(final int mnemonic) {
        final int index = this.titleWithMnemonic(mnemonic);
        if (index >= 0) {
            this.userOpen(index, true);
        }
    }

    @Override
    public boolean enterKeyboard() {
        if (this.hostRoot() == null) {
            return false;
        }
        for (int i = 0; i < this.spans.length; i++) {
            if (this.opens(i)) {
                this.keyboardIndex = i;
                this.hostRoot().grabKeys(this.node(), () -> this.keyboardIndex = -1);
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the keyboard's highlight to the next title that opens a menu, wrapping.
     */
    private void stepHighlight(final int direction) {
        final int count = this.spans.length;
        for (int step = 1; step < count; step++) {
            final int index = Math.floorMod(this.keyboardIndex + step * direction, count);
            if (this.opens(index)) {
                this.keyboardIndex = index;
                return;
            }
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (this.keyboardIndex < 0 || !event.action().isDown() || event.key().isModifier()) {
            return;
        }
        final Key key = event.key();
        final boolean press = event.action() == KeyAction.PRESS;
        final int mnemonic = Mnemonics.of(key);
        if (key == Key.LEFT || key == Key.RIGHT) {
            this.stepHighlight(key == Key.LEFT ? -1 : 1);
        } else if (key == Key.DOWN || key.isEnter() || key == Key.SPACE) {
            if (press) {
                final int index = this.keyboardIndex;
                this.hostRoot().releaseKeys(this.node());
                this.userOpen(index, true);
            }
        } else if (key == Key.ESCAPE) {
            if (press) {
                this.hostRoot().releaseKeys(this.node());
            }
        } else if (press && mnemonic != Mnemonics.NONE && this.titleWithMnemonic(mnemonic) >= 0) {
            this.hostRoot().releaseKeys(this.node());
            this.activateMnemonic(mnemonic);
        } else {
            this.hostRoot().releaseKeys(this.node());
            return;
        }
        event.consume();
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.pointerX = event.x();
        if (this.openIndex >= 0) {
            final int index = this.titleAt(event.x());
            if (index >= 0 && index != this.openIndex && this.entries.get(index).usable() && this.menuAt(index) != null) {
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
            this.closeMenu(index);
        } else if (this.entries.get(index).usable() && this.menuAt(index) != null) {
            this.userOpen(index, false);
        }
        event.consume();
    }
}
