package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.LabelBehavior;
import org.inspirenxe.strata.core.text.TextWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The core {@link Label}. Measuring breaks the text into lines with {@link TextWrap}, at the width the parent
 * offers when wrapping is on, and keeps them for painting. Each line is aligned on its own within the bounds.
 */
public final class LabelImpl extends CustomComponent<Label> implements Label {
    private String text;
    private TextStyle explicitStyle;
    private TextStyle themeStyle;
    private boolean wrap;
    private HorizontalAlignment textAlignment;

    // Computed during measure, drawn during paint
    private final List<TextSpan> lines;
    private final List<Float> lineWidths;
    private float lineHeight;

    /**
     * Creates a label that does not wrap and aligns its text to the left.
     *
     * @param ui the factory that owns the label
     * @param text the text, with {@code \n} for line breaks
     * @param behavior the decorator for the label's hooks, or {@code null} for none
     * @throws NullPointerException if {@code text} is null
     */
    public LabelImpl(final ComponentFactory ui, final String text, final LabelBehavior behavior) {
        super(ui, behavior);
        this.text = Objects.requireNonNull(text, "text");
        this.themeStyle = TextStyle.DEFAULT;
        this.textAlignment = HorizontalAlignment.LEFT;
        this.lines = new ArrayList<>();
        this.lineWidths = new ArrayList<>();
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public Label setText(final String text) {
        if (!this.text.equals(text)) {
            this.text = Objects.requireNonNull(text, "text");
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
    protected void onApplyTheme(final Theme theme) {
        this.themeStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        final TextStyle style = this.style();
        final float maxWidth = this.wrap ? availableWidth : Float.POSITIVE_INFINITY;

        this.lines.clear();
        this.lineWidths.clear();
        float width = 0f;
        for (final String line : TextWrap.wrap(this.text, style, maxWidth, metrics)) {
            final TextSpan span = new TextSpan(line, style);
            final float lineWidth = metrics.width(span);
            this.lines.add(span);
            this.lineWidths.add(lineWidth);
            width = Math.max(width, lineWidth);
        }
        this.lineHeight = metrics.lineHeight(style);
        return new Size(width, this.lineHeight * this.lines.size());
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        for (int i = 0; i < this.lines.size(); i++) {
            final float lineWidth = this.lineWidths.get(i);
            final float x = switch (this.textAlignment) {
                case LEFT, STRETCH -> bounds.x();
                case CENTER -> bounds.x() + (bounds.width() - lineWidth) * 0.5f;
                case RIGHT -> bounds.right() - lineWidth;
            };
            painter.text(this.lines.get(i), x, bounds.y() + i * this.lineHeight);
        }
    }

    @Override
    protected Object snapshotState() {
        return new Label.State(this.text, this.style(), this.wrap, this.textAlignment);
    }
}
