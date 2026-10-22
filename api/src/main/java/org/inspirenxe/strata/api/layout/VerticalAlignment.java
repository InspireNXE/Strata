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

/**
 * Where a component sits across the height of the slot its parent gives it. See
 * {@link org.inspirenxe.strata.api.component.Component#setAlignment}.
 */
public enum VerticalAlignment {
    /** Against the slot's top edge (after the margin), at the desired height. */
    TOP,
    /** Centered in the slot, at the desired height. */
    MIDDLE,
    /** Against the slot's bottom edge (before the margin), at the desired height. */
    BOTTOM,
    /**
     * As tall as the slot minus the margin, unless the component has a fixed height; the minimum and maximum still
     * apply.
     */
    STRETCH,
    /**
     * With the baseline of its first line of text on the line its neighbors share, so a label, a text box and a
     * button in a row show their text at one height although they pad it differently. A horizontal
     * {@link StackPanel}, each row of a {@link WrapPanel} and each row of a {@link GridPanel} line up the children
     * aligned this way: each moves down until its baseline meets the lowest baseline among them, and a child with no
     * baseline puts its bottom there. The baseline is what the component reports through
     * {@code CustomComponent.baseline()}; Strata's text controls report the baseline of their first line. Everywhere
     * else, and in a cell or line that a child spans with others, this acts as {@link #TOP}.
     */
    BASELINE
}
