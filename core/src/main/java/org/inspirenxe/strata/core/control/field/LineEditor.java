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
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * A single-line field's editing: a {@link TextEditor} for the caret, the selection, the blink, the keys and the
 * pointer over an {@link EditBuffer}, and the sideways scroll and drawing of one line. A {@link TextBoxImpl} and a
 * {@link PasswordTextBoxImpl} each own one and tell it, through an {@link Owner}, how to measure and draw their text,
 * whether copying is allowed, and how to apply the user's edits.
 *
 * <p>It never changes the text itself: every edit goes to the owner, which fires its requests and reports and then
 * places the caret with {@link #place}. Word movement and double clicks use the {@link TextBoundaries} it is given,
 * so a password box can make its whole text one word.</p>
 *
 * <p>The rectangles it draws are kept from frame to frame and made again only when what they cover moves, so a
 * frame that repeats the last one allocates nothing.</p>
 */
final class LineEditor {
    /**
     * How long, in milliseconds, the caret stays shown and then hidden. Each edit or caret move restarts the cycle
     * with the caret shown.
     */
    static final long BLINK_MILLIS = 500L;

    private final EditBuffer buffer;
    private final Owner owner;
    private final TextEditor editor;
    private final Runnable drawContent;

    private final Runnable drawPlain;
    private final Runnable drawSelected;
    private float scrollX;
    private FieldLook look = FieldLook.plain(Paint.NONE, Paint.NONE);
    private TextStyle underscoreStyle;
    private TextSpan underscore;

    // Derived from the text and the caret, worked out again only after a change
    private boolean visualsDirty;
    private float textWidth;
    private float caretX;
    private float selectionStartX;
    private float selectionEndX;

    // Drawn last frame, made again only when they move
    private Rect clipRect;
    private Rect caretRect;
    private Rect selectedRect;
    private Rect beforeClip;
    private Rect afterClip;

    // Set only while paint runs, for drawContent
    private Painter painter;
    private Rect contentBox;
    private float textY;
    private float lineHeight;
    private Paint selectionPaint;

    @SuppressWarnings("this-escape")
    LineEditor(final EditBuffer buffer, final TextBoundaries boundaries, final Owner owner) {
        this.buffer = buffer;
        this.owner = owner;
        this.editor = new TextEditor(buffer, boundaries, owner, null, this::invalidate);
        this.drawContent = this::drawContent;
        this.drawPlain = this::drawPlainText;
        this.drawSelected = this::drawSelectedText;
        this.contentBox = Rect.ZERO;
        this.selectionPaint = Paint.NONE;
        this.visualsDirty = true;
    }

    /**
     * Returns the editor under this line, for the owner's edits.
     */
    TextEditor editor() {
        return this.editor;
    }

    // ------------------------------------------------------------------
    // Caret and selection
    // ------------------------------------------------------------------

    int caret() {
        return this.editor.caret();
    }

    int anchor() {
        return this.editor.anchor();
    }

    int selectionStart() {
        return this.editor.selectionStart();
    }

    int selectionEnd() {
        return this.editor.selectionEnd();
    }

    /**
     * Selects a range for code, clamped to the text and snapped to code points.
     */
    void select(final int anchor, final int caret) {
        this.editor.select(anchor, caret);
    }

    void selectAll() {
        this.editor.selectAll();
    }

    /**
     * Puts the caret at an index with nothing selected, after the owner changed the text.
     */
    void place(final int index) {
        this.editor.place(index);
    }

    /**
     * Keeps the caret and anchor inside the text, after the owner cut it.
     */
    void clamp() {
        this.editor.clamp();
    }

    /**
     * Marks the drawing out of date and shows the caret again from the start of its blink.
     */
    void moved() {
        this.editor.moved();
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
        return this.editor.caretShown(this.look.blinkMillis());
    }

    // ------------------------------------------------------------------
    // Focus and pointer
    // ------------------------------------------------------------------

    /**
     * Places the caret for focus gained: at the end, with everything selected if the field selects all on focus. A
     * press that gave focus places the caret right after this, unless the field selects all on focus.
     */
    void focusGained(final boolean selectAll, final boolean fromPress) {
        this.editor.focusGained(selectAll, fromPress);
    }

    void focusLost() {
        this.editor.focusLost();
    }

    /**
     * Forgets that the press that gave focus should keep everything selected, for a press that landed elsewhere.
     */
    void cancelPress() {
        this.editor.cancelPress();
    }

    /**
     * Handles a left press; the owner has checked the button and that it is enabled.
     *
     * @param textX the press relative to where the text starts when not scrolled
     */
    void press(final PointerButtonEvent event, final float textX) {
        this.editor.press(event, this.indexAt(textX));
    }

    /**
     * Handles a left drag that did not start with a triple click; the owner has checked the button and that it is
     * enabled.
     *
     * @param textX the pointer relative to where the text starts when not scrolled
     */
    void drag(final DragEvent event, final float textX) {
        this.editor.drag(event, this.indexAt(textX));
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
        return this.editor.key(key, modifiers);
    }

    /**
     * Types a code point over the selection; control characters and line and paragraph separators are ignored.
     *
     * @return false for a code point the field leaves alone
     */
    boolean type(final int codePoint) {
        return this.editor.type(codePoint);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * Draws the selection and the text inside a clip of the content box, grown up and down by the selection's
     * reach, then the caret. Scrolls first, just enough to keep the caret in view.
     *
     * @param look how the caret and selection look
     * @param textStyle the style of the text, for an underscore caret
     * @param showCaret whether the caret may show at all: focused, enabled and editable
     */
    void paint(final Painter painter, final Rect contentBox, final float textY, final float lineHeight,
               final FieldLook look, final TextStyle textStyle, final boolean showCaret) {
        this.contentBox = contentBox;
        this.textY = textY;
        this.lineHeight = lineHeight;
        this.look = look;
        this.selectionPaint = look.selection();
        this.updateVisuals(this.owner.environment().text());
        this.revealCaret();

        this.painter = painter;
        try {
            final float reach = look.selectionExtend();
            if (reach > 0f) {
                this.clipRect = Rects.reuse(this.clipRect, contentBox.x(), contentBox.y() - reach, contentBox.width(),
                        contentBox.height() + reach * 2f);
                painter.clipped(this.clipRect, this.drawContent);
            } else {
                painter.clipped(contentBox, this.drawContent);
            }
        } finally {
            this.painter = null;
        }

        if (showCaret) {
            this.owner.repaintAfter(this.editor.untilBlink(look.blinkMillis()));
        }
        if (showCaret && this.caretShown()) {
            final float atX = contentBox.x() + this.caretX - this.scrollX;
            if (look.underscoreCaret() && this.editor.caret() == this.buffer.length()) {
                if (this.underscore == null || !textStyle.equals(this.underscoreStyle)) {
                    this.underscoreStyle = textStyle;
                    this.underscore = new TextSpan("_", textStyle);
                }
                painter.text(this.underscore, this.buffer.length() > 0 ? atX + 1f : atX, textY);
                return;
            }
            final float x = Math.max(contentBox.x(), Math.min(contentBox.right() - 1f, atX));
            final float reach = look.caretExtend();
            this.caretRect = Rects.reuse(this.caretRect, x, textY - reach, 1f, lineHeight + reach * 2f);
            look.caret().paint(painter, this.caretRect);
        }
    }

    /**
     * Runs inside the content clip during {@link #paint}; a field-held method reference, so painting allocates no
     * lambda per frame.
     */
    private void drawContent() {
        final float originX = this.contentBox.x() - this.scrollX;
        if (!this.editor.hasSelection()) {
            this.owner.drawText(this.painter, originX, this.contentBox.x(), this.textY, null);
            return;
        }
        final float reach = this.look.selectionExtend();
        this.selectedRect = Rects.reuse(this.selectedRect, originX + this.selectionStartX, this.textY - reach,
                this.selectionEndX - this.selectionStartX, this.lineHeight + reach * 2f);
        final Rect selected = this.selectedRect;
        this.selectionPaint.paint(this.painter, selected);
        if (this.look.selectedText() == null) {
            this.owner.drawText(this.painter, originX, this.contentBox.x(), this.textY, null);
            return;
        }
        // The text outside the selection as it is, and inside it in the selected style
        final Rect content = this.contentBox;
        final float top = content.y() - reach;
        final float height = content.height() + reach * 2f;
        this.beforeClip = Rects.reuse(this.beforeClip, content.x(), top, selected.x() - content.x(), height);
        this.afterClip = Rects.reuse(this.afterClip, selected.right(), top, content.right() - selected.right(), height);
        this.painter.clipped(this.beforeClip, this.drawPlain);
        this.painter.clipped(this.afterClip, this.drawPlain);
        this.painter.clipped(selected, this.drawSelected);
    }

    private void drawPlainText() {
        this.owner.drawText(this.painter, this.contentBox.x() - this.scrollX, this.contentBox.x(), this.textY, null);
    }

    private void drawSelectedText() {
        this.owner.drawText(this.painter, this.contentBox.x() - this.scrollX, this.contentBox.x(), this.textY,
                this.look.selectedText());
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
        this.caretX = this.owner.xForIndex(metrics, this.editor.caret());
        this.selectionStartX = this.owner.xForIndex(metrics, this.editor.selectionStart());
        this.selectionEndX = this.owner.xForIndex(metrics, this.editor.selectionEnd());
    }

    /**
     * What a single-line field tells its editor: how it edits, as every field does, and how it measures and draws its
     * one line. Every method runs on the UI thread.
     */
    interface Owner extends TextEditor.Host {

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
         *
         * @param style the style to draw the text in instead of its own, or null to keep its own
         */
        void drawText(Painter painter, float originX, float left, float y, TextStyle style);
    }
}
