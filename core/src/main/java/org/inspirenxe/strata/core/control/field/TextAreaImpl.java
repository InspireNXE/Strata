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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.TextHighlighter;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.layout.ScrollTrack;
import org.inspirenxe.strata.core.text.StyledText;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * The core {@link TextArea}. It edits like {@link TextBoxImpl} and lays its text out in rows: one per line, and more
 * where a line wraps. Rows are worked out again only when the text, the style or the content size changes, and each
 * row keeps its span, so painting measures nothing. The caret blinks every {@link TextBoxImpl#BLINK_MILLIS}
 * milliseconds.
 *
 * <p>A row that wraps keeps its trailing space, and the caret cannot sit after its last character: that position is
 * the start of the next row. The view scrolls to keep the caret in sight after the caret moves, but not on other
 * frames, so the wheel can scroll it away. Each wheel notch scrolls three lines, and selecting a whole line past its
 * end shows a 3-unit stub for the line break.</p>
 *
 * <p>Every user edit that would change the text fires {@link TextArea#EDIT} when a handler is on its path. While
 * the handlers run the area still holds the text and selection from before the edit. The final text then has its
 * line breaks turned into {@code \n} and is cut to the length limit, and the caret goes as many characters from its
 * end as the edit left it, with nothing selected. With no handler on the path the edit applies directly, without
 * putting the old text back first.</p>
 */
public final class TextAreaImpl extends CustomComponent<TextArea> implements TextArea {
    /**
     * How many lines one wheel notch scrolls.
     */
    public static final int WHEEL_LINES = 3;
    private static final Set<String> REQUIRED = Set.of(TextBox.STYLE_NORMAL, TextBox.STYLE_FOCUSED, TextBox.STYLE_DISABLED,
            ScrollPanel.STYLE_THUMB);
    private static final Paint DEFAULT_SELECTION = Paint.solid(0x806A9FFF);
    private static final Paint DEFAULT_CARET = Paint.solid(0xFFE0E0E0);
    private static final float GUTTER = ScrollPanel.BAR_WIDTH + ScrollPanel.BAR_GAP;
    private static final float LINE_BREAK_WIDTH = 3f;

    private final StringBuilder buffer;
    private final ScrollTrack bar;
    private final Runnable drawContent;
    private int maxLength;
    private boolean wrap;
    private int visibleLines;
    private Insets textPadding;
    private boolean readOnly;

    // The text and selection before the edit in progress, for a rejected edit to fall back to
    private String beforeText;
    private int beforeCaret;
    private int beforeAnchor;

    private int caret;
    private int anchor;
    private float goalX;
    private float scrollX;
    private float scrollY;
    private long blinkStart;
    private boolean revealPending;
    private boolean draggingBar;
    private float barGrab;

    // Double-click drags select whole words around the word first clicked
    private boolean wordDrag;
    private int dragWordStart;
    private int dragWordEnd;

    // Theme
    private Paint normal;
    private Paint focusedPaint;
    private Paint disabledPaint;
    private Paint selectionPaint;
    private Paint caretPaint;
    private Paint thumb;
    private Paint thumbHovered;
    private Paint track;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;
    private TextStyle readOnlyTextStyle;
    private float lineHeight;

    // Rows, rebuilt when the text, style or content size changes
    private boolean rowsDirty;
    private float rowsWidth;
    private float rowsHeight;
    private TextStyle rowsStyle;
    private int rowCount;
    private int[] rowStart;
    private int[] rowEnd;
    private boolean[] rowSoft;
    // Each row as drawn: one span per styled segment it crosses, and where each starts on the row
    private TextSpan[][] rowSpans;
    private float[][] rowSpanX;
    private final StyledText styled;
    private TextHighlighter highlighter;
    private float widestRow;
    private boolean barShown;
    private Rect contentBox;
    private Rect viewport;
    private float maxScrollY;

    // Derived from rows, caret and selection
    private boolean visualsDirty;
    private int caretRow;
    private float caretX;
    private float[] selectionX0;
    private float[] selectionX1;

    // Set only while paint runs, for drawContent
    private Painter painter;

    /**
     * Creates an empty text area that wraps.
     *
     * @param ui the factory that owns the text area
     */
    @SuppressWarnings("this-escape")
    public TextAreaImpl(final ComponentFactory ui) {
        super(ui);
        this.buffer = new StringBuilder();
        this.bar = new ScrollTrack(true);
        this.drawContent = this::drawContent;
        this.maxLength = Integer.MAX_VALUE;
        this.wrap = true;
        this.visibleLines = DEFAULT_VISIBLE_LINES;
        this.textPadding = Insets.of(4f);
        this.goalX = Float.NaN;
        this.normal = Paint.NONE;
        this.focusedPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.selectionPaint = DEFAULT_SELECTION;
        this.caretPaint = DEFAULT_CARET;
        this.thumb = Paint.NONE;
        this.thumbHovered = Paint.NONE;
        this.track = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.readOnlyTextStyle = TextStyle.DEFAULT;
        this.rowsDirty = true;
        this.rowStart = new int[8];
        this.rowEnd = new int[8];
        this.rowSoft = new boolean[8];
        this.rowSpans = new TextSpan[8][];
        this.rowSpanX = new float[8][];
        this.styled = new StyledText();
        this.highlighter = TextHighlighter.NONE;
        this.selectionX0 = new float[8];
        this.selectionX1 = new float[8];
        this.contentBox = Rect.ZERO;
        this.viewport = Rect.ZERO;
        this.visualsDirty = true;
        this.setCursor(CursorKind.TEXT);
        this.setMinSize(20f, 20f);
    }

    // ------------------------------------------------------------------
    // TextArea
    // ------------------------------------------------------------------

    @Override
    public String text() {
        return this.buffer.toString();
    }

    @Override
    public TextArea setText(final String text) {
        final String clean = truncate(normalize(Objects.requireNonNull(text, "text")), this.maxLength);
        if (!clean.contentEquals(this.buffer)) {
            final String previous = this.buffer.toString();
            this.buffer.setLength(0);
            this.buffer.append(clean);
            this.caret = 0;
            this.anchor = 0;
            this.scrollX = 0f;
            this.scrollY = 0f;
            this.changed(previous);
        }
        return this;
    }

    @Override
    public int maxLength() {
        return this.maxLength;
    }

    @Override
    public TextArea setMaxLength(final int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length must not be negative: " + maxLength);
        }
        this.maxLength = maxLength;
        if (this.codePoints() > maxLength) {
            final String previous = this.buffer.toString();
            this.buffer.setLength(this.buffer.offsetByCodePoints(0, maxLength));
            this.caret = Math.min(this.caret, this.buffer.length());
            this.anchor = Math.min(this.anchor, this.buffer.length());
            this.changed(previous);
        }
        return this;
    }

    @Override
    public boolean wraps() {
        return this.wrap;
    }

    @Override
    public TextArea setWrap(final boolean wrap) {
        if (this.wrap != wrap) {
            this.wrap = wrap;
            this.scrollX = 0f;
            this.rowsDirty = true;
            this.moved();
        }
        return this;
    }

    @Override
    public TextArea setVisibleLines(final int lines) {
        if (lines < 1) {
            throw new IllegalArgumentException("At least one line must show: " + lines);
        }
        this.visibleLines = lines;
        this.invalidateLayout();
        return this;
    }

    @Override
    public TextArea setTextPadding(final Insets padding) {
        this.textPadding = Objects.requireNonNull(padding, "padding");
        this.rowsDirty = true;
        this.invalidateLayout();
        return this;
    }

    @Override
    public int caret() {
        return this.caret;
    }

    @Override
    public int selectionStart() {
        return Math.min(this.anchor, this.caret);
    }

    @Override
    public int selectionEnd() {
        return Math.max(this.anchor, this.caret);
    }

    @Override
    public String selectedText() {
        return this.buffer.substring(this.selectionStart(), this.selectionEnd());
    }

    @Override
    public TextArea select(final int anchor, final int caret) {
        this.anchor = this.snap(anchor);
        this.caret = this.snap(caret);
        this.moved();
        return this;
    }

    @Override
    public TextArea selectAll() {
        this.anchor = 0;
        this.caret = this.buffer.length();
        this.moved();
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public TextHighlighter highlighter() {
        return this.highlighter;
    }

    @Override
    public TextArea setHighlighter(final TextHighlighter highlighter) {
        this.highlighter = Objects.requireNonNull(highlighter, "highlighter");
        this.rowsDirty = true;
        return this;
    }

    @Override
    public TextArea setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @Override
    public float scrollY() {
        return this.scrollY;
    }

    // ------------------------------------------------------------------
    // Theme and layout
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected boolean acceptsTextInput() {
        return true;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.normal = theme.paintOr(TextBox.STYLE_NORMAL, Paint.NONE);
        this.focusedPaint = theme.paintOr(TextBox.STYLE_FOCUSED, this.normal);
        this.disabledPaint = theme.paintOr(TextBox.STYLE_DISABLED, this.normal);
        this.selectionPaint = theme.paintOr(TextBox.STYLE_SELECTION, DEFAULT_SELECTION);
        this.caretPaint = theme.paintOr(TextBox.STYLE_CARET, DEFAULT_CARET);
        this.thumb = theme.paintOr(ScrollPanel.STYLE_THUMB, Paint.NONE);
        this.thumbHovered = theme.paintOr(ScrollPanel.STYLE_THUMB_HOVERED, this.thumb);
        this.track = theme.paintOr(ScrollPanel.STYLE_TRACK, Paint.NONE);
        this.textStyle = theme.textStyleOr(TextBox.STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(TextBox.STYLE_TEXT_DISABLED, this.textStyle);
        this.readOnlyTextStyle = theme.textStyleOr(TextBox.STYLE_TEXT_READ_ONLY, this.disabledTextStyle);
        this.rowsDirty = true;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.lineHeight = Math.max(metrics.lineHeight(this.textStyle),
                Math.max(metrics.lineHeight(this.disabledTextStyle), metrics.lineHeight(this.readOnlyTextStyle)));
        this.rowsDirty = true;
        return new Size(DEFAULT_WIDTH, this.visibleLines * this.lineHeight + this.textPadding.vertical());
    }

    private TextStyle style() {
        if (!this.isEnabled()) {
            return this.disabledTextStyle;
        }
        return this.readOnly ? this.readOnlyTextStyle : this.textStyle;
    }

    /**
     * Works out the content box and, if the text, style or size changed, the rows and the scroll bar.
     */
    private void ensureRows() {
        final Rect inner = this.bounds().shrink(this.textPadding);
        this.contentBox = inner;
        final TextStyle style = this.style();
        if (this.rowsDirty || inner.width() != this.rowsWidth || inner.height() != this.rowsHeight || style != this.rowsStyle) {
            this.rowsDirty = false;
            this.rowsWidth = inner.width();
            this.rowsHeight = inner.height();
            this.rowsStyle = style;
            this.styled.update(this.buffer.toString(), style, this.highlighter, this.isEnabled());
            final TextMetrics metrics = this.environment().text();
            this.buildRows(metrics, style, this.wrap ? inner.width() : Float.POSITIVE_INFINITY);
            this.barShown = this.rowCount * this.lineHeight > inner.height();
            if (this.barShown && this.wrap) {
                this.buildRows(metrics, style, Math.max(1f, inner.width() - GUTTER));
            }
            this.visualsDirty = true;
        }
        this.viewport = new Rect(inner.x(), inner.y(), Math.max(0f, inner.width() - (this.barShown ? GUTTER : 0f)),
                inner.height());
        final float total = this.rowCount * this.lineHeight;
        this.maxScrollY = Math.max(0f, total - this.viewport.height());
        this.scrollY = Math.max(0f, Math.min(this.scrollY, this.maxScrollY));
        this.bar.update(new Rect(inner.right() - ScrollPanel.BAR_WIDTH, inner.y(), ScrollPanel.BAR_WIDTH, inner.height()),
                this.viewport.height(), Math.max(total, this.viewport.height()));
    }

    private void buildRows(final TextMetrics metrics, final TextStyle style, final float width) {
        this.rowCount = 0;
        this.widestRow = 0f;
        int lineStart = 0;
        final int length = this.buffer.length();
        while (true) {
            int lineEnd = this.buffer.indexOf("\n", lineStart);
            if (lineEnd < 0) {
                lineEnd = length;
            }
            int start = lineStart;
            do {
                int end = this.fit(metrics, style, start, lineEnd, width);
                boolean soft = false;
                if (end < lineEnd) {
                    soft = true;
                    if (Character.isWhitespace(this.buffer.codePointAt(end))) {
                        // The space that does not fit stays at the end of the row
                        end = this.nextCodePoint(end);
                    } else {
                        // Break after the last whitespace that fits, or between characters
                        for (int i = end; i > start; i = this.previousCodePoint(i)) {
                            if (Character.isWhitespace(this.buffer.codePointBefore(i))) {
                                end = i;
                                break;
                            }
                        }
                    }
                }
                this.addRow(metrics, style, start, end, soft);
                start = end;
            } while (start < lineEnd);
            if (lineEnd >= length) {
                break;
            }
            lineStart = lineEnd + 1;
        }
    }

    /**
     * The furthest index from {@code start}, at most {@code end}, whose text fits a width, and at least one code point
     * past {@code start} while there is one.
     */
    private int fit(final TextMetrics metrics, final TextStyle style, final int start, final int end, final float width) {
        if (Float.isInfinite(width) || this.width(metrics, style, start, end) <= width) {
            return end;
        }
        int low = this.nextCodePoint(start);
        int high = end;
        while (low < high) {
            final int mid = this.snap(low + (high - low + 1) / 2);
            if (mid <= low) {
                break;
            }
            if (this.width(metrics, style, start, mid) <= width) {
                low = mid;
            } else {
                high = this.previousCodePoint(mid);
            }
        }
        return low;
    }

    private float width(final TextMetrics metrics, final TextStyle style, final int start, final int end) {
        // Each styled segment is measured in its own style; the field's style only fills the gaps between runs
        return start >= end ? 0f : this.styled.width(metrics, start, end);
    }

    private void addRow(final TextMetrics metrics, final TextStyle style, final int start, final int end, final boolean soft) {
        if (this.rowCount == this.rowStart.length) {
            final int grown = this.rowCount * 2;
            this.rowStart = Arrays.copyOf(this.rowStart, grown);
            this.rowEnd = Arrays.copyOf(this.rowEnd, grown);
            this.rowSoft = Arrays.copyOf(this.rowSoft, grown);
            this.rowSpans = Arrays.copyOf(this.rowSpans, grown);
            this.rowSpanX = Arrays.copyOf(this.rowSpanX, grown);
            this.selectionX0 = Arrays.copyOf(this.selectionX0, grown);
            this.selectionX1 = Arrays.copyOf(this.selectionX1, grown);
        }
        final TextSpan[] spans = this.styled.spans(start, end);
        final float[] offsets = StyledText.offsets(metrics, spans);
        this.rowStart[this.rowCount] = start;
        this.rowEnd[this.rowCount] = end;
        this.rowSoft[this.rowCount] = soft;
        this.rowSpans[this.rowCount] = spans;
        this.rowSpanX[this.rowCount] = offsets;
        final float rowWidth = StyledText.extent(metrics, spans, offsets);
        this.widestRow = Math.max(this.widestRow, rowWidth);
        this.rowCount++;
    }

    /**
     * The row an index is on: the last row starting at or before it.
     */
    private int rowOf(final int index) {
        int low = 0;
        int high = this.rowCount - 1;
        while (low < high) {
            final int mid = (low + high + 1) >>> 1;
            if (this.rowStart[mid] <= index) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * The furthest index the caret may take on a row.
     */
    private int rowLimit(final int row) {
        return this.rowSoft[row] ? this.previousCodePoint(this.rowEnd[row]) : this.rowEnd[row];
    }

    private float xOnRow(final int row, final int index) {
        return this.width(this.environment().text(), this.rowsStyle, this.rowStart[row], Math.min(index, this.rowEnd[row]));
    }

    /**
     * The code point boundary on a row nearest to {@code x}, measured from the row's start.
     */
    private int indexOnRow(final int row, final float x) {
        final int start = this.rowStart[row];
        final int limit = this.rowLimit(row);
        if (x <= 0f) {
            return start;
        }
        int previous = start;
        float previousX = 0f;
        int index = start;
        while (index < limit) {
            index = this.nextCodePoint(index);
            final float indexX = this.xOnRow(row, index);
            if (indexX >= x) {
                return x - previousX < indexX - x ? previous : index;
            }
            previous = index;
            previousX = indexX;
        }
        return limit;
    }

    /**
     * The index under a point given relative to the component's top-left corner.
     */
    private int indexAt(final float localX, final float localY) {
        this.ensureRows();
        final int row = Math.max(0, Math.min(this.rowCount - 1,
                (int) Math.floor((localY - this.textPadding.top() + this.scrollY) / this.lineHeight)));
        return this.indexOnRow(row, localX - this.textPadding.left() + this.scrollX);
    }

    private void updateVisuals() {
        if (!this.visualsDirty) {
            return;
        }
        this.visualsDirty = false;
        this.caretRow = this.rowOf(this.caret);
        this.caretX = this.xOnRow(this.caretRow, this.caret);
        final int start = this.selectionStart();
        final int end = this.selectionEnd();
        for (int row = 0; row < this.rowCount; row++) {
            final int rowStart = this.rowStart[row];
            final int rowEnd = this.rowEnd[row];
            if (start == end || end < rowStart || start > rowEnd || (start == rowEnd && this.rowSoft[row])) {
                this.selectionX0[row] = 0f;
                this.selectionX1[row] = 0f;
                continue;
            }
            this.selectionX0[row] = this.xOnRow(row, Math.max(start, rowStart));
            float x1 = this.xOnRow(row, Math.min(end, rowEnd));
            if (end > rowEnd && !this.rowSoft[row]) {
                x1 += LINE_BREAK_WIDTH;
            }
            this.selectionX1[row] = x1;
        }
    }

    /**
     * Scrolls just enough to keep the caret's row and column in the viewport.
     */
    private void revealCaret() {
        final float top = this.caretRow * this.lineHeight;
        if (top < this.scrollY) {
            this.scrollY = top;
        } else if (top + this.lineHeight > this.scrollY + this.viewport.height()) {
            this.scrollY = top + this.lineHeight - this.viewport.height();
        }
        this.scrollY = Math.max(0f, Math.min(this.scrollY, this.maxScrollY));
        final float view = this.viewport.width();
        if (this.caretX - this.scrollX > view - 1f) {
            this.scrollX = this.caretX - view + 1f;
        }
        if (this.caretX - this.scrollX < 0f) {
            this.scrollX = this.caretX;
        }
        this.scrollX = Math.max(0f, Math.min(this.scrollX, Math.max(0f, this.widestRow + 1f - view)));
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final Paint face;
        if (!this.isEnabled()) {
            face = this.disabledPaint;
        } else if (this.isFocused()) {
            face = this.focusedPaint;
        } else {
            face = this.normal;
        }
        face.paint(painter, bounds);

        this.ensureRows();
        this.updateVisuals();
        if (this.revealPending) {
            this.revealPending = false;
            this.revealCaret();
        }

        this.painter = painter;
        try {
            painter.clipped(this.viewport, this.drawContent);
        } finally {
            this.painter = null;
        }

        if (this.isFocused() && this.isEnabled() && !this.readOnly && this.caretShown()) {
            final float x = this.viewport.x() + this.caretX - this.scrollX;
            final float y = this.viewport.y() + this.caretRow * this.lineHeight - Math.round(this.scrollY);
            if (x >= this.viewport.x() && x < this.viewport.right() && y + this.lineHeight > this.viewport.y()
                    && y < this.viewport.bottom()) {
                this.caretPaint.paint(painter, new Rect(x, y, 1f, this.lineHeight));
            }
        }
        if (this.barShown) {
            this.bar.paint(painter, this.scrollY, this.maxScrollY, this.track, this.draggingBar ? this.thumbHovered : this.thumb);
        }
    }

    /**
     * Runs inside the viewport clip during {@link #paint}; a field-held method reference, so painting allocates no
     * lambda per frame.
     */
    private void drawContent() {
        final float originX = this.viewport.x() - this.scrollX;
        final float offset = Math.round(this.scrollY);
        final int first = Math.max(0, (int) Math.floor(offset / this.lineHeight));
        final int last = Math.min(this.rowCount - 1, (int) Math.floor((offset + this.viewport.height()) / this.lineHeight));
        for (int row = first; row <= last; row++) {
            final float y = this.viewport.y() + row * this.lineHeight - offset;
            if (this.selectionX1[row] > this.selectionX0[row]) {
                this.selectionPaint.paint(this.painter, new Rect(originX + this.selectionX0[row], y,
                        this.selectionX1[row] - this.selectionX0[row], this.lineHeight));
            }
            final TextSpan[] spans = this.rowSpans[row];
            final float[] offsets = this.rowSpanX[row];
            for (int i = 0; i < spans.length; i++) {
                this.painter.text(spans[i], originX + offsets[i], y);
            }
        }
    }

    private boolean caretShown() {
        return ((this.environment().millis() - this.blinkStart) / TextBoxImpl.BLINK_MILLIS) % 2 == 0;
    }

    // ------------------------------------------------------------------
    // Pointer
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        this.ensureRows();
        this.draggingBar = false;
        if (this.barShown && this.bar.track().contains(event.x(), event.y())) {
            final Rect thumbRect = this.bar.thumb(this.scrollY, this.maxScrollY);
            if (thumbRect.contains(event.x(), event.y())) {
                this.draggingBar = true;
                this.barGrab = event.y() - thumbRect.y();
            } else {
                final float page = Math.max(this.lineHeight, this.viewport.height() - this.lineHeight);
                this.scrollBy(event.y() < thumbRect.y() ? -page : page);
            }
            event.consume();
            return;
        }
        final int index = this.indexAt(event.localX(), event.localY());
        this.wordDrag = false;
        switch (event.clicks()) {
            case 1 -> {
                this.caret = index;
                if (!event.modifiers().shift()) {
                    this.anchor = index;
                }
            }
            case 2 -> {
                final int[] word = this.wordAround(index);
                this.anchor = word[0];
                this.caret = word[1];
                this.wordDrag = true;
                this.dragWordStart = word[0];
                this.dragWordEnd = word[1];
            }
            default -> {
                this.anchor = this.lineStart(index);
                this.caret = this.lineEnd(index);
            }
        }
        this.goalX = Float.NaN;
        this.moved();
        event.consume();
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.onDrag(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        if (this.draggingBar) {
            this.scrollTo(this.bar.offsetForThumbAt(event.y() - this.barGrab, this.maxScrollY));
            event.consume();
            return;
        }
        if (event.clicks() >= 3) {
            return;
        }
        final int index = this.indexAt(event.localX(), event.localY());
        if (this.wordDrag) {
            final int[] word = this.wordAround(index);
            if (word[0] < this.dragWordStart) {
                this.anchor = this.dragWordEnd;
                this.caret = word[0];
            } else {
                this.anchor = this.dragWordStart;
                this.caret = Math.max(word[1], this.dragWordEnd);
            }
        } else {
            this.caret = index;
        }
        this.goalX = Float.NaN;
        this.moved();
        event.consume();
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.draggingBar = false;
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        this.ensureRows();
        if (this.scrollBy(-event.scrollY() * this.lineHeight * WHEEL_LINES)) {
            event.consume();
        }
    }

    private boolean scrollBy(final float delta) {
        return this.scrollTo(this.scrollY + delta);
    }

    private boolean scrollTo(final float offset) {
        final float next = Math.max(0f, Math.min(offset, this.maxScrollY));
        if (next == this.scrollY) {
            return false;
        }
        this.scrollY = next;
        return true;
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        final Key key = event.key();
        final Modifiers modifiers = event.modifiers();
        if (key == Key.TAB) {
            return;
        }
        this.ensureRows();
        this.updateVisuals();
        final boolean shift = modifiers.shift();
        final boolean handled;
        if (modifiers.control() && !modifiers.alt()) {
            handled = this.controlKey(key, shift);
        } else if (!modifiers.alt()) {
            handled = this.plainKey(key, shift);
        } else {
            handled = false;
        }
        if (handled || (!modifiers.control() && !modifiers.alt() && key.isPrintable())) {
            event.consume();
        }
    }

    private boolean controlKey(final Key key, final boolean shift) {
        switch (key) {
            case A -> this.selectAll();
            case C -> this.copy();
            case X -> this.cut();
            case V -> this.paste();
            case LEFT -> this.moveTo(this.previousWordStart(this.caret), shift);
            case RIGHT -> this.moveTo(this.nextWordEnd(this.caret), shift);
            case HOME -> this.moveTo(0, shift);
            case END -> this.moveTo(this.buffer.length(), shift);
            case BACKSPACE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.deleteRange(this.previousWordStart(this.caret), this.caret);
                }
            }
            case DELETE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.deleteRange(this.caret, this.nextWordEnd(this.caret));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean plainKey(final Key key, final boolean shift) {
        if (key.isEnter()) {
            this.insert("\n");
            return true;
        }
        switch (key) {
            case LEFT -> {
                if (this.hasSelection() && !shift) {
                    this.moveTo(this.selectionStart(), false);
                } else {
                    this.moveTo(this.previousCodePoint(this.caret), shift);
                }
            }
            case RIGHT -> {
                if (this.hasSelection() && !shift) {
                    this.moveTo(this.selectionEnd(), false);
                } else {
                    this.moveTo(this.nextCodePoint(this.caret), shift);
                }
            }
            case UP -> this.moveRows(-1, shift);
            case DOWN -> this.moveRows(1, shift);
            case PAGE_UP -> this.moveRows(-this.pageRows(), shift);
            case PAGE_DOWN -> this.moveRows(this.pageRows(), shift);
            case HOME -> this.moveTo(this.rowStart[this.caretRow], shift);
            case END -> this.moveTo(this.rowLimit(this.caretRow), shift);
            case BACKSPACE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.deleteRange(this.previousCodePoint(this.caret), this.caret);
                }
            }
            case DELETE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.deleteRange(this.caret, this.nextCodePoint(this.caret));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private int pageRows() {
        return Math.max(1, (int) Math.floor(this.viewport.height() / this.lineHeight) - 1);
    }

    /**
     * Moves the caret up or down by rows, keeping the column it had when vertical movement began.
     */
    private void moveRows(final int rows, final boolean extend) {
        final float goal = Float.isNaN(this.goalX) ? this.caretX : this.goalX;
        final int target = this.caretRow + rows;
        final int index;
        if (target < 0) {
            index = 0;
        } else if (target >= this.rowCount) {
            index = this.buffer.length();
        } else {
            index = this.indexOnRow(target, goal);
        }
        this.moveTo(index, extend);
        this.goalX = goal;
    }

    @Override
    protected void onChar(final CharEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        final int codePoint = event.codePoint();
        if (Character.isISOControl(codePoint)) {
            return;
        }
        this.insert(Character.toString(codePoint));
        event.consume();
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    private void insert(final String text) {
        if (!this.beginEdit()) {
            return;
        }
        if (this.hasSelection()) {
            this.removeSelection();
        }
        final String fitted = truncate(text, this.maxLength - this.codePoints());
        if (!fitted.isEmpty()) {
            this.buffer.insert(this.caret, fitted);
            this.caret += fitted.length();
        }
        this.anchor = this.caret;
        this.edited();
    }

    private void copy() {
        if (this.hasSelection()) {
            this.environment().setClipboardText(this.selectedText());
        }
    }

    private void cut() {
        if (this.hasSelection() && !this.readOnly) {
            this.copy();
            this.deleteSelection();
        }
    }

    private void paste() {
        this.environment().clipboardText().map(TextAreaImpl::normalize).ifPresent(this::insert);
    }

    private void deleteSelection() {
        if (!this.beginEdit()) {
            return;
        }
        this.removeSelection();
        this.edited();
    }

    private void removeSelection() {
        final int start = this.selectionStart();
        this.buffer.delete(start, this.selectionEnd());
        this.caret = start;
        this.anchor = start;
    }

    private void deleteRange(final int start, final int end) {
        if (start >= end || !this.beginEdit()) {
            return;
        }
        this.buffer.delete(start, end);
        this.caret = start;
        this.anchor = start;
        this.edited();
    }

    private boolean beginEdit() {
        if (this.readOnly) {
            return false;
        }
        this.beforeText = this.buffer.toString();
        this.beforeCaret = this.caret;
        this.beforeAnchor = this.anchor;
        return true;
    }

    /**
     * Ends a user edit. With no {@link TextArea#EDIT} handler on the path, or when the edit left the text as it was,
     * this applies the edit at once. Otherwise it puts the text and selection back as they were before the edit and
     * fires the request with the edited text, which applies the final text through {@code proceedEdit} unless a
     * handler cancels it.
     */
    private void edited() {
        this.goalX = Float.NaN;
        if (!this.isHandled(EDIT)) {
            this.applyEdit();
            return;
        }
        final String candidate = this.buffer.toString();
        if (candidate.equals(this.beforeText)) {
            this.applyEdit();
            return;
        }
        final int fromEnd = candidate.length() - this.caret;
        this.buffer.setLength(0);
        this.buffer.append(this.beforeText);
        this.caret = this.beforeCaret;
        this.anchor = this.beforeAnchor;
        this.moved();
        this.request(EDIT, candidate, text -> this.proceedEdit(text, fromEnd));
    }

    /**
     * Applies the final text of an {@link TextArea#EDIT} request: turns its line breaks into {@code \n}, cuts
     * it to the length limit, and puts it in the area with the caret {@code fromEnd} characters before its end and
     * nothing selected. For the edited text itself that is where the edit left the caret. Then it applies the text
     * as the edit.
     */
    private void proceedEdit(final String text, final int fromEnd) {
        final String fitted = truncate(normalize(Objects.requireNonNull(text, "text")), this.maxLength);
        this.buffer.setLength(0);
        this.buffer.append(fitted);
        this.caret = this.snap(this.buffer.length() - fromEnd);
        this.anchor = this.caret;
        this.goalX = Float.NaN;
        this.applyEdit();
    }

    /**
     * Applies the edit the buffer holds, reporting {@link TextArea#EDITED} if the text changed.
     */
    private void applyEdit() {
        if (this.buffer.toString().equals(this.beforeText)) {
            this.moved();
            return;
        }
        this.changed(this.beforeText);
    }

    private void moveTo(final int index, final boolean extend) {
        this.caret = this.snap(index);
        if (!extend) {
            this.anchor = this.caret;
        }
        this.goalX = Float.NaN;
        this.moved();
    }

    private void moved() {
        this.visualsDirty = true;
        this.revealPending = true;
        this.blinkStart = this.environment().millis();
    }

    private void changed(final String previous) {
        this.rowsDirty = true;
        this.moved();
        this.report(EDITED, this.buffer.toString(), previous);
    }

    // ------------------------------------------------------------------
    // Text helpers
    // ------------------------------------------------------------------

    private boolean hasSelection() {
        return this.anchor != this.caret;
    }

    private int codePoints() {
        return this.buffer.codePointCount(0, this.buffer.length());
    }

    private int snap(final int index) {
        final int clamped = Math.max(0, Math.min(this.buffer.length(), index));
        if (clamped > 0 && clamped < this.buffer.length()
                && Character.isLowSurrogate(this.buffer.charAt(clamped))
                && Character.isHighSurrogate(this.buffer.charAt(clamped - 1))) {
            return clamped - 1;
        }
        return clamped;
    }

    private int previousCodePoint(final int index) {
        return index <= 0 ? 0 : index - Character.charCount(this.buffer.codePointBefore(index));
    }

    private int nextCodePoint(final int index) {
        return index >= this.buffer.length() ? this.buffer.length() : index + Character.charCount(this.buffer.codePointAt(index));
    }

    private int lineStart(final int index) {
        final int newline = this.buffer.lastIndexOf("\n", index - 1);
        return newline < 0 ? 0 : newline + 1;
    }

    private int lineEnd(final int index) {
        final int newline = this.buffer.indexOf("\n", index);
        return newline < 0 ? this.buffer.length() : newline;
    }

    private int previousWordStart(final int index) {
        int i = index;
        while (i > 0 && Character.isWhitespace(this.buffer.codePointBefore(i))) {
            i = this.previousCodePoint(i);
        }
        while (i > 0 && !Character.isWhitespace(this.buffer.codePointBefore(i))) {
            i = this.previousCodePoint(i);
        }
        return i;
    }

    private int nextWordEnd(final int index) {
        int i = index;
        while (i < this.buffer.length() && Character.isWhitespace(this.buffer.codePointAt(i))) {
            i = this.nextCodePoint(i);
        }
        while (i < this.buffer.length() && !Character.isWhitespace(this.buffer.codePointAt(i))) {
            i = this.nextCodePoint(i);
        }
        return i;
    }

    private int[] wordAround(final int index) {
        if (this.buffer.isEmpty()) {
            return new int[] {0, 0};
        }
        final int at = index >= this.buffer.length() ? this.previousCodePoint(index) : index;
        final boolean whitespace = Character.isWhitespace(this.buffer.codePointAt(at));
        int start = at;
        while (start > 0 && Character.isWhitespace(this.buffer.codePointBefore(start)) == whitespace
                && this.buffer.charAt(start - 1) != '\n') {
            start = this.previousCodePoint(start);
        }
        int end = at;
        while (end < this.buffer.length() && Character.isWhitespace(this.buffer.codePointAt(end)) == whitespace
                && this.buffer.charAt(end) != '\n') {
            end = this.nextCodePoint(end);
        }
        return new int[] {start, end};
    }

    private static String normalize(final String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String truncate(final String text, final int codePoints) {
        if (codePoints <= 0) {
            return "";
        }
        if (text.codePointCount(0, text.length()) <= codePoints) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, codePoints));
    }

    @Override
    protected Object snapshotState() {
        return new TextArea.State(this.buffer.toString(), this.caret, this.anchor, this.maxLength);
    }
}
