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
import org.inspirenxe.strata.api.layout.Orientation;

/**
 * A thin line that divides a panel, across or down.
 *
 * <p>A horizontal separator is {@linkplain #setThickness thickness} units tall and stretches across its slot; a
 * vertical one is that wide and stretches down it. Along the line it asks for no length, so it takes the length of
 * the slot it stretches over. It is not focusable, has no default behavior for input and fires no events.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_LINE} (paint, required): the line.</li>
 * </ul>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#separator()}, which is horizontal, or
 * {@link org.inspirenxe.strata.api.ComponentFactory#separator(Orientation)}. Do not implement this interface.</p>
 */
public interface Separator extends Component<Separator> {
    /**
     * Style key for the line (paint, required).
     */
    String STYLE_LINE = "separator";

    /**
     * Returns which way the line runs.
     *
     * @return the orientation; {@link Orientation#HORIZONTAL} unless given otherwise
     */
    Orientation orientation();

    /**
     * Sets which way the line runs, and aligns the separator to stretch along it: across and at the top for
     * {@link Orientation#HORIZONTAL}, down and at the left for {@link Orientation#VERTICAL}. A new layout runs.
     *
     * @param orientation the orientation
     * @return this separator
     */
    Separator setOrientation(Orientation orientation);

    /**
     * Returns the thickness.
     *
     * @return the line's thickness in UI units; 1 by default
     */
    float thickness();

    /**
     * Sets the line's thickness.
     *
     * @param thickness the thickness in UI units
     * @return this separator
     * @throws IllegalArgumentException if the thickness is negative or not finite
     */
    Separator setThickness(float thickness);
}
