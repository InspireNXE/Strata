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
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.text.Text;

import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * A single-line field for a password or another secret.
 *
 * <p>It edits like a {@link TextBox}, but keeps its text from showing up where nobody meant it to. It holds the text
 * in a {@code char[]} that Strata zeroes, gives it to code only as a {@code char[]}, and never puts it in an event, a
 * snapshot, the clipboard or a {@code toString}. It cannot keep the text from code in the same process that sets out
 * to read it: a {@link org.inspirenxe.strata.api.event.input.InputEvents#CHAR} handler on the box sees each typed
 * code point, and the engine delivers them.</p>
 *
 * <pre>{@code
 * PasswordTextBox password = factory.passwordTextBox().setPlaceholder("Password").setMaxLength(64);
 * PasswordTextBox confirm = factory.passwordTextBox().setPlaceholder("Repeat password").setRevealable(false);
 *
 * form.onEvent(TextEntry.MODIFIED, event -> save.setEnabled(!password.isEmpty() && password.matches(confirm)));
 * password.onEvent(PasswordTextBox.SUBMITTED, event -> password.withPassword(chars -> login(name.text(), chars)));
 * }</pre>
 *
 * <h2>Reading and setting the text</h2>
 * <ul>
 *     <li>{@link #withPassword} hands a reader a copy and zeroes the copy when the reader returns or throws.
 *     {@link #copyPassword()} returns a new array the caller owns and should zero with {@code Arrays.fill} when done.
 *     {@link #matches} compares two boxes without copying either, for a confirm field. Code that needs a
 *     {@link String}, such as a network library, makes one itself; the box never makes one from its text except as
 *     described under Peeking.</li>
 *     <li>{@link #setPassword} copies an array and {@link #clear()} zeroes and empties the box. There is no form that
 *     takes a {@code String}: a caller with one passes {@code toCharArray()}, which keeps the conversion in sight.</li>
 *     <li>The box zeroes and empties itself, firing nothing, after the screen that holds it closes or rebuilds and
 *     after the overlay that holds it closes. {@code Screen.closed()} runs first, so a screen can still read the box
 *     there. A collapsed expander or a swapped tab page takes the box out of the tree without wiping it, since the
 *     content comes back.</li>
 * </ul>
 *
 * <h2>Editing</h2>
 * <p>Editing works as in a {@link TextBox}, with these differences:</p>
 * <ul>
 *     <li>Every code point draws as the {@linkplain #setMaskCodePoint mask}. The caret and the selection sit on the
 *     mask glyphs. The placeholder is not masked.</li>
 *     <li>Ctrl+C and Ctrl+X do nothing, read-only or not, revealed or not.</li>
 *     <li>Ctrl+V pastes with line breaks removed. A paste that the length limit or the
 *     {@linkplain #setCodePointFilter code point filter} would change is refused whole, since a password silently cut
 *     short is worse than none.</li>
 *     <li>Ctrl+Left, Ctrl+Right, Ctrl+Backspace and Ctrl+Delete treat the whole text as one word, and a double click
 *     selects everything, so the caret does not show where spaces are.</li>
 *     <li>Typed characters are consumed while the box has focus, read-only included, so they stop at the box and do
 *     not reach ancestors' {@code CHAR} handlers. Control characters are not typed.</li>
 *     <li>Line breaks are removed from pastes and from {@link #setPassword}. The length limit counts code points and
 *     cuts as on a text box.</li>
 *     <li>It has no suggestions, highlighter or text filter. Its tooltip is only what code sets.</li>
 *     <li>While focused it asks the engine for text input of the
 *     {@link org.inspirenxe.strata.api.input.TextInputKind#SECRET} kind, so an engine can turn off input methods and
 *     suggestions that would show or keep the text.</li>
 * </ul>
 *
 * <h2>Peeking</h2>
 * <ul>
 *     <li>An eye at the right end of the box toggles between the mask and the text on a click, and so does Alt+F8
 *     while the box has focus. The eye cannot take focus, so Tab order is the same with or without it and a press on
 *     it leaves focus in the box.</li>
 *     <li>The eye shows while the box is {@linkplain #setRevealable revealable}, enabled and not empty. The text keeps
 *     the eye's room whenever the box is revealable, so it does not move when the eye appears.</li>
 *     <li>Text set from code cannot be revealed by the user until they clear the box or replace all of its text, so
 *     someone at an unlocked screen cannot read a remembered password. The eye draws disabled until then.
 *     {@link #setRevealed} still works; revealable governs only the user.</li>
 *     <li>The box hides the text again when it loses focus, is disabled or is made unrevealable, and when code sets
 *     its text, reporting {@link #REVEALED} each time. A wipe hides it too and fires nothing.</li>
 *     <li>While revealed the box makes a {@link String} of its text, once per edit, to measure and draw it, and drops
 *     it when the text hides again. A project that cannot accept that turns reveal off.</li>
 * </ul>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link TextEntry#MODIFY}: Fired when the user is about to change the text by typing, deleting, cutting or
 *     pasting, with no value. Cancel it to refuse the edit. An edit that would change nothing fires nothing, and a
 *     read-only box fires nothing.</li>
 *     <li>{@link TextEntry#MODIFIED}: Fired after every change of the text, with the new and previous lengths in code
 *     points, including {@link #setPassword}, {@link #clear()} and {@link #setMaxLength} cutting. Wiping fires
 *     nothing.</li>
 *     <li>{@link #REVEAL}: Fired when the user clicks the eye or presses Alt+F8, with the state it would set.</li>
 *     <li>{@link #REVEALED}: Fired after the text was shown or hidden, by the user, by {@link #setRevealed}, or by
 *     the box hiding itself.</li>
 *     <li>{@link #SUBMIT} and {@link #SUBMITTED}: Fired when the user presses Enter, with no value. A handler reads
 *     the box with {@link #withPassword}.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>A password box reads the {@link TextBox} keys for its face, text, selection, caret, placeholder and padding,
 * so every theme that styles text boxes styles it. Its own keys are optional:</p>
 * <ul>
 *     <li>{@value #STYLE_MASK} (text style): the mask while the box is enabled and editable. Falls back to
 *     {@value TextBox#STYLE_TEXT}. A disabled or read-only box draws the mask in the text box's disabled or read-only
 *     text style.</li>
 *     <li>{@value #STYLE_REVEAL} (paint): the eye while the text is hidden. Falls back to an eye drawn in the
 *     placeholder's color.</li>
 *     <li>{@value #STYLE_REVEAL_HOVERED} (paint): the eye under the pointer while the text is hidden. Falls back to
 *     {@value #STYLE_REVEAL}, else an eye drawn in the text color.</li>
 *     <li>{@value #STYLE_REVEAL_ACTIVE} (paint): the eye while the text shows, offering to hide it. Falls back to an
 *     eye with a slash drawn in the text color.</li>
 *     <li>{@value #STYLE_REVEAL_DISABLED} (paint): the eye while the user cannot reveal the text. Falls back to
 *     {@value #STYLE_REVEAL}, else an eye drawn in the disabled text color.</li>
 * </ul>
 *
 * <h2>Size</h2>
 * <p>As a {@link TextBox}: 120 units wide and one line plus the text padding tall, at least 20 by 20. The eye is a
 * square one line tall at the right of the text area, 2 units from the text.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#passwordTextBox()}. Do not implement this
 * interface.</p>
 */
public interface PasswordTextBox extends Component<PasswordTextBox>, Focusable<PasswordTextBox>,
        HasCursor<PasswordTextBox>, HasTooltip<PasswordTextBox>, TextEntry<PasswordTextBox> {
    /**
     * Style key for the mask while the box is enabled and editable (text style, optional).
     */
    String STYLE_MASK = "passwordbox.mask";
    /**
     * Style key for the eye while the text is hidden (paint, optional).
     */
    String STYLE_REVEAL = "passwordbox.reveal";
    /**
     * Style key for the eye under the pointer while the text is hidden (paint, optional).
     */
    String STYLE_REVEAL_HOVERED = "passwordbox.reveal.hovered";
    /**
     * Style key for the eye while the text shows (paint, optional).
     */
    String STYLE_REVEAL_ACTIVE = "passwordbox.reveal.active";
    /**
     * Style key for the eye while the user cannot reveal the text (paint, optional).
     */
    String STYLE_REVEAL_DISABLED = "passwordbox.reveal.disabled";
    /**
     * The mask a new box draws for each code point.
     */
    int DEFAULT_MASK = '*';

    /**
     * Fired when the user clicks the eye or presses Alt+F8, before the text shows or hides. The value is the state the
     * toggle would set: true to show the text. Cancel the event to keep the state, for example to ask for the account
     * password first, or set another value to apply that instead; a null value keeps the state. The eye and Alt+F8
     * ask to show the text only while the user may reveal it, as the class description says. Fires only for the
     * user's actions.
     */
    EventType<ValueRequestEvent<PasswordTextBox, Boolean>> REVEAL = EventType.valueRequest("password_text_box.reveal");
    /**
     * Fired after the text was shown or hidden: by the user, by {@link #setRevealed}, or by the box hiding itself as
     * the class description says. The value is true while the text shows, and {@code previous()} the state before.
     * A wipe fires nothing.
     */
    EventType<ValueEvent<PasswordTextBox, Boolean>> REVEALED = EventType.valueReport("password_text_box.revealed");
    /**
     * Fired when the user presses Enter or keypad Enter while the box has focus. A held key's repeats do not fire
     * it. Cancel it to ignore the key, which is still consumed. Enter is consumed only when a handler for this or
     * {@link #SUBMITTED} is on the box's path. Fires only for the user's actions.
     */
    EventType<RequestEvent<PasswordTextBox>> SUBMIT = EventType.request("password_text_box.submit");
    /**
     * Fired after a submit by the user that no handler of {@link #SUBMIT} canceled. It carries no value; read the
     * box with {@link #withPassword}.
     */
    EventType<ControlEvent<PasswordTextBox>> SUBMITTED = EventType.report("password_text_box.submitted");

    /**
     * Returns a copy of the text. Each call returns a new array, which the caller owns. Zero it with
     * {@code Arrays.fill(chars, '\0')} when done.
     *
     * @return the text's chars, never null; empty when there is no text
     */
    char[] copyPassword();

    /**
     * Hands a reader a copy of the text, and zeroes the copy when the reader returns or throws. A reader that keeps
     * the array sees zeros later.
     *
     * @param <R> what the reader returns
     * @param reader reads the text's chars
     * @return what the reader returned
     */
    <R> R withPassword(Function<char[], ? extends R> reader);

    /**
     * Returns whether two boxes hold the same text, compared in constant time for their lengths and without copying
     * either text.
     *
     * @param other the other box
     * @return true if both hold the same chars
     */
    boolean matches(PasswordTextBox other);

    /**
     * Replaces the text with a copy of {@code password}, its line breaks removed and cut to {@link #maxLength()}. The
     * caller's array is not kept; zero it if it should not linger. The caret goes to the end, the text hides if it
     * showed, and {@link TextEntry#MODIFIED} fires if the text changed. {@link TextEntry#MODIFY} does not. The user
     * cannot reveal text set this way until they clear the box or replace all of it.
     *
     * @param password the new text
     * @return this box
     */
    PasswordTextBox setPassword(char[] password);

    /**
     * Returns the mask.
     *
     * @return the code point drawn for each code point of the text; {@link #DEFAULT_MASK} by default
     */
    int maskCodePoint();

    /**
     * Sets the code point drawn for each code point of the text.
     *
     * @param codePoint the mask
     * @return this box
     * @throws IllegalArgumentException if {@code codePoint} is not a valid code point
     */
    PasswordTextBox setMaskCodePoint(int codePoint);

    /**
     * Returns whether the user may show the text.
     *
     * @return true by default
     */
    boolean isRevealable();

    /**
     * Sets whether the user may show the text. False removes the eye, ignores Alt+F8 and hides the text if it shows.
     * The text keeps room for the eye while this is true.
     *
     * @param revealable false to keep the user from showing the text
     * @return this box
     */
    PasswordTextBox setRevealable(boolean revealable);

    /**
     * Returns whether the text shows instead of the mask.
     *
     * @return false by default
     */
    boolean isRevealed();

    /**
     * Shows or hides the text. {@link #REVEALED} fires if the state changed; {@link #REVEAL} does not. Works whether
     * or not the box is revealable, since that setting governs only the user.
     *
     * @param revealed true to show the text
     * @return this box
     */
    PasswordTextBox setRevealed(boolean revealed);

    /**
     * Returns the placeholder.
     *
     * @return the placeholder; empty for a new box
     */
    Text placeholder();

    /**
     * Sets the hint drawn in the {@value TextBox#STYLE_PLACEHOLDER} style while the box is empty, such as
     * {@code "Password"}. The mask does not cover it.
     *
     * @param placeholder the hint, or {@code null} or empty for none
     * @return this box
     */
    PasswordTextBox setPlaceholder(Text placeholder);

    /**
     * Does the same as {@link #setPlaceholder(Text)} with literal text.
     *
     * @param placeholder the hint, or {@code null} or empty for none
     * @return this box
     */
    default PasswordTextBox setPlaceholder(final String placeholder) {
        return this.setPlaceholder(placeholder == null ? null : Text.of(placeholder));
    }

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
    PasswordTextBox setSelectAllOnFocus(boolean selectAll);

    /**
     * Sets which code points the user may type or paste. Typing a code point the filter refuses does nothing, and a
     * paste with any such code point is refused whole. The filter sees one code point at a time, on the UI thread,
     * and is the box's own: it does not bubble. {@link #setPassword} is not filtered.
     *
     * @param filter returns true for each code point to accept, or {@code null} to accept any; any by default
     * @return this box
     */
    PasswordTextBox setCodePointFilter(IntPredicate filter);

    /**
     * A password box's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. It never holds
     * the text. Indexes are char indexes into the text, as on the box.
     *
     * @param length the length in code points
     * @param caret the {@linkplain #caret() caret} index
     * @param anchor the end of the selection that stays put while the caret moves; equal to {@code caret} when
     *               nothing is selected
     * @param maxLength the {@linkplain #maxLength() maximum length}, in code points
     * @param revealed whether the text showed
     */
    record State(int length, int caret, int anchor, int maxLength, boolean revealed) {

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
