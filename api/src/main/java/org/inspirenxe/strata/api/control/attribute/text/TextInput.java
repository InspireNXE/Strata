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

import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.TextHighlighter;

/**
 * A control the user types into and whose text code may read, such as a text box or a text area.
 *
 * <p>Indexes are char indexes into {@link #text()}, from 0 to its length, and never fall inside a surrogate pair.
 * Lengths are counted in code points.</p>
 *
 * <p>Handle {@link #EDIT} to refuse or rewrite what the user types, and {@link #EDITED} to react to every change of
 * the text. A field fires these as well as {@link TextEntry#MODIFY} and {@link TextEntry#MODIFIED}: a user's edit
 * asks {@code MODIFY} and then, unless it was canceled, {@code EDIT}, and every change reports {@code EDITED} and
 * then {@code MODIFIED}.</p>
 *
 * <h2>Edits from code</h2>
 * <p>{@link #setText} replaces everything and puts the caret at the end. {@link #insert}, {@link #replace},
 * {@link #append} and {@link #delete} change part of the text and keep the caret and the selection with the chars
 * they were next to, so a chat box can put a name at the caret without losing the user's place. Like {@code setText}
 * they report {@code EDITED} and {@code MODIFIED} and ask nothing.</p>
 *
 * <h2>Undo</h2>
 * <p>The field records the user's edits and the edits from code, and Ctrl+Z undoes them, and Ctrl+Y and Ctrl+Shift+Z
 * redo them. Typing merges into one edit until the caret moves, the user pauses for a second, or a word ends with a
 * space or a line break; a paste, a cut, a deleted selection and a completed suggestion are an edit each, and a run of
 * Backspaces or of Deletes is one. An undo or a redo by key is the user's, so it asks {@code MODIFY} and {@code EDIT}
 * with the text it would restore, and a handler that refuses letters refuses an undo back to them too. {@link #undo}
 * and {@link #redo} from code ask nothing. {@code setText}, from code or a binding, clears the history, since the text
 * the user edited is gone.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface TextInput<S extends TextInput<S>> extends TextEntry<S> {
    /**
     * Asked before the user edits the text by typing, deleting, cutting, pasting, undoing or redoing, after
     * {@link TextEntry#MODIFY} if no handler canceled that. The value is the whole text the edit would produce. A
     * handler that rewrites the text of an undo or a redo makes it an edit of its own, with the caret at the end. While handlers run, the
     * control still holds the text and selection from before the edit. Cancel the event to reject the edit, or set
     * another text to rewrite it; the value must not be set to null. The control applies its own rules to the final
     * text, such as the {@linkplain #maxLength() length limit}, as each control describes. If a handler sets the
     * text from code, the final text replaces the text it set, and {@link #EDITED} reports that as the previous text.
     * An edit that would leave the text as it is fires nothing, and a read-only control fires nothing. Fires only for
     * the user's actions.
     */
    EventType<ValueRequestEvent<TextInput<?>, String>> EDIT = EventType.valueRequest("text_input.edit");
    /**
     * Told after the text changed, by the user or from code, before {@link TextEntry#MODIFIED}. The value is the new
     * text and the previous value the text before; neither is null.
     */
    EventType<ValueEvent<TextInput<?>, String>> EDITED = EventType.valueReport("text_input.edited");
    /**
     * The text, as {@link #text()} and {@link #setText} say. Binds two ways through {@link #EDITED}, so a bound
     * state takes each edit the user makes once {@link #EDIT} accepted it. A text set from the binding moves the
     * caret to the end when it differs from the control's, as {@code setText} does, so the binding writing back the
     * text the user just typed leaves the caret where it is.
     */
    PropertyKey<TextInput<?>, String> TEXT = PropertyKey.of("text_input.text", TextInput::text, TextInput::setText,
            EDITED);

    /**
     * Returns the text.
     *
     * @return the text, never null; empty when there is none
     */
    String text();

    /**
     * Replaces the text. If that changes the text, the caret moves to the end, the selection is cleared and the undo
     * history is cleared. Setting the text the control already holds changes nothing, so the caret, the selection and
     * the history stay as they are.
     *
     * @param text the text, or an empty string for none
     * @return this control
     */
    S setText(String text);

    /**
     * Puts text at the caret, over the selection if there is one, as code: the caret goes after the new text with
     * nothing selected. The text is cleaned as the control cleans typed text, such as losing line breaks in a
     * single-line field, and cut to the room the {@linkplain #maxLength() length limit} leaves. Reports
     * {@link #EDITED} and {@link TextEntry#MODIFIED} if the text changed, asks nothing, and is recorded for undo.
     *
     * <pre>{@code
     * chat.insert("@" + name + " ");
     * }</pre>
     *
     * @param text the text to put in
     * @return this control
     */
    S insert(String text);

    /**
     * Replaces a range of the text as code. The range is clamped to the text and its ends move off the middle of a
     * surrogate pair, as {@link #select} moves them. The new text is cleaned and cut as {@link #insert} says. The
     * caret and the selection anchor stay with the chars they were next to: after the range they shift with the
     * change, inside it they go to the end of the new text, and before it they stay. A caret at the end of the range
     * counts as after it. The view scrolls only as far as needed to keep a moved caret in sight. Reports
     * {@link #EDITED} and {@link TextEntry#MODIFIED} if the text changed, asks nothing, and is recorded for undo.
     *
     * @param start the char index the range starts at
     * @param end the char index after the range
     * @param text the text that replaces the range
     * @return this control
     * @throws IllegalArgumentException if {@code start} is greater than {@code end}
     */
    S replace(int start, int end, String text);

    /**
     * Adds text at the end as code, as {@link #replace} at the end of the text does, so a caret at the end stays at
     * the end and a caret elsewhere stays where it is.
     *
     * @param text the text to add
     * @return this control
     */
    S append(String text);

    /**
     * Removes a range of the text as code, as {@link #replace} with empty text does.
     *
     * @param start the char index the range starts at
     * @param end the char index after the range
     * @return this control
     * @throws IllegalArgumentException if {@code start} is greater than {@code end}
     */
    S delete(int start, int end);

    /**
     * Returns whether {@link #undo} would take an edit back.
     *
     * @return true if the history holds an edit to undo
     */
    boolean canUndo();

    /**
     * Returns whether {@link #redo} would make an undone edit again.
     *
     * @return true if an edit was undone and no edit came after
     */
    boolean canRedo();

    /**
     * Takes back the last recorded edit as code, asking nothing, and puts the caret and the selection back where they
     * were before it. Reports {@link #EDITED} and {@link TextEntry#MODIFIED}. Does nothing if there is nothing to
     * undo. Works on a read-only control too, as {@code setText} does.
     *
     * @return this control
     */
    S undo();

    /**
     * Makes the last undone edit again as code, asking nothing, and puts the caret and the selection where the edit
     * left them. Reports {@link #EDITED} and {@link TextEntry#MODIFIED}. Does nothing if there is nothing to redo. Any
     * new edit after an undo drops what could be redone.
     *
     * @return this control
     */
    S redo();

    /**
     * Forgets every recorded edit, so nothing can be undone or redone.
     *
     * @return this control
     */
    S clearHistory();

    /**
     * Returns how many edits the control keeps for undo.
     *
     * @return the limit; 100 by default
     */
    int historyLimit();

    /**
     * Sets how many edits the control keeps for undo. Past the limit the oldest are dropped, and a limit of 0 keeps
     * none.
     *
     * @param edits the most edits to keep
     * @return this control
     * @throws IllegalArgumentException if {@code edits} is negative
     */
    S setHistoryLimit(int edits);

    /**
     * Returns the selected text.
     *
     * @return the text between {@link #selectionStart()} and {@link #selectionEnd()}; empty when nothing is
     *         selected
     */
    String selectedText();

    /**
     * Returns the highlighter that styles parts of the text.
     *
     * @return the highlighter; {@link TextHighlighter#NONE} by default
     */
    TextHighlighter highlighter();

    /**
     * Sets a highlighter that styles parts of the text, for syntax coloring, highlighted matches and the like. The
     * control calls it whenever its text or its base style changes and draws each run it returns in the run's
     * style, measuring every run in its own style, so the caret and selection line up with styled text. A disabled
     * control ignores the runs and draws everything in its disabled style; a read-only control keeps them. Runs do
     * not change the line height, which stays the base style's.
     *
     * @param highlighter the highlighter, or {@link TextHighlighter#NONE} for plain text
     * @return this control
     */
    S setHighlighter(TextHighlighter highlighter);
}
