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
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.TextEntry;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.attribute.value.Validatable;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A single-line text field.
 *
 * <h2>Editing</h2>
 * <ul>
 *     <li>A click places the caret at the nearest character boundary. Shift+click extends the selection to it.</li>
 *     <li>Dragging selects by character. A drag that starts with a double click selects whole words. A double click
 *     selects the word, run of punctuation or run of spaces under the pointer; a triple click selects
 *     everything.</li>
 *     <li>Left and Right move by character, Home and End to either end. With Shift they extend the selection. With
 *     a selection and no Shift, Left and Right collapse it to its start or end. Backspace and Delete remove the
 *     character before or after the caret, or the selection. A character is what a reader sees as one, so a letter
 *     with its accents, a flag, or an emoji with its skin tone moves as one step and goes in one Backspace.</li>
 *     <li>Ctrl+Left and Ctrl+Right move by word; Ctrl+Backspace and Ctrl+Delete delete by word. They skip spaces,
 *     then stop at the other end of a word or of a run of punctuation, so {@code path/to/file} takes five steps.
 *     Words follow the Unicode word rules that {@link java.text.BreakIterator} applies, so an apostrophe or a
 *     decimal point inside a word stays in it.</li>
 *     <li>Ctrl+A selects all. Ctrl+C copies, Ctrl+X cuts and Ctrl+V pastes, through the host's clipboard. Pasted
 *     line breaks and other control characters, such as tabs, are removed, as typing never inserts them.</li>
 *     <li>A drag that starts with a plain press inside the selection carries the selected text out as
 *     {@code DataFormat.TEXT}, allowing a move and a copy, or only a copy from a read-only box; the press then
 *     places the caret only on a release that started no drag. A box that is enabled and editable takes text dropped
 *     on it at the boundary under the pointer, which shows a caret there while the drag is over it, as one edit
 *     through {@link #EDIT}, with its line breaks removed. A move inside the box itself is one edit that undoes in
 *     one step, and a drop inside the dragged text changes nothing. After a move to another component the box
 *     removes the text it gave away, as an edit too.</li>
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
 * <h2>Suggestions</h2>
 * <p>With a {@linkplain #setSuggestionProvider suggestion provider}, the box offers completions in a popup list
 * below it. After each edit by the user that changes the text, and on Ctrl+Space, the box asks the provider for
 * suggestions for its text and caret. If there are any, the list shows them with the first highlighted; if there are
 * none, it closes. While the list is open:</p>
 * <ul>
 *     <li>Up and Down move the highlight, wrapping at either end.</li>
 *     <li>Tab, Enter or keypad Enter accepts the highlighted suggestion, and a click accepts the one clicked. Enter
 *     then does not submit.</li>
 *     <li>Escape closes the list.</li>
 *     <li>Moving the caret with the keys, losing focus, {@link #setText} and a press outside the list close it.</li>
 * </ul>
 * <p>Accepting replaces the suggestion's range of the text with its text, puts the caret after it and closes the
 * list. The change is an edit by the user, so it goes through {@link #EDIT} like typing, and the list does not open
 * again until the next edit. A read-only or disabled box shows no suggestions.</p>
 *
 * <pre>{@code
 * TextBox name = factory.textBox()
 *         .setSuggestionProvider(TextBox.SuggestionProvider.words(List.of("Ana", "Ben", "Cleo", "Dev")))
 *         .onEvent(TextBox.COMPLETED, event -> greet(event.value().text()));
 * }</pre>
 *
 * <h2>Focus and the engine</h2>
 * <p>The box is focusable and tabbable and shows the text cursor. Focus by Tab puts the caret at the end; focus by
 * a click puts it at the click, unless the box {@linkplain #setSelectAllOnFocus selects all on focus}. While focused
 * it asks the engine for text input (required on SDL-based engines, which send no characters otherwise) and
 * consumes every key that types a character, so shortcuts the engine binds to those keys do not fire. Tab and
 * Escape are not consumed: Tab moves focus and Escape still reaches the engine. A key held with Alt neither edits
 * nor moves the caret, and reaches the engine too. Enter is consumed only when a handler for {@link #SUBMIT} or
 * {@link #SUBMITTED} is on the box or on an ancestor those events reach. While the suggestion list is open, the box
 * consumes Up, Down, Tab, Enter and Escape itself. A disabled box ignores input.</p>
 *
 * <h2>Text and indexes</h2>
 * <p>Caret and selection indexes count UTF-16 chars, like {@link String} indexes, but never fall inside a surrogate
 * pair, and the keys and the pointer keep the caret off the middle of a character as the editing list describes it.
 * {@link #select} may still put it there. {@link #maxLength()} counts code points. The box holds one line; line
 * breaks and other control characters given to {@link #setText} or pasted are removed. For a password, use a
 * {@link PasswordTextBox}, which keeps its text out of events, snapshots and the clipboard.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link TextEntry#MODIFY}: Fired when the user is about to edit the text by typing, deleting, cutting or
 *     pasting, before {@link TextInput#EDIT}. It carries no value. Cancel it to reject the edit and keep the text and
 *     selection as they were; {@code EDIT} then does not fire. It fires for the same edits as {@code EDIT}.</li>
 *     <li>{@link TextInput#EDIT}: Fired when the user is about to edit the text by typing, deleting, cutting or
 *     pasting, unless {@code MODIFY} was canceled. The value is the whole text the edit would produce. While handlers
 *     run, the box still holds the text and selection from before the edit. Cancel the event to reject the edit and
 *     keep the text and selection as they were, or set another text to rewrite it; the value must not be set to null.
 *     The final text has its line breaks and control characters removed and is cut to {@link #maxLength()} before
 *     it applies, and the caret goes as many characters before its end as the edit left it, with nothing selected. An
 *     edit that would leave the text as it is, such as typing into a full box, fires nothing, and a read-only box
 *     fires nothing. Fires only for the user's actions: {@link #setText} and {@link #setMaxLength} fire none.</li>
 *     <li>{@link TextInput#EDITED}: Fired after the text changed: by an edit of the user's, by {@link #setText}, or by
 *     {@link #setMaxLength} cutting it. The value is the new text and {@code previous()} the text before; neither is
 *     null. An edit whose final text equals the old one fires nothing.</li>
 *     <li>{@link TextEntry#MODIFIED}: Fired after every change that fires {@code EDITED}, after it, with the new and
 *     previous lengths in code points.</li>
 *     <li>{@link #COMPLETE}: Fired when the user accepts a suggestion, before the text changes. The value is the
 *     suggestion. Cancel it to keep the text as it is, or set another suggestion to apply that one instead.</li>
 *     <li>{@link #COMPLETED}: Fired after an accepted suggestion was applied, with the suggestion.</li>
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
 *     <li>{@value #STYLE_PLACEHOLDER} (text style, optional): the {@linkplain #setPlaceholder placeholder}. Falls
 *     back to {@value #STYLE_TEXT_DISABLED}.</li>
 *     <li>{@value #STYLE_PLACEHOLDER_HIDE} (metric, optional): any value but 0 hides the placeholder while the box
 *     has focus. Defaults to 0.</li>
 *     <li>{@value #STYLE_SELECTION} (paint, optional): drawn behind selected text. Falls back to translucent blue.</li>
 *     <li>{@value #STYLE_SELECTION_EXTEND} (metric, optional): how far the selection reaches above and below the
 *     line. Defaults to 0.</li>
 *     <li>{@value #STYLE_TEXT_SELECTED} (text style, optional): selected text is drawn in it. Defaults to none,
 *     which leaves selected text in its own style.</li>
 *     <li>{@value #STYLE_CARET} (paint, optional): the one-unit-wide caret. Falls back to light gray.</li>
 *     <li>{@value #STYLE_CARET_EXTEND} (metric, optional): how far the caret reaches above and below the line.
 *     Defaults to 0.</li>
 *     <li>{@value #STYLE_CARET_BLINK} (metric, optional): how long, in milliseconds, the caret stays shown and then
 *     hidden. Defaults to 500.</li>
 *     <li>{@value #STYLE_CARET_UNDERSCORE} (metric, optional): any value but 0 draws a caret at the end of the
 *     text as an underscore in the text's style. Defaults to 0.</li>
 *     <li>{@value #STYLE_SUGGESTIONS} (paint, optional): behind the suggestion list. Falls back to
 *     {@value org.inspirenxe.strata.api.control.collection.ComboBox#STYLE_POPUP}.</li>
 *     <li>{@value #STYLE_SUGGESTION_TEXT} (text style, optional): the text of each suggestion. Falls back to
 *     {@value #STYLE_TEXT}.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): the space between the box's edges and its text, unless
 *     {@link #setTextPadding(org.inspirenxe.strata.api.geometry.Insets)} sets one. Defaults to 4 at the sides and
 *     5 at the top and bottom.</li>
 * </ul>
 * <p>The suggestion rows are drawn with the {@link org.inspirenxe.strata.api.control.collection.ListView} row keys,
 * the highlighted one as the selected row.</p>
 *
 * <h2>Size</h2>
 * <p>The desired size is 120 units wide and one line plus the text padding tall (4 units left and right, 5 top and
 * bottom by default), and at least 20 tall, with a minimum width of 20. A smaller height set on it applies. Set a
 * width, or stretch it, to make it wider.</p>
 *
 * <pre>{@code
 * TextBox name = factory.textBox()
 *         .setMaxLength(32)
 *         .onEvent(TextBox.EDITED, event -> preview.setText("Hello, " + event.value()));
 * name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
 * }</pre>
 *
 * <p>It has a default {@linkplain #contextMenu() context menu}: Undo, Redo, Cut, Copy, Paste, Delete and Select
 * all, captioned from the {@code strata.edit.*} translation keys, each enabled only while it would do
 * something and acting as the matching key does. {@code setContextMenu(null)} removes it.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#textBox()}. Do not implement this
 * interface.</p>
 */
public interface TextBox extends Component<TextBox>, Focusable<TextBox>, HasCursor<TextBox>, HasTooltip<TextBox>,
        HasContextMenu<TextBox>, TextInput<TextBox>, Validatable<TextBox> {
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
     * Style key for the background while the box is enabled and its validation is an error, in place of the normal
     * and focused ones (paint, optional). Without it the box draws its normal face with a one-unit outline in
     * {@value Validatable#STYLE_ERROR}. Text areas, password boxes and the date and time pickers read it too.
     */
    String STYLE_INVALID = "textbox.invalid";
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
     * Style key for selected text (text style, optional). Selected text is drawn in this style instead of its own,
     * which suits a selection paint that hides the text's color. Defaults to none, which leaves selected text as it
     * is. Text areas and password boxes read it too.
     */
    String STYLE_TEXT_SELECTED = "textbox.text.selected";
    /**
     * Style key for how far the selection reaches above and below the line of text (metric, optional), in UI units.
     * Defaults to 0. Text areas and password boxes read it too.
     */
    String STYLE_SELECTION_EXTEND = "textbox.selection.extend";
    /**
     * Style key for the caret (paint, optional).
     */
    String STYLE_CARET = "textbox.caret";
    /**
     * Style key for how far the caret reaches above and below the line of text (metric, optional), in UI units.
     * Defaults to 0. Text areas and password boxes read it too.
     */
    String STYLE_CARET_EXTEND = "textbox.caret.extend";
    /**
     * Style key for how long the caret stays shown and then hidden (metric, optional), in milliseconds. Each edit or
     * caret move starts it shown again. Defaults to 500. Text areas and password boxes read it too.
     */
    String STYLE_CARET_BLINK = "textbox.caret.blink";
    /**
     * Style key for drawing a caret at the end of the text as an underscore (metric, optional): any value but 0
     * draws an underscore in the text's style, 1 unit after the text, or at its start in an empty field, instead of
     * the caret bar. Defaults to 0. Password boxes read it too; text areas always draw the bar.
     */
    String STYLE_CARET_UNDERSCORE = "textbox.caret.underscore";
    /**
     * Style key for behind the suggestion list (paint, optional).
     */
    String STYLE_SUGGESTIONS = "textbox.suggestions";
    /**
     * Style key for the text of each suggestion (text style, optional).
     */
    String STYLE_SUGGESTION_TEXT = "textbox.suggestion.text";
    /**
     * Style key for the placeholder (text style, optional).
     */
    String STYLE_PLACEHOLDER = "textbox.placeholder";
    /**
     * Style key for hiding the placeholder while the field has focus (metric, optional): any value but 0 hides it.
     * Defaults to 0, which shows it under the caret. Password boxes read it too.
     */
    String STYLE_PLACEHOLDER_HIDE = "textbox.placeholder.hide";
    /**
     * Style key for the space between a text box's edges and its text (insets, optional). Defaults to 4 at the
     * sides and 5 at the top and bottom. A spinner's field reads it, and a text area falls back to it.
     */
    String STYLE_PADDING = "textbox.padding";
    /**
     * How many suggestions show before the list scrolls, unless told otherwise.
     */
    int DEFAULT_MAX_VISIBLE_SUGGESTIONS = 8;

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
     * Fired when the user accepts a suggestion from the list, by Tab, Enter or a click, after the list closed and
     * before the text changes. The value is the suggestion. Cancel the event to keep the text as it is, or set
     * another suggestion to apply that one instead; the value must not be set to null. The final suggestion's range
     * is clamped to the text, and the change then goes through {@link #EDIT} like typing. Fires only for the user's
     * actions; there is no method to accept a suggestion from code.
     */
    EventType<ValueRequestEvent<TextBox, Suggestion>> COMPLETE = EventType.valueRequest("text_box.complete");
    /**
     * Fired after the suggestion a {@link #COMPLETE} request ended with was applied. The value is that suggestion,
     * and {@code previous()} equals it. A completion that {@link #EDIT} canceled fires nothing. {@link #EDITED}
     * reports the change of text as usual, before this.
     */
    EventType<ValueEvent<TextBox, Suggestion>> COMPLETED = EventType.valueReport("text_box.completed");

    /**
     * Returns the text.
     *
     * @return the current text, never null
     */
    String text();

    /**
     * Replaces the text, removing line breaks and other control characters and cutting it to {@link #maxLength()}.
     * If the result differs from the current text, the caret goes to the end with nothing selected and
     * {@link #EDITED} fires; otherwise nothing changes. {@link #EDIT} never fires.
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
     * Returns the placeholder.
     *
     * @return the placeholder; empty for a new box
     */
    Text placeholder();

    /**
     * Sets the hint drawn in the {@value #STYLE_PLACEHOLDER} style while the box is empty, such as
     * {@code "Search items"}. It shows whether or not the box has focus, unless the theme's
     * {@value #STYLE_PLACEHOLDER_HIDE} hides it while focused, goes away as soon as there is text, and is never part
     * of {@link #text()}.
     *
     * @param placeholder the hint, or empty for none
     * @return this box
     */
    TextBox setPlaceholder(Text placeholder);

    /**
     * Does the same as {@link #setPlaceholder(Text)} with literal text.
     *
     * @param placeholder the hint, or empty for none
     * @return this box
     */
    default TextBox setPlaceholder(final String placeholder) {
        return this.setPlaceholder(Text.of(placeholder));
    }

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
     * Returns how text narrower than the box sits in it.
     *
     * @return the alignment; {@link HorizontalAlignment#LEFT} by default
     */
    HorizontalAlignment textAlignment();

    /**
     * Sets how text narrower than the box sits in it, such as a column of prices aligned right. The caret and the
     * placeholder align with it. Text wider than the box starts at the left and scrolls sideways as before, so a
     * short value aligned right stays at the right. {@link HorizontalAlignment#STRETCH} counts as
     * {@link HorizontalAlignment#LEFT}.
     *
     * @param alignment the alignment
     * @return this box
     */
    TextBox setTextAlignment(HorizontalAlignment alignment);

    /**
     * Sets what offers suggestions as the user types, and closes the suggestion list if it is open. The box asks
     * the provider after each edit by the user that changes the text, and on Ctrl+Space, as the class description
     * says. It never asks while read-only or disabled.
     *
     * @param provider returns the suggestions for a text and caret, or {@code null} for none; none by default
     * @return this box
     */
    TextBox setSuggestionProvider(SuggestionProvider provider);

    /**
     * Returns how many suggestions show before the list scrolls.
     *
     * @return the maximum; {@value #DEFAULT_MAX_VISIBLE_SUGGESTIONS} by default
     */
    int maxVisibleSuggestions();

    /**
     * Sets how many suggestions show before the list scrolls. The list takes the new height the next time it opens or
     * its suggestions change.
     *
     * @param count the most rows the list shows at once
     * @return this box
     * @throws IllegalArgumentException if {@code count} is less than 1
     */
    TextBox setMaxVisibleSuggestions(int count);

    /**
     * Asks the provider for suggestions for the current text and caret, as Ctrl+Space does, and shows them, or closes
     * the list if there are none. Does nothing without a provider, while read-only or disabled, or while the box is
     * not in a shown tree.
     *
     * @return this box
     */
    TextBox showSuggestions();

    /**
     * Closes the suggestion list if it is open.
     *
     * @return this box
     */
    TextBox hideSuggestions();

    /**
     * Returns the suggestions the open list shows.
     *
     * @return an unmodifiable list in the provider's order; empty when the list is closed
     */
    List<Suggestion> shownSuggestions();

    /**
     * A completion for the text of a box: the text that replaces a range of the box's text when the user accepts it.
     * The list shows {@link #text()}.
     *
     * @param text what replaces the range; line breaks and control characters are removed when it applies
     * @param start the char index where the replaced range starts
     * @param end the char index where the replaced range ends, exclusive; equal to {@code start} to insert
     */
    record Suggestion(String text, int start, int end) {

        /**
         * Checks the text and the range.
         *
         * @throws IllegalArgumentException if {@code start} is negative or greater than {@code end}
         */
        public Suggestion {
            Objects.requireNonNull(text, "text");
            if (start < 0 || start > end) {
                throw new IllegalArgumentException("Range must satisfy 0 <= start <= end: " + start + " to " + end);
            }
        }
    }

    /**
     * Works out the suggestions for the text of a box.
     *
     * <pre>{@code
     * // Commands after a slash, replacing the whole text
     * box.setSuggestionProvider((text, caret) -> text.startsWith("/")
     *         ? commands.stream()
     *                 .filter(command -> command.startsWith(text))
     *                 .map(command -> new TextBox.Suggestion(command, 0, text.length()))
     *                 .toList()
     *         : List.of());
     * }</pre>
     */
    @FunctionalInterface
    interface SuggestionProvider {

        /**
         * Returns the suggestions for a text and caret, in the order the list shows them. Called on the UI thread
         * each time the box asks, so it should be quick.
         *
         * @param text the box's whole text
         * @param caret the caret, a char index from 0 to {@code text.length()}
         * @return the suggestions, or an empty list for none; never null
         */
        List<Suggestion> suggest(String text, int caret);

        /**
         * Returns a provider that completes the word at the caret from a list. The word is the run of characters
         * other than whitespace around the caret. Every entry that starts with the part of the word before the
         * caret, ignoring case, is suggested in the list's order, except an entry equal to the whole word. Each
         * suggestion replaces the whole word. A caret with no word before it gets no suggestions.
         *
         * @param words the words to offer; copied, so later changes to the collection are not seen
         * @return the provider
         */
        static SuggestionProvider words(final Collection<String> words) {
            final List<String> copy = List.copyOf(words);
            return (text, caret) -> {
                int start = caret;
                while (start > 0 && !Character.isWhitespace(text.codePointBefore(start))) {
                    start -= Character.charCount(text.codePointBefore(start));
                }
                if (start == caret) {
                    return List.of();
                }
                int end = caret;
                while (end < text.length() && !Character.isWhitespace(text.codePointAt(end))) {
                    end += Character.charCount(text.codePointAt(end));
                }
                final String typed = text.substring(start, caret);
                final String word = text.substring(start, end);
                final List<Suggestion> found = new ArrayList<>();
                for (final String candidate : copy) {
                    if (candidate.regionMatches(true, 0, typed, 0, typed.length()) && !candidate.equals(word)) {
                        found.add(new Suggestion(candidate, start, end));
                    }
                }
                return found;
            };
        }
    }

    /**
     * A text box's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. Indexes follow
     * the rules in the class description.
     *
     * @param text the text; never null
     * @param caret the {@linkplain #caret() caret} index
     * @param anchor the end of the selection that stays put while the caret moves; equal to {@code caret} when
     *               nothing is selected
     * @param maxLength the {@linkplain #maxLength() maximum length}, in code points
     */
    record State(String text, int caret, int anchor, int maxLength) {

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
