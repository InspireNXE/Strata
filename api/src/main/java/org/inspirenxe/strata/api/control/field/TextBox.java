package org.inspirenxe.strata.api.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.RequestEvent;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;
import org.inspirenxe.strata.api.geom.Insets;

import java.util.Objects;

/**
 * A single-line text field.
 *
 * <h2>Editing</h2>
 * <ul>
 *     <li>A click places the caret at the nearest character boundary. Shift+click extends the selection to it.</li>
 *     <li>Dragging selects by character. A drag that starts with a double click selects whole words. A double click
 *     selects the word (or run of spaces) under the pointer; a triple click selects everything.</li>
 *     <li>Left and Right move by character, Home and End to either end. With Shift they extend the selection. With
 *     a selection and no Shift, Left and Right collapse it to its start or end.</li>
 *     <li>Ctrl+Left and Ctrl+Right move by word; Ctrl+Backspace and Ctrl+Delete delete by word.</li>
 *     <li>Ctrl+A selects all. Ctrl+C copies, Ctrl+X cuts and Ctrl+V pastes, through the host's clipboard. Pasted
 *     line breaks are removed.</li>
 *     <li>Typing replaces the selection. Text past {@link #maxLength()} is dropped.</li>
 *     <li>A handler for {@link #EDIT} sees the text each edit would produce and may cancel or rewrite it, so a box
 *     can take only digits. {@link #EDITED} reports every change of the text.</li>
 *     <li>Enter, or keypad Enter, fires {@link #SUBMIT} and then {@link #SUBMITTED}.</li>
 * </ul>
 * <p>A {@linkplain #setReadOnly read-only} box still takes focus, selects and copies, but does not edit and shows no
 * caret.</p>
 * <p>The caret blinks every half second on the host's clock and shows again after each edit or move. When the text
 * is wider than the box, it scrolls sideways to keep the caret in view.</p>
 *
 * <h2>Focus and the engine</h2>
 * <p>The box is focusable and tabbable and shows the text cursor. Focus by Tab puts the caret at the end; focus by
 * a click puts it at the click, unless the box {@linkplain #setSelectAllOnFocus selects all on focus}. While focused
 * it asks the engine for text input (required on SDL-based engines, which send no characters otherwise) and
 * consumes every key that types a character, so engine shortcuts on those keys, such as a game's inventory key, do
 * not fire. Tab and Escape are not consumed: Tab moves focus and Escape still reaches the engine. Enter is consumed only
 * when a handler for {@link #SUBMIT} or {@link #SUBMITTED} is on the box or on an ancestor those events reach. A
 * disabled box ignores input.</p>
 *
 * <h2>Text and indexes</h2>
 * <p>Caret and selection indexes count UTF-16 chars, like {@link String} indexes, but never fall inside a surrogate
 * pair: editing works on whole code points. {@link #maxLength()} counts code points. The box holds one line; line
 * breaks given to {@link #setText} or pasted are removed. In password mode every code point is drawn as the mask
 * character, and copy and cut do nothing.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link TextInput#EDIT}: Fired when the user is about to edit the text by typing, deleting, cutting or
 *     pasting. The value is the whole text the edit would produce. While handlers run, the box still holds the text and
 *     selection from before the edit. Cancel the event to reject the edit and keep the text and selection as they were,
 *     or set another text to rewrite it; the value must not be set to null. The final text has its line breaks removed
 *     and is cut to {@link #maxLength()} before it applies, and the caret goes as many characters before its end as the
 *     edit left it, with nothing selected. An edit that would leave the text as it is, such as typing into a full box,
 *     fires nothing, and a read-only box fires nothing. Fires only for the user's actions: {@link #setText} and
 *     {@link #setMaxLength} fire none.</li>
 *     <li>{@link TextInput#EDITED}: Fired after the text changed: by an edit of the user's, by {@link #setText}, or by
 *     {@link #setMaxLength} cutting it. The value is the new text and {@code previous()} the text before; neither is
 *     null. An edit whose final text equals the old one fires nothing. Fires for changes made from code too.</li>
 * </ul>
 *
 * <pre>{@code
 * // Up to four digits
 * box.onEvent(TextInput.EDIT, event -> {
 *     if (!event.value().matches("\\d{0,4}")) {
 *         event.cancel();
 *     }
 * });
 * }</pre>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the background when not focused.</li>
 *     <li>{@value #STYLE_FOCUSED} (paint, required): the background when focused.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, required): the background when disabled.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the text. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the text when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>{@value #STYLE_TEXT_READ_ONLY} (text style, optional): the text when read-only. Falls back to
 *     {@value #STYLE_TEXT_DISABLED}.</li>
 *     <li>{@value #STYLE_SELECTION} (paint, optional): drawn behind selected text. Falls back to translucent blue.</li>
 *     <li>{@value #STYLE_CARET} (paint, optional): the one-unit-wide caret. Falls back to light gray.</li>
 * </ul>
 *
 * <h2>Size</h2>
 * <p>The desired size is 120 units wide and one line plus the text padding tall (4 units left and right, 5 top and
 * bottom by default), with a minimum of 20 by 20. Set a width, or stretch it, to make it wider.</p>
 *
 * <pre>{@code
 * TextBox name = factory.textBox()
 *         .setMaxLength(32)
 *         .onEvent(TextBox.EDITED, event -> preview.setText("Hello, " + event.value()));
 * name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#textBox()}. Do not implement this
 * interface.</p>
 */
public interface TextBox extends Component<TextBox>, TextInput<TextBox> {
    /**
     * Style key for the background when not focused (paint, required).
     */
    String STYLE_NORMAL = "textbox.normal";
    /**
     * Style key for the background when focused (paint, required).
     */
    String STYLE_FOCUSED = "textbox.focused";
    /**
     * Style key for the background when disabled (paint, required).
     */
    String STYLE_DISABLED = "textbox.disabled";
    /**
     * Style key for the text (text style, optional).
     */
    String STYLE_TEXT = "textbox.text";
    /**
     * Style key for the text when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "textbox.text.disabled";
    /**
     * Style key for the text when read-only (text style, optional).
     */
    String STYLE_TEXT_READ_ONLY = "textbox.text.readonly";
    /**
     * Style key for the selection highlight (paint, optional).
     */
    String STYLE_SELECTION = "textbox.selection";
    /**
     * Style key for the caret (paint, optional).
     */
    String STYLE_CARET = "textbox.caret";

    /**
     * Fired when the user presses Enter or keypad Enter while the box has focus, whether or not anything handles
     * {@link #SUBMITTED}. A held key's repeats do not fire it, and neither does a disabled box. Cancel it to ignore
     * the key, which is still consumed. Fires only for the user's actions.
     */
    EventType<RequestEvent<TextBox>> SUBMIT = EventType.request("text_box.submit");
    /**
     * Fired after a submit by the user that no handler of {@link #SUBMIT} canceled. The value is the text, and
     * {@code previous()} equals it. There is no method to submit from code, so only Enter fires it.
     */
    EventType<ValueEvent<TextBox, String>> SUBMITTED = EventType.valueReport("text_box.submitted");

    /**
     * Returns the text.
     *
     * @return the current text, never null
     */
    String text();

    /**
     * Replaces the text, removing line breaks and cutting it to {@link #maxLength()}, and puts the caret at the
     * end. {@link #EDITED} fires if the result differs from the current text; {@link #EDIT} does not.
     *
     * @param text the new text
     * @return this box
     */
    TextBox setText(String text);

    /**
     * Returns the length limit.
     *
     * @return the most code points the box holds; {@link Integer#MAX_VALUE} by default
     */
    int maxLength();

    /**
     * Limits the text length. Text already longer is cut, and {@link #EDITED} fires.
     *
     * @param maxLength the most code points the box may hold
     * @return this box
     * @throws IllegalArgumentException if {@code maxLength} is negative
     */
    TextBox setMaxLength(int maxLength);

    /**
     * Returns whether the text is masked.
     *
     * @return true in password mode
     */
    boolean isPassword();

    /**
     * Turns password mode on or off. In password mode each code point is drawn as the mask character, and Ctrl+C
     * and Ctrl+X do nothing. {@link #text()} and {@link #selectedText()} still return the real text.
     *
     * @param password true to mask the text
     * @return this box
     */
    TextBox setPassword(boolean password);

    /**
     * Sets the character drawn for each code point in password mode.
     *
     * @param codePoint the mask character; {@code '*'} by default
     * @return this box
     * @throws IllegalArgumentException if {@code codePoint} is not a valid code point
     */
    TextBox setMaskCodePoint(int codePoint);

    /**
     * Sets the space between the text and the box's edges.
     *
     * @param padding the padding; 4 left and right, 5 top and bottom by default
     * @return this box
     */
    TextBox setTextPadding(Insets padding);

    /**
     * Returns the caret position.
     *
     * @return a char index from 0 to {@code text().length()}
     */
    int caret();

    /**
     * Returns the start of the selection, the lower of the caret and the selection anchor.
     *
     * @return a char index; equal to {@link #selectionEnd()} when nothing is selected
     */
    int selectionStart();

    /**
     * Returns the end of the selection, the higher of the caret and the selection anchor.
     *
     * @return a char index; equal to {@link #selectionStart()} when nothing is selected
     */
    int selectionEnd();

    /**
     * Returns the selected text.
     *
     * @return the text between {@link #selectionStart()} and {@link #selectionEnd()}; empty when nothing is
     *         selected
     */
    String selectedText();

    /**
     * Selects a range. The anchor is the fixed end and the caret the moving end, as after a Shift+arrow. Indexes
     * outside the text are clamped, and an index inside a surrogate pair moves to the pair's start.
     *
     * @param anchor the fixed end of the selection
     * @param caret where the caret goes
     * @return this box
     */
    TextBox select(int anchor, int caret);

    /**
     * Selects all the text and puts the caret at the end.
     *
     * @return this box
     */
    TextBox selectAll();

    /**
     * Returns whether the box is read-only.
     *
     * @return false by default
     */
    boolean isReadOnly();

    /**
     * Makes the box read-only or editable. A read-only box can be focused, selected and copied from, but typing,
     * deleting, cutting and pasting do nothing, and no caret shows. {@link #setText} still works.
     *
     * @param readOnly true to stop editing
     * @return this box
     */
    TextBox setReadOnly(boolean readOnly);

    /**
     * Returns whether gaining focus selects all the text.
     *
     * @return false by default
     */
    boolean selectsAllOnFocus();

    /**
     * Sets whether gaining focus selects all the text, so typing replaces it. A click that gives focus still places
     * the caret where it lands.
     *
     * @param selectAll true to select all on focus
     * @return this box
     */
    TextBox setSelectAllOnFocus(boolean selectAll);

    /**
     * A text box's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. Indexes follow
     * the rules in the class description.
     *
     * @param text the text, unmasked even in password mode; never null
     * @param caret the {@linkplain #caret() caret} index
     * @param anchor the end of the selection that stays put while the caret moves; equal to {@code caret} when
     *               nothing is selected
     * @param maxLength the {@linkplain #maxLength() maximum length}, in code points
     * @param password whether the box was in {@linkplain #isPassword() password mode}
     */
    record State(String text, int caret, int anchor, int maxLength, boolean password) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }

        /**
         * Returns the start of the selection.
         *
         * @return the smaller of {@code caret} and {@code anchor}
         */
        public int selectionStart() {
            return Math.min(this.caret, this.anchor);
        }

        /**
         * Returns the end of the selection.
         *
         * @return the larger of {@code caret} and {@code anchor}
         */
        public int selectionEnd() {
            return Math.max(this.caret, this.anchor);
        }

        /**
         * Returns whether any text was selected.
         *
         * @return true if {@code caret} and {@code anchor} differ
         */
        public boolean hasSelection() {
            return this.caret != this.anchor;
        }
    }
}
