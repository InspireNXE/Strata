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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * How the text of a field whose text code may read, a {@link TextBoxImpl} or a {@link TextAreaImpl}, changes: the
 * {@code MODIFY}, {@code EDIT}, {@code EDITED} and {@code MODIFIED} pipeline of a user's edit over a
 * {@link PlainBuffer}, with the caret kept in a {@link TextEditor}, the edits code makes, and the field's
 * {@link EditHistory}.
 *
 * <p>A user's edit replaces a range with text cut to the room the length limit leaves, and puts the caret after it.
 * With no {@code MODIFY} or {@code EDIT} handler on the path, or when the edit left the text as it was, it applies at
 * once. Otherwise the field holds the text and selection from before the edit again while it asks {@code MODIFY} and
 * then {@code EDIT} with the edited text, and the final text is cleaned, cut to the limit and placed with the caret as
 * many chars from its end as the edit left it. The field's own filter, if it has one, then sees the final text and may
 * reject or rewrite it, and a change is recorded in the history and reported through {@link Field#userChanged}.</p>
 *
 * <p>An undo or a redo by key is the user's too: it asks {@code MODIFY} and {@code EDIT} with the text it would
 * restore, and applies only if neither was canceled and the filter keeps that text as it is. A handler that rewrites
 * the text turns it into an ordinary edit of the rewritten text. An undo or redo from code, and every edit from code,
 * asks nothing and reports through {@link Field#codeChanged}.</p>
 *
 * <p>Each edit keeps what it needs in locals, so a handler may set the text or edit again while it runs. The final
 * text is compared with, and reported against, the text the field holds when the handlers are done, which is the
 * text from before the edit unless a handler set another.</p>
 */
final class PlainEdits {
    private final PlainBuffer buffer;
    private final TextBoundaries boundaries;
    private final TextEditor editor;
    private final Field field;
    private final EditHistory history;

    PlainEdits(final PlainBuffer buffer, final TextBoundaries boundaries, final TextEditor editor, final Field field) {
        this.buffer = buffer;
        this.boundaries = boundaries;
        this.editor = editor;
        this.field = field;
        this.history = new EditHistory();
    }

    /**
     * Returns the field's undo and redo stacks.
     */
    EditHistory history() {
        return this.history;
    }

    /**
     * Replaces a range with text as an edit by the user: the text is cut to the room the length limit leaves, and
     * the caret goes after it. Does nothing while the field is read-only. With a handler on the path, the field holds
     * its old text and selection again while it asks {@code MODIFY} and then {@code EDIT}, which applies the final
     * text through {@link #proceed} unless a handler cancels it.
     *
     * @param text the text, already cleaned for the field
     * @param kind what made the edit, which decides whether it merges with the one before in the history
     * @return true if the edit got past every request and filter, even when its final text equals the old one
     */
    boolean edit(final int start, final int end, final String text, final EditHistory.Kind kind) {
        if (this.field.isReadOnly()) {
            return false;
        }
        final String before = this.buffer.toString();
        final int beforeCaret = this.editor.caret();
        final int beforeAnchor = this.editor.anchor();
        this.buffer.delete(start, end);
        final String fitted = truncate(text, this.field.maxLength() - this.codePoints());
        if (!fitted.isEmpty()) {
            this.buffer.insert(start, fitted);
        }
        this.editor.place(start + fitted.length());
        this.editor.forgetGoal();
        if (!this.field.hearsEdits()) {
            return this.apply(before, beforeAnchor, beforeCaret, kind);
        }
        final String candidate = this.buffer.toString();
        if (candidate.equals(before)) {
            return this.apply(before, beforeAnchor, beforeCaret, kind);
        }
        final int fromEnd = candidate.length() - this.editor.caret();
        this.buffer.set(before);
        this.editor.restore(beforeAnchor, beforeCaret);
        this.editor.moved();
        if (!this.field.askModify()) {
            return false;
        }
        final boolean[] applied = new boolean[1];
        this.field.askEdit(candidate, value -> applied[0] = this.proceed(value, fromEnd, kind));
        return applied[0];
    }

    /**
     * Applies the final text of an {@code EDIT} request: cleans it, cuts it to the length limit, and puts it in the
     * field with the caret {@code fromEnd} chars before its end and nothing selected. For the edited text itself that
     * is where the edit left the caret.
     *
     * @return whether the edit applied, as {@link #edit} returns it
     */
    private boolean proceed(final String text, final int fromEnd, final EditHistory.Kind kind) {
        // What the handlers left, which a handler that set the text changed
        final String current = this.buffer.toString();
        final int currentCaret = this.editor.caret();
        final int currentAnchor = this.editor.anchor();
        final String fitted = truncate(this.field.clean(Objects.requireNonNull(text, "text")), this.field.maxLength());
        this.buffer.set(fitted);
        this.editor.place(this.boundaries.snap(this.buffer.length() - fromEnd));
        this.editor.forgetGoal();
        return this.apply(current, currentAnchor, currentCaret, kind);
    }

    /**
     * Applies the edit the buffer holds: runs the field's filter, then records and reports the change if the text
     * changed.
     *
     * @param previous the text before this change, which a rejected edit puts back
     * @param previousAnchor the anchor that goes back with it
     * @param previousCaret the caret that goes back with it
     * @return false if the filter rejected the edit
     */
    private boolean apply(final String previous, final int previousAnchor, final int previousCaret,
                          final EditHistory.Kind kind) {
        final UnaryOperator<String> filter = this.field.filter();
        if (filter != null) {
            final String candidate = this.buffer.toString();
            final String result = filter.apply(candidate);
            if (result == null || result.equals(previous)) {
                this.buffer.set(previous);
                this.editor.restore(previousAnchor, previousCaret);
                this.editor.moved();
                return false;
            }
            if (!result.equals(candidate)) {
                final int fromEnd = this.buffer.length() - this.editor.caret();
                this.buffer.set(truncate(this.field.clean(result), this.field.maxLength()));
                this.editor.place(this.boundaries.snap(this.buffer.length() - fromEnd));
            }
        }
        if (this.buffer.contentEquals(previous)) {
            this.editor.moved();
            return true;
        }
        this.history.record(previous, this.buffer.toString(), previousAnchor, previousCaret, this.editor.anchor(),
                this.editor.caret(), kind, this.field.millis());
        this.field.userChanged(previous);
        return true;
    }

    // ------------------------------------------------------------------
    // Edits from code
    // ------------------------------------------------------------------

    /**
     * Replaces a range with text for code, asking nothing: the range is clamped and snapped as {@code select} snaps,
     * the text is cleaned for the field and cut to the room the length limit leaves. The caret and the anchor stay
     * with the chars they were next to: after the range they shift with the change, inside it they go to the end of
     * the new text, and before it they stay. With {@code collapse} both go to the end of the new text instead. The
     * change is recorded in the history and reported through {@link Field#codeChanged}; a change that leaves the text
     * as it is reports nothing and moves nothing.
     *
     * @throws IllegalArgumentException if {@code start} is greater than {@code end}
     */
    void change(final int start, final int end, final String text, final boolean collapse) {
        Objects.requireNonNull(text, "text");
        if (start > end) {
            throw new IllegalArgumentException("Start " + start + " is after end " + end);
        }
        final int from = this.boundaries.snap(start);
        final int to = Math.max(from, this.boundaries.snap(end));
        final String previous = this.buffer.toString();
        final int room = this.field.maxLength() - (this.codePoints() - this.buffer.codePointCount(from, to));
        final String fitted = truncate(this.field.clean(text), room);
        if (previous.regionMatches(from, fitted, 0, fitted.length()) && to - from == fitted.length()) {
            return;
        }
        final int anchor = this.editor.anchor();
        final int caret = this.editor.caret();
        this.buffer.replace(from, to, fitted);
        final int newEnd = from + fitted.length();
        if (collapse) {
            this.editor.place(newEnd);
        } else {
            this.editor.restore(shift(anchor, from, to, newEnd), shift(caret, from, to, newEnd));
        }
        if (this.editor.caret() != caret || this.editor.anchor() != anchor) {
            this.editor.forgetGoal();
        }
        this.history.record(previous, this.buffer.toString(), anchor, caret, this.editor.anchor(),
                this.editor.caret(), EditHistory.Kind.OTHER, this.field.millis());
        this.field.codeChanged(previous, this.editor.caret() != caret);
    }

    /**
     * Where an index goes when the chars from {@code start} to {@code end} become chars that end at {@code newEnd}.
     */
    private static int shift(final int index, final int start, final int end, final int newEnd) {
        if (index >= end) {
            return index + newEnd - end;
        }
        return index > start ? newEnd : index;
    }

    // ------------------------------------------------------------------
    // Undo and redo
    // ------------------------------------------------------------------

    /**
     * Takes back the last change. From the user, on a field that is not read-only, it asks {@code MODIFY} and
     * {@code EDIT} with the text it would restore, as the class describes; from code it asks nothing.
     *
     * @param user whether the user asked by key
     * @return true if there was a change to take back and the field was not read-only for the user, even when a
     *     handler refused the undo
     */
    boolean undo(final boolean user) {
        final EditHistory.Entry entry = this.history.peekUndo();
        if (entry == null || user && this.field.isReadOnly()) {
            return false;
        }
        final String current = this.buffer.toString();
        if (!entry.undoesIn(current)) {
            // The text moved under the history; nothing in it can apply any more
            this.history.clear();
            return false;
        }
        this.step(entry.undoneFrom(current), entry.anchorBefore, entry.caretBefore, user, true);
        return true;
    }

    /**
     * Makes the last undone change again, as {@link #undo} takes one back.
     *
     * @param user whether the user asked by key
     * @return true if there was a change to make again and the field was not read-only for the user
     */
    boolean redo(final boolean user) {
        final EditHistory.Entry entry = this.history.peekRedo();
        if (entry == null || user && this.field.isReadOnly()) {
            return false;
        }
        final String current = this.buffer.toString();
        if (!entry.redoesIn(current)) {
            this.history.clear();
            return false;
        }
        this.step(entry.redoneFrom(current), entry.anchorAfter, entry.caretAfter, user, false);
        return true;
    }

    private void step(final String target, final int anchor, final int caret, final boolean user,
                      final boolean undoing) {
        if (!user || !this.field.hearsEdits() && this.field.filter() == null) {
            this.restore(target, anchor, caret, undoing, user);
            return;
        }
        if (this.field.hearsEdits()) {
            if (!this.field.askModify()) {
                return;
            }
            this.field.askEdit(target, value -> {
                if (target.equals(value)) {
                    this.restore(target, anchor, caret, undoing, true);
                } else {
                    // A rewritten undo is an edit of its own, with the caret at the end of the text
                    this.proceed(value, 0, EditHistory.Kind.OTHER);
                }
            });
            return;
        }
        this.restore(target, anchor, caret, undoing, true);
    }

    /**
     * Puts the text an undo or redo leads to in the field, if the filter keeps it as it is, moves the entry to the
     * other stack, and reports.
     */
    private void restore(final String target, final int anchor, final int caret, final boolean undoing,
                         final boolean user) {
        final UnaryOperator<String> filter = this.field.filter();
        if (user && filter != null && !target.equals(filter.apply(target))) {
            return;
        }
        final String previous = this.buffer.toString();
        final int previousCaret = this.editor.caret();
        this.buffer.set(target);
        this.editor.restore(this.boundaries.snap(anchor), this.boundaries.snap(caret));
        this.editor.forgetGoal();
        if (undoing) {
            this.history.undone();
        } else {
            this.history.redone();
        }
        if (user) {
            this.field.userUndid(previous);
        } else {
            this.field.codeChanged(previous, this.editor.caret() != previousCaret);
        }
    }

    private int codePoints() {
        return this.buffer.codePointCount(0, this.buffer.length());
    }

    /**
     * Cuts text to a number of code points.
     *
     * @return the text, or its start; empty for a count of 0 or less
     */
    static String truncate(final String text, final int codePoints) {
        if (codePoints <= 0) {
            return "";
        }
        if (text.codePointCount(0, text.length()) <= codePoints) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, codePoints));
    }

    /**
     * What a field tells its edits: its rules for the text and its way to the requests and reports, which are
     * protected on the component. Every method runs on the UI thread.
     */
    interface Field {

        /**
         * Whether the field is read-only, which stops every edit by the user.
         */
        boolean isReadOnly();

        /**
         * The most code points the field holds.
         */
        int maxLength();

        /**
         * Returns text as the field holds it, such as with its line breaks and control characters removed.
         */
        String clean(String text);

        /**
         * The field's private filter, which gets the final text of each edit and returns the text to keep, or null
         * to reject the edit; null for none.
         */
        UnaryOperator<String> filter();

        /**
         * Whether a handler for {@code MODIFY} or {@code EDIT} is on the field's path.
         */
        boolean hearsEdits();

        /**
         * Asks {@code MODIFY}.
         *
         * @return false if a handler canceled it
         */
        boolean askModify();

        /**
         * Asks {@code EDIT} with the text the edit would produce, and runs {@code apply} with the final text unless a
         * handler canceled it.
         */
        void askEdit(String candidate, Consumer<String> apply);

        /**
         * The platform's clock in milliseconds, for merging typing in the history.
         */
        long millis();

        /**
         * Reports that a user's edit changed the text, which the field now holds.
         *
         * @param previous the text before the edit
         */
        void userChanged(String previous);

        /**
         * Reports that the user's undo or redo changed the text, which the field now holds.
         *
         * @param previous the text before it
         */
        void userUndid(String previous);

        /**
         * Reports that code changed the text, which the field now holds.
         *
         * @param previous the text before the change
         * @param caretMoved whether the caret's index changed, so a field may scroll to it
         */
        void codeChanged(String previous, boolean caretMoved);
    }
}
