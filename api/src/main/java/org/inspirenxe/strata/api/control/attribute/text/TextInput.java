package org.inspirenxe.strata.api.control.attribute.text;

import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.ValueEvent;
import org.inspirenxe.strata.api.event.ValueRequestEvent;
import org.inspirenxe.strata.api.geom.Insets;

/**
 * A control the user types text into, such as a text box or a text area.
 *
 * <p>Indexes are char indexes into {@link #text()}, from 0 to its length, and never fall inside a surrogate pair.
 * Lengths are counted in code points.</p>
 *
 * <p>Handle {@link #EDIT} to refuse or rewrite what the user types, and {@link #EDITED} to react to every change of
 * the text.</p>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface TextInput<S extends TextInput<S>> extends HasText<S> {
    /**
     * Asked before the user edits the text by typing, deleting, cutting or pasting. The value is the whole text the
     * edit would produce. While handlers run, the control still holds the text and selection from before the edit.
     * Cancel the event to reject the edit, or set another text to rewrite it; the value must not be set to null.
     * The control applies its own rules to the final text, such as the {@linkplain #maxLength() length limit}, as
     * each control describes. An edit that would leave the text as it is fires nothing, and a read-only control
     * fires nothing. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<TextInput<?>, String>> EDIT = EventType.valueRequest("text_input.edit");
    /**
     * Told after the text changed, by the user or from code. The value is the new text and the previous value the
     * text before; neither is null. Fires for changes made from code too.
     */
    EventType<ValueEvent<TextInput<?>, String>> EDITED = EventType.valueReport("text_input.edited");

    /**
     * Returns the length limit.
     *
     * @return the most code points the control holds; {@link Integer#MAX_VALUE} by default
     */
    int maxLength();

    /**
     * Limits the text length. Text already longer is cut, and {@link #EDITED} fires.
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
     * Makes the control read-only or editable. A read-only control can be focused, selected and copied from, but
     * typing, deleting, cutting and pasting do nothing, and no caret shows. {@link #setText} still works.
     *
     * @param readOnly true to stop editing
     * @return this control
     */
    S setReadOnly(boolean readOnly);

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
     * Sets the space between the text and the control's edges.
     *
     * @param padding the padding
     * @return this control
     */
    S setTextPadding(Insets padding);
}
