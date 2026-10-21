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
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * The caret, the selection, the blink, the sideways scroll, and the key and pointer handling of a single-line field,
 * over an {@link EditBuffer}. A {@link TextBoxImpl} and a {@link PasswordTextBoxImpl} each own one and tell it, through
 * an {@link Owner}, how to measure and draw their text, whether copying is allowed, and how to apply the user's edits.
 *
 * <p>It never changes the text itself: every edit goes to the owner, which fires its requests and reports and then
 * places the caret with {@link #place}. Word movement and double clicks use the {@link TextBoundaries} it is given,
 * so a password box can make its whole text one word.</p>
 */
final class LineEditor {
    /**
     * How long, in milliseconds, the caret stays shown and then hidden. Each edit or caret move restarts the cycle
     * with the caret shown.
     */
    static final long BLINK_MILLIS = 500L;

    private final EditBuffer buffer;
    private final TextBoundaries boundaries;
    private final Owner owner;
    private final Runnable drawContent;

    private int caret;
    private int anchor;
    private float scrollX;
    private long blinkStart;

    // Double-click drags select whole words around the word first clicked
    private boolean wordDrag;
    private int dragWordStart;
    private int dragWordEnd;
    // Set when a press gives focus to a field that selects all on focus, so that press keeps everything selected
    private boolean pressSelectsAll;
    // Set while a press keeps everything selected; a drag from it selects from where the press landed instead
    private boolean allFromPress;
    private int pressIndex;

    // Derived from the text and the caret, worked out again only after a change
    private boolean visualsDirty;
    private float textWidth;
    private float caretX;
    private float selectionStartX;
    private float selectionEndX;

    // Set only while paint runs, for drawContent
    private Painter painter;
    private Rect contentBox;
    private float textY;
    private float lineHeight;
    private Paint selectionPaint;

    LineEditor(final EditBuffer buffer, final TextBoundaries boundaries, final Owner owner) {
        this.buffer = buffer;
        this.boundaries = boundaries;
        this.owner = owner;
        this.drawContent = this::drawContent;
        this.contentBox = Rect.ZERO;
        this.selectionPaint = Paint.NONE;
        this.visualsDirty = true;
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
     * Selects a range for code, clamped to the text and snapped to code points.
     */
    void select(final int anchor, final int caret) {
        this.anchor = this.boundaries.snap(anchor);
        this.caret = this.boundaries.snap(caret);
        this.moved();
    }

    void selectAll() {
        this.anchor = 0;
        this.caret = this.buffer.length();
        this.moved();
    }

    /**
     * Puts the caret at an index with nothing selected, after the owner changed the text.
     */
    void place(final int index) {
        this.caret = index;
        this.anchor = index;
    }

    /**
     * Sets the caret and anchor as they are, for an owner putting back what it saved.
     */
    void restore(final int anchor, final int caret) {
        this.anchor = anchor;
        this.caret = caret;
    }

    /**
     * Keeps the caret and anchor inside the text, after the owner cut it.
     */
    void clamp() {
        this.caret = Math.min(this.caret, this.buffer.length());
        this.anchor = Math.min(this.anchor, this.buffer.length());
    }

    /**
     * Marks the drawing out of date and shows the caret again from the start of its blink.
     */
    void moved() {
        this.visualsDirty = true;
        this.blinkStart = this.owner.environment().millis();
    }

    /**
     * Marks the drawing out of date, after the owner's style or text changed.
     */
    void invalidate() {
        this.visualsDirty = true;
    }

    /**
     * Returns whether the caret is in the shown half of its blink.
     */
    boolean caretShown() {
        return ((this.owner.environment().millis() - this.blinkStart) / BLINK_MILLIS) % 2 == 0;
    }

    private void moveTo(final int index, final boolean extend) {
        this.owner.beforeMove();
        this.caret = this.boundaries.snap(index);
        if (!extend) {
            this.anchor = this.caret;
        }
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
     * Handles a left press; the owner has checked the button and that it is enabled.
     *
     * @param textX the press relative to where the text starts when not scrolled
     */
    void press(final PointerButtonEvent event, final float textX) {
        final int index = this.indexAt(textX);
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
                this.anchor = 0;
                this.caret = this.buffer.length();
            }
        }
        this.moved();
        event.consume();
    }

    /**
     * Handles a left drag that did not start with a triple click; the owner has checked the button and that it is
     * enabled.
     *
     * @param textX the pointer relative to where the text starts when not scrolled
     */
    void drag(final DragEvent event, final float textX) {
        final int index = this.indexAt(textX);
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
        this.moved();
        event.consume();
    }

    private int indexAt(final float textX) {
        final TextMetrics metrics = this.owner.environment().text();
        this.updateVisuals(metrics);
        final float x = textX + this.scrollX;
        return x <= 0f ? 0 : this.owner.indexForX(metrics, x);
    }

    // ------------------------------------------------------------------
    // Keyboard
    // ------------------------------------------------------------------

    /**
     * Handles the editing and movement keys. The owner has handled its own keys, such as Enter, first.
     *
     * @return true if the key did something
     */
    boolean key(final Key key, final Modifiers modifiers) {
        final boolean shift = modifiers.shift();
        if (modifiers.control() && !modifiers.alt()) {
            return this.controlKey(key, shift);
        }
        return this.plainKey(key, shift);
    }

    private boolean controlKey(final Key key, final boolean shift) {
        switch (key) {
            case A -> this.owner.selectAll();
            case C -> this.copy();
            case X -> this.cut();
            case V -> this.owner.paste(this.selectionStart(), this.selectionEnd());
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
                    this.moveTo(this.boundaries.previousCodePoint(this.caret), shift);
                }
            }
            case RIGHT -> {
                if (this.hasSelection() && !shift) {
                    this.moveTo(this.selectionEnd(), false);
                } else {
                    this.moveTo(this.boundaries.nextCodePoint(this.caret), shift);
                }
            }
            case HOME -> this.moveTo(0, shift);
            case END -> this.moveTo(this.buffer.length(), shift);
            case BACKSPACE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.delete(this.boundaries.previousCodePoint(this.caret), this.caret);
                }
            }
            case DELETE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else {
                    this.delete(this.caret, this.boundaries.nextCodePoint(this.caret));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Types a code point over the selection; control characters are ignored.
     *
     * @return false for a control character, which the field leaves alone
     */
    boolean type(final int codePoint) {
        if (Character.isISOControl(codePoint)) {
            return false;
        }
        this.owner.type(this.selectionStart(), this.selectionEnd(), codePoint);
        return true;
    }

    private void copy() {
        if (this.hasSelection() && this.owner.allowsCopy()) {
            this.owner.copy(this.selectionStart(), this.selectionEnd());
        }
    }

    private void cut() {
        if (this.hasSelection() && this.owner.allowsCopy() && !this.owner.isReadOnly()) {
            this.copy();
            this.deleteSelection();
        }
    }

    private void deleteSelection() {
        this.owner.delete(this.selectionStart(), this.selectionEnd());
    }

    private void delete(final int start, final int end) {
        if (start < end) {
            this.owner.delete(start, end);
        }
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * Draws the selection and the text inside a clip of the content box, then the caret. Scrolls first, just enough
     * to keep the caret in view.
     *
     * @param showCaret whether the caret may show at all: focused, enabled and editable
     */
    void paint(final Painter painter, final Rect contentBox, final float textY, final float lineHeight,
               final Paint selection, final Paint caretPaint, final boolean showCaret) {
        this.contentBox = contentBox;
        this.textY = textY;
        this.lineHeight = lineHeight;
        this.selectionPaint = selection;
        this.updateVisuals(this.owner.environment().text());
        this.revealCaret();

        this.painter = painter;
        try {
            painter.clipped(contentBox, this.drawContent);
        } finally {
            this.painter = null;
        }

        if (showCaret && this.caretShown()) {
            final float x = Math.max(contentBox.x(),
                    Math.min(contentBox.right() - 1f, contentBox.x() + this.caretX - this.scrollX));
            caretPaint.paint(painter, new Rect(x, textY, 1f, lineHeight));
        }
    }

    /**
     * Runs inside the content clip during {@link #paint}; a field-held method reference, so painting allocates no
     * lambda per frame.
     */
    private void drawContent() {
        final float originX = this.contentBox.x() - this.scrollX;
        if (this.hasSelection()) {
            this.selectionPaint.paint(this.painter, new Rect(originX + this.selectionStartX, this.textY,
                    this.selectionEndX - this.selectionStartX, this.lineHeight));
        }
        this.owner.drawText(this.painter, originX, this.contentBox.x(), this.textY);
    }

    /**
     * Scrolls just enough to keep the caret inside the content box, and never past the text's end.
     */
    private void revealCaret() {
        final float view = this.contentBox.width();
        if (this.caretX - this.scrollX > view - 1f) {
            this.scrollX = this.caretX - view + 1f;
        }
        if (this.caretX - this.scrollX < 0f) {
            this.scrollX = this.caretX;
        }
        this.scrollX = Math.max(0f, Math.min(this.scrollX, Math.max(0f, this.textWidth + 1f - view)));
    }

    private void updateVisuals(final TextMetrics metrics) {
        if (!this.visualsDirty) {
            return;
        }
        this.visualsDirty = false;
        this.textWidth = this.owner.layoutText(metrics);
        this.caretX = this.owner.xForIndex(metrics, this.caret);
        this.selectionStartX = this.owner.xForIndex(metrics, this.selectionStart());
        this.selectionEndX = this.owner.xForIndex(metrics, this.selectionEnd());
    }

    /**
     * What a field tells its editor. Every method runs on the UI thread.
     */
    interface Owner {

        /**
         * The field's environment, for the clock and text measurement.
         */
        Environment environment();

        /**
         * Whether the field is read-only, which stops cutting.
         */
        boolean isReadOnly();

        /**
         * Whether Ctrl+C and Ctrl+X may put the selection on the clipboard.
         */
        boolean allowsCopy();

        /**
         * Brings the drawn text up to date with the buffer and the style.
         *
         * @return the text's width
         */
        float layoutText(TextMetrics metrics);

        /**
         * The x of the boundary at a char index, from the start of the text. Called after {@link #layoutText}.
         */
        float xForIndex(TextMetrics metrics, int index);

        /**
         * The code point boundary nearest to {@code x}, measured from the start of the text and always above 0.
         * Called after {@link #layoutText}.
         */
        int indexForX(TextMetrics metrics, float x);

        /**
         * Draws the text with its start at {@code originX}, or the placeholder at {@code left} when there is no text.
         */
        void drawText(Painter painter, float originX, float left, float y);

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
    }
}
