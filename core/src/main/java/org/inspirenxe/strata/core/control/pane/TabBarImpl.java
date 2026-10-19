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

import java.util.Optional;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.pane.Tab;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The core {@link TabBar}. Each {@link TabImpl} is drawn by a private child component, so it can have its own hover
 * and tooltip. A tab
 * asks for its text plus 8 units on each side. Tabs stay enabled as components even when they cannot be selected,
 * so a disabled tab still shows its tooltip.
 */
public final class TabBarImpl extends CustomComposite<TabBar> implements TabBar {
    /**
     * The space on each side of a tab's text, in UI units.
     */
    public static final float TAB_PADDING = 8f;
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_SELECTED);

    private final ComponentFactory ui;
    private final List<TabView> views;
    private int selectedIndex;
    private float spacing;

    /**
     * Creates a bar with no tabs.
     *
     * @param ui the factory that owns the bar
     */
    @SuppressWarnings("this-escape")
    public TabBarImpl(final ComponentFactory ui) {
        super(ui);
        this.ui = ui;
        this.views = new ArrayList<>();
        this.selectedIndex = -1;
    }

    @Override
    public List<Tab> tabs() {
        return this.views.stream().<Tab>map(view -> view.tab).toList();
    }

    @Override
    public TabBar addTab(final Tab tab) {
        return this.addTab(this.views.size(), tab);
    }

    @Override
    public TabBar addTab(final int index, final Tab tab) {
        if (!(tab instanceof TabImpl impl)) {
            throw new IllegalArgumentException(tab + " is not a Strata tab");
        }
        Objects.checkIndex(index, this.views.size() + 1);
        impl.setBar(this);
        final TabView view = new TabView(this.ui, this, impl);
        this.views.add(index, view);
        this.addChild(view);
        if (this.selectedIndex < 0 && this.views.size() == 1) {
            this.selectedIndex = 0;
        } else if (this.selectedIndex >= index) {
            this.selectedIndex++;
            this.report(SELECTED, this.selectedIndex, this.selectedIndex - 1);
        }
        return this;
    }

    @Override
    public TabBar removeTab(final Tab tab) {
        final int index = this.indexOf(tab);
        if (index < 0) {
            return this;
        }
        final TabView view = this.views.remove(index);
        this.removeChild(view);
        view.tab.setBar(null);
        final int previous = this.selectedIndex;
        if (previous == index) {
            this.selectedIndex = Math.min(index, this.views.size() - 1);
            this.report(SELECTED, this.selectedIndex, previous);
        } else if (previous > index) {
            this.selectedIndex--;
            this.report(SELECTED, this.selectedIndex, previous);
        }
        return this;
    }

    @Override
    public TabBar addTab(final String text) {
        return this.addTab(new TabImpl(text));
    }

    private int indexOf(final Tab tab) {
        for (int i = 0; i < this.views.size(); i++) {
            if (this.views.get(i).tab == tab) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Called by a tab of this bar when it changed.
     */
    void tabChanged(final TabImpl tab) {
        final int index = this.indexOf(tab);
        if (index >= 0) {
            this.views.get(index).sync();
        }
    }

    @Override
    public int selectedIndex() {
        return this.selectedIndex;
    }

    @Override
    public Optional<Tab> selected() {
        return this.selectedIndex < 0 ? Optional.empty() : Optional.of(this.views.get(this.selectedIndex).tab);
    }

    @Override
    public TabBar selectItem(final Tab tab) {
        return this.select(this.indexOf(tab));
    }

    @Override
    public TabBar select(final int index) {
        if (index != -1) {
            Objects.checkIndex(index, this.views.size());
        }
        final int previous = this.selectedIndex;
        if (index != previous) {
            this.selectedIndex = index;
            this.report(SELECTED, index, previous);
        }
        return this;
    }

    /**
     * Selects a tab for the user through {@code SELECT}. The tab that is already selected skips the request
     * and changes nothing.
     */
    private void userSelect(final int index) {
        if (index != this.selectedIndex) {
            this.request(SELECT, index, this::applyUserSelect);
        }
    }

    /**
     * Selects the tab a {@link TabBar#SELECT} ended with, ignoring null, an index out of range or a tab that cannot
     * be selected.
     */
    private void applyUserSelect(final Integer index) {
        if (index != null && index >= 0 && index < this.views.size() && this.views.get(index).tab.isEnabled()) {
            this.select(index);
        }
    }

    @Override
    public float spacing() {
        return this.spacing;
    }

    @Override
    public TabBar setSpacing(final float spacing) {
        this.spacing = spacing;
        this.invalidateLayout();
        return this;
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        float width = 0f;
        float height = 0f;
        for (final TabView view : this.views) {
            final Size outer = context.measure(view, Float.POSITIVE_INFINITY, availableHeight);
            width += outer.width();
            height = Math.max(height, outer.height());
        }
        width += this.spacing * Math.max(0, this.views.size() - 1);
        return new Size(width, height);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final int count = this.views.size();
        if (count == 0) {
            return;
        }
        final float tabWidth = (bounds.width() - this.spacing * (count - 1)) / count;
        for (int i = 0; i < count; i++) {
            context.arrange(this.views.get(i), new Rect(bounds.x() + i * (tabWidth + this.spacing), bounds.y(), tabWidth,
                    bounds.height()));
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || this.views.isEmpty()) {
            return;
        }
        final int direction = switch (event.key()) {
            case LEFT -> -1;
            case RIGHT -> 1;
            default -> 0;
        };
        if (direction == 0) {
            return;
        }
        for (int i = this.selectedIndex + direction; i >= 0 && i < this.views.size(); i += direction) {
            if (this.views.get(i).tab.isEnabled()) {
                this.userSelect(i);
                break;
            }
        }
        event.consume();
    }

    /**
     * The component that draws one tab. It stretches over the slot the bar gives it.
     */
    private static final class TabView extends CustomComponent<TabView> implements HasCursor<TabView>,
            HasTooltip<TabView> {
        private final TabBarImpl bar;
        private final TabImpl tab;
        private Paint normal;
        private Paint selected;
        private Paint hovered;
        private Paint disabled;
        private TextStyle textStyle;
        private TextStyle selectedText;
        private TextStyle disabledText;
        private float lineHeight;
        private TextSpan span;
        private TextSpan selectedSpan;
        private TextSpan disabledSpan;
        private float textWidth;

        @SuppressWarnings("this-escape")
        TabView(final ComponentFactory ui, final TabBarImpl bar, final TabImpl tab) {
            super(ui);
            this.bar = bar;
            this.tab = tab;
            this.normal = Paint.NONE;
            this.selected = Paint.NONE;
            this.hovered = Paint.NONE;
            this.disabled = Paint.NONE;
            this.textStyle = TextStyle.DEFAULT;
            this.selectedText = TextStyle.DEFAULT;
            this.disabledText = TextStyle.DEFAULT;
            this.span = TextSpan.of(tab.text());
            this.selectedSpan = this.span;
            this.disabledSpan = this.span;
            this.setAnchor(Anchor.FILL);
            this.sync();
        }

        /**
         * Takes the tab's cursor and tooltip and lays out again, after the tab changed.
         */
        void sync() {
            this.setCursor(this.tab.isEnabled() ? CursorKind.HAND : null);
            this.setTooltip(this.tab.tooltip().orElse(null));
            this.invalidateLayout();
        }

        private int index() {
            return this.bar.views.indexOf(this);
        }

        @Override
        protected void onApplyTheme(final Theme theme) {
            this.normal = theme.paintOr(STYLE_NORMAL, Paint.NONE);
            this.selected = theme.paintOr(STYLE_SELECTED, this.normal);
            this.hovered = theme.paintOr(STYLE_HOVERED, this.normal);
            this.disabled = theme.paintOr(STYLE_DISABLED, this.normal);
            this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
            this.selectedText = theme.textStyleOr(STYLE_TEXT_SELECTED, this.textStyle);
            this.disabledText = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final TextMetrics metrics = context.text();
            final String text = this.tab.text();
            this.span = new TextSpan(text, this.textStyle);
            this.selectedSpan = new TextSpan(text, this.selectedText);
            this.disabledSpan = new TextSpan(text, this.disabledText);
            this.textWidth = Math.max(metrics.width(this.span), metrics.width(this.selectedSpan));
            this.lineHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.selectedText));
            return new Size(this.textWidth + TAB_PADDING * 2f, this.lineHeight + 7f);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            final boolean isSelected = this.index() == this.bar.selectedIndex;
            final Paint face;
            final TextSpan caption;
            if (isSelected) {
                face = this.selected;
                caption = this.selectedSpan;
            } else if (!this.tab.isEnabled()) {
                face = this.disabled;
                caption = this.disabledSpan;
            } else {
                face = this.isHovered() ? this.hovered : this.normal;
                caption = this.span;
            }
            face.paint(painter, bounds);
            painter.text(caption, bounds.x() + (bounds.width() - this.textWidth) * 0.5f,
                    bounds.y() + (bounds.height() - this.lineHeight) * 0.5f + 1f);
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT && this.tab.isEnabled()) {
                this.bar.userSelect(this.index());
                event.consume();
            }
        }
    }

}
