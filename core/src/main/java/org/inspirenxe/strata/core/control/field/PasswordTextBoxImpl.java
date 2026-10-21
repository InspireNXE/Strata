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
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.button.ButtonImpl;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.BuiltInComposite;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * The core {@link PasswordTextBox}. It edits through a {@code LineEditor} over a {@code SecretBuffer}, the same
 * editor a {@link TextBoxImpl} has over its text, with copying turned off and the whole text one word.
 *
 * <p>The box is a focusable composite, as a tab bar is, whose one private child is the eye: a square one line tall
 * at the right of the content box, {@link #REVEAL_GAP} units from the text, shown while the box is revealable and not
 * empty. The eye cannot take focus, so a press on it focuses the box; it consumes the press, so the caret stays put,
 * and a click on it asks {@link PasswordTextBox#REVEAL}. Without theme paints it draws an eye from filled squares of
 * the largest whole size that fits nine across, on a grid centered in its square: in the placeholder's color, the
 * text color when hovered or while the text shows (with a slash), and the disabled text color while the user cannot
 * reveal the text. It draws nothing while the box is disabled.</p>
 *
 * <p>Masked, the box measures the mask string for the whole length once per edit and places the boundary after the
 * k-th of n code points at k over n of that width, which is exact because every glyph is the same. Revealed, it makes
 * the shown text as a {@code String} once per edit, measures each code point of it in turn to place the boundaries,
 * and drops all of it when the text hides. Typed code points go into the buffer as ints.</p>
 *
 * <p>A user's edit asks {@link PasswordTextBox#MODIFY} with the buffer unchanged and applies only if it was not
 * canceled, so the box never needs a second copy of its text to fall back to. Every change reports
 * {@link PasswordTextBox#MODIFIED} with the lengths. Core never logs the text or a typed code point.</p>
 *
 * <p>The box wipes itself in {@link #onReleased()}, which the root calls after the screen that holds it closes or
 * rebuilds and after the overlay that holds it closes, and hides its text in {@link #onDisabled()} when it is
 * disabled.</p>
 */
public final class PasswordTextBoxImpl extends BuiltInComposite<PasswordTextBox> implements PasswordTextBox {
    /**
     * The space, in UI units, between the text and the eye.
     */
    public static final float REVEAL_GAP = 2f;

    private final ComponentFactoryImpl ui;
    private final SecretBuffer buffer;
    private final LineEditor editor;
    private final RevealEye eye;
    private int maxLength;
    // The padding in use: the one set, else the theme's
    private Insets textPadding;
    private Insets setPadding;
    private Insets themePadding;
    private boolean readOnly;
    private boolean selectAllOnFocus;
    private int maskCodePoint;
    private boolean revealable;
    private boolean revealed;
    // Set while the text came from code and the user has neither emptied it nor replaced all of it
    private boolean fromCode;
    private IntPredicate filter;

    // Theme
    private Paint normal;
    private Paint focusedPaint;
    private Paint disabledPaint;
    private FieldLook look;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;
    private TextStyle readOnlyTextStyle;
    private TextStyle maskStyle;
    private TextStyle placeholderStyle;
    private Text placeholder;
    // Made during measure
    private TextLine placeholderLine;
    private float lineHeight;

    // The text as drawn, made again after each edit: the mask, or the shown text and its boundaries
    private TextSpan drawn;
    private int drawnCount;
    private float drawnWidth;
    private float[] shownX;

    /**
     * Creates an empty box with no length limit, {@link PasswordTextBox#DEFAULT_MASK} as its mask, and reveal on.
     *
     * @param ui the factory that owns the box
     */
    @SuppressWarnings("this-escape")
    public PasswordTextBoxImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.ui = ui;
        this.buffer = new SecretBuffer();
        this.editor = new LineEditor(this.buffer, new TextBoundaries(this.buffer, true), new Editing());
        this.maxLength = Integer.MAX_VALUE;
        this.textPadding = TextBoxImpl.DEFAULT_PADDING;
        this.themePadding = TextBoxImpl.DEFAULT_PADDING;
        this.maskCodePoint = DEFAULT_MASK;
        this.revealable = true;
        this.normal = Paint.NONE;
        this.focusedPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.look = FieldLook.plain(TextBoxImpl.DEFAULT_SELECTION, TextBoxImpl.DEFAULT_CARET);
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.readOnlyTextStyle = TextStyle.DEFAULT;
        this.maskStyle = TextStyle.DEFAULT;
        this.placeholderStyle = TextStyle.DEFAULT;
        this.placeholder = Text.empty();
        this.placeholderLine = TextLine.EMPTY;
        this.setCursor(CursorKind.TEXT);
        this.setMinSize(20f, 0f);
        this.eye = new RevealEye(ui, this);
        this.addChild(this.eye);
        this.eye.sync();
    }

    // ------------------------------------------------------------------
    // Reading and setting the text
    // ------------------------------------------------------------------

    @Override
    public char[] copyPassword() {
        final char[] copy = new char[this.buffer.length()];
        this.buffer.copyTo(copy);
        return copy;
    }

    @Override
    public <R> R withPassword(final Function<char[], ? extends R> reader) {
        Objects.requireNonNull(reader, "reader");
        final char[] copy = this.copyPassword();
        try {
            return reader.apply(copy);
        } finally {
            Arrays.fill(copy, '\0');
        }
    }

    @Override
    public boolean matches(final PasswordTextBox other) {
        if (other instanceof PasswordTextBoxImpl impl) {
            return this.buffer.matches(impl.buffer);
        }
        return Objects.requireNonNull(other, "other").withPassword(this.buffer::matches);
    }

    @Override
    public PasswordTextBox setPassword(final char[] password) {
        Objects.requireNonNull(password, "password");
        final SecretBuffer next = new SecretBuffer();
        try {
            int count = 0;
            int i = 0;
            while (i < password.length && count < this.maxLength) {
                final char c = password[i];
                if (c == '\r' || c == '\n') {
                    i++;
                    continue;
                }
                final int size = Character.isHighSurrogate(c) && i + 1 < password.length
                        && Character.isLowSurrogate(password[i + 1]) ? 2 : 1;
                next.insert(next.length(), password, i, size);
                i += size;
                count++;
            }
            if (next.matches(this.buffer)) {
                return this;
            }
            final int before = this.length();
            this.buffer.clear();
            final char[] chars = new char[next.length()];
            try {
                next.copyTo(chars);
                this.buffer.insert(0, chars, 0, chars.length);
            } finally {
                Arrays.fill(chars, '\0');
            }
            this.editor.place(this.buffer.length());
            this.fromCode = !this.buffer.isEmpty();
            this.setRevealed(false);
            this.changed(before);
        } finally {
            next.clear();
        }
        return this;
    }

    @Override
    public int length() {
        return this.buffer.codePointCount(0, this.buffer.length());
    }

    @Override
    public boolean isEmpty() {
        return this.buffer.isEmpty();
    }

    @Override
    public PasswordTextBox clear() {
        if (this.buffer.isEmpty()) {
            return this;
        }
        final int before = this.length();
        this.buffer.clear();
        this.editor.place(0);
        this.fromCode = false;
        this.changed(before);
        return this;
    }

    @Override
    protected void onReleased() {
        this.buffer.clear();
        this.editor.place(0);
        this.fromCode = false;
        this.revealed = false;
        this.forgetDrawn();
        this.eye.sync();
    }

    @Override
    protected void onDisabled() {
        this.setRevealed(false);
        this.eye.sync();
    }

    @Override
    public int maxLength() {
        return this.maxLength;
    }

    @Override
    public PasswordTextBox setMaxLength(final int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length must not be negative: " + maxLength);
        }
        this.maxLength = maxLength;
        final int before = this.length();
        if (before > maxLength) {
            this.buffer.delete(this.buffer.offsetByCodePoints(0, maxLength), this.buffer.length());
            this.editor.clamp();
            this.changed(before);
        }
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public PasswordTextBox setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        this.editor.invalidate();
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
    public PasswordTextBox select(final int anchor, final int caret) {
        this.editor.select(anchor, caret);
        return this;
    }

    @Override
    public PasswordTextBox selectAll() {
        this.editor.selectAll();
        return this;
    }

    @Override
    public PasswordTextBox setTextPadding(final Insets padding) {
        this.setPadding = padding;
        this.textPadding = padding != null ? padding : this.themePadding;
        this.invalidateLayout();
        return this;
    }

    // ------------------------------------------------------------------
    // The mask and peeking
    // ------------------------------------------------------------------

    @Override
    public int maskCodePoint() {
        return this.maskCodePoint;
    }

    @Override
    public PasswordTextBox setMaskCodePoint(final int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Not a code point: " + codePoint);
        }
        this.maskCodePoint = codePoint;
        this.editor.invalidate();
        return this;
    }

    @Override
    public boolean isRevealable() {
        return this.revealable;
    }

    @Override
    public PasswordTextBox setRevealable(final boolean revealable) {
        if (this.revealable != revealable) {
            this.revealable = revealable;
            if (!revealable) {
                this.setRevealed(false);
            }
            this.eye.sync();
        }
        return this;
    }

    @Override
    public boolean isRevealed() {
        return this.revealed;
    }

    @Override
    public PasswordTextBox setRevealed(final boolean revealed) {
        if (this.revealed != revealed) {
            this.revealed = revealed;
            this.forgetDrawn();
            this.eye.sync();
            this.report(REVEALED, revealed, !revealed);
        }
        return this;
    }

    /**
     * Drops the drawn text, the shown {@code String} included, so the next paint makes it again.
     */
    private void forgetDrawn() {
        this.drawn = null;
        this.shownX = null;
        this.drawnCount = 0;
        this.drawnWidth = 0f;
        this.editor.invalidate();
    }

    /**
     * Returns whether the user may show the text now: revealable, enabled, not empty and not set from code.
     */
    private boolean userMayReveal() {
        return this.revealable && this.isEnabled() && !this.buffer.isEmpty() && !this.fromCode;
    }

    /**
     * Toggles the text for the user, from the eye or Alt+F8, through {@link PasswordTextBox#REVEAL}. Hiding is always
     * allowed; showing only while {@link #userMayReveal()}.
     */
    private void userToggleReveal() {
        if (!this.isEnabled() || !this.revealable) {
            return;
        }
        final boolean wanted = !this.revealed;
        if (wanted && !this.userMayReveal()) {
            return;
        }
        this.request(REVEAL, wanted, value -> {
            if (value != null) {
                this.setRevealed(value);
            }
        });
    }

    // ------------------------------------------------------------------
    // Shared with TextBox
    // ------------------------------------------------------------------

    @Override
    public Text placeholder() {
        return this.placeholder;
    }

    @Override
    public PasswordTextBox setPlaceholder(final Text placeholder) {
        this.placeholder = placeholder == null ? Text.empty() : placeholder;
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean selectsAllOnFocus() {
        return this.selectAllOnFocus;
    }

    @Override
    public PasswordTextBox setSelectAllOnFocus(final boolean selectAll) {
        this.selectAllOnFocus = selectAll;
        return this;
    }

    @Override
    public PasswordTextBox setCodePointFilter(final IntPredicate filter) {
        this.filter = filter;
        return this;
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return TextBoxImpl.REQUIRED;
    }

    @Override
    protected boolean acceptsTextInput() {
        return true;
    }

    @Override
    protected TextInputKind textInputKind() {
        return TextInputKind.SECRET;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.themePadding = theme.insetsOr(TextBox.STYLE_PADDING, TextBoxImpl.DEFAULT_PADDING);
        if (this.setPadding == null) {
            this.textPadding = this.themePadding;
        }
        this.normal = theme.paintOr(TextBox.STYLE_NORMAL, Paint.NONE);
        this.focusedPaint = theme.paintOr(TextBox.STYLE_FOCUSED, this.normal);
        this.disabledPaint = theme.paintOr(TextBox.STYLE_DISABLED, this.normal);
        this.look = FieldLook.of(theme, TextBoxImpl.DEFAULT_SELECTION, TextBoxImpl.DEFAULT_CARET);
        this.textStyle = theme.textStyleOr(TextBox.STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(TextBox.STYLE_TEXT_DISABLED, this.textStyle);
        this.readOnlyTextStyle = theme.textStyleOr(TextBox.STYLE_TEXT_READ_ONLY, this.disabledTextStyle);
        this.maskStyle = theme.textStyleOr(STYLE_MASK, this.textStyle);
        this.placeholderStyle = theme.textStyleOr(TextBox.STYLE_PLACEHOLDER, this.disabledTextStyle);
        this.forgetDrawn();
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.lineHeight = Math.max(metrics.lineHeight(this.textStyle),
                Math.max(metrics.lineHeight(this.disabledTextStyle), metrics.lineHeight(this.maskStyle)));
        this.placeholderLine = TextLine.of(this.placeholder, this.placeholderStyle, metrics);
        context.measure(this.eye, this.lineHeight, this.lineHeight);
        return new Size(TextBoxImpl.DEFAULT_WIDTH,
                Math.max(ButtonImpl.MIN_HEIGHT, this.lineHeight + this.textPadding.vertical()));
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect content = bounds.shrink(this.textPadding);
        final float y = content.y() + (content.height() - this.lineHeight) * 0.5f;
        context.arrange(this.eye, new Rect(content.right() - this.lineHeight, y, this.lineHeight, this.lineHeight));
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

        final Rect content = bounds.shrink(this.textPadding);
        // The text keeps the eye's room whenever the box is revealable, so it does not move when the eye appears
        final Rect text = this.revealable
                ? new Rect(content.x(), content.y(), Math.max(0f, content.width() - this.lineHeight - REVEAL_GAP),
                        content.height())
                : content;
        final float textY = content.y() + (content.height() - this.lineHeight) * 0.5f;
        this.editor.paint(painter, text, textY, this.lineHeight, this.look, this.shownStyle(),
                this.isFocused() && this.isEnabled() && !this.readOnly);
    }

    /**
     * The style of the shown text.
     */
    private TextStyle shownStyle() {
        if (!this.isEnabled()) {
            return this.disabledTextStyle;
        }
        return this.readOnly ? this.readOnlyTextStyle : this.textStyle;
    }

    /**
     * The style of the mask: its own key while enabled and editable, the text box's disabled and read-only styles
     * otherwise.
     */
    private TextStyle currentMaskStyle() {
        if (!this.isEnabled()) {
            return this.disabledTextStyle;
        }
        return this.readOnly ? this.readOnlyTextStyle : this.maskStyle;
    }

    // ------------------------------------------------------------------
    // Pointer and keyboard
    // ------------------------------------------------------------------

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        this.editor.press(event, event.localX() - this.textPadding.left());
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
        this.editor.drag(event, event.localX() - this.textPadding.left());
    }

    @Override
    protected void onFocusGained(final FocusEvent event) {
        this.editor.focusGained(this.selectAllOnFocus, this.isFocusFromPress());
    }

    @Override
    protected void onFocusLost(final FocusEvent event) {
        this.editor.focusLost();
        this.setRevealed(false);
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        final Key key = event.key();
        final Modifiers modifiers = event.modifiers();
        if (key.isEnter() && event.action() == KeyAction.PRESS) {
            final boolean heard = this.isHandled(SUBMIT) || this.isHandled(SUBMITTED);
            if (this.request(SUBMIT)) {
                this.report(SUBMITTED);
            }
            if (heard) {
                event.consume();
            }
            return;
        }
        if (key == Key.F8 && modifiers.alt() && !modifiers.control()) {
            if (event.action() == KeyAction.PRESS) {
                this.userToggleReveal();
            }
            event.consume();
            return;
        }
        final boolean handled = this.editor.key(key, modifiers);
        // Typing keys arrive as characters too; keep the engine from also reading them as shortcuts
        if (handled || (!modifiers.control() && !modifiers.alt() && key.isPrintable())) {
            event.consume();
        }
    }

    @Override
    protected void onChar(final CharEvent event) {
        // Typed characters stop here, read-only or not, so no ancestor's handler sees them
        this.editor.type(event.codePoint());
        event.consume();
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    /**
     * Finishes an edit by the user: the text no longer counts as set from code once the user emptied it or replaced
     * all of it. Then reports the change.
     */
    private void userEdited(final boolean replacedAll, final int before) {
        if (replacedAll || this.buffer.isEmpty()) {
            this.fromCode = false;
        }
        this.changed(before);
    }

    /**
     * Reports a change of the text, from any source, and shows or hides the eye.
     */
    private void changed(final int before) {
        this.editor.moved();
        this.forgetDrawn();
        this.eye.sync();
        if (this.isHandled(MODIFIED)) {
            this.report(MODIFIED, this.length(), before);
        }
    }

    private boolean replacesAll(final int start, final int end) {
        return start == 0 && end == this.buffer.length() && end > 0;
    }

    @Override
    protected Object snapshotState() {
        return new PasswordTextBox.State(this.length(), this.editor.caret(), this.editor.anchor(), this.maxLength,
                this.revealed);
    }

    /**
     * Returns the buffer, for tests that check what is left in it. Nothing else may call it.
     */
    SecretBuffer secretBuffer() {
        return this.buffer;
    }

    /**
     * What the box tells its {@link LineEditor}: how to measure and draw the mask or the shown text, and how the
     * user's edits apply. Copying is never allowed.
     */
    private final class Editing implements LineEditor.Owner {
        @Override
        public Environment environment() {
            return PasswordTextBoxImpl.this.environment();
        }

        @Override
        public boolean isReadOnly() {
            return PasswordTextBoxImpl.this.readOnly;
        }

        @Override
        public boolean allowsCopy() {
            return false;
        }

        @Override
        public float layoutText(final TextMetrics metrics) {
            final PasswordTextBoxImpl box = PasswordTextBoxImpl.this;
            final int count = box.length();
            box.drawnCount = count;
            box.shownX = null;
            if (count == 0) {
                box.drawn = null;
                box.drawnWidth = 0f;
                return 0f;
            }
            if (box.revealed) {
                final TextStyle style = box.shownStyle();
                final String shown = box.buffer.reveal();
                final float[] boundaries = new float[count + 1];
                float x = 0f;
                int k = 0;
                for (int i = 0; i < shown.length(); ) {
                    final int next = i + Character.charCount(shown.codePointAt(i));
                    x += metrics.width(new TextSpan(shown.substring(i, next), style));
                    boundaries[++k] = x;
                    i = next;
                }
                box.drawn = new TextSpan(shown, style);
                box.shownX = boundaries;
            } else {
                box.drawn = new TextSpan(Character.toString(box.maskCodePoint).repeat(count), box.currentMaskStyle());
            }
            box.drawnWidth = metrics.width(box.drawn);
            return box.drawnWidth;
        }

        @Override
        public float xForIndex(final TextMetrics metrics, final int index) {
            final PasswordTextBoxImpl box = PasswordTextBoxImpl.this;
            if (index == 0 || box.drawnCount == 0) {
                return 0f;
            }
            final int k = Math.min(box.drawnCount, box.buffer.codePointCount(0, index));
            if (box.shownX != null) {
                return box.shownX[k];
            }
            return box.drawnWidth * k / box.drawnCount;
        }

        @Override
        public int indexForX(final TextMetrics metrics, final float x) {
            final PasswordTextBoxImpl box = PasswordTextBoxImpl.this;
            final int count = box.drawnCount;
            if (count == 0) {
                return 0;
            }
            int k;
            if (box.shownX != null) {
                k = count;
                for (int i = 1; i <= count; i++) {
                    if (box.shownX[i] >= x) {
                        k = x - box.shownX[i - 1] < box.shownX[i] - x ? i - 1 : i;
                        break;
                    }
                }
            } else {
                k = Math.round(x * count / box.drawnWidth);
            }
            return box.buffer.offsetByCodePoints(0, Math.max(0, Math.min(count, k)));
        }

        @Override
        public void drawText(final Painter painter, final float originX, final float left, final float y,
                             final TextStyle style) {
            final PasswordTextBoxImpl box = PasswordTextBoxImpl.this;
            if (box.buffer.isEmpty()) {
                if (!box.placeholderLine.isEmpty() && !(box.look.hidePlaceholder() && box.isFocused())) {
                    box.placeholderLine.draw(painter, left, y);
                }
                return;
            }
            if (box.drawn != null) {
                painter.text(style == null ? box.drawn : new TextSpan(box.drawn.text(), style), originX, y);
            }
        }

        @Override
        public void selectAll() {
            PasswordTextBoxImpl.this.selectAll();
        }

        @Override
        public void type(final int start, final int end, final int codePoint) {
            final PasswordTextBoxImpl box = PasswordTextBoxImpl.this;
            if (box.readOnly || box.filter != null && !box.filter.test(codePoint)) {
                return;
            }
            final int before = box.length();
            final boolean fits = before - box.buffer.codePointCount(start, end) < box.maxLength;
            if (!fits && start == end) {
                return;
            }
            if (!box.request(MODIFY)) {
                return;
            }
            final boolean all = box.replacesAll(start, end);
            box.buffer.delete(start, end);
            int caret = start;
            if (fits) {
                box.buffer.insertCodePoint(start, codePoint);
                caret += Character.charCount(codePoint);
            }
            box.editor.place(caret);
            box.userEdited(all, before);
        }

        @Override
        public void delete(final int start, final int end) {
            final PasswordTextBoxImpl box = PasswordTextBoxImpl.this;
            if (box.readOnly || !box.request(MODIFY)) {
                return;
            }
            final int before = box.length();
            final boolean all = box.replacesAll(start, end);
            box.buffer.delete(start, end);
            box.editor.place(start);
            box.userEdited(all, before);
        }

        @Override
        public void paste(final int start, final int end) {
            final PasswordTextBoxImpl box = PasswordTextBoxImpl.this;
            if (box.readOnly) {
                return;
            }
            final Optional<String> clipboard = box.environment().clipboardText();
            if (clipboard.isEmpty()) {
                return;
            }
            final String text = TextBoxImpl.sanitize(clipboard.get());
            if (text.isEmpty()) {
                return;
            }
            // Refused whole: a password silently cut short or filtered is worse than none
            if (box.filter != null && !text.codePoints().allMatch(box.filter)) {
                return;
            }
            final int before = box.length();
            if ((long) before - box.buffer.codePointCount(start, end) + text.codePointCount(0, text.length())
                    > box.maxLength) {
                return;
            }
            if (!box.request(MODIFY)) {
                return;
            }
            final boolean all = box.replacesAll(start, end);
            box.buffer.delete(start, end);
            box.buffer.insert(start, text);
            box.editor.place(start + text.length());
            box.userEdited(all, before);
        }

        @Override
        public void copy(final int start, final int end) {
            // Never called: allowsCopy is false
        }

        @Override
        public void beforeMove() {
        }
    }

    /**
     * The eye at the right end of the box: a private child, so it has its own hover, an arrow cursor and a tooltip.
     * It cannot take focus.
     */
    private static final class RevealEye extends BuiltInComponent<RevealEye> implements HasCursor<RevealEye>,
            HasTooltip<RevealEye> {
        // An eye on a grid nine squares wide, as runs of {row, first column, column after the last}
        private static final int[][] EYE = {
                {1, 3, 6},
                {2, 1, 3}, {2, 6, 8},
                {3, 0, 1}, {3, 3, 6}, {3, 8, 9},
                {4, 0, 1}, {4, 3, 6}, {4, 8, 9},
                {5, 0, 1}, {5, 3, 6}, {5, 8, 9},
                {6, 1, 3}, {6, 6, 8},
                {7, 3, 6}};
        private static final int GRID = 9;

        private final PasswordTextBoxImpl box;
        private final Tooltip show;
        private final Tooltip hide;
        // The theme's paints, or null to draw the eye
        private Paint reveal;
        private Paint revealHovered;
        private Paint revealActive;
        private Paint revealDisabled;
        private int placeholderColor;
        private int textColor;
        private int disabledColor;
        // The squares of the drawn eye and its slash, worked out again only when the bounds change
        private Rect drawnFor;
        private Rect[] eyeRects;
        private Rect[] slashRects;

        @SuppressWarnings("this-escape")
        RevealEye(final ComponentFactoryImpl ui, final PasswordTextBoxImpl box) {
            super(ui);
            this.box = box;
            this.show = Tooltip.text(Text.translatable("strata.password.show"));
            this.hide = Tooltip.text(Text.translatable("strata.password.hide"));
            this.eyeRects = new Rect[0];
            this.slashRects = new Rect[0];
            this.setCursor(CursorKind.ARROW);
            this.setAnchor(Anchor.FILL);
        }

        /**
         * Shows the eye while the box is revealable and not empty, and picks its tooltip.
         */
        void sync() {
            this.setVisible(this.box.revealable && !this.box.buffer.isEmpty());
            // A disabled box's eye takes no pointer input, so whether it is enabled does not matter here
            final Tooltip wanted;
            if (this.box.revealed) {
                wanted = this.hide;
            } else {
                wanted = this.box.revealable && !this.box.buffer.isEmpty() && !this.box.fromCode ? this.show : null;
            }
            // Set only on a change, so typing does not refresh a tooltip that is showing
            if (this.tooltip() != wanted) {
                this.setTooltip(wanted);
            }
        }

        @Override
        protected void onApplyTheme(final Theme theme) {
            this.reveal = theme.paint(STYLE_REVEAL).orElse(null);
            this.revealHovered = theme.paint(STYLE_REVEAL_HOVERED).orElse(this.reveal);
            this.revealActive = theme.paint(STYLE_REVEAL_ACTIVE).orElse(null);
            this.revealDisabled = theme.paint(STYLE_REVEAL_DISABLED).orElse(this.reveal);
            final TextStyle text = theme.textStyleOr(TextBox.STYLE_TEXT, TextStyle.DEFAULT);
            final TextStyle disabled = theme.textStyleOr(TextBox.STYLE_TEXT_DISABLED, text);
            this.textColor = text.color();
            this.disabledColor = disabled.color();
            this.placeholderColor = theme.textStyleOr(TextBox.STYLE_PLACEHOLDER, disabled).color();
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(this.box.lineHeight, this.box.lineHeight);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            if (!this.box.isEnabled()) {
                return;
            }
            if (this.box.revealed) {
                this.paintOr(painter, bounds, this.revealActive, this.textColor, true);
            } else if (!this.box.userMayReveal()) {
                this.paintOr(painter, bounds, this.revealDisabled, this.disabledColor, false);
            } else if (this.isHovered()) {
                this.paintOr(painter, bounds, this.revealHovered, this.textColor, false);
            } else {
                this.paintOr(painter, bounds, this.reveal, this.placeholderColor, false);
            }
        }

        private void paintOr(final Painter painter, final Rect bounds, final Paint paint, final int color,
                             final boolean slash) {
            if (paint != null) {
                paint.paint(painter, bounds);
                return;
            }
            if (!bounds.equals(this.drawnFor)) {
                this.layoutEye(bounds);
            }
            for (final Rect rect : this.eyeRects) {
                painter.fill(rect, color);
            }
            if (slash) {
                for (final Rect rect : this.slashRects) {
                    painter.fill(rect, color);
                }
            }
        }

        /**
         * Works out the squares of the eye and the slash for a square, each square the largest whole size that fits
         * nine across, the grid centered.
         */
        private void layoutEye(final Rect bounds) {
            this.drawnFor = bounds;
            final float size = Math.min(bounds.width(), bounds.height());
            final float cell = Math.max(1f, (float) Math.floor(size / GRID));
            final float x = bounds.x() + (float) Math.floor((bounds.width() - cell * GRID) * 0.5f);
            final float y = bounds.y() + (float) Math.floor((bounds.height() - cell * GRID) * 0.5f);
            this.eyeRects = new Rect[EYE.length];
            for (int i = 0; i < EYE.length; i++) {
                final int[] run = EYE[i];
                this.eyeRects[i] = new Rect(x + run[1] * cell, y + run[0] * cell, (run[2] - run[1]) * cell, cell);
            }
            this.slashRects = new Rect[GRID];
            for (int i = 0; i < GRID; i++) {
                this.slashRects[i] = new Rect(x + i * cell, y + i * cell, cell, cell);
            }
        }

        @Override
        protected void onPointerPress(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT) {
                // The press focused the box; keep its caret where it was
                this.box.editor.cancelPress();
                event.consume();
            }
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            if (event.button() == MouseButton.LEFT) {
                this.box.userToggleReveal();
                event.consume();
            }
        }
    }
}
