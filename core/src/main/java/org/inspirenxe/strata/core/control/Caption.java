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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.text.TextLine;

/**
 * Draws a one-line caption centered in a control, cut where it would leave the control's clip area, and scrolled
 * back and forth when the theme asks for it. Each control that has a caption keeps one and reads three keys into
 * it: insets for the clip area, and two metrics for scrolling.
 *
 * <p>A caption that fits the clip area is centered in the control less its padding, then moved inside the clip
 * area if centering put it partly outside. A caption wider than the clip area is drawn from the clip area's left
 * edge and cut at its right, or, when the scroll metric is above 0, slides between its two ends, starting from the
 * end: one full sweep there and back takes the overflow in units times the metric, in milliseconds, but no less
 * than the minimum metric. The slide eases at each end and follows the platform's clock.</p>
 *
 * <p>The clip area is the bounds less the clip insets. A control with no clip insets in its theme uses its
 * padding's left and right and the full height, so a caption is cut where the padding starts.</p>
 *
 * <p>A caption can have something before it, such as an icon and a gap, which the control draws itself where
 * {@link #start} says. The two are centered together as one piece when they fit the clip area; otherwise the piece
 * starts at the clip area's left edge and the caption alone is cut or scrolled in the rest of the clip area.</p>
 *
 * <p>The clip rectangle is kept from one frame to the next while it stays the same, so drawing a cut caption
 * allocates nothing once the control's bounds settle.</p>
 */
public final class Caption {
    private Insets clip;
    private float scrollRate;
    private float scrollMinimum;

    // The caption being drawn, kept so the clip callback needs no new object
    private Painter painter;
    private TextLine line;
    private float lineX;
    private float lineY;
    private final Runnable drawClipped = this::drawLine;
    private final RectCache clipRect = new RectCache();

    /**
     * Creates a caption drawer with no clip insets and no scrolling, until {@link #applyTheme} reads them.
     */
    public Caption() {
    }

    /**
     * Reads the clip insets and the two scroll metrics.
     *
     * @param theme the theme
     * @param clipKey the key of the clip insets
     * @param scrollKey the key of the scroll metric, in milliseconds per unit of overflow
     * @param scrollMinimumKey the key of the shortest sweep, in milliseconds
     */
    public void applyTheme(final Theme theme, final String clipKey, final String scrollKey,
                           final String scrollMinimumKey) {
        this.clip = theme.insets(clipKey).orElse(null);
        this.scrollRate = Math.max(0f, theme.metricOr(scrollKey, 0f));
        this.scrollMinimum = Math.max(0f, theme.metricOr(scrollMinimumKey, 0f));
    }

    /**
     * Draws a caption centered across a control less its padding, at a given top.
     *
     * @param painter what to draw with
     * @param line the caption
     * @param bounds the control's bounds
     * @param padding the control's padding, which centering leaves out
     * @param y the caption's top
     * @param millis the platform's clock, for scrolling
     * @return true if the caption slides, so it looks different on the next frame and the control should ask for
     *     one
     */
    public boolean draw(final Painter painter, final TextLine line, final Rect bounds, final Insets padding,
                        final float y, final long millis) {
        return this.draw(painter, line, bounds, padding, 0f, line.width(), y, millis);
    }

    /**
     * Draws a caption with something before it, the two centered together as a piece of a given width, at a given
     * top. The caller draws what comes before the caption at {@link #start}. The caption is drawn {@code lead} units
     * after the piece's start, and cut or scrolled at the clip area's right when it does not fit there.
     *
     * @param painter what to draw with
     * @param line the caption
     * @param bounds the control's bounds
     * @param padding the control's padding, which centering leaves out
     * @param lead the width of what comes before the caption, its gap included
     * @param width the width of the whole piece, as measured
     * @param y the caption's top
     * @param millis the platform's clock, for scrolling
     * @return true if the caption slides, so it looks different on the next frame and the control should ask for
     *     one
     */
    public boolean draw(final Painter painter, final TextLine line, final Rect bounds, final Insets padding,
                        final float lead, final float width, final float y, final long millis) {
        return this.draw(painter, line, bounds, padding, lead, 0f, width, y, millis);
    }

    /**
     * Draws a caption inside a piece of a given width with something before it, after it, or both, as
     * {@link #draw(Painter, TextLine, Rect, Insets, float, float, float, long)} does, and cut or scrolled
     * {@code trail} units before the clip area's right. A control that runs right to left puts its icon after the
     * caption this way.
     *
     * @param painter what to draw with
     * @param line the caption
     * @param bounds the control's bounds
     * @param padding the control's padding, which centering leaves out
     * @param lead the width of what comes before the caption, its gap included
     * @param trail the width of what comes after the caption, its gap included
     * @param width the width of the whole piece, as measured
     * @param y the caption's top
     * @param millis the platform's clock, for scrolling
     * @return true if the caption slides
     */
    public boolean draw(final Painter painter, final TextLine line, final Rect bounds, final Insets padding,
                        final float lead, final float trail, final float width, final float y, final long millis) {
        final float clipRight = bounds.right() - (this.clip != null ? this.clip.right() : padding.right()) - trail;
        final float left = this.start(bounds, padding, width) + lead;
        final float space = Math.max(0f, clipRight - left);
        final float overflow = line.width() - space;
        if (overflow <= 0f) {
            line.draw(painter, left, y);
            return false;
        }
        float x = left;
        if (this.scrollRate > 0f) {
            final double period = Math.max(overflow * (double) this.scrollRate, Math.max(1d, this.scrollMinimum));
            final double eased = Math.sin(Math.PI / 2d * Math.cos(Math.PI * 2d * millis / period)) / 2d + 0.5d;
            final double shift = eased * overflow;
            // On whole units the caption steps a unit at a time, as a whole-unit engine would draw it anyway
            x -= (float) (overflow == Math.rint(overflow) ? Math.floor(shift) : shift);
        }
        final float top = this.clip != null ? bounds.y() + this.clip.top() : bounds.y();
        final float bottom = this.clip != null ? bounds.bottom() - this.clip.bottom() : bounds.bottom();
        this.painter = painter;
        this.line = line;
        this.lineX = x;
        this.lineY = y;
        try {
            painter.clipped(this.clipRect.at(left, top, space, Math.max(0f, bottom - top)), this.drawClipped);
        } finally {
            this.painter = null;
            this.line = null;
        }
        return this.scrollRate > 0f;
    }

    /**
     * Returns the right edge of the clip area, where a caption too wide for the control is cut.
     *
     * @param bounds the control's bounds
     * @param padding the control's padding, which is the clip area without a clip key
     * @return the clip area's right edge
     */
    public float clipRight(final Rect bounds, final Insets padding) {
        return bounds.right() - (this.clip != null ? this.clip.right() : padding.right());
    }

    /**
     * Returns where a piece of a given width starts: centered across the control less its padding and moved inside
     * the clip area when it fits there, or else at the clip area's left edge.
     *
     * @param bounds the control's bounds
     * @param padding the control's padding, which centering leaves out
     * @param width the piece's width
     * @return the piece's left edge
     */
    public float start(final Rect bounds, final Insets padding, final float width) {
        final float clipLeft = bounds.x() + (this.clip != null ? this.clip.left() : padding.left());
        final float clipRight = bounds.right() - (this.clip != null ? this.clip.right() : padding.right());
        if (width > clipRight - clipLeft) {
            return clipLeft;
        }
        final float centered = center(bounds.x() + padding.left(), bounds.width() - padding.horizontal(), width);
        return Math.max(clipLeft, Math.min(centered, clipRight - width));
    }


    private void drawLine() {
        this.line.draw(this.painter, this.lineX, this.lineY);
    }

    /**
     * Returns where something of a given size starts when centered in a space. When both are whole numbers the
     * result is {@code start + floor(space / 2) - floor(size / 2)}, so an odd space less an even size leaves the
     * spare unit on the right and the result stays on a whole unit; otherwise it is exactly
     * {@code start + (space - size) / 2}.
     *
     * @param start where the space starts
     * @param space the space's length
     * @param size the length of what is centered
     * @return where it starts
     */
    public static float center(final float start, final float space, final float size) {
        if (space == Math.rint(space) && size == Math.rint(size)) {
            return start + (float) (Math.floor(space / 2d) - Math.floor(size / 2d));
        }
        return start + (space - size) * 0.5f;
    }
}
