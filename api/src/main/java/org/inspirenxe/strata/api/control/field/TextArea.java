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
package org.inspirenxe.strata.api.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.TextEntry;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * Text the user edits over several lines, such as a description or a note.
 *
 * <p>It edits like a {@link TextBox}, with these differences. Enter and keypad Enter insert a new line. Up and Down
 * move the caret a row, keeping its horizontal position across short rows, and Page Up and Page Down move it a
 * page. Home and End go to the start and end of the row, and with Ctrl to the start and end of the text. A triple
 * click selects the line. Pasted text keeps its line breaks and tabs: {@code \r\n} and every other line break, such
 * as {@code \r} or the line separator U+2028, become {@code \n}, and other control characters are dropped.</p>
 *
 * <p>With wrapping on, which is the default, a line too long for the width breaks after the last space that fits,
 * or between characters when a word alone is too wide, as a wrapping label's does. The spaces where the line breaks
 * stay at the end of the row, past its edge if they do not fit, and a caret among them is drawn at the edge.
 * With wrapping off, rows are the lines of the text, and a row wider than the view scrolls sideways, with a scroll
 * bar at the bottom edge, by horizontal wheel, by dragging the thumb and to keep the caret in sight. Text taller than
 * the view scrolls vertically, with a scroll bar at the right edge, by wheel, by dragging the thumb and to keep the
 * caret in sight.</p>
 *
 * <p>It is focusable and tabbable, accepts text input and shows the text cursor. Tab is left to focus traversal. It
 * asks for {@value #DEFAULT_WIDTH} units of width and enough height for {@linkplain #setVisibleLines four lines}, and
 * text sits 4 units in from each edge unless the padding is changed.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link TextEntry#MODIFY}: Fired when the user is about to edit the text by typing, pressing Enter, deleting,
 *     cutting or pasting, before {@link TextInput#EDIT}. It carries no value. Cancel it to reject the edit and keep
 *     the text and selection as they were; {@code EDIT} then does not fire. It fires for the same edits as
 *     {@code EDIT}.</li>
 *     <li>{@link TextInput#EDIT}: Fired when the user is about to edit the text by typing, pressing Enter, deleting,
 *     cutting or pasting, unless {@code MODIFY} was canceled. The value is the whole text the edit would produce.
 *     While handlers run, the area still holds the text and selection from before the edit. Cancel the event to
 *     reject the edit and keep the text and selection as they were, or set another text to rewrite it; the value must
 *     not be set to null. The final text has its line breaks turned into {@code \n} and its control characters other
 *     than tabs dropped, as pasted text does, and is cut to
 *     {@link #maxLength()} before it applies, and the caret goes as many characters before its end as the edit left
 *     it, with nothing selected. An edit that would leave the text as it is, such as typing into a full area, fires
 *     nothing, and a read-only area fires nothing. Fires only for the user's actions: {@link #setText} and
 *     {@link #setMaxLength} fire none.</li>
 *     <li>{@link TextInput#EDITED}: Fired after the text changed: by an edit of the user's, by {@link #setText}, or by
 *     {@link #setMaxLength} cutting it. The value is the new text and {@code previous()} the text before, both with
 *     lines separated by {@code \n}; neither is null. An edit whose final text equals the old one fires nothing.</li>
 *     <li>{@link TextEntry#MODIFIED}: Fired after every change that fires {@code EDITED}, after it, with the new and
 *     previous lengths in code points, line breaks included.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>A text area reads the {@link TextBox} keys, so the two look alike, and the {@link
 * org.inspirenxe.strata.api.layout.ScrollPanel} keys for its scroll bar. Its own {@value #STYLE_PADDING} sets the
 * space around its text. Of the caret keys it reads the paint, the reach and the blink, and always draws the caret
 * as a bar.</p>
 *
 * <pre>{@code
 * TextArea notes = factory.textArea().setText(item.notes()).setMaxLength(500)
 *         .onEvent(TextArea.EDITED, event -> item.setNotes(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#textArea()}. Do not implement this
 * interface.</p>
 */
public interface TextArea extends Component<TextArea>, Focusable<TextArea>, HasCursor<TextArea>, HasTooltip<TextArea>,
        TextInput<TextArea> {
    /**
     * The width, in UI units, a text area asks for.
     */
    float DEFAULT_WIDTH = 160f;
    /**
     * How many lines a text area asks room for unless {@linkplain #setVisibleLines told} otherwise.
     */
    int DEFAULT_VISIBLE_LINES = 4;
    /**
     * Style key for the space between a text area's edges and its text (insets, optional). Falls back to
     * {@value TextBox#STYLE_PADDING}, and defaults to 4 on every side without either.
     */
    String STYLE_PADDING = "textarea.padding";

    /**
     * Returns the placeholder.
     *
     * @return the placeholder; empty for a new area
     */
    Text placeholder();

    /**
     * Sets the hint drawn on the first line in the {@value TextBox#STYLE_PLACEHOLDER} style while the area is empty,
     * such as {@code "Add a note"}. It goes away as soon as there is text, hides while the area has focus when the
     * theme's {@value TextBox#STYLE_PLACEHOLDER_HIDE} says so, and is never part of {@link #text()}.
     *
     * @param placeholder the hint, or empty for none
     * @return this area
     */
    TextArea setPlaceholder(Text placeholder);

    /**
     * Does the same as {@link #setPlaceholder(Text)} with literal text.
     *
     * @param placeholder the hint, or empty for none
     * @return this area
     */
    default TextArea setPlaceholder(final String placeholder) {
        return this.setPlaceholder(Text.of(placeholder));
    }

    /**
     * Returns the text.
     *
     * @return the text, with lines separated by {@code \n}
     */
    String text();

    /**
     * Replaces the text, cut to the length limit. If that changes the text, the caret goes to its end with nothing
     * selected and the view scrolls to the top, where it stays until the caret moves, so the area shows the start of
     * the new text. {@link #EDITED} fires if the text changed; {@link #EDIT} does not. Setting the text it already
     * holds changes nothing.
     *
     * @param text the new text; its line breaks become {@code \n} and its control characters other than tabs are
     *     dropped, as pasted text's are
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
     * @return the vertical scroll offset in UI units, as of the last paint or the last call that set it
     */
    float scrollY();

    /**
     * Scrolls to an offset. The offset is at least 0, and once the area has been laid out it is at most what its rows
     * need; the next paint clamps it again to the rows in the area's size. The view stays there until the caret
     * moves, the user scrolls, or it is set again.
     *
     * @param scrollY the vertical offset in UI units
     * @return this text area
     * @throws IllegalArgumentException if the offset is NaN
     */
    TextArea setScrollY(float scrollY);

    /**
     * Scrolls the least distance that shows the caret's row and column, as moving the caret does. Before the area has
     * been laid out, it scrolls on the next paint.
     *
     * @return this text area
     */
    TextArea scrollToCaret();

    /**
     * Returns how many lines the text has: one more than its line breaks, whatever the wrapping.
     *
     * @return the number of lines, at least 1
     */
    int lineCount();

    /**
     * Returns the line a char index is on.
     *
     * @param index a char index; clamped to the text
     * @return the line, from 0
     */
    int lineOf(int index);

    /**
     * Returns where a line starts.
     *
     * @param line the line, from 0
     * @return the char index of the line's first char, or the text's length for an empty last line
     * @throws IndexOutOfBoundsException if {@code line} is negative or not less than {@link #lineCount()}
     */
    int lineStart(int line);

    /**
     * Returns the column of a char index on its line, counted in code points from the line's start, as a code
     * editor shows it.
     *
     * @param index a char index; clamped to the text, and moved off the middle of a surrogate pair
     * @return the column, from 0
     */
    int columnOf(int index);

    /**
     * Returns whether Tab types a tab character.
     *
     * @return false by default
     */
    boolean acceptsTab();

    /**
     * Makes Tab type a tab character instead of moving focus, for an area used as a small code editor. While it is on
     * and the area is editable, Tab types a tab, Shift+Tab does nothing, and Control+Tab and Control+Shift+Tab move
     * focus. A read-only area lets Tab move focus as usual.
     *
     * @param accepts true to type tabs
     * @return this text area
     */
    TextArea setAcceptsTab(boolean accepts);

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
