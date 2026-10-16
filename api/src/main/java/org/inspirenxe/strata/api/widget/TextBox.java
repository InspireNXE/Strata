package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Insets;

import java.util.Objects;
import java.util.function.Consumer;

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
 * </ul>
 * <p>The caret blinks every half second on the host's clock and shows again after each edit or move. When the text
 * is wider than the box, it scrolls sideways to keep the caret in view.</p>
 *
 * <h2>Focus and the engine</h2>
 * <p>The box is focusable and tabbable and shows the text cursor. Focus by Tab puts the caret at the end; focus by
 * a click puts it at the click. While focused it asks the engine for text input (required on Minecraft 26.3, which
 * sends no characters otherwise) and consumes every key that types a character, so engine shortcuts on those keys,
 * such as Minecraft's inventory key, do not fire. Tab, Escape and Enter are not consumed: Tab moves focus and
 * Escape still reaches the engine. A disabled box ignores input.</p>
 *
 * <h2>Text and indexes</h2>
 * <p>Caret and selection indexes count UTF-16 chars, like {@link String} indexes, but never fall inside a surrogate
 * pair: editing works on whole code points. {@link #maxLength()} counts code points. The box holds one line; line
 * breaks given to {@link #setText} or pasted are removed. In password mode every code point is drawn as the mask
 * character, and copy and cut do nothing.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_NORMAL} (paint, required): the background when not focused.</li>
 *     <li>{@value #STYLE_FOCUSED} (paint, required): the background when focused.</li>
 *     <li>{@value #STYLE_DISABLED} (paint, required): the background when disabled.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the text. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the text when disabled. Falls back to
 *     {@value #STYLE_TEXT}.</li>
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
 *         .onTextChanged(text -> preview.setText("Hello, " + text));
 * name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#textBox()}. Do not implement this
 * interface.</p>
 */
public interface TextBox extends Component<TextBox> {
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
     * Style key for the selection highlight (paint, optional).
     */
    String STYLE_SELECTION = "textbox.selection";
    /**
     * Style key for the caret (paint, optional).
     */
    String STYLE_CARET = "textbox.caret";

    /**
     * Returns the text.
     *
     * @return the current text, never null
     */
    String text();

    /**
     * Replaces the text, removing line breaks and cutting it to {@link #maxLength()}, and puts the caret at the
     * end. Text-changed listeners run if the result differs from the current text.
     *
     * @param text the new text
     * @return this box
     * @throws NullPointerException if {@code text} is null
     */
    TextBox setText(String text);

    /**
     * Returns the length limit.
     *
     * @return the most code points the box holds; {@link Integer#MAX_VALUE} by default
     */
    int maxLength();

    /**
     * Limits the text length. Text already longer is cut, and text-changed listeners run.
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
     * @throws NullPointerException if {@code padding} is null
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
     * Adds a listener told the new text after every change: typing, deleting, cutting, pasting,
     * {@link #setText} and {@link #setMaxLength} cutting the text. Listeners run in the order they were added.
     *
     * @param listener receives the new text
     * @return this box
     * @throws NullPointerException if {@code listener} is null
     */
    TextBox onTextChanged(Consumer<String> listener);

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
         *
         * @throws NullPointerException if {@code text} is null
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
