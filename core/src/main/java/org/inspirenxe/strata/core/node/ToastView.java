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
import org.inspirenxe.strata.api.overlay.Overlays;
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
 * Draws a text toast: the message wrapped to fit a third of the width offered, inside the theme's toast padding or
 * {@link #PADDING}. Its background and text style come from the toast keys when the theme has them and from the
 * tooltip keys otherwise. Without either background it draws a dark box with a purple one-unit edge. With the
 * theme's toast title style the first line is drawn in it, lines are the theme's line gap apart, and the box is at
 * least the theme's least width.
 */
final class ToastView extends BuiltInComponent<ToastView> {
    /**
     * The space between the text and the box's edge, in UI units.
     */
    static final Insets PADDING = Insets.of(6f);
    private static final Paint FALLBACK_BACKGROUND = Paint.solid(0xF0100010);
    private static final Border FALLBACK_EDGE = Border.of(0x805000FF, 1f);

    private final Text message;
    private final List<List<TextSpan>> lines;
    private final List<Float> lineHeights;
    private TextStyle textStyle;
    // The height of the characters, which each line is at least as tall as
    private float lineHeight;
    private Insets padding = PADDING;
    private TextStyle titleStyle;
    private float lineGap;
    private float minimumWidth;

    ToastView(final ComponentFactoryImpl factory, final Text message) {
        super(factory);
        this.message = message;
        this.lines = new ArrayList<>();
        this.lineHeights = new ArrayList<>();
        this.textStyle = TextStyle.DEFAULT;
        this.setBackground(FALLBACK_BACKGROUND);
        this.setBorder(FALLBACK_EDGE);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        final Optional<Paint> themed = theme.paint(Overlays.STYLE_TOAST).or(() -> theme.paint(Tooltip.STYLE_BACKGROUND));
        this.setBackground(themed.orElse(FALLBACK_BACKGROUND));
        this.setBorder(themed.isPresent() ? Border.NONE : FALLBACK_EDGE);
        this.textStyle = theme.textStyleOr(Overlays.STYLE_TOAST_TEXT, theme.textStyleOr(Tooltip.STYLE_TEXT, TextStyle.DEFAULT));
        this.padding = theme.insetsOr(Overlays.STYLE_TOAST_PADDING, PADDING);
        this.titleStyle = theme.textStyle(Overlays.STYLE_TOAST_TITLE).orElse(null);
        this.lineGap = Math.max(0f, theme.metricOr(Overlays.STYLE_TOAST_LINE_GAP, 0f));
        this.minimumWidth = Math.max(0f, theme.metricOr(Overlays.STYLE_TOAST_WIDTH_MINIMUM, 0f));
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.lines.clear();
        final float wrapAt = Float.isInfinite(availableWidth) ? availableWidth : Math.max(1f, availableWidth / 3f - this.padding.horizontal());
        final List<TextSpan> spans = this.message.resolve(this.textStyle);
        this.lines.addAll(RichText.wrap(spans, wrapAt, metrics));
        if (this.titleStyle != null && !this.lines.isEmpty()) {
            final List<TextSpan> title = new ArrayList<>();
            for (final TextSpan span : this.lines.get(0)) {
                title.add(new TextSpan(span.text(), this.titleStyle));
            }
            this.lines.set(0, title);
        }
        final InlineMetrics inline = InlineMetrics.of(metrics);
        this.lineHeight = inline.textHeight(spans, this.textStyle);
        this.lineHeights.clear();
        float width = 0f;
        float height = 0f;
        for (final List<TextSpan> line : this.lines) {
            width = Math.max(width, RichText.width(line, metrics));
            final float lineHeight = Math.max(this.lineHeight, inline.lineHeight(line, this.textStyle));
            this.lineHeights.add(lineHeight);
            height += lineHeight;
        }
        height += this.lineGap * Math.max(0, this.lines.size() - 1);
        return new Size(Math.max(this.minimumWidth, width + this.padding.horizontal()), height + this.padding.vertical());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final InlineMetrics metrics = InlineMetrics.of(this.environment().text());
        float y = bounds.y() + this.padding.top();
        for (int i = 0; i < this.lines.size(); i++) {
            final float lineHeight = this.lineHeights.get(i);
            metrics.draw(painter, this.lines.get(i), bounds.x() + this.padding.left(), y, lineHeight, this.lineHeight);
            y += lineHeight + this.lineGap;
        }
    }

    @Override
    public String toString() {
        return "ToastView[" + this.message.plain() + "]";
    }
}
