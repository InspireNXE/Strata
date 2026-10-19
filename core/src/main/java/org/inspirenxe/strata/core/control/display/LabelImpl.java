package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.LabelBehavior;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.text.RichText;
import org.inspirenxe.strata.core.text.TextWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The core {@link Label}. Measuring breaks the text into lines with {@link RichText} (plain text wraps exactly as
 * {@link TextWrap} wraps it), at the width the parent offers when wrapping is on, and keeps them for painting. Each
 * line is aligned on its own within the bounds. The line height is the tallest of the styles in use.
 *
 * <p>With {@link TextOverflow#ELLIPSIS}, the shortened lines are worked out when the bounds' width changes and kept
 * until the next change, so painting allocates nothing in between.</p>
 */
public final class LabelImpl extends CustomComponent<Label> implements Label {
    private String text;
    private List<TextSpan> spans;
    private TextStyle explicitStyle;
    private TextStyle themeStyle;
    private boolean wrap;
    private HorizontalAlignment textAlignment;
    private float textScale;
    private TextOverflow overflow;

    // Computed during measure, drawn during paint
    private final List<List<TextSpan>> lines;
    private final List<Float> lineWidths;
    private float lineHeight;

    // Shortened lines for the last bounds width
    private final List<List<TextSpan>> shownLines;
    private final List<Float> shownWidths;
    private float shownForWidth;

    // Set only while paint runs, for drawLines
    private final Runnable drawLines;
    private Rect paintBounds;
    private Painter painter;

    /**
     * Creates a label that does not wrap and aligns its text to the left.
     *
     * @param ui the factory that owns the label
     * @param text the text, with {@code \n} for line breaks
     * @param behavior the decorator for the label's hooks, or {@code null} for none
     */
    public LabelImpl(final ComponentFactory ui, final String text, final LabelBehavior behavior) {
        super(ui, behavior);
        this.text = Objects.requireNonNull(text, "text");
        this.spans = List.of();
        this.themeStyle = TextStyle.DEFAULT;
        this.textAlignment = HorizontalAlignment.LEFT;
        this.textScale = 1f;
        this.overflow = TextOverflow.VISIBLE;
        this.lines = new ArrayList<>();
        this.lineWidths = new ArrayList<>();
        this.shownLines = new ArrayList<>();
        this.shownWidths = new ArrayList<>();
        this.shownForWidth = Float.NaN;
        this.drawLines = this::drawLines;
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public Label setText(final String text) {
        Objects.requireNonNull(text, "text");
        if (!this.text.equals(text) || !this.spans.isEmpty()) {
            this.text = text;
            this.spans = List.of();
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    public List<TextSpan> spans() {
        return this.spans;
    }

    @Override
    public Label setSpans(final List<TextSpan> spans) {
        final List<TextSpan> copy = List.copyOf(spans);
        if (!copy.equals(this.spans)) {
            this.spans = copy;
            final StringBuilder joined = new StringBuilder();
            for (final TextSpan span : copy) {
                joined.append(span.text());
            }
            this.text = joined.toString();
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    public TextStyle style() {
        return this.explicitStyle != null ? this.explicitStyle : this.themeStyle;
    }

    @Override
    public Label setStyle(final TextStyle style) {
        this.explicitStyle = style;
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean wraps() {
        return this.wrap;
    }

    @Override
    public Label setWrap(final boolean wrap) {
        this.wrap = wrap;
        this.invalidateLayout();
        return this;
    }

    @Override
    public HorizontalAlignment textAlignment() {
        return this.textAlignment;
    }

    @Override
    public Label setTextAlignment(final HorizontalAlignment textAlignment) {
        this.textAlignment = Objects.requireNonNull(textAlignment, "textAlignment");
        return this;
    }

    @Override
    public float textScale() {
        return this.textScale;
    }

    @Override
    public Label setTextScale(final float scale) {
        if (!(scale > 0f) || Float.isInfinite(scale)) {
            throw new IllegalArgumentException("Text scale must be finite and greater than 0: " + scale);
        }
        this.textScale = scale;
        this.invalidateLayout();
        return this;
    }

    @Override
    public TextOverflow overflow() {
        return this.overflow;
    }

    @Override
    public Label setOverflow(final TextOverflow overflow) {
        this.overflow = Objects.requireNonNull(overflow, "overflow");
        this.invalidateLayout();
        return this;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.themeStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        final List<TextSpan> runs = this.spans.isEmpty() ? List.of(new TextSpan(this.text, this.style())) : this.spans;
        final float maxWidth = this.wrap ? availableWidth / this.textScale : Float.POSITIVE_INFINITY;

        this.lines.clear();
        this.lineWidths.clear();
        this.shownForWidth = Float.NaN;
        float width = 0f;
        for (final List<TextSpan> line : RichText.wrap(runs, maxWidth, metrics)) {
            final float lineWidth = RichText.width(line, metrics);
            this.lines.add(line);
            this.lineWidths.add(lineWidth);
            width = Math.max(width, lineWidth);
        }
        float height = 0f;
        for (final TextSpan run : runs) {
            height = Math.max(height, metrics.lineHeight(run.style()));
        }
        this.lineHeight = height;

        float scaledWidth = width * this.textScale;
        if (this.overflow == TextOverflow.ELLIPSIS && scaledWidth > availableWidth) {
            scaledWidth = Math.max(0f, availableWidth);
        }
        return new Size(scaledWidth, this.lineHeight * this.lines.size() * this.textScale);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.overflow == TextOverflow.ELLIPSIS && bounds.width() != this.shownForWidth) {
            this.shorten(bounds.width() / this.textScale);
            this.shownForWidth = bounds.width();
        }
        this.paintBounds = bounds;
        this.painter = painter;
        try {
            painter.scaled(this.textScale, this.drawLines);
        } finally {
            this.painter = null;
            this.paintBounds = null;
        }
    }

    private void shorten(final float width) {
        final TextMetrics metrics = this.environment().text();
        this.shownLines.clear();
        this.shownWidths.clear();
        for (int i = 0; i < this.lines.size(); i++) {
            final List<TextSpan> line = this.lines.get(i);
            if (this.lineWidths.get(i) <= width) {
                this.shownLines.add(line);
                this.shownWidths.add(this.lineWidths.get(i));
            } else {
                final List<TextSpan> shortened = RichText.ellipsize(line, width, metrics);
                this.shownLines.add(shortened);
                this.shownWidths.add(RichText.width(shortened, metrics));
            }
        }
    }

    /**
     * Runs inside the text scale during {@link #paint}, in unscaled coordinates; a field-held method reference, so
     * painting allocates no lambda per frame.
     */
    private void drawLines() {
        final boolean shortened = this.overflow == TextOverflow.ELLIPSIS;
        final List<List<TextSpan>> shown = shortened ? this.shownLines : this.lines;
        final List<Float> widths = shortened ? this.shownWidths : this.lineWidths;
        final float scale = this.textScale;
        final float left = this.paintBounds.x() / scale;
        final float top = this.paintBounds.y() / scale;
        final float width = this.paintBounds.width() / scale;
        final TextMetrics metrics = this.environment().text();
        for (int i = 0; i < shown.size(); i++) {
            final float lineWidth = widths.get(i);
            float x = switch (this.textAlignment) {
                case LEFT, STRETCH -> left;
                case CENTER -> left + (width - lineWidth) * 0.5f;
                case RIGHT -> left + width - lineWidth;
            };
            final float y = top + i * this.lineHeight;
            final List<TextSpan> line = shown.get(i);
            for (int s = 0; s < line.size(); s++) {
                final TextSpan span = line.get(s);
                this.painter.text(span, x, y);
                if (s + 1 < line.size()) {
                    x += metrics.width(span);
                }
            }
        }
    }

    @Override
    protected Object snapshotState() {
        return new Label.State(this.text, this.style(), this.wrap, this.textAlignment);
    }
}
