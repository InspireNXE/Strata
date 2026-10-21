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
package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.text.InlineMetrics;
import org.inspirenxe.strata.core.text.RichText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws a {@link Tooltip.Lines} tooltip: lines of spans one line height plus one unit apart, inside the theme's
 * tooltip padding or {@link #PADDING}, on the theme's tooltip background and frame, both drawn over the box grown by
 * the theme's tooltip outset. Without a background key it draws a dark box with a purple one-unit edge.
 */
final class TooltipView extends BuiltInComponent<TooltipView> {
    /**
     * The space between the text and the box's edge, in UI units.
     */
    static final Insets PADDING = Insets.of(4f);
    /**
     * Extra space between lines, in UI units.
     */
    static final float LINE_GAP = 1f;
    private static final Paint FALLBACK_BACKGROUND = Paint.solid(0xF0100010);
    private static final Border FALLBACK_EDGE = Border.of(0x805000FF, 1f);

    private final Tooltip tooltip;
    private final List<List<TextSpan>> lines;
    private final List<Float> lineHeights;
    private final List<Float> textHeights;
    private TextStyle textStyle;
    private Insets padding = PADDING;
    private Paint background = Paint.NONE;
    private Paint frame = Paint.NONE;
    private Insets outset = Insets.NONE;

    TooltipView(final ComponentFactoryImpl factory, final Tooltip tooltip) {
        super(factory);
        this.tooltip = tooltip;
        this.lines = new ArrayList<>();
        this.lineHeights = new ArrayList<>();
        this.textHeights = new ArrayList<>();
        this.textStyle = TextStyle.DEFAULT;
        this.setBackground(FALLBACK_BACKGROUND);
        this.setBorder(FALLBACK_EDGE);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        final Optional<Paint> themed = theme.paint(Tooltip.STYLE_BACKGROUND);
        // A themed background is drawn here rather than as the container background, so it can reach past the box
        this.setBackground(themed.isPresent() ? Paint.NONE : FALLBACK_BACKGROUND);
        this.setBorder(themed.isPresent() ? Border.NONE : FALLBACK_EDGE);
        this.background = themed.orElse(Paint.NONE);
        this.frame = theme.paintOr(Tooltip.STYLE_FRAME, Paint.NONE);
        this.outset = theme.insetsOr(Tooltip.STYLE_OUTSET, Insets.NONE);
        this.textStyle = theme.textStyleOr(Tooltip.STYLE_TEXT, TextStyle.DEFAULT);
        this.padding = theme.insetsOr(Tooltip.STYLE_PADDING, PADDING);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.lines.clear();
        this.lineHeights.clear();
        this.textHeights.clear();
        if (this.tooltip instanceof final Tooltip.Lines lines) {
            for (final Text text : lines.lines()) {
                this.lines.addAll(RichText.wrap(text.resolve(this.textStyle), Float.POSITIVE_INFINITY, metrics));
            }
        }
        float width = 0f;
        float height = 0f;
        for (final List<TextSpan> line : this.lines) {
            width = Math.max(width, RichText.width(line, metrics));
            final InlineMetrics inline = InlineMetrics.of(metrics);
            final float lineHeight = inline.lineHeight(line, this.textStyle);
            this.lineHeights.add(lineHeight);
            this.textHeights.add(inline.textHeight(line, this.textStyle));
            height += lineHeight;
        }
        height += LINE_GAP * Math.max(0, this.lines.size() - 1);
        return new Size(width + this.padding.horizontal(), height + this.padding.vertical());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.background != Paint.NONE || this.frame != Paint.NONE) {
            final Rect area = bounds.grow(this.outset);
            this.background.paint(painter, area);
            this.frame.paint(painter, area);
        }
        final InlineMetrics metrics = InlineMetrics.of(this.environment().text());
        float y = bounds.y() + this.padding.top();
        for (int i = 0; i < this.lines.size(); i++) {
            metrics.draw(painter, this.lines.get(i), bounds.x() + this.padding.left(), y, this.lineHeights.get(i),
                    this.textHeights.get(i));
            y += this.lineHeights.get(i) + LINE_GAP;
        }
    }

    @Override
    public String toString() {
        return "TooltipView[" + this.tooltip + "]";
    }
}
