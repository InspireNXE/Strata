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
package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.core.text.BidiRuns;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.layout.Flow;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.MnemonicTarget;
import org.inspirenxe.strata.core.text.InlineMetrics;
import org.inspirenxe.strata.core.text.Mnemonics;
import org.inspirenxe.strata.core.text.RichText;
import org.inspirenxe.strata.core.text.TextWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The core {@link Label}. Measuring resolves the text against the label's style and breaks it into lines with
 * {@link RichText} (plain text wraps exactly as
 * {@link TextWrap} wraps it), at the width the parent offers when wrapping is on, and keeps them for painting. Each
 * line is aligned on its own within the bounds. The line height is the tallest of the styles in use.
 *
 * <p>The baseline is the first line's: its characters' top, centered in the line as painting centers them, plus the
 * ascent of the label's style, times the text scale.</p>
 *
 * <p>With {@link TextOverflow#ELLIPSIS}, the shortened lines are worked out when the bounds' width changes and kept
 * until the next change, so painting allocates nothing in between.</p>
 *
 * <p>With mnemonic parsing on, the resolved text's markers are taken out as {@link Mnemonics} describes, and the
 * lines are broken from the marked text while mnemonics show. A label whose mnemonics start or stop showing lays
 * out again on the next frame. Alt with its mnemonic focuses the next tab stop after it.</p>
 */
public final class LabelImpl extends BuiltInComponent<Label> implements Label, MnemonicTarget {
    private Text text;
    private TextStyle explicitStyle;
    private TextStyle themeStyle;
    private boolean wrap;
    private HorizontalAlignment textAlignment;
    private float textScale;
    private TextOverflow overflow;
    private boolean mnemonicParsing;

    // Computed during measure, drawn during paint
    private int mnemonic = Mnemonics.NONE;
    // Whether the lines were broken from the marked text
    private boolean measuredMarked;
    private final List<List<TextSpan>> lines;
    private final List<Float> lineWidths;
    private final List<Float> lineHeights;
    // The height of the characters, which each line is at least as tall as
    private float lineHeight;

    // Each line in drawing order, by the bidirectional algorithm, and the direction they were ordered for
    private final List<BidiRuns> lineRuns = new ArrayList<>();
    private boolean runsRightToLeft;

    // Shortened lines for the last bounds width, and each in drawing order
    private final List<List<TextSpan>> shownLines;
    private final List<BidiRuns> shownRuns = new ArrayList<>();
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
     */
    public LabelImpl(final ComponentFactoryImpl ui, final Text text) {
        super(ui);
        this.text = Objects.requireNonNull(text, "text");
        this.themeStyle = TextStyle.DEFAULT;
        this.textAlignment = HorizontalAlignment.LEFT;
        this.textScale = 1f;
        this.overflow = TextOverflow.VISIBLE;
        this.lines = new ArrayList<>();
        this.lineWidths = new ArrayList<>();
        this.lineHeights = new ArrayList<>();
        this.shownLines = new ArrayList<>();
        this.shownWidths = new ArrayList<>();
        this.shownForWidth = Float.NaN;
        this.drawLines = this::drawLines;
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public Label setText(final Text text) {
        Objects.requireNonNull(text, "text");
        if (!this.text.equals(text)) {
            this.text = text;
            if (this.wrap || this.overflow == TextOverflow.ELLIPSIS) {
                // Its size depends on the space offered, so its parent measures it again
                this.invalidateLayout();
            } else {
                // Text of the same size is only drawn again
                this.node().invalidateMeasure();
            }
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
    public boolean isMnemonicParsing() {
        return this.mnemonicParsing;
    }

    @Override
    public Label setMnemonicParsing(final boolean parsing) {
        if (parsing != this.mnemonicParsing) {
            this.mnemonicParsing = parsing;
            this.mnemonic = Mnemonics.NONE;
            this.invalidateLayout();
        }
        return this;
    }

    private boolean showsMnemonics() {
        return this.hostRoot() != null && this.hostRoot().showsMnemonics();
    }

    /**
     * Resolves the text and, with parsing on, takes its mnemonic out, marked while mnemonics show.
     */
    private List<TextSpan> runs() {
        final List<TextSpan> runs = this.text.resolve(this.style());
        if (!this.mnemonicParsing) {
            return runs;
        }
        final Mnemonics parsed = Mnemonics.parse(runs);
        this.mnemonic = parsed.mnemonic();
        this.measuredMarked = this.showsMnemonics();
        return this.measuredMarked ? parsed.marked() : parsed.plain();
    }

    @Override
    public boolean hasMnemonic(final int mnemonic) {
        return this.mnemonicParsing && this.mnemonic == mnemonic;
    }

    /**
     * Focuses the next tab stop after the label, as Alt with its mnemonic does.
     */
    @Override
    public void activateMnemonic(final int mnemonic) {
        if (this.hostRoot() != null) {
            this.hostRoot().focusNextAfter(this.node());
        }
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.themeStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        final TextMetrics metrics = context.text();
        final List<TextSpan> runs = this.runs();
        final InlineMetrics inline = InlineMetrics.of(metrics);
        if (this.wrap) {
            return new Size(longestWord(runs, metrics) * this.textScale,
                    inline.textHeight(runs, this.style()) * this.textScale);
        }
        float width = 0f;
        float height = 0f;
        final float least = inline.textHeight(runs, this.style());
        for (final List<TextSpan> line : RichText.wrap(runs, Float.POSITIVE_INFINITY, metrics)) {
            width = Math.max(width, RichText.width(line, metrics));
            height += Math.max(least, inline.lineHeight(line, this.style()));
        }
        if (this.overflow == TextOverflow.ELLIPSIS) {
            width = Math.min(width, metrics.width(new TextSpan(RichText.ELLIPSIS, this.style())));
        }
        return new Size(width * this.textScale, height * this.textScale);
    }

    /**
     * Returns the width of the widest run of text between spaces and line breaks. An inline element counts as part
     * of the word it touches.
     */
    private static float longestWord(final List<TextSpan> runs, final TextMetrics metrics) {
        float longest = 0f;
        float word = 0f;
        for (final TextSpan span : runs) {
            if (span.isInline()) {
                word += metrics.width(span);
                continue;
            }
            final String text = span.text();
            int start = 0;
            for (int i = 0; i <= text.length(); i++) {
                final boolean end = i == text.length();
                if (end || text.charAt(i) == ' ' || text.charAt(i) == '\n') {
                    if (i > start) {
                        word += metrics.width(span.withText(text.substring(start, i)));
                    }
                    if (!end) {
                        longest = Math.max(longest, word);
                        word = 0f;
                    }
                    start = i + 1;
                }
            }
        }
        return Math.max(longest, word);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        final List<TextSpan> runs = this.runs();
        final float maxWidth = this.wrap ? availableWidth / this.textScale : Float.POSITIVE_INFINITY;

        this.lines.clear();
        this.lineRuns.clear();
        this.lineWidths.clear();
        this.shownForWidth = Float.NaN;
        // The label's direction is each line's paragraph direction
        final boolean rightToLeft = this.isRightToLeft();
        this.runsRightToLeft = rightToLeft;
        float width = 0f;
        for (final List<TextSpan> line : RichText.wrap(runs, maxWidth, metrics)) {
            final float lineWidth = RichText.width(line, metrics);
            this.lines.add(line);
            this.lineRuns.add(BidiRuns.of(line, rightToLeft));
            this.lineWidths.add(lineWidth);
            width = Math.max(width, lineWidth);
        }
        final InlineMetrics inline = InlineMetrics.of(metrics);
        this.lineHeight = inline.textHeight(runs, this.style());
        this.lineHeights.clear();
        float height = 0f;
        for (final List<TextSpan> line : this.lines) {
            final float lineHeight = Math.max(this.lineHeight, inline.lineHeight(line, this.style()));
            this.lineHeights.add(lineHeight);
            height += lineHeight;
        }

        float scaledWidth = width * this.textScale;
        if (this.overflow == TextOverflow.ELLIPSIS && scaledWidth > availableWidth) {
            scaledWidth = Math.max(0f, availableWidth);
        }
        return new Size(scaledWidth, height * this.textScale);
    }

    @Override
    protected float baseline() {
        if (this.lineHeights.isEmpty()) {
            return Float.NaN;
        }
        final float top = (this.lineHeights.get(0) - this.lineHeight) * 0.5f;
        return (top + this.environment().text().ascent(this.style())) * this.textScale;
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.mnemonic != Mnemonics.NONE && this.measuredMarked != this.showsMnemonics()) {
            // Broken again from the other text on the next frame; both measure the same
            this.invalidateLayout();
        }
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
        this.shownRuns.clear();
        this.shownWidths.clear();
        for (int i = 0; i < this.lines.size(); i++) {
            final List<TextSpan> line = this.lines.get(i);
            if (this.lineWidths.get(i) <= width) {
                this.shownLines.add(line);
                this.shownRuns.add(this.lineRuns.get(i));
                this.shownWidths.add(this.lineWidths.get(i));
            } else {
                final List<TextSpan> shortened = RichText.ellipsize(line, width, metrics);
                this.shownLines.add(shortened);
                this.shownRuns.add(BidiRuns.of(shortened, this.runsRightToLeft));
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
        final List<BidiRuns> shown = shortened ? this.shownRuns : this.lineRuns;
        final List<Float> widths = shortened ? this.shownWidths : this.lineWidths;
        final float scale = this.textScale;
        final float left = this.paintBounds.x() / scale;
        final float top = this.paintBounds.y() / scale;
        final float width = this.paintBounds.width() / scale;
        final InlineMetrics metrics = InlineMetrics.of(this.environment().text());
        // Right to left, left means the start
        final HorizontalAlignment alignment = Flow.resolve(this.textAlignment, this.isRightToLeft());
        float y = top;
        for (int i = 0; i < shown.size(); i++) {
            final float lineWidth = widths.get(i);
            final float x = switch (alignment) {
                case LEFT, STRETCH -> left;
                case CENTER -> left + (width - lineWidth) * 0.5f;
                case RIGHT -> left + width - lineWidth;
            };
            final float lineHeight = this.lineHeights.get(i);
            metrics.draw(this.painter, shown.get(i), x, y, lineHeight, this.lineHeight);
            y += lineHeight;
        }
    }

    @Override
    protected Object snapshotState() {
        return new Label.State(this.text, this.style(), this.wrap, this.textAlignment);
    }
}
