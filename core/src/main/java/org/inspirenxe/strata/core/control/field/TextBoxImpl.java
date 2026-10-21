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
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
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
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextHighlighter;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.collection.ListViewImpl;
import org.inspirenxe.strata.core.control.button.ButtonImpl;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.StyledText;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The core {@link TextBox}, a single-line field ported from Wintergreen's {@code TextBox}.
 *
 * <p>The box is focusable and tabbable, shows a text cursor, accepts text input, is at least 20 units wide, and
 * measures at least 20 tall, though a smaller height set on it applies. It
 * asks for {@link #DEFAULT_WIDTH} units of width, so give it a fixed width or stretch it to change that. Text sits
 * 4 units in from the left and right edges and 5 from the top and bottom unless the padding is changed. Text wider
 * than the box scrolls sideways to keep the caret in view. The caret, the selection, the scroll and the key and
 * pointer handling are a {@code LineEditor}, which the password box also uses, over a {@code PlainBuffer}.</p>
 *
 * <p>Every user edit that would change the text fires {@link TextBox#MODIFY} and then {@link TextBox#EDIT} when a
 * handler for either is on its path. While the handlers run the box still holds the text and selection from before
 * the edit; a canceled {@code MODIFY} ends the edit there. The final text then has its line breaks dropped, is cut to
 * the length limit and goes through the internal filter, and the caret goes as many characters from its end as the
 * edit left it, with nothing selected. With neither handler on the path the edit applies directly, without putting
 * the old text back first. Every change reports {@link TextBox#EDITED} and then {@link TextBox#MODIFIED}. Enter fires
 * {@link TextBox#SUBMIT} on every press, but is consumed only when a handler for {@link TextBox#SUBMIT} or
 * {@link TextBox#SUBMITTED} is on the path, so an ancestor's key handler still sees Enter from a box nobody
 * submits.</p>
 *
 * <p>The suggestion list is a {@link ListViewImpl} in a popup, not focusable, built when it first opens and dropped
 * when it closes. The box routes the list's input to itself, so the list fires no events. Its rows are one line of
 * {@link TextBox#STYLE_SUGGESTION_TEXT} plus 4 units tall, with the text 4 units in from the left, and it is at least
 * as wide as the box. A press on a row takes focus away from the box, as a press on anything that cannot take focus
 * does; the box keeps the list open through that and takes focus back without moving the caret.</p>
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
public final class TextBoxImpl extends BuiltInComponent<TextBox> implements TextBox {
    /**
     * How long, in milliseconds, the caret stays shown and then hidden. Each edit or caret move restarts the cycle
     * with the caret shown.
     */
    public static final long BLINK_MILLIS = LineEditor.BLINK_MILLIS;
    /**
     * The width, in UI units, the box asks for when measured.
     */
    public static final float DEFAULT_WIDTH = 120f;
    /**
     * The padding around the text when the theme sets no {@value #STYLE_PADDING}: 4 at the sides and 5 at the top
     * and bottom.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(4f, 5f);
    static final Set<String> REQUIRED = Set.of(STYLE_NORMAL, STYLE_FOCUSED, STYLE_DISABLED);
    static final Paint DEFAULT_SELECTION = Paint.solid(0x806A9FFF);
    static final Paint DEFAULT_CARET = Paint.solid(0xFFE0E0E0);

    private final ComponentFactoryImpl ui;
    private final PlainBuffer buffer;
    private final LineEditor editor;
    private int maxLength;
    // The padding in use: the one set, else the theme's
    private Insets textPadding;
    private Insets setPadding;
    private Insets themePadding;
    private UnaryOperator<String> filter;
    private boolean readOnly;
    private boolean selectAllOnFocus;

    // Suggestions
    private SuggestionProvider suggestionProvider;
    private int maxVisibleSuggestions;
    private List<Suggestion> suggestions;
    private Overlay suggestionPopup;
    private ListViewImpl<Suggestion> suggestionList;
    // Set while an accepted suggestion applies, so the edit does not ask for suggestions again
    private boolean completing;
    // Whether the last user edit applied, for a completion to report
    private boolean editApplied;
    // Set while the box takes focus back from a press on the list, so the caret stays put
    private boolean refocusing;

    // The text and selection before the edit in progress, for a rejected edit to fall back to
    private String beforeText;
    private int beforeCaret;
    private int beforeAnchor;

    // Theme
    private Paint normal;
    private Paint focusedPaint;
    private Paint disabledPaint;
    private FieldLook look;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;
    private TextStyle readOnlyTextStyle;
    private Paint suggestionsPaint;
    private TextStyle suggestionTextStyle;
    private Text placeholder;
    private TextStyle placeholderStyle;
    // Made during measure
    private TextLine placeholderLine;
    private float lineHeight;

    // The text as drawn: one span per styled segment, and where each starts
    private final StyledText styled;
    private TextHighlighter highlighter;
    private TextSpan[] displaySpans;
    private float[] displayX;

    /**
     * Creates an empty box with no length limit.
     *
     * @param ui the factory that owns the box
     */
    @SuppressWarnings("this-escape")
    public TextBoxImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.ui = ui;
        this.buffer = new PlainBuffer();
        this.editor = new LineEditor(this.buffer, new TextBoundaries(this.buffer, false), new Editing());
        this.maxLength = Integer.MAX_VALUE;
        this.textPadding = DEFAULT_PADDING;
        this.themePadding = DEFAULT_PADDING;
        this.maxVisibleSuggestions = DEFAULT_MAX_VISIBLE_SUGGESTIONS;
        this.suggestions = List.of();
        this.normal = Paint.NONE;
        this.focusedPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.look = FieldLook.plain(DEFAULT_SELECTION, DEFAULT_CARET);
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.readOnlyTextStyle = TextStyle.DEFAULT;
        this.suggestionsPaint = Paint.NONE;
        this.suggestionTextStyle = TextStyle.DEFAULT;
        this.placeholder = Text.empty();
        this.placeholderStyle = TextStyle.DEFAULT;
        this.placeholderLine = TextLine.EMPTY;
        this.styled = new StyledText();
        this.highlighter = TextHighlighter.NONE;
        this.displaySpans = new TextSpan[0];
        this.displayX = new float[0];
        this.setCursor(CursorKind.TEXT);
        this.setMinSize(20f, 0f);
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
        if (!this.buffer.contentEquals(clean)) {
            final String previous = this.buffer.toString();
            this.buffer.set(clean);
            this.editor.place(this.buffer.length());
            this.hideSuggestions();
            this.changed(previous);
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
    public TextBox clear() {
        return this.setText("");
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
            final String previous = this.buffer.toString();
            this.buffer.truncate(this.buffer.offsetByCodePoints(0, maxLength));
            this.editor.clamp();
            this.changed(previous);
        }
        return this;
    }

    @Override
    public Text placeholder() {
        return this.placeholder;
    }

    @Override
    public TextBox setPlaceholder(final Text placeholder) {
        this.placeholder = placeholder == null ? Text.empty() : placeholder;
        this.invalidateLayout();
        return this;
    }

    @Override
    public TextBox setTextPadding(final Insets padding) {
        this.setPadding = padding;
        this.textPadding = padding != null ? padding : this.themePadding;
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
    public TextBox select(final int anchor, final int caret) {
        this.editor.select(anchor, caret);
        return this;
    }

    @Override
    public TextBox selectAll() {
        this.editor.selectAll();
        return this;
    }

    /**
     * Sets a filter for the parts of Strata that hold a box privately, such as a spinner's field. After an edit's
     * {@link TextBox#EDIT} handlers, the filter gets the text the edit produced and returns the text to keep: the
     * same string to accept it, another string to replace it, or {@code null} to reject the edit. Replaced text is
     * cleaned of line breaks and cut to the length limit, and the caret keeps its distance from the end of the text.
     * {@link #setText} is not filtered.
     *
     * @param filter the filter, or {@code null} for none
     */
    void setInternalFilter(final UnaryOperator<String> filter) {
        this.filter = filter;
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
    public TextBox setHighlighter(final TextHighlighter highlighter) {
        this.highlighter = Objects.requireNonNull(highlighter, "highlighter");
        this.editor.invalidate();
        return this;
    }

    @Override
    public TextBox setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        this.editor.invalidate();
        if (readOnly) {
            this.hideSuggestions();
        }
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
    // Suggestions
    // ------------------------------------------------------------------

    @Override
    public TextBox setSuggestionProvider(final SuggestionProvider provider) {
        this.suggestionProvider = provider;
        return this.hideSuggestions();
    }

    @Override
    public int maxVisibleSuggestions() {
        return this.maxVisibleSuggestions;
    }

    @Override
    public TextBox setMaxVisibleSuggestions(final int count) {
        if (count < 1) {
            throw new IllegalArgumentException("At least one suggestion must show: " + count);
        }
        this.maxVisibleSuggestions = count;
        return this;
    }

    @Override
    public TextBox showSuggestions() {
        if (this.suggestionProvider == null || this.readOnly || !this.isEnabled() || !this.isAttached()) {
            return this;
        }
        final List<Suggestion> found = List.copyOf(this.suggestionProvider.suggest(this.buffer.toString(), this.caret()));
        if (found.isEmpty()) {
            return this.hideSuggestions();
        }
        final float rowHeight = this.environment().text().lineHeight(this.suggestionTextStyle) + 4f;
        this.suggestions = found;
        if (this.suggestionList == null) {
            final ListViewImpl<Suggestion> list = new ListViewImpl<>(this.ui, this::suggestionCell);
            list.route(this::pressSuggestion, this::acceptSuggestion, (index, previous) -> { });
            list.setFocusable(false);
            list.setTabbable(false);
            list.setActivateOnClick(true);
            this.nodeOf(list).setBackground(this.suggestionsPaint);
            list.setItems(found);
            this.suggestionList = list;
            final Overlay popup = this.ui.overlays().popup(list, this.bounds());
            this.suggestionPopup = popup;
            popup.onEvent(Overlay.CLOSED, event -> {
                if (this.suggestionPopup == popup) {
                    this.suggestionPopup = null;
                    this.suggestionList = null;
                    this.suggestions = List.of();
                }
            });
        } else {
            this.suggestionList.setItems(found);
        }
        this.suggestionList.setRowHeight(rowHeight);
        this.suggestionList.setMinSize(this.bounds().width(), 0f);
        this.suggestionList.setHeight(rowHeight * Math.min(this.maxVisibleSuggestions, found.size()));
        this.suggestionList.select(0);
        return this;
    }

    @Override
    public TextBox hideSuggestions() {
        final Overlay popup = this.suggestionPopup;
        this.suggestionPopup = null;
        this.suggestionList = null;
        this.suggestions = List.of();
        if (popup != null) {
            popup.close();
        }
        return this;
    }

    @Override
    public List<Suggestion> shownSuggestions() {
        return this.suggestions;
    }

    private Label suggestionCell(final Suggestion suggestion) {
        final Label label = this.ui.cellPadded(
                this.ui.label(suggestion.text()).setStyle(this.suggestionTextStyle));
        label.setOverflow(TextOverflow.ELLIPSIS);
        label.setAnchor(Anchor.MIDDLE_LEFT);
        return label;
    }

    /**
     * Highlights the row a press landed on and takes focus back from the press.
     */
    private void pressSuggestion(final int index) {
        if (this.suggestionList != null) {
            this.suggestionList.applySelect(index);
        }
        this.refocus();
    }

    /**
     * Takes focus back without moving the caret, after a press on the list took it.
     */
    private void refocus() {
        if (!this.isFocused()) {
            this.refocusing = true;
            try {
                this.requestFocus();
            } finally {
                this.refocusing = false;
            }
        }
    }

    /**
     * Closes the list and asks to apply the suggestion at an index of it. Does nothing for an index outside it.
     */
    private void acceptSuggestion(final int index) {
        if (index < 0 || index >= this.suggestions.size()) {
            return;
        }
        final Suggestion suggestion = this.suggestions.get(index);
        this.hideSuggestions();
        this.refocus();
        this.request(COMPLETE, suggestion, this::applySuggestion);
    }

    /**
     * Applies the suggestion a {@link TextBox#COMPLETE} request ended with, as an edit by the user: replaces its range,
     * clamped to the text, with its text cut to the room the length limit leaves, and puts the caret after it. Then
     * reports {@link TextBox#COMPLETED} if the edit applied. A null suggestion is ignored.
     */
    private void applySuggestion(final Suggestion suggestion) {
        if (suggestion == null || !this.isEnabled() || !this.beginEdit()) {
            return;
        }
        final int start = this.snap(suggestion.start());
        final int end = this.snap(suggestion.end());
        final int kept = this.codePoints() - this.buffer.codePointCount(start, end);
        final String fitted = truncate(sanitize(suggestion.text()), this.maxLength - kept);
        this.buffer.replace(start, end, fitted);
        this.editor.place(start + fitted.length());
        this.editApplied = false;
        this.completing = true;
        try {
            this.edited();
        } finally {
            this.completing = false;
        }
        if (this.editApplied) {
            this.report(COMPLETED, suggestion, suggestion);
        }
    }

    /**
     * Handles a key for the open suggestion list.
     *
     * @return true if the list used the key
     */
    private boolean suggestionKey(final KeyEvent event) {
        final Modifiers modifiers = event.modifiers();
        if (modifiers.control() || modifiers.alt() || this.suggestionList == null) {
            return false;
        }
        final Key key = event.key();
        final boolean press = event.action() == KeyAction.PRESS;
        final int count = this.suggestions.size();
        final int current = this.suggestionList.selectedIndex();
        switch (key) {
            case UP -> this.suggestionList.select(current <= 0 ? count - 1 : current - 1);
            case DOWN -> this.suggestionList.select(current < 0 || current >= count - 1 ? 0 : current + 1);
            case ESCAPE -> {
                if (!press) {
                    return false;
                }
                this.hideSuggestions();
            }
            default -> {
                if (!press || current < 0 || !(key.isEnter() || key == Key.TAB && !modifiers.shift())) {
                    return false;
                }
                this.acceptSuggestion(current);
            }
        }
        return true;
    }

    /**
     * Asks for suggestions again after an edit by the user, if the box has a provider and focus.
     */
    private void suggestAfterEdit() {
        if (this.suggestionProvider != null && this.isFocused()) {
            this.showSuggestions();
        }
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
        this.themePadding = theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING);
        if (this.setPadding == null) {
            this.textPadding = this.themePadding;
        }
        this.normal = theme.paintOr(STYLE_NORMAL, Paint.NONE);
        this.focusedPaint = theme.paintOr(STYLE_FOCUSED, this.normal);
        this.disabledPaint = theme.paintOr(STYLE_DISABLED, this.normal);
        this.look = FieldLook.of(theme, DEFAULT_SELECTION, DEFAULT_CARET);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(STYLE_TEXT_DISABLED, this.textStyle);
        this.readOnlyTextStyle = theme.textStyleOr(STYLE_TEXT_READ_ONLY, this.disabledTextStyle);
        this.suggestionsPaint = theme.paintOr(STYLE_SUGGESTIONS, theme.paintOr(ComboBox.STYLE_POPUP, Paint.NONE));
        this.suggestionTextStyle = theme.textStyleOr(STYLE_SUGGESTION_TEXT, this.textStyle);
        this.placeholderStyle = theme.textStyleOr(STYLE_PLACEHOLDER, this.disabledTextStyle);
        if (this.suggestionList != null) {
            this.nodeOf(this.suggestionList).setBackground(this.suggestionsPaint);
        }
        this.editor.invalidate();
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.lineHeight = Math.max(context.text().lineHeight(this.textStyle), context.text().lineHeight(this.disabledTextStyle));
        this.placeholderLine = TextLine.of(this.placeholder, this.placeholderStyle, context.text());
        return new Size(DEFAULT_WIDTH, Math.max(ButtonImpl.MIN_HEIGHT, this.lineHeight + this.textPadding.vertical()));
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

        final Rect contentBox = bounds.shrink(this.textPadding);
        final float textY = contentBox.y() + (contentBox.height() - this.lineHeight) * 0.5f;
        this.editor.paint(painter, contentBox, textY, this.lineHeight, this.look, this.style(),
                this.isFocused() && this.isEnabled() && !this.readOnly);
    }

    private TextStyle style() {
        if (!this.isEnabled()) {
            return this.disabledTextStyle;
        }
        return this.readOnly ? this.readOnlyTextStyle : this.textStyle;
    }

    /**
     * Brings the styled segments up to the current text, style and highlighter; cheap when nothing changed.
     */
    private void syncStyled() {
        this.styled.update(this.buffer.toString(), this.style(), this.highlighter, this.isEnabled());
    }

    // ------------------------------------------------------------------
    // Pointer
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

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    @Override
    protected void onFocusGained(final FocusEvent event) {
        if (this.refocusing) {
            this.editor.moved();
            return;
        }
        this.editor.focusGained(this.selectAllOnFocus, this.isFocusFromPress());
    }

    @Override
    protected void onFocusLost(final FocusEvent event) {
        this.editor.focusLost();
        // A press on the list takes focus first; keep the list open for it
        if (this.suggestionList == null
                || !this.nodeOf(this.suggestionList).isHoverWithin()) {
            this.hideSuggestions();
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        if (this.suggestionPopup != null && this.suggestionKey(event)) {
            event.consume();
            return;
        }
        final Key key = event.key();
        final Modifiers modifiers = event.modifiers();
        if (key == Key.SPACE && modifiers.control() && !modifiers.alt() && this.suggestionProvider != null) {
            if (event.action() == KeyAction.PRESS) {
                this.showSuggestions();
            }
            event.consume();
            return;
        }
        if (key.isEnter() && event.action() == KeyAction.PRESS) {
            final boolean heard = this.isHandled(SUBMIT) || this.isHandled(SUBMITTED);
            if (this.request(SUBMIT)) {
                final String text = this.buffer.toString();
                this.report(SUBMITTED, text, text);
            }
            if (heard) {
                event.consume();
            }
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
        if (!this.isEnabled()) {
            return;
        }
        final int codePoint = event.codePoint();
        if (Character.isISOControl(codePoint)) {
            return;
        }
        // Ctrl+Space asks for suggestions; an engine that also types its space must not insert it
        if (codePoint == ' ' && event.modifiers().control() && !event.modifiers().alt() && this.suggestionProvider != null) {
            event.consume();
            return;
        }
        this.editor.type(codePoint);
        event.consume();
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    /**
     * Replaces a range with text as an edit by the user: the text is cut to the room the length limit leaves, and
     * the caret goes after it.
     */
    private void edit(final int start, final int end, final String text) {
        if (!this.beginEdit()) {
            return;
        }
        this.buffer.delete(start, end);
        final String fitted = truncate(text, this.maxLength - this.codePoints());
        if (!fitted.isEmpty()) {
            this.buffer.insert(start, fitted);
        }
        this.editor.place(start + fitted.length());
        this.edited();
    }

    /**
     * Remembers the text and selection before a user edit, for a rejected edit to fall back to.
     *
     * @return false if the box is read-only and the edit must not happen
     */
    private boolean beginEdit() {
        if (this.readOnly) {
            return false;
        }
        this.beforeText = this.buffer.toString();
        this.beforeCaret = this.editor.caret();
        this.beforeAnchor = this.editor.anchor();
        return true;
    }

    /**
     * Ends a user edit. With no {@link TextBox#MODIFY} or {@link TextBox#EDIT} handler on the path, or when the edit
     * left the text as it was, this applies the edit at once. Otherwise it puts the text and selection back as they
     * were before the edit and asks {@code MODIFY}; unless that was canceled, it fires {@code EDIT} with the edited
     * text, which applies the final text through {@code proceedEdit} unless a handler cancels it.
     */
    private void edited() {
        if (!this.isHandled(MODIFY) && !this.isHandled(EDIT)) {
            this.applyEdit();
            return;
        }
        final String candidate = this.buffer.toString();
        if (candidate.equals(this.beforeText)) {
            this.applyEdit();
            return;
        }
        final int fromEnd = candidate.length() - this.editor.caret();
        this.buffer.set(this.beforeText);
        this.editor.restore(this.beforeAnchor, this.beforeCaret);
        this.editor.moved();
        if (!this.request(MODIFY)) {
            return;
        }
        this.request(EDIT, candidate, text -> this.proceedEdit(text, fromEnd));
    }

    /**
     * Applies the final text of an {@link TextBox#EDIT} request: drops line breaks from the text, cuts it to the
     * length limit, and puts it in the box with the caret {@code fromEnd} characters before its end and nothing
     * selected. For the edited text itself that is where the edit left the caret. Then it applies the text as the
     * edit.
     */
    private void proceedEdit(final String text, final int fromEnd) {
        final String fitted = truncate(sanitize(Objects.requireNonNull(text, "text")), this.maxLength);
        this.buffer.set(fitted);
        this.editor.place(this.snap(this.buffer.length() - fromEnd));
        this.applyEdit();
    }

    /**
     * Applies the edit the buffer holds: runs the internal filter, then reports {@link TextBox#EDITED} and
     * {@link TextBox#MODIFIED} if the text changed.
     */
    private void applyEdit() {
        if (this.filter != null) {
            final String candidate = this.buffer.toString();
            final String result = this.filter.apply(candidate);
            if (result == null || result.equals(this.beforeText)) {
                this.buffer.set(this.beforeText);
                this.editor.restore(this.beforeAnchor, this.beforeCaret);
                this.editor.moved();
                return;
            }
            if (!result.equals(candidate)) {
                final int fromEnd = this.buffer.length() - this.editor.caret();
                this.buffer.set(truncate(sanitize(result), this.maxLength));
                this.editor.place(this.snap(this.buffer.length() - fromEnd));
            }
        }
        this.editApplied = true;
        if (this.buffer.contentEquals(this.beforeText)) {
            this.editor.moved();
            return;
        }
        this.changed(this.beforeText);
        if (!this.completing) {
            this.suggestAfterEdit();
        }
    }

    /**
     * Reports a change of the text, from any source: {@link TextBox#EDITED}, then {@link TextBox#MODIFIED}.
     */
    private void changed(final String previous) {
        this.editor.moved();
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

    /**
     * Single line only: newlines are dropped, as Wintergreen did for pasted text.
     */
    static String sanitize(final String text) {
        return text.replace("\r", "").replace("\n", "");
    }

    static String truncate(final String text, final int codePoints) {
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
        return new TextBox.State(this.buffer.toString(), this.editor.caret(), this.editor.anchor(), this.maxLength);
    }

    /**
     * What the box tells its {@link LineEditor}: how to measure and draw its styled text, and how the user's edits
     * apply.
     */
    private final class Editing implements LineEditor.Owner {
        @Override
        public Environment environment() {
            return TextBoxImpl.this.environment();
        }

        @Override
        public boolean isReadOnly() {
            return TextBoxImpl.this.readOnly;
        }

        @Override
        public boolean allowsCopy() {
            return true;
        }

        @Override
        public float layoutText(final TextMetrics metrics) {
            final TextBoxImpl box = TextBoxImpl.this;
            box.syncStyled();
            box.displaySpans = box.styled.spans(0, box.buffer.length());
            box.displayX = StyledText.offsets(metrics, box.displaySpans);
            return StyledText.extent(metrics, box.displaySpans, box.displayX);
        }

        @Override
        public float xForIndex(final TextMetrics metrics, final int index) {
            return index == 0 ? 0f : TextBoxImpl.this.styled.width(metrics, 0, index);
        }

        @Override
        public int indexForX(final TextMetrics metrics, final float x) {
            final TextBoxImpl box = TextBoxImpl.this;
            int previous = 0;
            float previousX = 0f;
            int index = 0;
            while (index < box.buffer.length()) {
                index += Character.charCount(box.buffer.codePointAt(index));
                final float indexX = this.xForIndex(metrics, index);
                if (indexX >= x) {
                    return x - previousX < indexX - x ? previous : index;
                }
                previous = index;
                previousX = indexX;
            }
            return box.buffer.length();
        }

        @Override
        public void drawText(final Painter painter, final float originX, final float left, final float y,
                             final TextStyle style) {
            final TextBoxImpl box = TextBoxImpl.this;
            if (box.buffer.isEmpty()) {
                if (!box.placeholderLine.isEmpty() && !(box.look.hidePlaceholder() && box.isFocused())) {
                    box.placeholderLine.draw(painter, left, y);
                }
                return;
            }
            for (int i = 0; i < box.displaySpans.length; i++) {
                final TextSpan span = box.displaySpans[i];
                painter.text(style == null ? span : new TextSpan(span.text(), style), originX + box.displayX[i], y);
            }
        }

        @Override
        public void selectAll() {
            TextBoxImpl.this.selectAll();
        }

        @Override
        public void type(final int start, final int end, final int codePoint) {
            TextBoxImpl.this.edit(start, end, Character.toString(codePoint));
        }

        @Override
        public void delete(final int start, final int end) {
            TextBoxImpl.this.edit(start, end, "");
        }

        @Override
        public void paste(final int start, final int end) {
            TextBoxImpl.this.environment().clipboardText()
                    .map(TextBoxImpl::sanitize)
                    .ifPresent(text -> TextBoxImpl.this.edit(start, end, text));
        }

        @Override
        public void copy(final int start, final int end) {
            TextBoxImpl.this.environment().setClipboardText(TextBoxImpl.this.buffer.substring(start, end));
        }

        @Override
        public void beforeMove() {
            TextBoxImpl.this.hideSuggestions();
        }
    }
}
