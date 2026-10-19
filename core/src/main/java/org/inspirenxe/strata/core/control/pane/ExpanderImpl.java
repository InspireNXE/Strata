package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The core {@link Expander}. The content is a child only while expanded. The expander shows the hand cursor while
 * the pointer is over the header, and no cursor of its own elsewhere.
 */
public final class ExpanderImpl extends CustomComposite<Expander> implements Expander {
    /**
     * The space at the left of the arrow and between the arrow and the title, in UI units.
     */
    public static final float PADDING = 4f;

    private final List<Consumer<Boolean>> listeners;
    private String title;
    private Component<?> content;
    private boolean expanded;
    private float headerHeight;
    private float pointerY;

    // Theme
    private Paint header;
    private Paint headerHovered;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;

    // Computed during measure
    private TextSpan span;
    private TextSpan disabledSpan;
    private float lineHeight;

    /**
     * Creates a collapsed expander with no content.
     *
     * @param ui the factory that owns the expander
     * @param title the header's title
     */
    @SuppressWarnings("this-escape")
    public ExpanderImpl(final ComponentFactory ui, final String title) {
        super(ui);
        this.listeners = new ArrayList<>();
        this.title = Objects.requireNonNull(title, "title");
        this.header = Paint.NONE;
        this.headerHovered = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.span = TextSpan.of(title);
        this.disabledSpan = this.span;
        this.pointerY = Float.NaN;
        this.setFocusable(true);
        this.setTabbable(true);
    }

    @Override
    public String title() {
        return this.title;
    }

    @Override
    public Expander setTitle(final String title) {
        this.title = Objects.requireNonNull(title, "title");
        this.invalidateLayout();
        return this;
    }

    @Override
    public Component<?> content() {
        return this.content;
    }

    @Override
    public Expander setContent(final Component<?> content) {
        if (content != null && content.parent() != null && content != this.content) {
            throw new IllegalArgumentException(content + " already has a parent");
        }
        if (this.content != null && this.expanded) {
            this.removeChild(this.content);
        }
        this.content = content;
        if (content != null && this.expanded) {
            this.addChild(content);
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isExpanded() {
        return this.expanded;
    }

    @Override
    public Expander setExpanded(final boolean expanded) {
        if (this.expanded == expanded) {
            return this;
        }
        this.expanded = expanded;
        if (this.content != null) {
            if (expanded) {
                this.addChild(this.content);
            } else {
                this.removeChild(this.content);
            }
        }
        this.invalidateLayout();
        for (final Consumer<Boolean> listener : List.copyOf(this.listeners)) {
            listener.accept(expanded);
        }
        return this;
    }

    @Override
    public void toggle() {
        if (this.isEnabled()) {
            this.setExpanded(!this.expanded);
        }
    }

    @Override
    public Expander onExpandedChanged(final Consumer<Boolean> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.header = theme.paintOr(STYLE_HEADER, Paint.NONE);
        this.headerHovered = theme.paintOr(STYLE_HEADER_HOVERED, this.header);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.span = new TextSpan(this.title, this.textStyle);
        this.disabledSpan = new TextSpan(this.title, this.disabledTextStyle);
        this.lineHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        this.headerHeight = this.lineHeight + 6f;
        final float headerWidth = PADDING + Rings.ARROW_LONG + PADDING
                + Math.max(metrics.width(this.span), metrics.width(this.disabledSpan)) + PADDING;
        if (!this.expanded || this.content == null) {
            return new Size(headerWidth, this.headerHeight);
        }
        final float below = Float.isInfinite(availableHeight) ? availableHeight : Math.max(0f, availableHeight - this.headerHeight);
        final Size inner = context.measure(this.content, availableWidth, below);
        return new Size(Math.max(headerWidth, inner.width()), this.headerHeight + inner.height());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        if (this.expanded && this.content != null) {
            context.arrange(this.content, new Rect(bounds.x(), bounds.y() + this.headerHeight, bounds.width(),
                    Math.max(0f, bounds.height() - this.headerHeight)));
        }
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final Rect head = new Rect(bounds.x(), bounds.y(), bounds.width(), this.headerHeight);
        final boolean lit = this.isFocused() || this.isHovered() && this.overHeader(this.pointerY);
        (lit ? this.headerHovered : this.header).paint(painter, head);
        final boolean enabled = this.isEnabled();
        final int color = (enabled ? this.textStyle : this.disabledTextStyle).color();
        final float arrowX = head.x() + PADDING;
        if (this.expanded) {
            Rings.down(painter, color, arrowX, head.y() + (head.height() - Rings.ARROW_SHORT) * 0.5f);
        } else {
            Rings.right(painter, color, arrowX + (Rings.ARROW_LONG - Rings.ARROW_SHORT) * 0.5f,
                    head.y() + (head.height() - Rings.ARROW_LONG) * 0.5f);
        }
        painter.text(enabled ? this.span : this.disabledSpan, arrowX + Rings.ARROW_LONG + PADDING,
                head.y() + (head.height() - this.lineHeight) * 0.5f);
    }

    private boolean overHeader(final float y) {
        return y >= this.bounds().y() && y < this.bounds().y() + this.headerHeight;
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.pointerY = event.y();
        this.setCursor(this.overHeader(event.y()) ? CursorKind.HAND : null);
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() == MouseButton.LEFT && this.overHeader(event.y())) {
            this.toggle();
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        // Keys bubble up from the content; only the header's own keys flip it
        if (event.target() == this && event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
            this.toggle();
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new Expander.State(this.expanded, this.title);
    }
}
