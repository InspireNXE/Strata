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
 * How a user's edit of a field whose text code may read, a {@link TextBoxImpl} or a {@link TextAreaImpl}, becomes its
 * new text: the {@code MODIFY}, {@code EDIT}, {@code EDITED} and {@code MODIFIED} pipeline over a
 * {@link PlainBuffer}, with the caret kept in a {@link TextEditor}.
 *
 * <p>An edit replaces a range with text cut to the room the length limit leaves, and puts the caret after it. With no
 * {@code MODIFY} or {@code EDIT} handler on the path, or when the edit left the text as it was, it applies at once.
 * Otherwise the field holds the text and selection from before the edit again while it asks {@code MODIFY} and then
 * {@code EDIT} with the edited text, and the final text is cleaned, cut to the limit and placed with the caret as many
 * chars from its end as the edit left it. Then the field's own filter, if it has one, gets the last word, and a
 * change is reported through {@link Field#userChanged}.</p>
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

    PlainEdits(final PlainBuffer buffer, final TextBoundaries boundaries, final TextEditor editor, final Field field) {
        this.buffer = buffer;
        this.boundaries = boundaries;
        this.editor = editor;
        this.field = field;
    }

    /**
     * Replaces a range with text as an edit by the user: the text is cut to the room the length limit leaves, and
     * the caret goes after it. Does nothing while the field is read-only. With a handler on the path, the field holds
     * its old text and selection again while it asks {@code MODIFY} and then {@code EDIT}, which applies the final
     * text through {@link #proceed} unless a handler cancels it.
     *
     * @param text the text, already cleaned for the field
     * @return true if the edit got past every request and filter, even when its final text equals the old one
     */
    boolean edit(final int start, final int end, final String text) {
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
            return this.apply(before, beforeAnchor, beforeCaret);
        }
        final String candidate = this.buffer.toString();
        if (candidate.equals(before)) {
            return this.apply(before, beforeAnchor, beforeCaret);
        }
        final int fromEnd = candidate.length() - this.editor.caret();
        this.buffer.set(before);
        this.editor.restore(beforeAnchor, beforeCaret);
        this.editor.moved();
        if (!this.field.askModify()) {
            return false;
        }
        final boolean[] applied = new boolean[1];
        this.field.askEdit(candidate, value -> applied[0] = this.proceed(value, fromEnd));
        return applied[0];
    }

    /**
     * Applies the final text of an {@code EDIT} request: cleans it, cuts it to the length limit, and puts it in the
     * field with the caret {@code fromEnd} chars before its end and nothing selected. For the edited text itself that
     * is where the edit left the caret.
     *
     * @return whether the edit applied, as {@link #edit} returns it
     */
    private boolean proceed(final String text, final int fromEnd) {
        // What the handlers left, which a handler that set the text changed
        final String current = this.buffer.toString();
        final int currentCaret = this.editor.caret();
        final int currentAnchor = this.editor.anchor();
        final String fitted = truncate(this.field.clean(Objects.requireNonNull(text, "text")), this.field.maxLength());
        this.buffer.set(fitted);
        this.editor.place(this.boundaries.snap(this.buffer.length() - fromEnd));
        this.editor.forgetGoal();
        return this.apply(current, currentAnchor, currentCaret);
    }

    /**
     * Applies the edit the buffer holds: runs the field's filter, then reports the change if the text changed.
     *
     * @param previous the text before this change, which a rejected edit puts back
     * @param previousAnchor the anchor that goes back with it
     * @param previousCaret the caret that goes back with it
     * @return false if the filter rejected the edit
     */
    private boolean apply(final String previous, final int previousAnchor, final int previousCaret) {
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
        this.field.userChanged(previous);
        return true;
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
         * Whether the field is read-only, which stops every edit.
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
         * Reports that a user's edit changed the text, which the field now holds.
         *
         * @param previous the text before the edit
         */
        void userChanged(String previous);
    }
}
