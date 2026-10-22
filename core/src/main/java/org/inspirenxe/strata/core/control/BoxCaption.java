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
import org.inspirenxe.strata.core.text.MnemonicLine;
import org.inspirenxe.strata.core.text.Mnemonics;
import org.inspirenxe.strata.core.text.TextLine;

/**
 * The layout a checkbox, a radio button and a toggle switch share: a box at the left edge, vertically centered, and
 * a caption a gap to its right, centered on the box's middle. Right to left, the box is at the right edge and the
 * caption a gap to its left. Each control keeps one, measures its caption and sizes
 * its box through it, and draws its own box in the rectangle {@link #box} gives.
 *
 * <p>The caption is resolved in the normal and disabled styles, and the wider and taller of the two count, so
 * enabling or disabling the control needs no new layout. Its mnemonic marker is taken out, and each is also kept
 * with the mnemonic underlined, for drawing while mnemonics show. An empty caption leaves the gap out. The box's rectangle is
 * kept from one frame to the next while the control's bounds and the box size stay the same, so the same rectangle
 * comes back and painting allocates nothing.</p>
 */
public final class BoxCaption {
    private final float gap;
    private MnemonicLine caption = MnemonicLine.EMPTY;
    private MnemonicLine disabledCaption = MnemonicLine.EMPTY;
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
        this.caption = MnemonicLine.of(text, style, metrics);
        this.disabledCaption = MnemonicLine.of(text, disabledStyle, metrics);
        this.captionWidth = Math.max(this.caption.plain().width(), this.disabledCaption.plain().width());
        this.captionHeight = Math.max(this.caption.plain().height(), this.disabledCaption.plain().height());
        return this.captionHeight;
    }

    /**
     * Returns the caption's mnemonic as of the last measure.
     *
     * @return the mnemonic in lower case, or {@link Mnemonics#NONE}
     */
    public int mnemonic() {
        return this.caption.mnemonic();
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
        final float total = this.caption.plain().isEmpty() ? width : width + this.gap + this.captionWidth;
        return new Size(total, Math.max(height, this.captionHeight));
    }

    /**
     * Returns the box's rectangle in a control's bounds: at the left edge, or the right edge right to left,
     * centered vertically.
     *
     * @param bounds the control's bounds
     * @param rightToLeft whether the control runs right to left
     * @return the box, the same rectangle as last time while it did not move or change size
     */
    public Rect box(final Rect bounds, final boolean rightToLeft) {
        final float middle = bounds.y() + bounds.height() * 0.5f;
        return this.box.at(rightToLeft ? bounds.right() - this.boxWidth : bounds.x(), middle - this.boxHeight * 0.5f,
                this.boxWidth, this.boxHeight);
    }

    /**
     * Returns the caption's baseline in a control of a given height, as {@link #paintCaption} places the caption.
     *
     * @param height the control's height
     * @return the baseline below the control's top, or NaN for an empty caption
     */
    public float baseline(final float height) {
        if (this.caption.plain().isEmpty()) {
            return Float.NaN;
        }
        return height * 0.5f - this.captionHeight * 0.5f + this.caption.plain().baseline();
    }

    /**
     * Draws the caption the gap beside the box, right of it or left of it right to left, centered on the bounds'
     * middle, in the normal or disabled style. Draws nothing for an empty caption.
     *
     * @param painter what to draw with
     * @param bounds the control's bounds
     * @param enabled false for the disabled style
     * @param showMnemonic true to underline the mnemonic
     * @param rightToLeft whether the control runs right to left
     */
    public void paintCaption(final Painter painter, final Rect bounds, final boolean enabled,
                             final boolean showMnemonic, final boolean rightToLeft) {
        if (this.caption.plain().isEmpty()) {
            return;
        }
        final float middle = bounds.y() + bounds.height() * 0.5f;
        final TextLine line = (enabled ? this.caption : this.disabledCaption).line(showMnemonic);
        final float x = rightToLeft ? bounds.right() - this.boxWidth - this.gap - line.width()
                : bounds.x() + this.boxWidth + this.gap;
        line.draw(painter, x, middle - this.captionHeight * 0.5f);
    }
}
