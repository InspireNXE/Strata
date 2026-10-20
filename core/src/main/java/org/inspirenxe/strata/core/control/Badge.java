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

import org.inspirenxe.strata.api.control.display.Avatar;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

/**
 * A count drawn as a small badge, with the shared {@code badge.*} keys: the number on
 * {@value Avatar#STYLE_BADGE} in the {@value Avatar#STYLE_BADGE_TEXT} style with {@value Avatar#STYLE_BADGE_PADDING}
 * around it. A control keeps one per count it shows, reads the theme into it, measures it during layout and paints
 * it into a rectangle it keeps, so painting allocates nothing.
 *
 * <p>A count of 0 shows nothing, and a count above {@link Avatar#MAX_BADGE} shows as {@code 99+}. The badge is the
 * number's width plus the padding's sides wide, and a line plus the padding's top and bottom tall.</p>
 */
public final class Badge {
    /**
     * The background when the theme sets no {@value Avatar#STYLE_BADGE}: a red fill.
     */
    public static final Paint DEFAULT_BACKGROUND = Paint.solid(0xFFD32F2F);
    /**
     * The padding when the theme sets no {@value Avatar#STYLE_BADGE_PADDING}: 2 at the sides and 1 at the top and
     * bottom.
     */
    public static final Insets DEFAULT_PADDING = new Insets(2f, 1f, 2f, 1f);

    private Paint background;
    private TextStyle style;
    private Insets padding;
    private int count;
    private TextSpan span;
    private float width;
    private float height;

    /**
     * Creates a badge with a count of 0 and the default looks.
     */
    public Badge() {
        this.background = DEFAULT_BACKGROUND;
        this.style = TextStyle.DEFAULT;
        this.padding = DEFAULT_PADDING;
        this.span = new TextSpan("", this.style);
    }

    /**
     * Returns the text a count shows as.
     *
     * @param count the count
     * @return an empty string for 0 or less, the number up to {@link Avatar#MAX_BADGE}, and {@code 99+} above it
     */
    public static String text(final int count) {
        if (count <= 0) {
            return "";
        }
        return count > Avatar.MAX_BADGE ? Avatar.MAX_BADGE + "+" : Integer.toString(count);
    }

    /**
     * Reads the background, text style and padding from a theme. Measure again before painting.
     *
     * @param theme the theme
     */
    public void applyTheme(final Theme theme) {
        this.background = theme.paintOr(Avatar.STYLE_BADGE, DEFAULT_BACKGROUND);
        this.style = theme.textStyleOr(Avatar.STYLE_BADGE_TEXT, TextStyle.DEFAULT);
        this.padding = theme.insetsOr(Avatar.STYLE_BADGE_PADDING, DEFAULT_PADDING);
    }

    /**
     * Returns the count.
     *
     * @return the count; 0 for a new badge
     */
    public int count() {
        return this.count;
    }

    /**
     * Sets the count. Measure again before painting.
     *
     * @param count the count; 0 hides the badge
     */
    public void setCount(final int count) {
        this.count = count;
    }

    /**
     * Returns whether the badge shows anything.
     *
     * @return true for a count above 0
     */
    public boolean isShown() {
        return this.count > 0;
    }

    /**
     * Works out the badge's text and size, during layout.
     *
     * @param metrics the engine's text metrics
     */
    public void measure(final TextMetrics metrics) {
        this.span = new TextSpan(Badge.text(this.count), this.style);
        this.width = metrics.width(this.span) + this.padding.horizontal();
        this.height = metrics.lineHeight(this.style) + this.padding.vertical();
    }

    /**
     * Returns the width found by the last {@link #measure}.
     *
     * @return the width in UI units
     */
    public float width() {
        return this.width;
    }

    /**
     * Returns the height found by the last {@link #measure}.
     *
     * @return the height in UI units
     */
    public float height() {
        return this.height;
    }

    /**
     * Returns the rectangle the badge takes with its bottom-right corner at a point.
     *
     * @param right the x of the corner
     * @param bottom the y of the corner
     * @return the badge's rectangle
     */
    public Rect cornerAt(final float right, final float bottom) {
        return new Rect(right - this.width, bottom - this.height, this.width, this.height);
    }

    /**
     * Draws the background over a rectangle and the number inside the padding. Draws nothing while the count is 0.
     *
     * @param painter what to draw with
     * @param rect the badge's rectangle, usually from {@link #cornerAt}
     */
    public void paint(final Painter painter, final Rect rect) {
        if (!this.isShown()) {
            return;
        }
        this.background.paint(painter, rect);
        painter.text(this.span, rect.x() + this.padding.left(), rect.y() + this.padding.top());
    }
}
