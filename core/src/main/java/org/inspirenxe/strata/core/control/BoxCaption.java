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
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.core.text.TextLine;

/**
 * The layout a checkbox, a radio button and a toggle switch share: a box at the left edge, vertically centered, and
 * a caption a gap to its right, centered on the box's middle. Each control keeps one, measures its caption and sizes
 * its box through it, and draws its own box in the rectangle {@link #box} gives.
 *
 * <p>The caption is resolved in the normal and disabled styles, and the wider and taller of the two count, so
 * enabling or disabling the control needs no new layout. An empty caption leaves the gap out. The box's rectangle is
 * kept from one frame to the next while the control's bounds and the box size stay the same, so the same rectangle
 * comes back and painting allocates nothing.</p>
 */
public final class BoxCaption {
    private final float gap;
    private TextLine caption = TextLine.EMPTY;
    private TextLine disabledCaption = TextLine.EMPTY;
    private float captionWidth;
    private float captionHeight;
    private float boxWidth;
    private float boxHeight;
    private final RectCache box = new RectCache();

    /**
     * Creates the layout with no caption and an empty box.
     *
     * @param gap the space between the box and the caption, in UI units
     */
    public BoxCaption(final float gap) {
        this.gap = gap;
    }

    /**
     * Resolves and measures the caption in the normal and disabled styles.
     *
     * @param text the caption
     * @param style the normal style
     * @param disabledStyle the style while the control is disabled
     * @param metrics the text metrics
     * @return the taller of the two captions' heights
     */
    public float measureCaption(final Text text, final TextStyle style, final TextStyle disabledStyle,
                                final TextMetrics metrics) {
        this.caption = TextLine.of(text, style, metrics);
        this.disabledCaption = TextLine.of(text, disabledStyle, metrics);
        this.captionWidth = Math.max(this.caption.width(), this.disabledCaption.width());
        this.captionHeight = Math.max(this.caption.height(), this.disabledCaption.height());
        return this.captionHeight;
    }

    /**
     * Sets the box's size and returns the control's desired size: the box, plus the gap and the caption unless the
     * caption is empty, and as tall as the taller of the box and the caption.
     *
     * @param width the box's width
     * @param height the box's height
     * @return the desired size
     */
    public Size size(final float width, final float height) {
        this.boxWidth = width;
        this.boxHeight = height;
        final float total = this.caption.isEmpty() ? width : width + this.gap + this.captionWidth;
        return new Size(total, Math.max(height, this.captionHeight));
    }

    /**
     * Returns the box's rectangle in a control's bounds: at the left edge, centered vertically.
     *
     * @param bounds the control's bounds
     * @return the box, the same rectangle as last time while it did not move or change size
     */
    public Rect box(final Rect bounds) {
        final float middle = bounds.y() + bounds.height() * 0.5f;
        return this.box.at(bounds.x(), middle - this.boxHeight * 0.5f, this.boxWidth, this.boxHeight);
    }

    /**
     * Draws the caption the gap right of the box, centered on the bounds' middle, in the normal or disabled style.
     * Draws nothing for an empty caption.
     *
     * @param painter what to draw with
     * @param bounds the control's bounds
     * @param enabled false for the disabled style
     */
    public void paintCaption(final Painter painter, final Rect bounds, final boolean enabled) {
        if (this.caption.isEmpty()) {
            return;
        }
        final float middle = bounds.y() + bounds.height() * 0.5f;
        (enabled ? this.caption : this.disabledCaption).draw(painter, bounds.x() + this.boxWidth + this.gap,
                middle - this.captionHeight * 0.5f);
    }
}
