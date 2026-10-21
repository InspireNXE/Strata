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
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's own type, which the setters return
 */
public interface TextInput<S extends TextInput<S>> extends TextEntry<S> {
    /**
     * Asked before the user edits the text by typing, deleting, cutting or pasting, after {@link TextEntry#MODIFY}
     * if no handler canceled that. The value is the whole text the edit would produce. While handlers run, the
     * control still holds the text and selection from before the edit. Cancel the event to reject the edit, or set
     * another text to rewrite it; the value must not be set to null. The control applies its own rules to the final
     * text, such as the {@linkplain #maxLength() length limit}, as each control describes. An edit that would leave
     * the text as it is fires nothing, and a read-only control fires nothing. Fires only for the user's actions.
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
     * caret to the end, as {@code setText} does.
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
     * Replaces the text. The caret moves to the end and the selection is cleared.
     *
     * @param text the text, or an empty string for none
     * @return this control
     */
    S setText(String text);

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
