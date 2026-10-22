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
package org.inspirenxe.strata.api.control.attribute.text;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.state.PropertyKey;

/**
 * A control the user types into, without access to what was typed: a text box, a text area or a password box.
 *
 * <p>Nothing here carries the text. {@link TextInput} adds the text and its events for the fields whose text code
 * may read; a {@link org.inspirenxe.strata.api.control.field.PasswordTextBox} has only this, so a handler written
 * against it learns how long the text is and when it changed, never what it says.</p>
 *
 * <p>Indexes are char indexes into the text, from 0 to its length in chars, and never fall inside a surrogate pair.
 * Lengths are counted in code points. On a control whose text code cannot read, the indexes are into text the
 * caller cannot see, and out-of-range indexes are still clamped.</p>
 *
 * <p>Handle {@link #MODIFY} to refuse what the user types into any field, and {@link #MODIFIED} to react to every
 * change, such as enabling a Save button while every field of a form has something in it.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface TextEntry<S extends TextEntry<S>> extends EventTarget<S> {
    /**
     * Asked before the user changes the text by typing, deleting, cutting or pasting. It carries no value; cancel it
     * to refuse the edit, which leaves the text and the selection as they were. While handlers run, the control
     * still holds the text from before the edit. An edit that would leave the text as it is fires nothing, and a
     * read-only control fires nothing. Each control says what it asks after this. Fires only for the user's actions.
     */
    EventType<RequestEvent<TextEntry<?>>> MODIFY = EventType.request("text_entry.modify");
    /**
     * Told after the text changed, by the user or from code. The value is the new length in code points and the
     * previous value the length before; they are equal when an edit replaced text with text as long.
     */
    EventType<ValueEvent<TextEntry<?>, Integer>> MODIFIED = EventType.valueReport("text_entry.modified");
    /**
     * Told after the caret or the selection anchor moved, by the user or from code: a key, a press or a drag, an
     * edit, focus, or a call such as {@link #select}. The value is where they are now and the previous value where
     * they were. It fires once for each input or call that moved them, after {@link TextInput#EDITED} and
     * {@link #MODIFIED} when an edit moved them. There is no request, since moving the caret changes nothing a
     * handler would need to refuse. A field whose text code cannot read reports it too: its indexes show only where in
     * the length the caret is.
     */
    EventType<ValueEvent<TextEntry<?>, Caret>> CARET_MOVED = EventType.valueReport("text_entry.caret_moved");
    /**
     * Whether the control is read-only, as {@link #isReadOnly()} and {@link #setReadOnly} say. Binds one way.
     */
    PropertyKey<TextEntry<?>, Boolean> READ_ONLY = PropertyKey.of("text_entry.read_only", TextEntry::isReadOnly,
            TextEntry::setReadOnly);

    /**
     * Returns how long the text is.
     *
     * @return the length in code points, as {@link #maxLength()} counts
     */
    int length();

    /**
     * Returns whether there is no text.
     *
     * @return true if the length is 0
     */
    boolean isEmpty();

    /**
     * Removes all the text. {@link #MODIFIED} fires if there was any; {@link #MODIFY} does not.
     *
     * @return this control
     */
    S clear();

    /**
     * Returns the length limit.
     *
     * @return the most code points the control holds; {@link Integer#MAX_VALUE} by default
     */
    int maxLength();

    /**
     * Limits the text length. Text already longer is cut, which reports the change.
     *
     * @param maxLength the most code points the control may hold
     * @return this control
     * @throws IllegalArgumentException if {@code maxLength} is negative
     */
    S setMaxLength(int maxLength);

    /**
     * Returns whether the control is read-only.
     *
     * @return false by default
     */
    boolean isReadOnly();

    /**
     * Makes the control read-only or editable. A read-only control can be focused and selected, but typing,
     * deleting, cutting and pasting do nothing, and no caret shows. Setting the text from code still works.
     *
     * @param readOnly true to stop editing
     * @return this control
     */
    S setReadOnly(boolean readOnly);

    /**
     * Returns the caret position.
     *
     * @return a char index from 0 to the text's length in chars
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
     * Selects a range. The anchor is the fixed end and the caret the moving end, as after a Shift+arrow. Indexes
     * outside the text are clamped, and an index inside a surrogate pair moves to the pair's start.
     *
     * @param anchor the fixed end of the selection
     * @param caret where the caret goes
     * @return this control
     */
    S select(int anchor, int caret);

    /**
     * Selects all the text and puts the caret at the end.
     *
     * @return this control
     */
    S selectAll();

    /**
     * Sets the space between the text and the control's edges. Padding set here stays through theme changes.
     *
     * @param padding the padding; {@link #clearTextPadding()} goes back to the theme's
     * @return this control
     */
    S setTextPadding(Insets padding);

    /**
     * Goes back to the theme's text padding for the control, or the control's default when the theme has none. The
     * padding then follows theme changes again.
     *
     * @return this control
     */
    S clearTextPadding();

    /**
     * Sets the same text padding on all four sides.
     *
     * @param all the text padding on every side
     * @return this control
     */
    default S setTextPadding(final float all) {
        return this.setTextPadding(Insets.of(all));
    }

    /**
     * Sets one text padding for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the text padding on the left and right
     * @param vertical the text padding on the top and bottom
     * @return this control
     */
    default S setTextPadding(final float horizontal, final float vertical) {
        return this.setTextPadding(Insets.of(horizontal, vertical));
    }

    /**
     * Sets the text padding on each side.
     *
     * @param left the text padding on the left
     * @param top the text padding on the top
     * @param right the text padding on the right
     * @param bottom the text padding on the bottom
     * @return this control
     */
    default S setTextPadding(final float left, final float top, final float right, final float bottom) {
        return this.setTextPadding(new Insets(left, top, right, bottom));
    }

    /**
     * Where the caret and the selection anchor are, as {@link #CARET_MOVED} reports them.
     *
     * @param anchor the fixed end of the selection, a char index
     * @param caret the caret, the moving end of the selection, a char index
     */
    record Caret(int anchor, int caret) {

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
         * Returns whether any text is selected.
         *
         * @return true if {@code caret} and {@code anchor} differ
         */
        public boolean hasSelection() {
            return this.caret != this.anchor;
        }
    }
}
