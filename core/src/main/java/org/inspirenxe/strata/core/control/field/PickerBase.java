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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Root;
import org.inspirenxe.strata.core.text.DateFormats;
import org.inspirenxe.strata.core.text.TextLine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * What the date and time pickers share: the field of parts over a {@link SegmentedField}, its draft, its keys, its
 * drawing and size, and the button that opens a popup.
 *
 * <p>The field draws with the text box's faces and text styles, each part a {@link TextLine} made again only when
 * the part's text, the enabled state or the bounds change. The button is {@value DatePicker#BUTTON_WIDTH} units wide
 * at the right edge; its icon, when the theme has one, is a square two units smaller than the button's width,
 * centered, and otherwise the button draws the combo box's down arrow in the text color. The field asks for the widest
 * text any value could make: the widest digit times each part's digits, the wider name of the half of the day, and
 * the separators, or the placeholder if it is wider.</p>
 *
 * @param <T> the picker's own type
 * @param <V> the type of its value
 */
abstract class PickerBase<T extends Component<T>, V> extends BuiltInComponent<T> {
    /**
     * The space the icon leaves around it inside the button, in UI units.
     */
    static final float ICON_INSET = 1f;

    /**
     * The factory that owns the picker.
     */
    final ComponentFactoryImpl ui;
    /**
     * The parts and their draft.
     */
    final SegmentedField field;
    private Text placeholder;
    private boolean clearable;
    private Overlay popup;

    // Theme
    private Insets padding;
    private Paint normal;
    private Paint focusedPaint;
    private Paint disabledPaint;
    private Paint segmentPaint;
    private Paint button;
    private Paint buttonHovered;
    private Paint buttonDisabled;
    private Paint icon;
    private Paint popupPaint;
    private TextStyle textStyle;
    private TextStyle disabledTextStyle;
    private TextStyle placeholderStyle;

    // Made during measure
    private float lineHeight;
    private TextLine placeholderLine;
    private long themeVersion;

    // Made when the field's text, the enabled state or the bounds change
    private long builtVersion;
    private long builtTheme;
    private boolean builtEnabled;
    private Rect builtBounds;
    private TextLine[] partLines;
    private TextLine[] literalLines;
    private float[] partX;
    private float[] literalX;
    private Rect[] partRects;
    private Rect buttonRect;
    private Rect iconRect;
    private float textY;

    // Pointer
    private float pointerX;
    private float pointerY;

    /**
     * Creates a picker with no popup open.
     *
     * @param ui the factory that owns the picker
     * @param placeholder what the empty field shows
     */
    @SuppressWarnings("this-escape")
    PickerBase(final ComponentFactoryImpl ui, final Text placeholder) {
        super(ui);
        this.ui = ui;
        this.field = new SegmentedField();
        this.placeholder = placeholder;
        this.clearable = true;
        this.padding = TextBoxImpl.DEFAULT_PADDING;
        this.normal = Paint.NONE;
        this.focusedPaint = Paint.NONE;
        this.disabledPaint = Paint.NONE;
        this.segmentPaint = Paint.NONE;
        this.button = Paint.NONE;
        this.buttonHovered = Paint.NONE;
        this.buttonDisabled = Paint.NONE;
        this.icon = Paint.NONE;
        this.popupPaint = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.disabledTextStyle = TextStyle.DEFAULT;
        this.placeholderStyle = TextStyle.DEFAULT;
        this.placeholderLine = TextLine.EMPTY;
        this.builtVersion = -1L;
        this.partLines = new TextLine[0];
        this.literalLines = new TextLine[0];
        this.partX = new float[0];
        this.literalX = new float[0];
        this.partRects = new Rect[0];
        this.buttonRect = Rect.ZERO;
        this.iconRect = Rect.ZERO;
        this.pointerX = Float.NaN;
    }

    // ------------------------------------------------------------------
    // What each picker supplies
    // ------------------------------------------------------------------

    /**
     * Returns the value.
     *
     * @return the value, or null when empty
     */
    abstract V value();

    /**
     * Returns the value the draft makes, if it is complete, valid and one the user may pick.
     *
     * @return the value, or null
     */
    abstract V draftValue();

    /**
     * Asks to set a value the user chose, through the picker's requests.
     *
     * @param value the value, or null to clear
     */
    abstract void userPick(V value);

    /**
     * Lays the field's parts out for the current formats and settings.
     *
     * @param formats the current formats
     */
    abstract void layoutField(DateFormats formats);

    /**
     * Fills the draft from the value, marking it clean.
     */
    abstract void loadField();

    /**
     * Fills the draft's empty parts from what a popup starts an empty value at.
     */
    abstract void fillForPopup();

    /**
     * Builds or updates the popup's content for opening.
     *
     * @return the content
     */
    abstract Component<?> popupContent();

    /**
     * Called after the popup opened, to move focus into it if the picker does.
     */
    void popupOpened() {
    }

    /**
     * Returns the key of the button's icon.
     *
     * @return the style key
     */
    abstract String iconKey();

    /**
     * Returns the keys the picker needs beyond the text box's.
     *
     * @return the extra required keys
     */
    abstract Set<String> extraRequiredKeys();

    // ------------------------------------------------------------------
    // Shared settings
    // ------------------------------------------------------------------

    /**
     * Returns what the field shows while empty and unfocused.
     *
     * @return the placeholder
     */
    public Text placeholder() {
        return this.placeholder;
    }

    /**
     * Sets what the field shows while empty and unfocused.
     *
     * @param placeholder the placeholder
     * @return this picker
     */
    public T setPlaceholder(final Text placeholder) {
        this.placeholder = Objects.requireNonNull(placeholder, "placeholder");
        this.invalidateLayout();
        return this.self();
    }

    /**
     * Returns whether the user can clear the value with Delete.
     *
     * @return true by default
     */
    public boolean isClearable() {
        return this.clearable;
    }

    /**
     * Sets whether the user can clear the value with Delete.
     *
     * @param clearable true to let the user clear the value
     * @return this picker
     */
    public T setClearable(final boolean clearable) {
        this.clearable = clearable;
        return this.self();
    }

    /**
     * Returns whether the popup is open.
     *
     * @return true while open
     */
    public boolean isOpen() {
        return this.popup != null && this.popup.isOpen();
    }

    /**
     * Opens the popup, taking a draft first as focus loss would. Does nothing while open, disabled or detached.
     *
     * @return this picker
     */
    public T open() {
        if (!this.canOpen()) {
            return this.self();
        }
        this.commit(true);
        final Component<?> content = this.popupContent();
        this.nodeOf(content).setBackground(this.popupPaint);
        this.popup = this.ui.overlays().popup(content, this.bounds());
        this.popup.onEvent(Overlay.CLOSED, event -> {
            this.popup = null;
            this.report(Closable.CLOSED);
            final Root root = this.hostRoot();
            if (root != null && root.focused() == null && this.isAttached() && this.isEnabled()) {
                this.requestFocus();
            }
        });
        this.report(Openable.OPENED);
        this.popupOpened();
        return this.self();
    }

    /**
     * Closes the popup. Does nothing while closed.
     *
     * @return this picker
     */
    public T close() {
        if (this.popup != null) {
            this.popup.close();
        }
        return this.self();
    }

    private boolean canOpen() {
        return !this.isOpen() && this.isAttached() && this.isEnabled();
    }

    private void userOpen() {
        if (this.canOpen() && this.request(Openable.OPEN)) {
            this.open();
        }
    }

    /**
     * Returns the current date and time from the platform's wall clock.
     *
     * @return now
     */
    final LocalDateTime now() {
        return LocalDateTime.now(this.ui.platform().clock().wall());
    }

    /**
     * Returns today from the platform's wall clock.
     *
     * @return today
     */
    final LocalDate today() {
        return LocalDate.now(this.ui.platform().clock().wall());
    }

    /**
     * Lays the field out again after a setting that changes its parts, keeping the value.
     */
    final void relayoutField() {
        final DateFormats formats = this.field.formats();
        if (formats != null) {
            this.layoutField(formats);
            this.loadField();
        }
        this.invalidateLayout();
    }

    /**
     * Shows the value in the field, dropping any draft. Pickers call this whenever the value changes.
     */
    final void valueChanged() {
        if (this.field.formats() != null) {
            this.loadField();
        }
    }

    // ------------------------------------------------------------------
    // The draft
    // ------------------------------------------------------------------

    /**
     * Takes the draft as the value if it is complete and valid.
     *
     * @param drop whether to drop a draft that is not complete and valid
     */
    final void commit(final boolean drop) {
        if (!this.field.isDirty()) {
            return;
        }
        final V candidate = this.draftValue();
        if (candidate == null) {
            if (drop) {
                this.loadField();
            }
            return;
        }
        if (!candidate.equals(this.value())) {
            this.userPick(candidate);
        }
        this.loadField();
    }

    /**
     * Steps the focused part while the popup is open and sets the value at once, starting an empty value where the
     * popup would.
     */
    private void stepOpen(final int direction) {
        if (!this.field.isComplete()) {
            this.fillForPopup();
        }
        this.field.step(direction, this.now());
        final V candidate = this.draftValue();
        if (candidate != null && !candidate.equals(this.value())) {
            this.userPick(candidate);
        }
        this.loadField();
    }

    // ------------------------------------------------------------------
    // Theme, layout and painting
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        final Set<String> keys = new HashSet<>(TextBoxImpl.REQUIRED);
        keys.addAll(this.extraRequiredKeys());
        return keys;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.padding = theme.insetsOr(DatePicker.STYLE_PADDING,
                theme.insetsOr(TextBox.STYLE_PADDING, TextBoxImpl.DEFAULT_PADDING));
        this.normal = theme.paintOr(TextBox.STYLE_NORMAL, Paint.NONE);
        this.focusedPaint = theme.paintOr(TextBox.STYLE_FOCUSED, this.normal);
        this.disabledPaint = theme.paintOr(TextBox.STYLE_DISABLED, this.normal);
        this.segmentPaint = theme.paintOr(DatePicker.STYLE_SEGMENT_FOCUSED,
                theme.paintOr(TextBox.STYLE_SELECTION, TextBoxImpl.DEFAULT_SELECTION));
        this.button = theme.paintOr(DatePicker.STYLE_BUTTON,
                theme.paintOr(Spinner.STYLE_BUTTON, theme.paintOr(Button.STYLE_NORMAL, Paint.NONE)));
        this.buttonHovered = theme.paintOr(DatePicker.STYLE_BUTTON_HOVERED,
                theme.paintOr(Spinner.STYLE_BUTTON_HOVERED, theme.paintOr(Button.STYLE_HOVERED, this.button)));
        this.buttonDisabled = theme.paintOr(DatePicker.STYLE_BUTTON_DISABLED,
                theme.paintOr(Spinner.STYLE_BUTTON_DISABLED, theme.paintOr(Button.STYLE_DISABLED, this.button)));
        this.icon = theme.paintOr(this.iconKey(), Paint.NONE);
        this.popupPaint = theme.paintOr(DatePicker.STYLE_POPUP, theme.paintOr(ComboBox.STYLE_POPUP, Paint.NONE));
        this.textStyle = theme.textStyleOr(TextBox.STYLE_TEXT, TextStyle.DEFAULT);
        this.disabledTextStyle = theme.textStyleOr(TextBox.STYLE_TEXT_DISABLED, this.textStyle);
        this.placeholderStyle = theme.textStyleOr(TextBox.STYLE_PLACEHOLDER, this.disabledTextStyle);
        this.themeVersion++;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final DateFormats formats = DateFormats.current();
        if (formats != this.field.formats()) {
            this.layoutField(formats);
            this.loadField();
        }
        final TextMetrics metrics = context.text();
        this.lineHeight = Math.max(metrics.lineHeight(this.textStyle), metrics.lineHeight(this.disabledTextStyle));
        this.placeholderLine = TextLine.of(this.placeholder, this.placeholderStyle, metrics);
        float digit = metrics.width(new TextSpan("-", this.textStyle));
        for (char c = '0'; c <= '9'; c++) {
            digit = Math.max(digit, metrics.width(new TextSpan(String.valueOf(c), this.textStyle)));
        }
        final float half = Math.max(metrics.width(new TextSpan("--", this.textStyle)),
                Math.max(metrics.width(new TextSpan(formats.am(), this.textStyle)),
                        metrics.width(new TextSpan(formats.pm(), this.textStyle))));
        float width = 0f;
        for (int i = 0; i < this.field.count(); i++) {
            final SegmentedField.Kind kind = this.field.kind(i);
            width += kind == SegmentedField.Kind.HALF ? half : digit * kind.digits;
            width += metrics.width(new TextSpan(this.field.literal(i), this.textStyle));
        }
        width = Math.max(width, this.placeholderLine.width());
        this.themeVersion++;
        return new Size((float) Math.ceil(this.padding.horizontal() + width) + DatePicker.BUTTON_WIDTH,
                this.lineHeight + this.padding.vertical());
    }

    /**
     * Makes the part lines and rectangles again when the field's text, the enabled state, the theme or the bounds
     * changed.
     */
    private void build(final Rect bounds) {
        final boolean enabled = this.isEnabled();
        if (this.builtVersion == this.field.version() && this.builtEnabled == enabled
                && this.builtTheme == this.themeVersion && bounds.equals(this.builtBounds)) {
            return;
        }
        final TextMetrics metrics = this.environment().text();
        final TextStyle style = enabled ? this.textStyle : this.disabledTextStyle;
        final int count = this.field.count();
        this.partLines = new TextLine[count];
        this.literalLines = new TextLine[count];
        this.partX = new float[count];
        this.literalX = new float[count];
        this.partRects = new Rect[count];
        final Rect content = this.contentBox(bounds);
        this.textY = content.y() + (float) Math.floor((content.height() - this.lineHeight) * 0.5f);
        float x = content.x();
        for (int i = 0; i < count; i++) {
            final TextLine part = TextLine.of(Text.of(this.field.text(i)), style, metrics);
            this.partLines[i] = part;
            this.partX[i] = x;
            this.partRects[i] = new Rect(x, this.textY, part.width(), this.lineHeight);
            x += part.width();
            final TextLine literal = TextLine.of(Text.of(this.field.literal(i)), style, metrics);
            this.literalLines[i] = literal;
            this.literalX[i] = x;
            x += literal.width();
        }
        this.buttonRect = new Rect(bounds.right() - DatePicker.BUTTON_WIDTH, bounds.y(), DatePicker.BUTTON_WIDTH,
                bounds.height());
        final float side = Math.max(0f, Math.min(this.buttonRect.width(), this.buttonRect.height()) - 2f * ICON_INSET);
        this.iconRect = new Rect(this.buttonRect.x() + (float) Math.floor((this.buttonRect.width() - side) * 0.5f),
                this.buttonRect.y() + (float) Math.floor((this.buttonRect.height() - side) * 0.5f), side, side);
        this.builtVersion = this.field.version();
        this.builtEnabled = enabled;
        this.builtTheme = this.themeVersion;
        this.builtBounds = bounds;
    }

    private Rect contentBox(final Rect bounds) {
        return new Rect(bounds.x() + this.padding.left(), bounds.y() + this.padding.top(),
                Math.max(0f, bounds.width() - this.padding.horizontal() - DatePicker.BUTTON_WIDTH),
                Math.max(0f, bounds.height() - this.padding.vertical()));
    }

    /**
     * Returns whether the field shows its placeholder: empty, unfocused and with no draft.
     *
     * @return true while the placeholder shows
     */
    final boolean showsPlaceholder() {
        return this.value() == null && !this.isFocused() && !this.field.isDirty();
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.build(bounds);
        final boolean enabled = this.isEnabled();
        final Paint face;
        if (!enabled) {
            face = this.disabledPaint;
        } else if (this.isFocused() || this.isOpen()) {
            face = this.focusedPaint;
        } else {
            face = this.normal;
        }
        face.paint(painter, bounds);

        if (this.showsPlaceholder()) {
            this.placeholderLine.draw(painter, bounds.x() + this.padding.left(), this.textY);
        } else {
            final int focused = this.isFocused() && enabled ? this.field.focus() : -1;
            for (int i = 0; i < this.partLines.length; i++) {
                if (i == focused) {
                    this.segmentPaint.paint(painter, this.partRects[i]);
                }
                this.partLines[i].draw(painter, this.partX[i], this.textY);
                this.literalLines[i].draw(painter, this.literalX[i], this.textY);
            }
        }

        final Paint buttonFace;
        if (!enabled) {
            buttonFace = this.buttonDisabled;
        } else if (this.isHovered() && this.buttonRect.contains(this.pointerX, this.pointerY)) {
            buttonFace = this.buttonHovered;
        } else {
            buttonFace = this.button;
        }
        buttonFace.paint(painter, this.buttonRect);
        if (this.icon != Paint.NONE) {
            this.icon.paint(painter, this.iconRect);
        } else {
            final int color = (enabled ? this.textStyle : this.disabledTextStyle).color();
            Rings.down(painter, color,
                    this.buttonRect.x() + (float) Math.floor((this.buttonRect.width() - Rings.ARROW_LONG) * 0.5f),
                    this.buttonRect.y() + (float) Math.floor((this.buttonRect.height() - Rings.ARROW_SHORT) * 0.5f));
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected boolean acceptsTextInput() {
        return true;
    }

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.pointerX = event.x();
        this.pointerY = event.y();
    }

    @Override
    protected void onPointerEnter(final PointerEvent event) {
        this.pointerX = event.x();
        this.pointerY = event.y();
    }

    @Override
    protected void onPointerPress(final PointerButtonEvent event) {
        this.pointerX = event.x();
        this.pointerY = event.y();
        if (event.button() != MouseButton.LEFT || !this.isEnabled()) {
            return;
        }
        this.build(this.bounds());
        if (this.buttonRect.contains(event.x(), event.y())) {
            this.userOpen();
            event.consume();
            return;
        }
        int nearest = -1;
        float distance = Float.MAX_VALUE;
        for (int i = 0; i < this.partRects.length; i++) {
            final Rect part = this.partRects[i];
            final float away = event.x() < part.x() ? part.x() - event.x()
                    : event.x() > part.right() ? event.x() - part.right() : 0f;
            if (away < distance) {
                distance = away;
                nearest = i;
            }
        }
        if (nearest >= 0) {
            this.field.setFocus(nearest);
        }
        event.consume();
    }

    @Override
    protected void onFocusGained(final FocusEvent event) {
        if (this.field.focus() < 0) {
            this.field.setFocus(0);
        }
    }

    @Override
    protected void onFocusLost(final FocusEvent event) {
        this.commit(true);
        if (!this.isOpen()) {
            this.field.setFocus(-1);
        }
    }

    @Override
    protected void onDisabled() {
        this.close();
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        final Key key = event.key();
        final boolean press = event.action() == KeyAction.PRESS;
        // Alt+F4 is left alone, since platforms use it to close the window
        if (key == Key.F4 && !event.modifiers().control() && !event.modifiers().alt()
                || key == Key.DOWN && event.modifiers().alt()) {
            if (press) {
                if (this.isOpen()) {
                    this.close();
                } else {
                    this.commit(true);
                    this.userOpen();
                }
            }
            event.consume();
            return;
        }
        if (event.modifiers().control() || event.modifiers().alt()) {
            return;
        }
        if (this.isOpen()) {
            switch (key) {
                case UP -> this.stepOpen(1);
                case DOWN -> this.stepOpen(-1);
                case LEFT -> this.field.moveFocus(-1);
                case RIGHT -> this.field.moveFocus(1);
                case ESCAPE -> {
                    this.loadField();
                    this.close();
                }
                default -> {
                    if (key.isEnter() && press) {
                        this.commit(false);
                        this.close();
                    } else {
                        return;
                    }
                }
            }
            event.consume();
            return;
        }
        switch (key) {
            case LEFT -> {
                if (!this.field.moveFocus(-1)) {
                    return;
                }
            }
            case RIGHT -> {
                if (!this.field.moveFocus(1)) {
                    return;
                }
            }
            case HOME -> this.field.setFocus(0);
            case END -> this.field.setFocus(this.field.count() - 1);
            case UP -> this.field.step(1, this.now());
            case DOWN -> this.field.step(-1, this.now());
            case BACKSPACE -> this.field.clearFocused();
            case DELETE -> {
                if (!this.clearable) {
                    return;
                }
                this.loadField();
                if (this.value() != null) {
                    this.userPick(null);
                }
            }
            case ESCAPE -> {
                if (!this.field.isDirty()) {
                    return;
                }
                this.loadField();
            }
            default -> {
                if (key.isEnter() && press && this.field.isDirty()) {
                    this.commit(false);
                } else {
                    return;
                }
            }
        }
        event.consume();
    }

    @Override
    protected void onChar(final CharEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        final int codePoint = event.codePoint();
        if (codePoint >= '0' && codePoint <= '9') {
            this.field.typeDigit(codePoint - '0');
        } else if (!this.field.typeSeparator(codePoint)) {
            this.field.typeLetter(codePoint);
        }
        event.consume();
    }
}
