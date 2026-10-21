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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.pane.Tab;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Caption;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The core {@link TabBar}. Each {@link TabImpl} is drawn by a private child component, so it can have its own hover
 * and tooltip. A tab asks for its text plus the larger of its normal and selected padding, so selecting it never moves
 * its neighbors. Tabs stay enabled as components even when they cannot be selected, so a disabled tab still shows its
 * tooltip.
 */
public final class TabBarImpl extends BuiltInComposite<TabBar> implements TabBar {
    /**
     * The space around a tab's text, in UI units, when the theme sets no {@value #STYLE_PADDING}: 8 at the sides,
     * 4 at the top and 3 at the bottom.
     */
    public static final Insets DEFAULT_PADDING = new Insets(8f, 4f, 8f, 3f);
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_SELECTED);

    private final ComponentFactoryImpl ui;
    private final List<TabView> views;
    private int selectedIndex;
    private float spacing;
    // True while the keyboard brought or kept focus here, which lights the selected tab
    private boolean keyboardFocus;

    /**
     * Creates a bar with no tabs.
     *
     * @param ui the factory that owns the bar
     */
    @SuppressWarnings("this-escape")
    public TabBarImpl(final ComponentFactoryImpl ui) {
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
    public TabBar addTab(final Text text) {
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
        final float room = bounds.width() - this.spacing * (count - 1);
        // Whole widths for a whole room, so tab edges and centered labels land on whole units
        final boolean whole = room == Math.rint(room);
        final float tabWidth = whole ? (float) Math.floor(room / count) : room / count;
        final int spare = whole ? (int) (room - tabWidth * count) : 0;
        float x = bounds.x();
        for (int i = 0; i < count; i++) {
            final float width = tabWidth + (i < spare ? 1f : 0f);
            context.arrange(this.views.get(i), new Rect(x, bounds.y(), width, bounds.height()));
            x += width + this.spacing;
        }
    }

    @Override
    protected void onFocusGained(final FocusEvent event) {
        this.keyboardFocus = !this.isFocusFromPress();
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || this.views.isEmpty()) {
            return;
        }
        this.keyboardFocus = true;
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
    private static final class TabView extends BuiltInComponent<TabView> implements HasCursor<TabView>,
            HasTooltip<TabView> {
        private final TabBarImpl bar;
        private final TabImpl tab;
        private Paint normal;
        private Paint selected;
        private Paint hovered;
        private Paint disabled;
        private Paint selectedDisabled;
        private Paint selectedHovered;
        private Paint selectedFill;
        private Insets selectedFillInsets;
        private Paint underline;
        private Paint underlineDisabled;
        private final Caption caption;
        private TextStyle textStyle;
        private TextStyle selectedText;
        private TextStyle disabledText;
        private Insets padding;
        private Insets selectedPadding;
        private float lineHeight;
        private TextLine span;
        private TextLine selectedSpan;
        private TextLine disabledSpan;
        private float textWidth;

        @SuppressWarnings("this-escape")
        TabView(final ComponentFactoryImpl ui, final TabBarImpl bar, final TabImpl tab) {
            super(ui);
            this.bar = bar;
            this.tab = tab;
            this.normal = Paint.NONE;
            this.selected = Paint.NONE;
            this.hovered = Paint.NONE;
            this.disabled = Paint.NONE;
            this.selectedDisabled = Paint.NONE;
            this.selectedHovered = Paint.NONE;
            this.selectedFill = Paint.NONE;
            this.selectedFillInsets = Insets.NONE;
            this.underline = Paint.NONE;
            this.underlineDisabled = Paint.NONE;
            this.caption = new Caption();
            this.textStyle = TextStyle.DEFAULT;
            this.selectedText = TextStyle.DEFAULT;
            this.disabledText = TextStyle.DEFAULT;
            this.span = TextLine.EMPTY;
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
            this.selectedDisabled = theme.paintOr(STYLE_SELECTED_DISABLED, this.disabled);
            this.selectedHovered = theme.paintOr(STYLE_SELECTED_HOVERED, this.selected);
            this.selectedFill = theme.paintOr(STYLE_SELECTED_FILL, Paint.NONE);
            this.selectedFillInsets = theme.insetsOr(STYLE_SELECTED_FILL_INSETS, Insets.NONE);
            this.underline = theme.paintOr(STYLE_UNDERLINE, Paint.NONE);
            this.underlineDisabled = theme.paintOr(STYLE_UNDERLINE_DISABLED, this.underline);
            this.caption.applyTheme(theme, STYLE_CLIP, STYLE_TEXT_SCROLL, STYLE_TEXT_SCROLL_MINIMUM);
            this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
            this.selectedText = theme.textStyleOr(STYLE_TEXT_SELECTED, this.textStyle);
            this.disabledText = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
            this.padding = theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING);
            this.selectedPadding = theme.insetsOr(STYLE_PADDING_SELECTED, this.padding);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final TextMetrics metrics = context.text();
            final Text text = this.tab.text();
            this.span = TextLine.of(text, this.textStyle, metrics);
            this.selectedSpan = TextLine.of(text, this.selectedText, metrics);
            this.disabledSpan = TextLine.of(text, this.disabledText, metrics);
            this.textWidth = Math.max(this.span.width(), this.selectedSpan.width());
            this.lineHeight = Math.max(this.span.height(), this.selectedSpan.height());
            // Sized for the larger padding, so selecting a tab never moves its neighbors
            final float sides = Math.max(this.padding.left() + this.padding.right(),
                    this.selectedPadding.left() + this.selectedPadding.right());
            final float ends = Math.max(this.padding.top() + this.padding.bottom(),
                    this.selectedPadding.top() + this.selectedPadding.bottom());
            return new Size(this.textWidth + sides, this.lineHeight + ends);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            final boolean isSelected = this.index() == this.bar.selectedIndex;
            // A disabled bar draws every tab disabled, keeping the selected one distinct
            final boolean enabled = this.tab.isEnabled() && this.bar.isEnabled();
            final Paint face;
            final TextLine caption;
            if (isSelected) {
                face = !enabled ? this.selectedDisabled
                        : this.isHovered() || this.bar.isFocused() && this.bar.keyboardFocus ? this.selectedHovered
                        : this.selected;
                caption = enabled ? this.selectedSpan : this.disabledSpan;
            } else if (!enabled) {
                face = this.disabled;
                caption = this.disabledSpan;
            } else {
                face = this.isHovered() ? this.hovered : this.normal;
                caption = this.span;
            }
            face.paint(painter, bounds);
            if (isSelected) {
                if (this.selectedFill != Paint.NONE) {
                    this.selectedFill.paint(painter, bounds.shrink(this.selectedFillInsets));
                }
                final Paint line = enabled ? this.underline : this.underlineDisabled;
                if (line != Paint.NONE) {
                    final float width = Math.max(0f, Math.min(caption.width(), bounds.width() - 4f));
                    // Half the spare room on the left, rounded down on whole units
                    final float spare = bounds.width() - width;
                    final float left = bounds.x() + (spare == Math.rint(spare) ? (float) Math.floor(spare / 2f) : spare / 2f);
                    line.paint(painter, new Rect(left,
                            bounds.bottom() - 2f, width, 1f));
                }
            }
            // Centered in the tab less its state's padding
            final Insets pad = isSelected ? this.selectedPadding : this.padding;
            this.caption.draw(painter, caption, bounds, pad,
                    bounds.y() + pad.top() + (bounds.height() - pad.top() - pad.bottom() - this.lineHeight) * 0.5f,
                    this.environment().millis());
        }

        @Override
        protected void onPointerPress(final PointerButtonEvent event) {
            if (this.activatesOnPress()) {
                this.clicked(event);
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (!this.activatesOnPress()) {
                this.clicked(event);
            }
        }

        /**
         * Acts on a pointer press or click, whichever the platform's conventions say buttons act on.
         */
        private void clicked(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT && this.tab.isEnabled()) {
                // A clicked tab takes the bar's focus, as a clicked button takes its own
                if (this.bar.isEnabled()) {
                    this.bar.requestFocus();
                    this.bar.keyboardFocus = false;
                }
                this.bar.userSelect(this.index());
                event.consume();
            }
        }
    }

}
