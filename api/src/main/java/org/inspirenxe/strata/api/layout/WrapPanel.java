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
package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Container;

import java.util.Objects;

/**
 * A container that places its children one after another and starts a new line when the next child would pass the
 * end, as chips in a tag editor or the buttons of a toolbar that narrows. With {@link Orientation#HORIZONTAL}, the
 * default, the lines are rows filled left to right and stacked top to bottom; with {@link Orientation#VERTICAL} they
 * are columns filled top to bottom and stacked left to right.
 *
 * <p>Each child is measured with the panel's content width offered (its content height in columns) and keeps the
 * size it asks for. A line is as tall as its tallest child (as wide, in columns), and each child sits in its line
 * by its own alignment across the line, so a chip with {@link VerticalAlignment#MIDDLE} is centered in its row. A
 * child that is wider than the panel gets a line of its own. With unlimited width, as inside a horizontal scroll
 * panel, every child is in one line. Hidden children take no space and no gap.</p>
 *
 * <p>The {@linkplain #setLineAlignment line alignment} places each line's children along it: at the start, the
 * center or the end, or spread with the spare room shared among the gaps. In a column, {@link HorizontalAlignment#LEFT}
 * means the top and {@link HorizontalAlignment#RIGHT} the bottom.</p>
 *
 * <pre>{@code
 * WrapPanel tags = factory.wrapPanel().setSpacing(4, 2);
 * for (String tag : List.of("red", "large", "on sale")) {
 *     tags.add(factory.button(tag));
 * }
 * }</pre>
 *
 * <p>The panel's smallest size, which a parent sharing space gives it at least, is its widest and tallest child's
 * minimum plus the padding.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#wrapPanel()}. Do not implement this
 * interface.</p>
 */
public interface WrapPanel extends Container<WrapPanel> {

    /**
     * Returns the direction each line fills in.
     *
     * @return the orientation; {@link Orientation#HORIZONTAL} by default
     */
    Orientation orientation();

    /**
     * Sets the direction each line fills in: {@link Orientation#HORIZONTAL} fills rows, {@link Orientation#VERTICAL}
     * fills columns.
     *
     * @param orientation the orientation
     * @return this panel
     */
    WrapPanel setOrientation(Orientation orientation);

    /**
     * Returns the gap between neighboring children in a line.
     *
     * @return the spacing in UI units; 0 by default
     */
    float spacing();

    /**
     * Returns the gap between lines.
     *
     * @return the spacing in UI units; 0 by default
     */
    float lineSpacing();

    /**
     * Sets the gaps between neighboring children in a line and between lines. Gaps are added between children and
     * lines only, never before the first or after the last.
     *
     * @param spacing the gap between children in a line, in UI units
     * @param lineSpacing the gap between lines, in UI units
     * @return this panel
     * @throws IllegalArgumentException if a spacing is infinite or NaN
     */
    WrapPanel setSpacing(float spacing, float lineSpacing);

    /**
     * Returns where each line's children sit along it.
     *
     * @return the line alignment; {@link HorizontalAlignment#LEFT} by default
     */
    HorizontalAlignment lineAlignment();

    /**
     * Sets where each line's children sit along it when the line has room to spare: against the start
     * ({@link HorizontalAlignment#LEFT}), centered, against the end ({@link HorizontalAlignment#RIGHT}), or spread
     * ({@link HorizontalAlignment#STRETCH}), where the spare room is shared equally among the line's gaps so its
     * first child sits at the start and its last at the end. A line of one child stays at the start when spread. In
     * a column, the start is the top.
     *
     * @param alignment the line alignment
     * @return this panel
     */
    WrapPanel setLineAlignment(HorizontalAlignment alignment);

    /**
     * A wrap panel's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. It says nothing
     * about the children.
     *
     * @param orientation the {@linkplain #orientation() orientation}; never null
     * @param spacing the {@linkplain #spacing() gap} between children in a line
     * @param lineSpacing the {@linkplain #lineSpacing() gap} between lines
     * @param lineAlignment the {@linkplain #lineAlignment() line alignment}; never null
     */
    record State(Orientation orientation, float spacing, float lineSpacing, HorizontalAlignment lineAlignment) {

        /**
         * Checks the orientation and the line alignment.
         */
        public State {
            Objects.requireNonNull(orientation, "orientation");
            Objects.requireNonNull(lineAlignment, "lineAlignment");
        }
    }
}
