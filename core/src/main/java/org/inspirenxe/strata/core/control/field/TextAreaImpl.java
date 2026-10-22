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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextHighlighter;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.layout.ScrollTrack;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.StyledText;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The core {@link TextArea}. It edits like {@link TextBoxImpl} and lays its text out in rows: one per line, and more
 * where a line wraps. Rows are worked out again only when the text, the style or the content size changes, and each
 * row keeps its span, so painting measures nothing. The caret stays shown and then hidden for the theme's
 * {@value TextBox#STYLE_CARET_BLINK}, {@link TextBoxImpl#BLINK_MILLIS} milliseconds each by default. Setting the text
 * puts the caret at its end and scrolls back to the top, and the view stays there until the caret moves, so new text
 * shows its start.
 *
 * <p>A row that wraps keeps its trailing space, and the caret cannot sit after its last character: that position is
 * the start of the next row. The view scrolls to keep the caret in sight after the caret moves, but not on other
 * frames, so the wheel can scroll it away. Each wheel notch scrolls three lines, and selecting a whole line past its
 * end shows a 3-unit stub for the line break.</p>
 *
 * <p>A vertical scroll bar shows when the rows are taller than the area. An area that does not wrap also shows a
 * horizontal one along the bottom when its widest row does not fit; a horizontal wheel turn scrolls it by three
 * lines' height.</p>
 *
 * <p>Every user edit that would change the text fires {@link TextArea#MODIFY} and then {@link TextArea#EDIT} when a
 * handler for either is on its path. While the handlers run the area still holds the text and selection from before
 * the edit; a canceled {@code MODIFY} ends the edit there. The final text then has its line breaks turned into
 * {@code \n} and its control characters other than tabs dropped, and is cut to the length limit, and the caret goes as many characters from its end as the edit left it,
 * with nothing selected. With neither handler on the path the edit applies directly, without putting the old text
 * back first. Every change reports {@link TextArea#EDITED} and then {@link TextArea#MODIFIED}.</p>
 *
 * <p>The text is a {@code PlainBuffer}. The caret, the selection, the keys and the pointer are a {@code TextEditor},
 * which the text box and the password box share, with the area's rows plugged in for Home, End, Up, Down, Page Up,
 * Page Down and triple clicks, and a user's edit goes through the {@code PlainEdits} the text box uses too. The rows
 * and their drawing are the area's own.</p>
 */
public final class TextAreaImpl extends BuiltInComponent<TextArea> implements TextArea {
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
    /**
     * The padding around the text when the theme sets neither {@value TextArea#STYLE_PADDING} nor
     * {@value TextBox#STYLE_PADDING}: 4 on every side.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(4f);

    private final PlainBuffer buffer;
    private final TextBoundaries boundaries;
    private final TextEditor editor;
    private final PlainEdits edits;
    private final ScrollTrack bar;
    private final ScrollTrack horizontalBar;
    private final Runnable drawContent;
    private int maxLength;
    private boolean wrap;
    private int visibleLines;
    // The padding in use: the one set, else the theme's
    private Insets textPadding;
    private Insets setPadding;
    private Insets themePadding;
    private boolean readOnly;

    private float scrollX;
    private float scrollY;
    private boolean revealPending;
    private boolean draggingBar;
    private boolean draggingHorizontalBar;
    private float barGrab;

    // Theme
    private Paint normal;
    private Paint focusedPaint;
    private Paint disabledPaint;
    private FieldLook look;
    private Text placeholder;
    private TextStyle placeholderStyle;
    private TextLine placeholderLine;
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
    private boolean horizontalBarShown;
    private Rect contentBox;
    private Rect viewport;
    private float maxScrollX;
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
    public TextAreaImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.buffer = new PlainBuffer();
        this.boundaries = new TextBoundaries(this.buffer, false);
        this.editor = new TextEditor(this.buffer, this.boundaries, new Editing(), new RowMoves(), this::caretMoved);
        this.edits = new PlainEdits(this.buffer, this.boundaries, this.editor, new Edits());
        this.bar = new ScrollTrack(true);
        this.horizontalBar = new ScrollTrack(false);
        this.drawContent = this::drawContent;
        this.maxLength = Integer.MAX_VALUE;
        this.wrap = true;
        this.visibleLines = DEFAULT_VISIBLE_LINES;
        this.textPadding = DEFAULT_PADDING;
        this.themePadding = DEFAULT_PADDING;
        this.normal = Paint.NONE;
        this.focusedPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.look = FieldLook.plain(DEFAULT_SELECTION, DEFAULT_CARET);
        this.placeholder = Text.empty();
        this.placeholderStyle = TextStyle.DEFAULT;
        this.placeholderLine = TextLine.EMPTY;
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
        final String clean = PlainEdits.truncate(FieldText.lines(Objects.requireNonNull(text, "text")), this.maxLength);
        if (!this.buffer.contentEquals(clean)) {
            final String previous = this.buffer.toString();
            this.buffer.set(clean);
            this.editor.place(this.buffer.length());
            this.scrollX = 0f;
            this.scrollY = 0f;
            this.changed(previous, false);
        }
        return this;
    }

    @Override
    public int length() {
        return this.codePoints();
    }

    @Override
    public boolean isEmpty() {
        return this.buffer.isEmpty();
    }

    @Override
    public TextArea clear() {
        return this.setText("");
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
            this.buffer.truncate(this.buffer.offsetByCodePoints(0, maxLength));
            this.editor.clamp();
            this.changed(previous, true);
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
            this.editor.moved();
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
        this.setPadding = padding;
        this.textPadding = padding != null ? padding : this.themePadding;
        this.rowsDirty = true;
        this.invalidateLayout();
        return this;
    }

    @Override
    public int caret() {
        return this.editor.caret();
    }

    @Override
    public int selectionStart() {
        return this.editor.selectionStart();
    }

    @Override
    public int selectionEnd() {
        return this.editor.selectionEnd();
    }

    @Override
    public String selectedText() {
        return this.buffer.substring(this.selectionStart(), this.selectionEnd());
    }

    @Override
    public TextArea select(final int anchor, final int caret) {
        this.editor.select(anchor, caret);
        return this;
    }

    @Override
    public TextArea selectAll() {
        this.editor.selectAll();
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
        this.themePadding = themePadding(theme);
        if (this.setPadding == null) {
            this.textPadding = this.themePadding;
        }
        this.normal = theme.paintOr(TextBox.STYLE_NORMAL, Paint.NONE);
        this.focusedPaint = theme.paintOr(TextBox.STYLE_FOCUSED, this.normal);
        this.disabledPaint = theme.paintOr(TextBox.STYLE_DISABLED, this.normal);
        this.look = FieldLook.of(theme, DEFAULT_SELECTION, DEFAULT_CARET);
        this.thumb = theme.paintOr(ScrollPanel.STYLE_THUMB, Paint.NONE);
        this.thumbHovered = theme.paintOr(ScrollPanel.STYLE_THUMB_HOVERED, this.thumb);
        this.track = theme.paintOr(ScrollPanel.STYLE_TRACK, Paint.NONE);
        this.textStyle = theme.textStyleOr(TextBox.STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(TextBox.STYLE_TEXT_DISABLED, this.textStyle);
        this.readOnlyTextStyle = theme.textStyleOr(TextBox.STYLE_TEXT_READ_ONLY, this.disabledTextStyle);
        this.placeholderStyle = theme.textStyleOr(TextBox.STYLE_PLACEHOLDER, this.disabledTextStyle);
        this.rowsDirty = true;
    }

    private static Insets themePadding(final Theme theme) {
        return theme.insets(STYLE_PADDING).or(() -> theme.insets(TextBox.STYLE_PADDING)).orElse(DEFAULT_PADDING);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.placeholderLine = TextLine.of(this.placeholder, this.placeholderStyle, metrics);
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
            final float total = this.rowCount * this.lineHeight;
            this.barShown = total > inner.height();
            if (this.barShown && this.wrap) {
                this.buildRows(metrics, style, Math.max(1f, inner.width() - GUTTER));
            }
            // The caret may sit one unit past the widest row
            this.horizontalBarShown = !this.wrap && this.widestRow + 1f > inner.width() - (this.barShown ? GUTTER : 0f);
            if (this.horizontalBarShown && !this.barShown && total > inner.height() - GUTTER) {
                this.barShown = true;
            }
            this.visualsDirty = true;
        }
        final float bottomGutter = this.horizontalBarShown ? GUTTER : 0f;
        this.viewport = new Rect(inner.x(), inner.y(), Math.max(0f, inner.width() - (this.barShown ? GUTTER : 0f)),
                Math.max(0f, inner.height() - bottomGutter));
        final float total = this.rowCount * this.lineHeight;
        this.maxScrollY = Math.max(0f, total - this.viewport.height());
        this.scrollY = Math.max(0f, Math.min(this.scrollY, this.maxScrollY));
        this.bar.update(new Rect(inner.right() - ScrollPanel.BAR_WIDTH, inner.y(), ScrollPanel.BAR_WIDTH,
                Math.max(0f, inner.height() - bottomGutter)), this.viewport.height(), Math.max(total, this.viewport.height()));
        final float wide = this.widestRow + 1f;
        this.maxScrollX = this.wrap ? 0f : Math.max(0f, wide - this.viewport.width());
        this.scrollX = Math.max(0f, Math.min(this.scrollX, this.maxScrollX));
        this.horizontalBar.update(new Rect(inner.x(), inner.bottom() - ScrollPanel.BAR_WIDTH, this.viewport.width(),
                ScrollPanel.BAR_WIDTH), this.viewport.width(), Math.max(wide, this.viewport.width()));
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
            final int mid = this.boundaries.snap(low + (high - low + 1) / 2);
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
        final int caret = this.editor.caret();
        this.caretRow = this.rowOf(caret);
        this.caretX = this.xOnRow(this.caretRow, caret);
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
        this.scrollX = Math.max(0f, Math.min(this.scrollX, this.maxScrollX));
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

        if (this.isFocused() && this.isEnabled() && !this.readOnly && this.editor.caretShown(this.look.blinkMillis())) {
            final float x = this.viewport.x() + this.caretX - this.scrollX;
            final float y = this.viewport.y() + this.caretRow * this.lineHeight - Math.round(this.scrollY);
            if (x >= this.viewport.x() && x < this.viewport.right() && y + this.lineHeight > this.viewport.y()
                    && y < this.viewport.bottom()) {
                final float reach = this.look.caretExtend();
                this.look.caret().paint(painter, new Rect(x, y - reach, 1f, this.lineHeight + reach * 2f));
            }
        }
        if (this.barShown) {
            this.bar.paint(painter, this.scrollY, this.maxScrollY, this.track, this.draggingBar ? this.thumbHovered : this.thumb);
        }
        if (this.horizontalBarShown) {
            this.horizontalBar.paint(painter, this.scrollX, this.maxScrollX, this.track,
                    this.draggingHorizontalBar ? this.thumbHovered : this.thumb);
        }
    }

    /**
     * Runs inside the viewport clip during {@link #paint}; a field-held method reference, so painting allocates no
     * lambda per frame.
     */
    private void drawContent() {
        final float originX = this.viewport.x() - this.scrollX;
        final float offset = Math.round(this.scrollY);
        if (this.buffer.length() == 0) {
            if (!this.placeholderLine.isEmpty() && !(this.look.hidePlaceholder() && this.isFocused())) {
                this.placeholderLine.draw(this.painter, this.viewport.x(), this.viewport.y() - offset);
            }
            return;
        }
        final int first = Math.max(0, (int) Math.floor(offset / this.lineHeight));
        final int last = Math.min(this.rowCount - 1, (int) Math.floor((offset + this.viewport.height()) / this.lineHeight));
        final TextStyle selectedStyle = this.look.selectedText();
        for (int row = first; row <= last; row++) {
            final float y = this.viewport.y() + row * this.lineHeight - offset;
            final TextSpan[] spans = this.rowSpans[row];
            final float[] offsets = this.rowSpanX[row];
            if (this.selectionX1[row] > this.selectionX0[row]) {
                final Rect selected = new Rect(originX + this.selectionX0[row], y,
                        this.selectionX1[row] - this.selectionX0[row], this.lineHeight);
                this.look.selection().paint(this.painter, selected);
                if (selectedStyle != null) {
                    // The row outside the selection as it is, and inside it in the selected style
                    final float left = this.viewport.x();
                    final float right = this.viewport.right();
                    this.painter.clipped(new Rect(left, y, Math.max(0f, selected.x() - left), this.lineHeight),
                            () -> this.drawRow(spans, offsets, originX, y, null));
                    this.painter.clipped(new Rect(selected.right(), y, Math.max(0f, right - selected.right()),
                            this.lineHeight), () -> this.drawRow(spans, offsets, originX, y, null));
                    this.painter.clipped(selected, () -> this.drawRow(spans, offsets, originX, y, selectedStyle));
                    continue;
                }
            }
            this.drawRow(spans, offsets, originX, y, null);
        }
    }

    private void drawRow(final TextSpan[] spans, final float[] offsets, final float originX, final float y,
                         final TextStyle style) {
        for (int i = 0; i < spans.length; i++) {
            this.painter.text(style == null ? spans[i] : new TextSpan(spans[i].text(), style), originX + offsets[i], y);
        }
    }

    @Override
    public Text placeholder() {
        return this.placeholder;
    }

    @Override
    public TextArea setPlaceholder(final Text placeholder) {
        this.placeholder = placeholder == null ? Text.empty() : placeholder;
        this.invalidateLayout();
        return this;
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
        this.draggingHorizontalBar = false;
        if (this.horizontalBarShown && this.horizontalBar.track().contains(event.x(), event.y())) {
            final Rect thumbRect = this.horizontalBar.thumb(this.scrollX, this.maxScrollX);
            if (thumbRect.contains(event.x(), event.y())) {
                this.draggingHorizontalBar = true;
                this.barGrab = event.x() - thumbRect.x();
            } else {
                final float page = Math.max(this.lineHeight, this.viewport.width() - this.lineHeight);
                this.scrollXTo(this.scrollX + (event.x() < thumbRect.x() ? -page : page));
            }
            event.consume();
            return;
        }
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
        this.editor.press(event, this.indexAt(event.localX(), event.localY()));
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
        if (this.draggingHorizontalBar) {
            this.scrollXTo(this.horizontalBar.offsetForThumbAt(event.x() - this.barGrab, this.maxScrollX));
            event.consume();
            return;
        }
        if (event.clicks() >= 3) {
            return;
        }
        this.editor.drag(event, this.indexAt(event.localX(), event.localY()));
    }

    @Override
    protected void onDragEnd(final DragEvent event) {
        this.draggingBar = false;
        this.draggingHorizontalBar = false;
    }

    @Override
    protected void onScroll(final ScrollEvent event) {
        this.ensureRows();
        final boolean vertical = this.scrollBy(-event.scrollY() * this.lineHeight * WHEEL_LINES);
        final boolean horizontal = this.scrollXTo(this.scrollX - event.scrollX() * this.lineHeight * WHEEL_LINES);
        if (vertical || horizontal) {
            event.consume();
        }
    }

    private boolean scrollXTo(final float offset) {
        final float next = Math.max(0f, Math.min(offset, this.maxScrollX));
        if (next == this.scrollX) {
            return false;
        }
        this.scrollX = next;
        return true;
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
        final boolean handled;
        if (key.isEnter() && !modifiers.control() && !modifiers.alt()) {
            this.edits.edit(this.editor.selectionStart(), this.editor.selectionEnd(), "\n");
            handled = true;
        } else {
            handled = this.editor.key(key, modifiers);
        }
        if (handled || TextEditor.typesCharacter(key, modifiers)) {
            event.consume();
        }
    }

    @Override
    protected void onChar(final CharEvent event) {
        if (this.isEnabled() && this.editor.type(event.codePoint())) {
            event.consume();
        }
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    /**
     * Marks the caret and selection out of date and scrolls to the caret on the next frame, after the caret or the
     * text changed.
     */
    private void caretMoved() {
        this.visualsDirty = true;
        this.revealPending = true;
    }

    /**
     * Reports a change of the text, from any source: {@link TextArea#EDITED}, then {@link TextArea#MODIFIED}.
     *
     * @param reveal whether the next frame scrolls to the caret; false keeps the view where it is
     */
    private void changed(final String previous, final boolean reveal) {
        this.rowsDirty = true;
        this.editor.moved();
        if (!reveal) {
            this.revealPending = false;
        }
        this.report(EDITED, this.buffer.toString(), previous);
        if (this.isHandled(MODIFIED)) {
            this.report(MODIFIED, this.codePoints(), previous.codePointCount(0, previous.length()));
        }
    }

    // ------------------------------------------------------------------
    // Text helpers
    // ------------------------------------------------------------------

    private int codePoints() {
        return this.buffer.codePointCount(0, this.buffer.length());
    }

    private int previousCodePoint(final int index) {
        return this.boundaries.previousCodePoint(index);
    }

    private int nextCodePoint(final int index) {
        return this.boundaries.nextCodePoint(index);
    }

    private int lineStart(final int index) {
        final int newline = this.buffer.lastIndexOf("\n", index - 1);
        return newline < 0 ? 0 : newline + 1;
    }

    private int lineEnd(final int index) {
        final int newline = this.buffer.indexOf("\n", index);
        return newline < 0 ? this.buffer.length() : newline;
    }

    @Override
    protected Object snapshotState() {
        return new TextArea.State(this.buffer.toString(), this.editor.caret(), this.editor.anchor(), this.maxLength);
    }

    /**
     * What the area tells its {@link TextEditor}: how the user's edits apply. Copying is always allowed.
     */
    private final class Editing implements TextEditor.Host {
        @Override
        public Environment environment() {
            return TextAreaImpl.this.environment();
        }

        @Override
        public boolean isReadOnly() {
            return TextAreaImpl.this.readOnly;
        }

        @Override
        public boolean allowsCopy() {
            return true;
        }

        @Override
        public void selectAll() {
            TextAreaImpl.this.selectAll();
        }

        @Override
        public void type(final int start, final int end, final int codePoint) {
            TextAreaImpl.this.edits.edit(start, end, Character.toString(codePoint));
        }

        @Override
        public void delete(final int start, final int end) {
            TextAreaImpl.this.edits.edit(start, end, "");
        }

        @Override
        public void paste(final int start, final int end) {
            TextAreaImpl.this.environment().clipboardText()
                    .map(FieldText::lines)
                    .ifPresent(text -> TextAreaImpl.this.edits.edit(start, end, text));
        }

        @Override
        public void copy(final int start, final int end) {
            TextAreaImpl.this.environment().setClipboardText(TextAreaImpl.this.buffer.substring(start, end));
        }

        @Override
        public void beforeMove() {
        }
    }

    /**
     * The area's rows, as its {@link TextEditor} moves through them. The area brings its rows and caret up to date
     * before it hands the editor a key.
     */
    private final class RowMoves implements TextEditor.Rows {
        @Override
        public int rowStart(final int index) {
            return TextAreaImpl.this.rowStart[TextAreaImpl.this.rowOf(index)];
        }

        @Override
        public int rowEnd(final int index) {
            return TextAreaImpl.this.rowLimit(TextAreaImpl.this.rowOf(index));
        }

        @Override
        public int lineStart(final int index) {
            return TextAreaImpl.this.lineStart(index);
        }

        @Override
        public int lineEnd(final int index) {
            return TextAreaImpl.this.lineEnd(index);
        }

        @Override
        public float caretX() {
            return TextAreaImpl.this.caretX;
        }

        @Override
        public int indexAt(final int count, final float x) {
            final TextAreaImpl area = TextAreaImpl.this;
            final int target = area.caretRow + count;
            if (target < 0) {
                return 0;
            }
            return target >= area.rowCount ? area.buffer.length() : area.indexOnRow(target, x);
        }

        @Override
        public int pageRows() {
            return Math.max(1, (int) Math.floor(TextAreaImpl.this.viewport.height() / TextAreaImpl.this.lineHeight) - 1);
        }
    }

    /**
     * What the area tells its {@link PlainEdits}: its length limit and line rule, and its way to the edit requests and
     * reports. It has no filter of its own.
     */
    private final class Edits implements PlainEdits.Field {
        @Override
        public boolean isReadOnly() {
            return TextAreaImpl.this.readOnly;
        }

        @Override
        public int maxLength() {
            return TextAreaImpl.this.maxLength;
        }

        @Override
        public String clean(final String text) {
            return FieldText.lines(text);
        }

        @Override
        public UnaryOperator<String> filter() {
            return null;
        }

        @Override
        public boolean hearsEdits() {
            return TextAreaImpl.this.isHandled(MODIFY) || TextAreaImpl.this.isHandled(EDIT);
        }

        @Override
        public boolean askModify() {
            return TextAreaImpl.this.request(MODIFY);
        }

        @Override
        public void askEdit(final String candidate, final Consumer<String> apply) {
            TextAreaImpl.this.request(EDIT, candidate, apply);
        }

        @Override
        public void userChanged(final String previous) {
            TextAreaImpl.this.changed(previous, true);
        }
    }
}
