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
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.text.Text;

/**
 * A small sign that something is under way, such as joining a server or loading a list: a row of three dots, one
 * of them lit, with an optional caption above them. The lit dot moves from the first to the last and back, one step
 * every 300 milliseconds ({@link #FRAME_MILLIS}) of the host's clock, for as long as the indicator is shown. Hide or
 * remove it when the work is done. For work whose progress is known, use a {@link ProgressBar}; for work whose
 * length is not, an indeterminate one is the other choice.
 *
 * <p>The dots are 4 units square ({@link #DOT_SIZE}) with 4 units ({@link #DOT_GAP}) between them, centered across
 * the bounds and at their bottom. The caption is centered across the bounds, the same gap above the dots. The
 * indicator asks for the wider of the caption and the dots, and for the dots' height plus, with a caption, one line
 * and the gap. It is not focusable, takes no input and fires no events.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_DOT} (paint, required): each unlit dot.</li>
 *     <li>{@value #STYLE_DOT_ACTIVE} (paint, required): the lit dot.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default style.</li>
 *     <li>{@value #STYLE_DOTS_TEXT} (text style, optional): when set, the dots are drawn as the letters
 *     {@code o o o} in this style, the lit one a capital {@code O}, one line tall and centered where the dots
 *     would be, instead of with the two dot paints.</li>
 * </ul>
 *
 * <pre>{@code
 * BusyIndicator joining = factory.busyIndicator("Joining world");
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#busyIndicator()}. Do not implement this
 * interface.</p>
 */
public interface BusyIndicator extends Component<BusyIndicator>, HasText<BusyIndicator> {
    /**
     * Style key for an unlit dot (paint, required).
     */
    String STYLE_DOT = "busy.dot";
    /**
     * Style key for the lit dot (paint, required).
     */
    String STYLE_DOT_ACTIVE = "busy.dot.active";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "busy.text";
    /**
     * Style key for drawing the dots as letters (text style, optional). See the class description.
     */
    String STYLE_DOTS_TEXT = "busy.dots.text";
    /**
     * How wide and tall each dot is, in UI units.
     */
    float DOT_SIZE = 4f;
    /**
     * The space between two dots, and between the caption and the dots, in UI units.
     */
    float DOT_GAP = 4f;
    /**
     * How long the lit dot stays on one dot, in milliseconds.
     */
    long FRAME_MILLIS = 300L;

    /**
     * Returns the caption.
     *
     * @return the caption; empty by default
     */
    @Override
    Text text();

    /**
     * Sets the caption drawn above the dots. A new layout runs, since the size depends on it.
     *
     * @param text the caption, or an empty string for none
     * @return this indicator
     */
    @Override
    BusyIndicator setText(Text text);
}
