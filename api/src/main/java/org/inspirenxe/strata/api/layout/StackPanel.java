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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;

import java.util.Objects;

/**
 * A container that lays its children out one after another, in a column ({@link Orientation#VERTICAL}, the
 * default) or a row ({@link Orientation#HORIZONTAL}), with a gap between them.
 *
 * <p>Along the orientation, each child gets exactly the space it asks for, unless it has a
 * {@linkplain #setWeight weight}. Children with a weight share whatever the others and the gaps leave, in
 * proportion to their weights, so a column of a header, a body with weight 1 and a footer gives the body the rest
 * of the height. Across the orientation, each child gets the full content size and aligns itself in it: in a column,
 * a child with {@link HorizontalAlignment#STRETCH} spans the width and one with {@link HorizontalAlignment#CENTER}
 * is centered. Hidden children take no space and no gap.</p>
 *
 * <p>With {@linkplain #setSpread spreading} on and no visible weighted child, the gaps grow equally until the
 * children cover the whole content box, with the first child at the start and the last at the end.</p>
 *
 * <pre>{@code
 * // A header, a body that takes the rest of the height, and a footer
 * StackPanel frame = factory.stackPanel()
 *         .add(header)
 *         .add(body, 1)
 *         .add(footer);
 *
 * // A toolbar with Help pushed to the right
 * StackPanel toolbar = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(4)
 *         .add(newButton)
 *         .add(factory.spacer(), 1)
 *         .add(helpButton);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#stackPanel()}. Do not implement this
 * interface.</p>
 */
public interface StackPanel extends Container<StackPanel> {

    /**
     * Returns the direction children are laid out in.
     *
     * @return the orientation; {@link Orientation#VERTICAL} by default
     */
    Orientation orientation();

    /**
     * Sets the direction children are laid out in.
     *
     * @param orientation the orientation
     * @return this panel
     */
    StackPanel setOrientation(Orientation orientation);

    /**
     * Returns the gap between visible children.
     *
     * @return the spacing in UI units; 0 by default
     */
    float spacing();

    /**
     * Sets the gap between visible children. The gap is added between children only, not before the first or
     * after the last. While {@linkplain #setSpread spreading}, it is the smallest gap.
     *
     * @param spacing the spacing in UI units
     * @return this panel
     * @throws IllegalArgumentException if the spacing is infinite or NaN
     */
    StackPanel setSpacing(float spacing);

    /**
     * Returns whether the gaps grow to spread the children across the content box.
     *
     * @return false by default
     */
    boolean isSpread();

    /**
     * Sets whether the gaps grow to spread the children across the content box. While on, the panel still asks for
     * its children and the {@linkplain #spacing() spacing}; when its slot gives it more room along the orientation,
     * the extra is shared equally between the gaps, so the first child sits at the start and the last at the end. A
     * single visible child stays at the start. Spreading does nothing while a visible child has a
     * {@linkplain #setWeight weight}, since that child takes the extra room.
     *
     * @param spread true to spread the children
     * @return this panel
     */
    StackPanel setSpread(boolean spread);

    /**
     * Returns a child's weight.
     *
     * @param child a child of this panel
     * @return its weight, or 0 if it has none or is not a child
     */
    float weight(Component<?> child);

    /**
     * Sets a child's share of the space left along the orientation. Children with a weight above 0 share whatever
     * the other children and the gaps leave, in proportion to their weights: weight 2 gets twice the space of weight
     * 1. A weighted child is placed in its share by its own alignment, so give it
     * {@link HorizontalAlignment#STRETCH} or {@link VerticalAlignment#STRETCH} along the orientation to fill it.
     * Its share stays within the child's minimum and {@linkplain Component#maxWidth() maximum} size along the
     * orientation, margin included. The minimum is the larger of the {@linkplain Component#minWidth() minimum size}
     * and what the content needs (a label's longest word, a button's caption, a panel's children), or the fixed
     * size where one is set. A child whose share would cross a limit gets the limit, and the other weighted children share the rest by their weights. Beyond
     * that its own desired size does not count, so a weighted child shrinks when the others need the room, and when
     * the minimums do not fit the children run past the end of the panel. Measured without a limit along the
     * orientation, the panel asks for enough room to give every weighted child its desired size while keeping the
     * proportions, though no child counts for more than its maximum. The weight is forgotten when the child is
     * removed.
     *
     * @param child a child of this panel
     * @param weight the weight, or 0 to give the child its desired size again
     * @return this panel
     * @throws IllegalArgumentException if {@code child} is not a child of this panel, or {@code weight} is negative,
     *                                  infinite or NaN
     */
    StackPanel setWeight(Component<?> child, float weight);

    /**
     * Adds a child with a weight, as {@link #add(Component)} and then {@link #setWeight} do.
     *
     * @param child the component to add
     * @param weight the weight, or 0 for none
     * @return this panel
     * @throws IllegalArgumentException as for {@link #add(Component)} and {@link #setWeight}; a bad weight leaves the
     *                                  child added without one
     */
    default StackPanel add(final Component<?> child, final float weight) {
        return this.add(child).setWeight(child, weight);
    }

    /**
     * A stack panel's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. It says
     * nothing about the children or their weights.
     *
     * @param orientation the {@linkplain #orientation() orientation}; never null
     * @param spacing the {@linkplain #spacing() gap} between children
     * @param spread whether the gaps {@linkplain #isSpread() spread} the children
     */
    record State(Orientation orientation, float spacing, boolean spread) {

        /**
         * Checks the orientation.
         */
        public State {
            Objects.requireNonNull(orientation, "orientation");
        }
    }
}
