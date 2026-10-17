package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.ProgressBar;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link ProgressBar}. The fill covers the value's share of the bounds' width from the left edge, over the
 * whole height.
 */
public final class ProgressBarImpl extends CustomComponent<ProgressBar> implements ProgressBar {
    /**
     * The width the bar asks for, in UI units.
     */
    public static final float DEFAULT_WIDTH = 100f;
    private static final Set<String> REQUIRED = Set.of(STYLE_TRACK, STYLE_FILL);

    private float value;
    private String text;
    private Paint trackPaint;
    private Paint fillPaint;
    private TextStyle textStyle;
    private TextSpan span;
    private float textWidth;
    private float lineHeight;

    /**
     * Creates an empty bar with no caption.
     *
     * @param ui the factory that owns the bar
     */
    public ProgressBarImpl(final ComponentFactory ui) {
        super(ui);
        this.text = "";
        this.trackPaint = Paint.NONE;
        this.fillPaint = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.span = TextSpan.of("");
    }

    @Override
    public float value() {
        return this.value;
    }

    @Override
    public ProgressBar setValue(final float value) {
        this.value = Float.isNaN(value) ? 0f : Math.max(0f, Math.min(1f, value));
        return this;
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public ProgressBar setText(final String text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.trackPaint = theme.paintOr(STYLE_TRACK, Paint.NONE);
        this.fillPaint = theme.paintOr(STYLE_FILL, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.span = new TextSpan(this.text, this.textStyle);
        this.textWidth = metrics.width(this.span);
        this.lineHeight = metrics.lineHeight(this.textStyle);
        return new Size(Math.max(DEFAULT_WIDTH, this.textWidth + 4f), this.lineHeight + 4f);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.trackPaint.paint(painter, bounds);
        if (this.value > 0f) {
            this.fillPaint.paint(painter, new Rect(bounds.x(), bounds.y(), bounds.width() * this.value, bounds.height()));
        }
        if (!this.text.isEmpty()) {
            painter.text(this.span, bounds.x() + (bounds.width() - this.textWidth) * 0.5f,
                    bounds.y() + (bounds.height() - this.lineHeight) * 0.5f);
        }
    }
}
