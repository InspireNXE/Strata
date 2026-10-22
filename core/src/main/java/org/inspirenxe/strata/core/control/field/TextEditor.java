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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The caret, the selection, the blink, and the key and pointer handling every text field shares, over an
 * {@link EditBuffer}. A {@link LineEditor} adds a single line's scrolling and drawing for the text box and the
 * password box; the {@link TextAreaImpl} lays out its own rows and plugs its row movement in as {@link Rows}.
 *
 * <p>It never changes the text itself: every edit goes to the {@link Host}, which fires its requests and reports and
 * then places the caret with {@link #place}. Character and word movement and double clicks use the
 * {@link TextBoundaries} it is given. Keys held with Alt are never editing keys, so they reach the engine.</p>
 *
 * <p>It remembers where it last reported the caret and the anchor, and {@link #settle} tells the host when either
 * moved since, so each field reports {@code CARET_MOVED} once at the end of an input or a call.</p>
 */
final class TextEditor {
    private final EditBuffer buffer;
    private final TextBoundaries boundaries;
    private final Host host;
    private final Rows rows;
    private final Runnable onMoved;
    private int caret;
    private int anchor;
    // Where CARET_MOVED last said they were
    private int reportedCaret;
    private int reportedAnchor;
    private long blinkStart;
    // The x that Up and Down keep while they move the caret from row to row, or NaN
    private float goal;

    // Double-click drags select whole words around the word first clicked
    private boolean wordDrag;
    private int dragWordStart;
    private int dragWordEnd;
    // Set when a press gives focus to a field that selects all on focus, so that press keeps everything selected
    private boolean pressSelectsAll;
    // Set while a press keeps everything selected; a drag from it selects from where the press landed instead
    private boolean allFromPress;
    private int pressIndex;

    /**
     * Creates an editor with the caret at the start.
     *
     * @param rows the rows the text is laid out in, or null for a single line
     * @param onMoved runs after the caret, the selection or the text changed, so the field draws them again
     */
    TextEditor(final EditBuffer buffer, final TextBoundaries boundaries, final Host host, final Rows rows,
               final Runnable onMoved) {
        this.buffer = buffer;
        this.boundaries = boundaries;
        this.host = host;
        this.rows = rows;
        this.onMoved = onMoved;
        this.goal = Float.NaN;
    }

    // ------------------------------------------------------------------
    // Caret and selection
    // ------------------------------------------------------------------

    int caret() {
        return this.caret;
    }

    int anchor() {
        return this.anchor;
    }

    int selectionStart() {
        return Math.min(this.anchor, this.caret);
    }

    int selectionEnd() {
        return Math.max(this.anchor, this.caret);
    }

    boolean hasSelection() {
        return this.anchor != this.caret;
    }

    /**
     * Selects a range for code, clamped to the text and snapped to code points. Up and Down then start from the new
     * caret.
     */
    void select(final int anchor, final int caret) {
        this.anchor = this.boundaries.snap(anchor);
        this.caret = this.boundaries.snap(caret);
        this.goal = Float.NaN;
        this.moved();
    }

    /**
     * Selects everything with the caret at the end. Up and Down then start from there.
     */
    void selectAll() {
        this.anchor = 0;
        this.caret = this.buffer.length();
        this.goal = Float.NaN;
        this.moved();
    }

    /**
     * Puts the caret at an index with nothing selected, after the host changed the text. Up and Down then start from
     * there.
     */
    void place(final int index) {
        this.caret = index;
        this.anchor = index;
        this.goal = Float.NaN;
    }

    /**
     * Sets the caret and anchor as they are, for a host putting back what it saved.
     */
    void restore(final int anchor, final int caret) {
        this.anchor = anchor;
        this.caret = caret;
    }

    /**
     * Keeps the caret and anchor inside the text, after the host cut it.
     */
    void clamp() {
        this.caret = Math.min(this.caret, this.buffer.length());
        this.anchor = Math.min(this.anchor, this.buffer.length());
    }

    /**
     * Tells the host the caret or the anchor moved, if either differs from what it last told. Each field calls this at
     * the end of every input handler and every public method that can move them, so an input or a call reports once,
     * after the edit's own reports.
     */
    void settle() {
        if (this.caret == this.reportedCaret && this.anchor == this.reportedAnchor) {
            return;
        }
        final int previousAnchor = this.reportedAnchor;
        final int previousCaret = this.reportedCaret;
        this.reportedAnchor = this.anchor;
        this.reportedCaret = this.caret;
        this.host.caretMoved(previousAnchor, previousCaret, this.anchor, this.caret);
    }

    /**
     * Takes the caret and anchor as reported without telling the host, for a field that wipes itself silently.
     */
    void settleQuietly() {
        this.reportedAnchor = this.anchor;
        this.reportedCaret = this.caret;
    }

    /**
     * Marks the drawing out of date after the caret or the text changed, and shows the caret again from the start
     * of its blink.
     */
    void moved() {
        this.blinkStart = this.host.environment().millis();
        this.onMoved.run();
    }

    /**
     * Forgets the x that Up and Down keep, so the next of them starts from where the caret is.
     */
    void forgetGoal() {
        this.goal = Float.NaN;
    }

    /**
     * Returns whether the caret is in the shown half of its blink.
     *
     * @param blinkMillis how long the caret stays shown and then hidden
     */
    boolean caretShown(final long blinkMillis) {
        return ((this.host.environment().millis() - this.blinkStart) / blinkMillis) % 2 == 0;
    }

    /**
     * Returns how long until the caret next turns on or off, for the field to ask for a frame then.
     *
     * @param blinkMillis how long the caret stays shown and then hidden, above 0
     * @return the delay in milliseconds, at least 1
     */
    long untilBlink(final long blinkMillis) {
        return blinkMillis - Math.floorMod(this.host.environment().millis() - this.blinkStart, blinkMillis);
    }

    private void moveTo(final int index, final boolean extend) {
        this.host.beforeMove();
        this.caret = this.boundaries.snap(index);
        if (!extend) {
            this.anchor = this.caret;
        }
        this.goal = Float.NaN;
        this.moved();
    }

    // ------------------------------------------------------------------
    // Focus and pointer
    // ------------------------------------------------------------------

    /**
     * Places the caret for focus gained: at the end, with everything selected if the field selects all on focus. A
     * press that gave focus places the caret right after this, unless the field selects all on focus.
     */
    void focusGained(final boolean selectAll, final boolean fromPress) {
        this.pressSelectsAll = selectAll && fromPress;
        this.anchor = selectAll ? 0 : this.buffer.length();
        this.caret = this.buffer.length();
        this.moved();
    }

    void focusLost() {
        this.pressSelectsAll = false;
        this.allFromPress = false;
    }

    /**
     * Forgets that the press that gave focus should keep everything selected, for a press that landed elsewhere.
     */
    void cancelPress() {
        this.pressSelectsAll = false;
    }

    /**
     * Handles a left press on the text; the host has checked the button and that it is enabled. One click places the
     * caret, or with Shift extends the selection; two select the word around the index; three select the line, which
     * is the whole text of a single-line field.
     *
     * @param index the boundary under the press
     */
    void press(final PointerButtonEvent event, final int index) {
        this.wordDrag = false;
        this.allFromPress = this.pressSelectsAll && event.clicks() == 1 && !event.modifiers().shift();
        this.pressSelectsAll = false;
        this.pressIndex = index;
        switch (event.clicks()) {
            case 1 -> {
                if (this.allFromPress) {
                    this.anchor = 0;
                    this.caret = this.buffer.length();
                } else {
                    this.caret = index;
                    if (!event.modifiers().shift()) {
                        this.anchor = index;
                    }
                }
            }
            case 2 -> {
                final int[] word = this.boundaries.wordAround(index);
                this.anchor = word[0];
                this.caret = word[1];
                this.wordDrag = true;
                this.dragWordStart = word[0];
                this.dragWordEnd = word[1];
            }
            default -> {
                this.anchor = this.rows == null ? 0 : this.rows.lineStart(index);
                this.caret = this.rows == null ? this.buffer.length() : this.rows.lineEnd(index);
            }
        }
        this.goal = Float.NaN;
        this.moved();
        event.consume();
    }

    /**
     * Handles a left drag over the text that did not start with a triple click; the host has checked the button and
     * that it is enabled.
     *
     * @param index the boundary under the pointer
     */
    void drag(final DragEvent event, final int index) {
        if (this.wordDrag) {
            final int[] word = this.boundaries.wordAround(index);
            if (word[0] < this.dragWordStart) {
                this.anchor = this.dragWordEnd;
                this.caret = word[0];
            } else {
                this.anchor = this.dragWordStart;
                this.caret = Math.max(word[1], this.dragWordEnd);
            }
        } else {
            if (this.allFromPress) {
                this.allFromPress = false;
                this.anchor = this.pressIndex;
            }
            this.caret = index;
        }
        this.goal = Float.NaN;
        this.moved();
        event.consume();
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    /**
     * Returns whether a field consumes a key it did not use because the key types a character, which arrives as a
     * character too. That keeps the engine from also reading it as a shortcut.
     */
    static boolean typesCharacter(final Key key, final Modifiers modifiers) {
        return !modifiers.control() && !modifiers.alt() && key.isPrintable();
    }

    /**
     * Handles the editing and movement keys. The host has handled its own keys, such as Enter, first.
     *
     * @return true if the key did something
     */
    boolean key(final Key key, final Modifiers modifiers) {
        if (modifiers.alt()) {
            return false;
        }
        final boolean shift = modifiers.shift();
        if (modifiers.control()) {
            return this.controlKey(key, shift);
        }
        return this.plainKey(key, shift);
    }

    private boolean controlKey(final Key key, final boolean shift) {
        switch (key) {
            case A -> this.host.selectAll();
            case C -> this.copy();
            case X -> this.cut();
            case V -> this.host.paste(this.selectionStart(), this.selectionEnd());
            case LEFT -> this.moveTo(this.boundaries.previousWordStart(this.caret), shift);
            case RIGHT -> this.moveTo(this.boundaries.nextWordEnd(this.caret), shift);
            case HOME -> this.moveTo(0, shift);
            case END -> this.moveTo(this.buffer.length(), shift);
            case BACKSPACE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.delete(this.boundaries.previousWordStart(this.caret), this.caret);
                }
            }
            case DELETE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.delete(this.caret, this.boundaries.nextWordEnd(this.caret));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean plainKey(final Key key, final boolean shift) {
        switch (key) {
            case LEFT -> {
                if (this.hasSelection() && !shift) {
                    this.moveTo(this.selectionStart(), false);
                } else {
                    this.moveTo(this.boundaries.previous(this.caret), shift);
                }
            }
            case RIGHT -> {
                if (this.hasSelection() && !shift) {
                    this.moveTo(this.selectionEnd(), false);
                } else {
                    this.moveTo(this.boundaries.next(this.caret), shift);
                }
            }
            case UP -> {
                return this.moveRows(-1, shift);
            }
            case DOWN -> {
                return this.moveRows(1, shift);
            }
            case PAGE_UP -> {
                return this.rows != null && this.moveRows(-this.rows.pageRows(), shift);
            }
            case PAGE_DOWN -> {
                return this.rows != null && this.moveRows(this.rows.pageRows(), shift);
            }
            case HOME -> this.moveTo(this.rows == null ? 0 : this.rows.rowStart(this.caret), shift);
            case END -> this.moveTo(this.rows == null ? this.buffer.length() : this.rows.rowEnd(this.caret), shift);
            case BACKSPACE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.delete(this.boundaries.previous(this.caret), this.caret);
                }
            }
            case DELETE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.delete(this.caret, this.boundaries.next(this.caret));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves the caret up or down by rows, keeping the x it had when vertical movement began.
     *
     * @return false for a single-line field, which has no rows to move through
     */
    private boolean moveRows(final int count, final boolean extend) {
        if (this.rows == null) {
            return false;
        }
        final float x = Float.isNaN(this.goal) ? this.rows.caretX() : this.goal;
        this.moveTo(this.rows.indexAt(count, x), extend);
        this.goal = x;
        return true;
    }

    /**
     * Types a code point over the selection; control characters and line and paragraph separators are ignored.
     *
     * @return false for a code point the field leaves alone
     */
    boolean type(final int codePoint) {
        if (!FieldText.isTypeable(codePoint)) {
            return false;
        }
        this.host.type(this.selectionStart(), this.selectionEnd(), codePoint);
        return true;
    }

    private void copy() {
        if (this.hasSelection() && this.host.allowsCopy()) {
            this.host.copy(this.selectionStart(), this.selectionEnd());
        }
    }

    private void cut() {
        if (this.hasSelection() && this.host.allowsCopy() && !this.host.isReadOnly()) {
            this.copy();
            this.deleteSelection();
        }
    }

    private void deleteSelection() {
        this.host.delete(this.selectionStart(), this.selectionEnd());
    }

    private void delete(final int start, final int end) {
        if (start < end) {
            this.host.delete(start, end);
        }
    }

    /**
     * What a field tells its editor about its state and how the user's edits apply. Every method runs on the UI
     * thread.
     */
    interface Host {

        /**
         * The field's environment, for the clock and the clipboard.
         */
        Environment environment();

        /**
         * Asks the field's host for a frame after a time, for the caret's next blink.
         */
        void repaintAfter(long millis);

        /**
         * Whether the field is read-only, which stops cutting.
         */
        boolean isReadOnly();

        /**
         * Whether Ctrl+C and Ctrl+X may put the selection on the clipboard.
         */
        boolean allowsCopy();

        /**
         * Selects everything, for Ctrl+A.
         */
        void selectAll();

        /**
         * Applies the user typing a code point over a range, which is empty when nothing is selected.
         */
        void type(int start, int end, int codePoint);

        /**
         * Applies the user deleting a non-empty range.
         */
        void delete(int start, int end);

        /**
         * Applies the user pasting over a range, which is empty when nothing is selected.
         */
        void paste(int start, int end);

        /**
         * Puts a range of the text on the clipboard. Called only when {@link #allowsCopy} is true.
         */
        void copy(int start, int end);

        /**
         * Called before a key moves the caret.
         */
        void beforeMove();

        /**
         * Reports that the caret or the anchor moved, from {@link #settle}.
         */
        void caretMoved(int previousAnchor, int previousCaret, int anchor, int caret);
    }

    /**
     * The rows of a field that lays its text out in more than one, for Home and End, Up and Down, Page Up and Page
     * Down, and triple clicks. The field keeps its rows up to date before it hands the editor a key or a press.
     */
    interface Rows {

        /**
         * Where Home goes from an index: the start of its row.
         */
        int rowStart(int index);

        /**
         * Where End goes from an index: the furthest the caret may go on its row.
         */
        int rowEnd(int index);

        /**
         * Where a triple click starts selecting: the start of the line around the index.
         */
        int lineStart(int index);

        /**
         * Where a triple click stops selecting: the end of the line around the index, before its line break.
         */
        int lineEnd(int index);

        /**
         * The x of the caret on its row, from the row's start.
         */
        float caretX();

        /**
         * The index nearest to an x on the row some rows away from the caret's: the start of the text above the first
         * row and its end below the last.
         *
         * @param count how many rows down, or up when negative
         * @param x the x to keep, from the row's start
         */
        int indexAt(int count, float x);

        /**
         * How many rows Page Up and Page Down move.
         */
        int pageRows();
    }
}
