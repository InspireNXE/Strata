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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The undo and redo stacks of a field whose text code may read. Each entry is the difference between the text before
 * and after a change: where it starts, the chars it removed and the chars it inserted, with the caret and anchor on
 * both sides, so undoing puts the removed chars back and the selection where it was.
 *
 * <p>An entry is worked out from the whole text before and after, by their common start and end, so an edit a
 * handler rewrote or a filter changed is recorded as what really happened. Typing merges into the entry before it
 * while that entry is open: the new text follows straight on from it, the caret has not moved since, and less than
 * {@link #MERGE_MILLIS} passed. Typing whitespace closes the entry after it, so the next word is an entry of its own.
 * Backspaces merge the same way into a run that grows to the left, and Deletes into one that stays where it is. Every
 * other change is an entry of its own and closes the one before.</p>
 *
 * <p>Recording a change clears the redo stack. The undo stack keeps at most {@link #limit()} entries and drops the
 * oldest past that; a limit of 0 keeps none.</p>
 */
final class EditHistory {
    /**
     * How many entries a field keeps by default.
     */
    static final int DEFAULT_LIMIT = 100;
    /**
     * The longest pause, in milliseconds, between two keys that still merge.
     */
    static final long MERGE_MILLIS = 1000L;

    private final Deque<Entry> undo;
    private final Deque<Entry> redo;
    private int limit;

    EditHistory() {
        this.undo = new ArrayDeque<>();
        this.redo = new ArrayDeque<>();
        this.limit = DEFAULT_LIMIT;
    }

    int limit() {
        return this.limit;
    }

    /**
     * Sets how many entries the undo stack keeps, dropping the oldest past it, and the whole redo stack if the limit
     * is 0.
     */
    void setLimit(final int limit) {
        this.limit = limit;
        while (this.undo.size() > limit) {
            this.undo.removeFirst();
        }
        if (limit == 0) {
            this.redo.clear();
        }
    }

    boolean canUndo() {
        return !this.undo.isEmpty();
    }

    boolean canRedo() {
        return !this.redo.isEmpty();
    }

    void clear() {
        this.undo.clear();
        this.redo.clear();
    }

    /**
     * Stops the last entry from taking more typing.
     */
    void close() {
        final Entry last = this.undo.peekLast();
        if (last != null) {
            last.open = false;
        }
    }

    /**
     * Records a change of the text.
     *
     * @param before the text before
     * @param after the text after; differs from {@code before}
     * @param now the platform's clock, in milliseconds
     */
    void record(final String before, final String after, final int anchorBefore, final int caretBefore,
                final int anchorAfter, final int caretAfter, final Kind kind, final long now) {
        this.redo.clear();
        if (this.limit == 0) {
            return;
        }
        final int shorter = Math.min(before.length(), after.length());
        int prefix = 0;
        while (prefix < shorter && before.charAt(prefix) == after.charAt(prefix)) {
            prefix++;
        }
        if (kind != Kind.OTHER) {
            // A key changes nothing before the selection, so "a" typed before another "a" is placed where it was typed
            prefix = Math.min(prefix, Math.min(anchorBefore, caretBefore));
        }
        int suffix = 0;
        while (suffix < shorter - prefix
                && before.charAt(before.length() - 1 - suffix) == after.charAt(after.length() - 1 - suffix)) {
            suffix++;
        }
        final String removed = before.substring(prefix, before.length() - suffix);
        final String inserted = after.substring(prefix, after.length() - suffix);
        final Entry last = this.undo.peekLast();
        if (last != null && this.merges(last, prefix, removed, inserted, anchorBefore, caretBefore, kind, now)) {
            switch (kind) {
                case TYPE -> last.inserted = last.inserted + inserted;
                case BACKSPACE -> {
                    last.start = prefix;
                    last.removed = removed + last.removed;
                }
                default -> last.removed = last.removed + removed;
            }
            last.anchorAfter = anchorAfter;
            last.caretAfter = caretAfter;
            last.time = now;
            last.open = !(kind == Kind.TYPE && endsWithWhitespace(inserted));
            return;
        }
        if (last != null) {
            last.open = false;
        }
        final Entry entry = new Entry(prefix, removed, inserted, anchorBefore, caretBefore, anchorAfter, caretAfter,
                kind, now);
        entry.open = kind != Kind.OTHER && !(kind == Kind.TYPE && endsWithWhitespace(inserted));
        this.undo.addLast(entry);
        while (this.undo.size() > this.limit) {
            this.undo.removeFirst();
        }
    }

    private boolean merges(final Entry last, final int start, final String removed, final String inserted,
                           final int anchorBefore, final int caretBefore, final Kind kind, final long now) {
        if (!last.open || last.kind != kind || now - last.time >= MERGE_MILLIS || anchorBefore != last.anchorAfter
                || caretBefore != last.caretAfter) {
            return false;
        }
        return switch (kind) {
            case TYPE -> removed.isEmpty() && start == last.start + last.inserted.length();
            case BACKSPACE -> inserted.isEmpty() && last.inserted.isEmpty() && start + removed.length() == last.start;
            case DELETE -> inserted.isEmpty() && last.inserted.isEmpty() && start == last.start;
            case OTHER -> false;
        };
    }

    private static boolean endsWithWhitespace(final String text) {
        return !text.isEmpty() && Character.isWhitespace(text.codePointBefore(text.length()));
    }

    /**
     * Returns the entry an undo would take back, without taking it.
     *
     * @return the entry, or null if there is none
     */
    Entry peekUndo() {
        return this.undo.peekLast();
    }

    /**
     * Returns the entry a redo would make again, without taking it.
     *
     * @return the entry, or null if there is none
     */
    Entry peekRedo() {
        return this.redo.peekLast();
    }

    /**
     * Moves the last entry from the undo stack to the redo stack, once its undo applied.
     */
    void undone() {
        final Entry entry = this.undo.removeLast();
        entry.open = false;
        this.redo.addLast(entry);
    }

    /**
     * Moves the last entry from the redo stack back to the undo stack, once its redo applied.
     */
    void redone() {
        this.undo.addLast(this.redo.removeLast());
        while (this.undo.size() > this.limit) {
            this.undo.removeFirst();
        }
    }

    /**
     * What made a change, which decides whether it merges with the change before it.
     */
    enum Kind {
        /**
         * The user typed, which merges with typing just before.
         */
        TYPE,
        /**
         * Backspace deleted before the caret, which merges with a run of them.
         */
        BACKSPACE,
        /**
         * Delete deleted after the caret, which merges with a run of them.
         */
        DELETE,
        /**
         * Anything else: a paste, a cut, a deleted selection, a completed suggestion, or an edit from code.
         */
        OTHER
    }

    /**
     * One change: the chars at {@code start} that were {@code removed} became {@code inserted}.
     */
    static final class Entry {
        int start;
        String removed;
        String inserted;
        final int anchorBefore;
        final int caretBefore;
        int anchorAfter;
        int caretAfter;
        final Kind kind;
        long time;
        boolean open;

        Entry(final int start, final String removed, final String inserted, final int anchorBefore,
              final int caretBefore, final int anchorAfter, final int caretAfter, final Kind kind, final long time) {
            this.start = start;
            this.removed = removed;
            this.inserted = inserted;
            this.anchorBefore = anchorBefore;
            this.caretBefore = caretBefore;
            this.anchorAfter = anchorAfter;
            this.caretAfter = caretAfter;
            this.kind = kind;
            this.time = time;
        }

        /**
         * Returns the text before this change, given the text after it.
         */
        String undoneFrom(final String after) {
            return after.substring(0, this.start) + this.removed + after.substring(this.start + this.inserted.length());
        }

        /**
         * Returns the text after this change, given the text before it.
         */
        String redoneFrom(final String before) {
            return before.substring(0, this.start) + this.inserted + before.substring(this.start + this.removed.length());
        }

        /**
         * Returns whether this change's inserted chars are where it left them in a text, so undoing it is sound.
         */
        boolean undoesIn(final String after) {
            return this.start + this.inserted.length() <= after.length()
                    && after.startsWith(this.inserted, this.start);
        }

        /**
         * Returns whether this change's removed chars are where it found them in a text, so redoing it is sound.
         */
        boolean redoesIn(final String before) {
            return this.start + this.removed.length() <= before.length()
                    && before.startsWith(this.removed, this.start);
        }
    }
}
