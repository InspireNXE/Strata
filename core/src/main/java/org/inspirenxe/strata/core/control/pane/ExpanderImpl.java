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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComposite;
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
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;

/**
 * The core {@link Expander}. The content is a child only while expanded. The expander shows the hand cursor while
 * the pointer is over the header, and no cursor of its own elsewhere.
 */
public final class ExpanderImpl extends CustomComposite<Expander> implements Expander {
    /**
     * The space around the header's arrow and title when the theme sets no {@value #STYLE_HEADER_PADDING}: 4 at
     * the sides and 3 at the top and bottom.
     */
    public static final Insets DEFAULT_HEADER_PADDING = Insets.of(4f, 3f);
    /**
     * The space between the arrow and the title, in UI units.
     */
    public static final float ARROW_GAP = 4f;

    private Insets headerPadding = DEFAULT_HEADER_PADDING;

    private Text title;
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
    private TextLine span;
    private TextLine disabledSpan;
    private float lineHeight;

    /**
     * Creates a collapsed expander with no content.
     *
     * @param ui the factory that owns the expander
     * @param title the header's title
     */
    @SuppressWarnings("this-escape")
    public ExpanderImpl(final ComponentFactory ui, final Text title) {
        super(ui);
        this.title = Objects.requireNonNull(title, "title");
        this.header = Paint.NONE;
        this.headerHovered = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.span = TextLine.EMPTY;
        this.disabledSpan = this.span;
        this.pointerY = Float.NaN;
    }

    @Override
    public Text title() {
        return this.title;
    }

    @Override
    public Expander setTitle(final Text title) {
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
            this.addContent(content);
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
                this.addContent(this.content);
            } else {
                this.removeChild(this.content);
            }
        }
        this.invalidateLayout();
        this.report(EXPANDED, expanded, !expanded);
        return this;
    }

    @Override
    public void toggle() {
        if (this.isEnabled()) {
            this.setExpanded(!this.expanded);
        }
    }

    /**
     * Expands or collapses for the user through {@link Expander#EXPAND}, applying the final value unless it is null.
     * Does nothing while disabled.
     */
    private void userToggle() {
        if (this.isEnabled()) {
            this.request(EXPAND, !this.expanded, expanded -> {
                if (expanded != null) {
                    this.setExpanded(expanded);
                }
            });
        }
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.header = theme.paintOr(STYLE_HEADER, Paint.NONE);
        this.headerHovered = theme.paintOr(STYLE_HEADER_HOVERED, this.header);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.headerPadding = theme.insetsOr(STYLE_HEADER_PADDING, DEFAULT_HEADER_PADDING);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.span = TextLine.of(this.title, this.textStyle, metrics);
        this.disabledSpan = TextLine.of(this.title, this.disabledTextStyle, metrics);
        this.lineHeight = Math.max(this.span.height(), this.disabledSpan.height());
        this.headerHeight = this.lineHeight + this.headerPadding.vertical();
        final float headerWidth = this.headerPadding.left() + Rings.ARROW_LONG + ARROW_GAP
                + Math.max(this.span.width(), this.disabledSpan.width()) + this.headerPadding.right();
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
        final Insets padding = this.headerPadding;
        final float innerY = head.y() + padding.top();
        final float innerHeight = head.height() - padding.vertical();
        final float arrowX = head.x() + padding.left();
        if (this.expanded) {
            Rings.down(painter, color, arrowX, innerY + (innerHeight - Rings.ARROW_SHORT) * 0.5f);
        } else {
            Rings.right(painter, color, arrowX + (Rings.ARROW_LONG - Rings.ARROW_SHORT) * 0.5f,
                    innerY + (innerHeight - Rings.ARROW_LONG) * 0.5f);
        }
        (enabled ? this.span : this.disabledSpan).draw(painter, arrowX + Rings.ARROW_LONG + ARROW_GAP,
                innerY + (innerHeight - this.lineHeight) * 0.5f);
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
            this.userToggle();
            event.consume();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        // Keys bubble up from the content; only the header's own keys flip it
        if (event.target() == this && event.action() == KeyAction.PRESS && (event.key().isEnter() || event.key() == Key.SPACE)) {
            this.userToggle();
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new Expander.State(this.expanded, this.title);
    }
}
