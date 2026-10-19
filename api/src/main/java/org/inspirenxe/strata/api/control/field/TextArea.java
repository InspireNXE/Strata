package org.inspirenxe.strata.api.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.geometry.Insets;

import java.util.Objects;

/**
 * Text the user edits over several lines, such as a description or a note.
 *
 * <p>It edits like a {@link TextBox}, with these differences. Enter and keypad Enter insert a new line. Up and Down
 * move the caret a row, keeping its horizontal position across short rows, and Page Up and Page Down move it a
 * page. Home and End go to the start and end of the row, and with Ctrl to the start and end of the text. A triple
 * click selects the line. Pasted text keeps its line breaks, and {@code \r\n} and {@code \r} become {@code \n}.</p>
 *
 * <p>With wrapping on, which is the default, a line too long for the width breaks after the last space that fits,
 * or between characters when a word alone is too wide. A space where the line breaks stays at the end of the row.
 * With wrapping off, rows are the lines of the text and the view scrolls sideways to keep the caret in sight. Text
 * taller than the view scrolls vertically, with a scroll bar at the right edge, by wheel, by dragging the thumb and to
 * keep the caret in sight.</p>
 *
 * <p>It is focusable and tabbable, accepts text input and shows the text cursor. Tab is left to focus traversal. It
 * asks for {@value #DEFAULT_WIDTH} units of width and enough height for {@linkplain #setVisibleLines four lines}, and
 * text sits 4 units in from each edge unless the padding is changed.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link TextInput#EDIT}: Fired when the user is about to edit the text by typing, pressing Enter, deleting,
 *     cutting or pasting. The value is the whole text the edit would produce. While handlers run, the area still holds
 *     the text and selection from before the edit. Cancel the event to reject the edit and keep the text and selection
 *     as they were, or set another text to rewrite it; the value must not be set to null. The final text has its
 *     {@code \r\n} and {@code \r} turned into {@code \n} and is cut to {@link #maxLength()} before it applies, and the
 *     caret goes as many characters before its end as the edit left it, with nothing selected. An edit that would leave
 *     the text as it is, such as typing into a full area, fires nothing, and a read-only area fires nothing. Fires only
 *     for the user's actions: {@link #setText} and {@link #setMaxLength} fire none.</li>
 *     <li>{@link TextInput#EDITED}: Fired after the text changed: by an edit of the user's, by {@link #setText}, or by
 *     {@link #setMaxLength} cutting it. The value is the new text and {@code previous()} the text before, both with
 *     lines separated by {@code \n}; neither is null. An edit whose final text equals the old one fires nothing. Fires
 *     for changes made from code too.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>A text area reads the {@link TextBox} keys, so the two look alike, and the {@link
 * org.inspirenxe.strata.api.layout.ScrollPanel} keys for its scroll bar.</p>
 *
 * <pre>{@code
 * TextArea notes = factory.textArea().setText(item.notes()).setMaxLength(500)
 *         .onEvent(TextArea.EDITED, event -> item.setNotes(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#textArea()}. Do not implement this
 * interface.</p>
 */
public interface TextArea extends Component<TextArea>, TextInput<TextArea> {
    /**
     * The width, in UI units, a text area asks for.
     */
    float DEFAULT_WIDTH = 160f;
    /**
     * How many lines a text area asks room for unless {@linkplain #setVisibleLines told} otherwise.
     */
    int DEFAULT_VISIBLE_LINES = 4;

    /**
     * Returns the text.
     *
     * @return the text, with lines separated by {@code \n}
     */
    String text();

    /**
     * Replaces the text, cut to the length limit, puts the caret at its start and scrolls to the top.
     * {@link #EDITED} fires if the text changed; {@link #EDIT} does not.
     *
     * @param text the new text; {@code \r\n} and {@code \r} become {@code \n}
     * @return this text area
     */
    TextArea setText(String text);

    /**
     * Returns the length limit.
     *
     * @return the most code points the text may hold, line breaks included; {@link Integer#MAX_VALUE} by default
     */
    int maxLength();

    /**
     * Sets the length limit, cutting the text if it is longer. {@link #EDITED} fires if the text was cut.
     *
     * @param maxLength the most code points the text may hold
     * @return this text area
     * @throws IllegalArgumentException if {@code maxLength} is negative
     */
    TextArea setMaxLength(int maxLength);

    /**
     * Returns whether long lines wrap.
     *
     * @return true if long lines wrap; true by default
     */
    boolean wraps();

    /**
     * Turns wrapping on or off.
     *
     * @param wrap true to wrap long lines, false to scroll sideways
     * @return this text area
     */
    TextArea setWrap(boolean wrap);

    /**
     * Sets how many lines of text the desired height has room for. A new layout runs.
     *
     * @param lines the number of lines
     * @return this text area
     * @throws IllegalArgumentException if {@code lines} is less than 1
     */
    TextArea setVisibleLines(int lines);

    /**
     * Sets the space between the edge and the text. A new layout runs.
     *
     * @param padding the space in UI units; 4 on each side by default
     * @return this text area
     */
    TextArea setTextPadding(Insets padding);

    /**
     * Returns the caret position.
     *
     * @return the caret as a {@code char} index into the text
     */
    int caret();

    /**
     * Returns where the selection starts.
     *
     * @return the lower of the caret and the selection anchor
     */
    int selectionStart();

    /**
     * Returns where the selection ends.
     *
     * @return the higher of the caret and the selection anchor
     */
    int selectionEnd();

    /**
     * Returns the selected text.
     *
     * @return the selected text, or an empty string if nothing is selected
     */
    String selectedText();

    /**
     * Selects a range, clamped to the text. An index in the middle of a surrogate pair moves to its start.
     *
     * @param anchor the end that stays put
     * @param caret the end that moves
     * @return this text area
     */
    TextArea select(int anchor, int caret);

    /**
     * Selects all the text.
     *
     * @return this text area
     */
    TextArea selectAll();

    /**
     * Returns whether the text is read-only.
     *
     * @return true if read-only; false by default
     */
    boolean isReadOnly();

    /**
     * Makes the text read-only or editable. Read-only text can still be selected, copied and scrolled, and draws in
     * {@value TextBox#STYLE_TEXT_READ_ONLY}.
     *
     * @param readOnly true to make it read-only
     * @return this text area
     */
    TextArea setReadOnly(boolean readOnly);

    /**
     * Returns how far the text is scrolled down.
     *
     * @return the vertical scroll offset in UI units, as of the last paint
     */
    float scrollY();

    /**
     * A text area's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param text the text; never null
     * @param caret the caret index
     * @param anchor the selection anchor index
     * @param maxLength the length limit
     */
    record State(String text, int caret, int anchor, int maxLength) {

        /**
         * Checks the text.
         */
        public State {
            Objects.requireNonNull(text, "text");
        }
    }
}
