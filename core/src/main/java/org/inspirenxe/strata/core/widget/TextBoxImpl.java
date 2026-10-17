package org.inspirenxe.strata.core.widget;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.CharEvent;
import org.inspirenxe.strata.api.event.DragEvent;
import org.inspirenxe.strata.api.event.FocusEvent;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.api.widget.TextBoxBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The core {@link TextBox}, a single-line field ported from Wintergreen's {@code TextBox}.
 *
 * <p>The box is focusable and tabbable, shows a text cursor, accepts text input and is at least 20 by 20 units. It
 * asks for {@link #DEFAULT_WIDTH} units of width, so give it a fixed width or stretch it to change that. Text sits
 * 4 units in from the left and right edges and 5 from the top and bottom unless the padding is changed. Text wider
 * than the box scrolls sideways to keep the caret in view.</p>
 *
 * <p>Where it departs from Wintergreen:</p>
 * <ul>
 *     <li>A click places the caret when it happens. Wintergreen waited for the next frame because only painting
 *     could measure text; here the {@code Environment} measures at any time.</li>
 *     <li>The clipboard and the time come from the host's platform instead of AWT and the wall clock.</li>
 *     <li>Editing works in whole code points, so surrogate pairs are never split.</li>
 *     <li>Double-clicking the first letter of a word selects that word. Wintergreen selected the word before it.</li>
 * </ul>
 */
public final class TextBoxImpl extends CustomComponent<TextBox> implements TextBox {
    /**
     * How long, in milliseconds, the caret stays shown and then hidden. Each edit or caret move restarts the cycle
     * with the caret shown.
     */
    public static final long BLINK_MILLIS = 500L;
    /**
     * The width, in UI units, the box asks for when measured.
     */
    public static final float DEFAULT_WIDTH = 120f;
    private static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_FOCUSED, STYLE_DISABLED);
    private static final Paint DEFAULT_SELECTION = Paint.solid(0x806A9FFF);
    private static final Paint DEFAULT_CARET = Paint.solid(0xFFE0E0E0);

    private final StringBuilder buffer;
    private final List<Consumer<String>> listeners;
    private final List<Consumer<String>> submitListeners;
    private final Runnable drawContent;
    private int maxLength;
    private boolean password;
    private int maskCodePoint;
    private Insets textPadding;
    private UnaryOperator<String> filter;
    private boolean readOnly;
    private boolean selectAllOnFocus;

    // The text and selection before the edit in progress, for the filter to fall back to
    private String beforeText;
    private int beforeCaret;
    private int beforeAnchor;

    private int caret;
    private int anchor;
    private float scrollX;
    private long blinkStart;

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
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;
    private TextStyle readOnlyTextStyle;

    // Derived from text and caret, recomputed only after a change
    private boolean visualsDirty;
    private TextSpan display;
    private float textWidth;
    private float caretX;
    private float selectionStartX;
    private float selectionEndX;
    private float lineHeight;
    private Rect contentBox;
    private float textY;
    // Set only while paint runs, for drawContent
    private Painter painter;

    /**
     * Creates an empty box with no length limit and password mode off.
     *
     * @param ui the factory that owns the box
     * @param behavior the decorator for the box's hooks, or {@code null} for none
     */
    @SuppressWarnings("this-escape")
    public TextBoxImpl(final ComponentFactory ui, final TextBoxBehavior behavior) {
        super(ui, behavior);
        this.buffer = new StringBuilder();
        this.listeners = new ArrayList<>();
        this.submitListeners = new ArrayList<>();
        this.drawContent = this::drawContent;
        this.maxLength = Integer.MAX_VALUE;
        this.maskCodePoint = '*';
        this.textPadding = Insets.of(4f, 5f);
        this.normal = Paint.NONE;
        this.focusedPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.selectionPaint = DEFAULT_SELECTION;
        this.caretPaint = DEFAULT_CARET;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.readOnlyTextStyle = TextStyle.DEFAULT;
        this.display = TextSpan.of("");
        this.contentBox = Rect.ZERO;
        this.visualsDirty = true;
        this.setFocusable(true);
        this.setTabbable(true);
        this.setCursor(CursorKind.TEXT);
        this.setMinSize(20f, 20f);
    }

    // ------------------------------------------------------------------
    // TextBox
    // ------------------------------------------------------------------

    @Override
    public String text() {
        return this.buffer.toString();
    }

    @Override
    public TextBox setText(final String text) {
        final String clean = truncate(sanitize(Objects.requireNonNull(text, "text")), this.maxLength);
        if (!clean.contentEquals(this.buffer)) {
            this.buffer.setLength(0);
            this.buffer.append(clean);
            this.caret = this.buffer.length();
            this.anchor = this.caret;
            this.changed();
        }
        return this;
    }

    @Override
    public int maxLength() {
        return this.maxLength;
    }

    @Override
    public TextBox setMaxLength(final int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length must not be negative: " + maxLength);
        }
        this.maxLength = maxLength;
        if (this.codePoints() > maxLength) {
            this.buffer.setLength(this.buffer.offsetByCodePoints(0, maxLength));
            this.caret = Math.min(this.caret, this.buffer.length());
            this.anchor = Math.min(this.anchor, this.buffer.length());
            this.changed();
        }
        return this;
    }

    @Override
    public boolean isPassword() {
        return this.password;
    }

    @Override
    public TextBox setPassword(final boolean password) {
        this.password = password;
        this.visualsDirty = true;
        return this;
    }

    @Override
    public TextBox setMaskCodePoint(final int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Not a code point: " + codePoint);
        }
        this.maskCodePoint = codePoint;
        this.visualsDirty = true;
        return this;
    }

    @Override
    public TextBox setTextPadding(final Insets padding) {
        this.textPadding = Objects.requireNonNull(padding, "padding");
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
    public TextBox select(final int anchor, final int caret) {
        this.anchor = this.snap(anchor);
        this.caret = this.snap(caret);
        this.moved();
        return this;
    }

    @Override
    public TextBox selectAll() {
        this.anchor = 0;
        this.caret = this.buffer.length();
        this.moved();
        return this;
    }

    @Override
    public TextBox onTextChanged(final Consumer<String> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @Override
    public TextBox setFilter(final UnaryOperator<String> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    public TextBox onSubmit(final Consumer<String> listener) {
        this.submitListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public TextBox setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        this.visualsDirty = true;
        return this;
    }

    @Override
    public boolean selectsAllOnFocus() {
        return this.selectAllOnFocus;
    }

    @Override
    public TextBox setSelectAllOnFocus(final boolean selectAll) {
        this.selectAllOnFocus = selectAll;
        return this;
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
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
        this.normal = theme.paintOr(STYLE_NORMAL, Paint.NONE);
        this.focusedPaint = theme.paintOr(STYLE_FOCUSED, this.normal);
        this.disabledPaint = theme.paintOr(STYLE_DISABLED, this.normal);
        this.selectionPaint = theme.paintOr(STYLE_SELECTION, DEFAULT_SELECTION);
        this.caretPaint = theme.paintOr(STYLE_CARET, DEFAULT_CARET);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.readOnlyTextStyle = theme.textStyleOr(STYLE_TEXT_READ_ONLY, this.disabledTextStyle);
        this.visualsDirty = true;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.lineHeight = Math.max(context.text().lineHeight(this.textStyle), context.text().lineHeight(this.disabledTextStyle));
        return new Size(DEFAULT_WIDTH, this.lineHeight + this.textPadding.vertical());
    }

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

        this.contentBox = bounds.shrink(this.textPadding);
        this.textY = this.contentBox.y() + (this.contentBox.height() - this.lineHeight) * 0.5f;
        this.updateVisuals(this.environment().text());
        this.revealCaret();

        this.painter = painter;
        try {
            painter.clipped(this.contentBox, this.drawContent);
        } finally {
            this.painter = null;
        }

        if (this.isFocused() && this.isEnabled() && !this.readOnly && this.caretShown()) {
            final float x = Math.max(this.contentBox.x(),
                    Math.min(this.contentBox.right() - 1f, this.contentBox.x() + this.caretX - this.scrollX));
            this.caretPaint.paint(painter, new Rect(x, this.textY, 1f, this.lineHeight));
        }
    }

    /**
     * Runs inside the content clip during {@link #paint}; a field-held method reference, so painting allocates no
     * lambda per frame.
     */
    private void drawContent() {
        final float originX = this.contentBox.x() - this.scrollX;
        if (this.hasSelection()) {
            this.selectionPaint.paint(this.painter, new Rect(originX + this.selectionStartX, this.textY,
                    this.selectionEndX - this.selectionStartX, this.lineHeight));
        }
        this.painter.text(this.display, originX, this.textY);
    }

    private boolean caretShown() {
        return ((this.environment().millis() - this.blinkStart) / BLINK_MILLIS) % 2 == 0;
    }

    /**
     * Scrolls just enough to keep the caret inside the content box, and never past the text's end.
     */
    private void revealCaret() {
        final float view = this.contentBox.width();
        if (this.caretX - this.scrollX > view - 1f) {
            this.scrollX = this.caretX - view + 1f;
        }
        if (this.caretX - this.scrollX < 0f) {
            this.scrollX = this.caretX;
        }
        this.scrollX = Math.max(0f, Math.min(this.scrollX, Math.max(0f, this.textWidth + 1f - view)));
    }

    private void updateVisuals(final TextMetrics metrics) {
        if (!this.visualsDirty) {
            return;
        }
        this.visualsDirty = false;
        this.display = new TextSpan(this.displayPrefix(this.buffer.length()), this.style());
        this.textWidth = metrics.width(this.display);
        this.caretX = this.xForIndex(metrics, this.caret);
        this.selectionStartX = this.xForIndex(metrics, this.selectionStart());
        this.selectionEndX = this.xForIndex(metrics, this.selectionEnd());
    }

    private TextStyle style() {
        if (!this.isEnabled()) {
            return this.disabledTextStyle;
        }
        return this.readOnly ? this.readOnlyTextStyle : this.textStyle;
    }

    private String displayPrefix(final int index) {
        if (this.password) {
            return Character.toString(this.maskCodePoint).repeat(this.buffer.codePointCount(0, index));
        }
        return this.buffer.substring(0, index);
    }

    private float xForIndex(final TextMetrics metrics, final int index) {
        return index == 0 ? 0f : metrics.width(new TextSpan(this.displayPrefix(index), this.style()));
    }

    /**
     * The code point boundary nearest to {@code x}, measured from the start of the text.
     */
    private int indexForX(final TextMetrics metrics, final float x) {
        if (x <= 0f) {
            return 0;
        }
        int previous = 0;
        float previousX = 0f;
        int index = 0;
        while (index < this.buffer.length()) {
            index = this.nextCodePoint(index);
            final float indexX = this.xForIndex(metrics, index);
            if (indexX >= x) {
                return x - previousX < indexX - x ? previous : index;
            }
            previous = index;
            previousX = indexX;
        }
        return this.buffer.length();
    }

    /**
     * The index under a point given relative to the component's left edge.
     */
    private int indexAt(final float localX) {
        return this.indexForX(this.environment().text(), localX - this.textPadding.left() + this.scrollX);
    }

    // ------------------------------------------------------------------
    // Pointer
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        final int index = this.indexAt(event.localX());
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
                this.anchor = 0;
                this.caret = this.buffer.length();
            }
        }
        this.moved();
        event.consume();
    }

    @Override
    protected void onDragStart(final DragEvent event) {
        this.dragTo(event);
    }

    @Override
    protected void onDrag(final DragEvent event) {
        this.dragTo(event);
    }

    private void dragTo(final DragEvent event) {
        if (event.button() != MouseButton.LEFT || !this.isEnabled() || event.clicks() >= 3) {
            return;
        }
        final int index = this.indexAt(event.localX());
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
        this.moved();
        event.consume();
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    @Override
    protected void onFocusGained(final FocusEvent event) {
        // A press that gave focus places the caret right after this; keyboard focus starts at the end
        this.anchor = this.selectAllOnFocus ? 0 : this.buffer.length();
        this.caret = this.buffer.length();
        this.moved();
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        final Key key = event.key();
        final Modifiers modifiers = event.modifiers();
        if (key.isEnter() && event.action() == KeyAction.PRESS && !this.submitListeners.isEmpty()) {
            final String text = this.buffer.toString();
            for (final Consumer<String> listener : List.copyOf(this.submitListeners)) {
                listener.accept(text);
            }
            event.consume();
            return;
        }
        final boolean shift = modifiers.shift();
        final boolean handled;
        if (modifiers.control() && !modifiers.alt()) {
            handled = this.controlKey(key, shift);
        } else {
            handled = this.plainKey(key, shift);
        }
        // Typing keys arrive as characters too; keep the engine from also reading them as shortcuts
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
            case HOME -> this.moveTo(0, shift);
            case END -> this.moveTo(this.buffer.length(), shift);
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
        if (this.hasSelection() && !this.password) {
            this.environment().setClipboardText(this.selectedText());
        }
    }

    private void cut() {
        if (this.hasSelection() && !this.password && !this.readOnly) {
            this.copy();
            this.deleteSelection();
        }
    }

    private void paste() {
        this.environment().clipboardText().map(TextBoxImpl::sanitize).ifPresent(this::insert);
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

    /**
     * Remembers the text and selection before a user edit, for the filter to fall back to.
     *
     * @return false if the box is read-only and the edit must not happen
     */
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
     * Ends a user edit: runs the filter, then tells listeners if the text changed.
     */
    private void edited() {
        if (this.filter != null) {
            final String candidate = this.buffer.toString();
            final String result = this.filter.apply(candidate);
            if (result == null || result.equals(this.beforeText)) {
                this.buffer.setLength(0);
                this.buffer.append(this.beforeText);
                this.caret = this.beforeCaret;
                this.anchor = this.beforeAnchor;
                this.moved();
                return;
            }
            if (!result.equals(candidate)) {
                final int fromEnd = this.buffer.length() - this.caret;
                this.buffer.setLength(0);
                this.buffer.append(truncate(sanitize(result), this.maxLength));
                this.caret = this.snap(this.buffer.length() - fromEnd);
                this.anchor = this.caret;
            }
        }
        if (this.buffer.toString().equals(this.beforeText)) {
            this.moved();
            return;
        }
        this.changed();
    }

    private void moveTo(final int index, final boolean extend) {
        this.caret = this.snap(index);
        if (!extend) {
            this.anchor = this.caret;
        }
        this.moved();
    }

    private void moved() {
        this.visualsDirty = true;
        this.blinkStart = this.environment().millis();
    }

    private void changed() {
        this.moved();
        final String text = this.buffer.toString();
        for (final Consumer<String> listener : List.copyOf(this.listeners)) {
            listener.accept(text);
        }
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

    /**
     * Clamps to the text and moves off the middle of a surrogate pair.
     */
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

    /**
     * For Ctrl+Left and Ctrl+Backspace: skip whitespace leftwards, then the word before it.
     */
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

    /**
     * For Ctrl+Right and Ctrl+Delete: skip whitespace rightwards, then the word after it.
     */
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

    /**
     * The run of word or whitespace characters around an index, as {start, end}. At the very end of the text, the
     * run before it.
     */
    private int[] wordAround(final int index) {
        if (this.buffer.isEmpty()) {
            return new int[] {0, 0};
        }
        final int at = index >= this.buffer.length() ? this.previousCodePoint(index) : index;
        final boolean whitespace = Character.isWhitespace(this.buffer.codePointAt(at));
        int start = at;
        while (start > 0 && Character.isWhitespace(this.buffer.codePointBefore(start)) == whitespace) {
            start = this.previousCodePoint(start);
        }
        int end = at;
        while (end < this.buffer.length() && Character.isWhitespace(this.buffer.codePointAt(end)) == whitespace) {
            end = this.nextCodePoint(end);
        }
        return new int[] {start, end};
    }

    /**
     * Single line only: newlines are dropped, as Wintergreen did for pasted text.
     */
    private static String sanitize(final String text) {
        return text.replace("\r", "").replace("\n", "");
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
        return new TextBox.State(this.buffer.toString(), this.caret, this.anchor, this.maxLength, this.password);
    }
}
