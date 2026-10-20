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
package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.text.Text;

/**
 * A bar filled from the left to show how far a value is through its range, with an optional caption centered on it,
 * such as {@code 12/36}. The range is 0 to 1 by default, so the value is the fraction filled.
 *
 * <p>It asks for 100 units of width and one line plus 4 units of height; stretch it or give it a size. It is not
 * focusable and has no default behavior for input, so it never fires {@link Ranged#CHANGE}.</p>
 *
 * <h2>Indeterminate</h2>
 * <p>For work whose length is not known, {@link #setIndeterminate(boolean)} replaces the fill with a segment a
 * quarter of the bar's width, at least 8 units, that slides in at the left edge and out at the right, once every 1.5
 * seconds, and then starts again. It follows the host's clock and moves in whole units. The value and the caption
 * are kept, and the caption is still drawn; the value shows again once the bar is no longer indeterminate.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_TRACK} (paint, required): the whole bar, under the fill.</li>
 *     <li>{@value #STYLE_FILL} (paint, required): the filled part.</li>
 *     <li>{@value #STYLE_SEGMENT} (paint, optional): the moving segment of an indeterminate bar. Falls back to
 *     {@value #STYLE_FILL}.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default style.</li>
 * </ul>
 *
 * <pre>{@code
 * ProgressBar slots = factory.progressBar()
 *         .setRange(0, limit)
 *         .setValue(used)
 *         .setText(used + "/" + limit);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#progressBar()}. Do not implement this
 * interface.</p>
 */
public interface ProgressBar extends Component<ProgressBar>, HasCursor<ProgressBar>, HasTooltip<ProgressBar>,
        HasText<ProgressBar>, Ranged<ProgressBar> {
    /**
     * Style key for the bar's track (paint, required).
     */
    String STYLE_TRACK = "progress.track";
    /**
     * Style key for the filled part (paint, required).
     */
    String STYLE_FILL = "progress.fill";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "progress.text";
    /**
     * Style key for the moving segment of an indeterminate bar (paint, optional).
     */
    String STYLE_SEGMENT = "progress.segment";
    /**
     * Style key for the space around the bar's text (insets, optional). The bar is at least as wide as its text
     * plus this left and right, as tall as a line plus this top and bottom, and its text centers in it less this
     * padding. Defaults to 2 on every side.
     */
    String STYLE_PADDING = "progress.padding";

    /**
     * Returns whether the bar shows a moving segment instead of its value.
     *
     * @return true if indeterminate; false by default
     */
    boolean isIndeterminate();

    /**
     * Shows a moving segment instead of the value, for work whose length is not known, or shows the value again.
     * The value is kept either way, and no event fires.
     *
     * @param indeterminate true for a moving segment
     * @return this bar
     */
    ProgressBar setIndeterminate(boolean indeterminate);

    /**
     * Returns the value.
     *
     * @return a value within the range; 0 by default
     */
    @Override
    double value();

    /**
     * Sets the value, clamped to the range. {@link Ranged#CHANGED} fires if it changed.
     *
     * @param value the value
     * @return this bar
     * @throws IllegalArgumentException if {@code value} is NaN
     */
    @Override
    ProgressBar setValue(double value);

    /**
     * Returns the lower end of the range, where the bar is empty.
     *
     * @return the minimum; 0 by default
     */
    @Override
    double min();

    /**
     * Returns the upper end of the range, where the bar is full.
     *
     * @return the maximum; 1 by default
     */
    @Override
    double max();

    /**
     * Sets the range and clamps the value to it. {@link Ranged#CHANGED} fires if that changed the value.
     *
     * @param min the value at which the bar is empty
     * @param max the value at which the bar is full
     * @return this bar
     * @throws IllegalArgumentException if either end is not finite, or {@code max} is not greater than {@code min}
     */
    @Override
    ProgressBar setRange(double min, double max);

    /**
     * Returns the caption.
     *
     * @return the caption; empty by default
     */
    @Override
    Text text();

    /**
     * Sets the caption drawn centered on the bar.
     *
     * @param text the caption, or an empty string for none
     * @return this bar
     */
    @Override
    ProgressBar setText(Text text);
}
