package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.text.RichText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws a text toast: the message wrapped at a third of the width offered, inside {@link #PADDING} units of padding.
 * It uses the toast keys when the theme has them and the tooltip keys otherwise, and without either background a
 * dark box with a light one-unit edge.
 */
final class ToastView extends CustomComponent<ToastView> {
    /**
     * The space between the text and the box's edge, in UI units.
     */
    static final float PADDING = 6f;
    private static final Paint FALLBACK_BACKGROUND = Paint.solid(0xF0100010);
    private static final Border FALLBACK_EDGE = Border.of(0x805000FF, 1f);

    private final String message;
    private final List<List<TextSpan>> lines;
    private TextStyle textStyle;
    private float lineHeight;

    ToastView(final ComponentFactory factory, final String message) {
        super(factory);
        this.message = message;
        this.lines = new ArrayList<>();
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
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.lines.clear();
        final float wrapAt = Float.isInfinite(availableWidth) ? availableWidth : Math.max(1f, availableWidth / 3f - PADDING * 2f);
        this.lines.addAll(RichText.wrap(List.of(new TextSpan(this.message, this.textStyle)), wrapAt, metrics));
        this.lineHeight = metrics.lineHeight(this.textStyle);
        float width = 0f;
        for (final List<TextSpan> line : this.lines) {
            width = Math.max(width, RichText.width(line, metrics));
        }
        return new Size(width + PADDING * 2f, this.lines.size() * this.lineHeight + PADDING * 2f);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final TextMetrics metrics = this.environment().text();
        float y = bounds.y() + PADDING;
        for (final List<TextSpan> line : this.lines) {
            float x = bounds.x() + PADDING;
            for (final TextSpan span : line) {
                painter.text(span, x, y);
                x += metrics.width(span);
            }
            y += this.lineHeight;
        }
    }

    @Override
    public String toString() {
        return "ToastView[" + this.message + "]";
    }
}
