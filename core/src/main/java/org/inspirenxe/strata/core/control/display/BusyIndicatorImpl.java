package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link BusyIndicator}. The lit dot follows the frames 0, 1, 2, 1 and repeats, one frame every
 * {@link BusyIndicator#FRAME_MILLIS} milliseconds of the environment's clock. The caption is measured during layout
 * and the dots' rectangles are worked out again only when the bounds change, so painting allocates nothing.
 */
public final class BusyIndicatorImpl extends CustomComponent<BusyIndicator> implements BusyIndicator {
    private static final Set<String> REQUIRED = Set.of(STYLE_DOT, STYLE_DOT_ACTIVE);
    private static final int[] FRAMES = {0, 1, 2, 1};
    private static final int DOT_COUNT = 3;
    private static final float DOTS_WIDTH = DOT_COUNT * DOT_SIZE + (DOT_COUNT - 1) * DOT_GAP;

    private final Rect[] dots;
    private Rect dotBounds;
    private String text;
    private Paint dotPaint;
    private Paint activePaint;
    private TextStyle textStyle;
    private TextSpan span;
    private float textWidth;
    private float lineHeight;

    /**
     * Creates an indicator with no caption.
     *
     * @param ui the factory that owns the indicator
     */
    public BusyIndicatorImpl(final ComponentFactory ui) {
        super(ui);
        this.dots = new Rect[DOT_COUNT];
        this.dotBounds = null;
        this.text = "";
        this.dotPaint = Paint.NONE;
        this.activePaint = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.span = TextSpan.of("");
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public BusyIndicator setText(final String text) {
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
        this.dotPaint = theme.paintOr(STYLE_DOT, Paint.NONE);
        this.activePaint = theme.paintOr(STYLE_DOT_ACTIVE, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        if (this.text.isEmpty()) {
            this.textWidth = 0f;
            this.lineHeight = 0f;
            return new Size(DOTS_WIDTH, DOT_SIZE);
        }
        final TextMetrics metrics = context.text();
        this.span = new TextSpan(this.text, this.textStyle);
        this.textWidth = metrics.width(this.span);
        this.lineHeight = metrics.lineHeight(this.textStyle);
        return new Size(Math.max(DOTS_WIDTH, this.textWidth), this.lineHeight + DOT_GAP + DOT_SIZE);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (!bounds.equals(this.dotBounds)) {
            final float left = bounds.x() + (bounds.width() - DOTS_WIDTH) * 0.5f;
            final float top = bounds.bottom() - DOT_SIZE;
            for (int i = 0; i < DOT_COUNT; i++) {
                this.dots[i] = new Rect(left + i * (DOT_SIZE + DOT_GAP), top, DOT_SIZE, DOT_SIZE);
            }
            this.dotBounds = bounds;
        }
        final long step = Math.floorDiv(this.environment().millis(), FRAME_MILLIS);
        final int lit = FRAMES[(int) Math.floorMod(step, (long) FRAMES.length)];
        for (int i = 0; i < DOT_COUNT; i++) {
            (i == lit ? this.activePaint : this.dotPaint).paint(painter, this.dots[i]);
        }
        if (!this.text.isEmpty()) {
            painter.text(this.span, bounds.x() + (bounds.width() - this.textWidth) * 0.5f,
                    bounds.bottom() - DOT_SIZE - DOT_GAP - this.lineHeight);
        }
    }
}
