package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
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
 * The core {@link TabBar}. Each tab is a private child component, so it can have its own hover and tooltip. A tab
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
    private final List<Tab> tabs;
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
        this.tabs = new ArrayList<>();
        this.selectedIndex = -1;
        this.setFocusable(true);
        this.setTabbable(true);
    }

    @Override
    public TabBar addTab(final String text) {
        final Tab tab = new Tab(this.ui, this, Objects.requireNonNull(text, "text"));
        this.tabs.add(tab);
        this.addChild(tab);
        if (this.selectedIndex < 0) {
            this.selectedIndex = 0;
        }
        return this;
    }

    @Override
    public int tabCount() {
        return this.tabs.size();
    }

    @Override
    public String tabText(final int index) {
        return this.tabs.get(index).text;
    }

    @Override
    public TabBar setTabText(final int index, final String text) {
        this.tabs.get(index).text = Objects.requireNonNull(text, "text");
        this.tabs.get(index).invalidateLayout();
        return this;
    }

    @Override
    public boolean isTabEnabled(final int index) {
        return this.tabs.get(index).selectable;
    }

    @Override
    public TabBar setTabEnabled(final int index, final boolean enabled) {
        final Tab tab = this.tabs.get(index);
        tab.selectable = enabled;
        tab.setCursor(enabled ? CursorKind.HAND : null);
        return this;
    }

    @Override
    public TabBar setTabTooltip(final int index, final Tooltip tooltip) {
        this.tabs.get(index).setTooltip(tooltip);
        return this;
    }

    @Override
    public int selectedIndex() {
        return this.selectedIndex;
    }

    @Override
    public TabBar select(final int index) {
        Objects.checkIndex(index, this.tabs.size());
        final int previous = this.selectedIndex;
        if (index != previous) {
            this.selectedIndex = index;
            this.report(SELECTED, index, previous);
        }
        return this;
    }

    /**
     * Selects a tab for the user through {@link TabBar#SELECT}. The tab that is already selected skips the request
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
        if (index != null && index >= 0 && index < this.tabs.size() && this.tabs.get(index).selectable) {
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
        for (final Tab tab : this.tabs) {
            final Size outer = context.measure(tab, Float.POSITIVE_INFINITY, availableHeight);
            width += outer.width();
            height = Math.max(height, outer.height());
        }
        width += this.spacing * Math.max(0, this.tabs.size() - 1);
        return new Size(width, height);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final int count = this.tabs.size();
        if (count == 0) {
            return;
        }
        final float tabWidth = (bounds.width() - this.spacing * (count - 1)) / count;
        for (int i = 0; i < count; i++) {
            context.arrange(this.tabs.get(i), new Rect(bounds.x() + i * (tabWidth + this.spacing), bounds.y(), tabWidth,
                    bounds.height()));
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || this.tabs.isEmpty()) {
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
        for (int i = this.selectedIndex + direction; i >= 0 && i < this.tabs.size(); i += direction) {
            if (this.tabs.get(i).selectable) {
                this.userSelect(i);
                break;
            }
        }
        event.consume();
    }

    /**
     * One tab. It stretches over the slot the bar gives it.
     */
    private static final class Tab extends CustomComponent<Tab> {
        private final TabBarImpl bar;
        private String text;
        private boolean selectable;
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
        Tab(final ComponentFactory ui, final TabBarImpl bar, final String text) {
            super(ui);
            this.bar = bar;
            this.text = text;
            this.selectable = true;
            this.normal = Paint.NONE;
            this.selected = Paint.NONE;
            this.hovered = Paint.NONE;
            this.disabled = Paint.NONE;
            this.textStyle = TextStyle.DEFAULT;
            this.selectedText = TextStyle.DEFAULT;
            this.disabledText = TextStyle.DEFAULT;
            this.span = TextSpan.of(text);
            this.selectedSpan = this.span;
            this.disabledSpan = this.span;
            this.setCursor(CursorKind.HAND);
            this.setAnchor(Anchor.FILL);
        }

        private int index() {
            return this.bar.tabs.indexOf(this);
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
            this.span = new TextSpan(this.text, this.textStyle);
            this.selectedSpan = new TextSpan(this.text, this.selectedText);
            this.disabledSpan = new TextSpan(this.text, this.disabledText);
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
            } else if (!this.selectable) {
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
            if (event.button() == MouseButton.LEFT && this.selectable) {
                this.bar.userSelect(this.index());
                event.consume();
            }
        }
    }

}
