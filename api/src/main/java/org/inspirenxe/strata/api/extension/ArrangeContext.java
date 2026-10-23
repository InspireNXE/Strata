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
package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * What a container may use while it places its children. Valid only during the arrange call.
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface ArrangeContext {

    /**
     * Returns the engine's text measurement.
     *
     * @return the text metrics
     */
    TextMetrics text();

    /**
     * Returns the space a child asked for in the last measure: its desired size plus margin.
     *
     * @param child a child of the container being arranged
     * @return the child's outer size, or zero if it is hidden
     */
    Size outerSize(Component<?> child);

    /**
     * Returns the smallest space a child can work in, for sharing space among children. It is the child's content
     * minimum ({@link CustomComponent#measureMinimum}), raised to its minimum size and capped by its maximum size, or
     * its fixed size on an axis that has one, plus its margin. Strata works it out when first asked after the child's
     * layout was invalidated and keeps it until the next time, so it may be read before or after the child is
     * measured.
     *
     * @param child a child of the component being arranged
     * @return the child's outer minimum, or zero if it is hidden
     * @throws IllegalArgumentException if {@code child} was made by another factory or has no Strata implementation
     */
    Size minimumSize(Component<?> child);

    /**
     * Returns where a child's first baseline lies, as {@link MeasureContext#baseline} does: from the top of the
     * space it asked for in the last measure, margin included.
     *
     * @param child a child of the container being arranged
     * @return the baseline in UI units from the top of the child's margin, or NaN if the child has none or is hidden
     * @throws IllegalArgumentException if {@code child} was made by another factory or has no Strata implementation
     */
    float baseline(Component<?> child);

    /**
     * Measures a child, for one added during arrange, such as a row that scrolled into view. Measure such a child
     * before giving it a slot.
     *
     * @param child a child of the component being arranged
     * @param availableWidth the width offered to the child, margin included; may be infinite
     * @param availableHeight the height offered to the child, margin included; may be infinite
     * @return the space the child needs: its desired size plus its margin, or zero if it is hidden
     * @throws IllegalArgumentException if {@code child} was made by another factory or has no Strata implementation
     */
    Size measure(Component<?> child, float availableWidth, float availableHeight);

    /**
     * Gives a child its slot. The child shrinks the slot by its margin and places itself in what remains by its
     * alignment and size limits, which sets its bounds. A slot smaller than the child's desired size is allowed;
     * the child then overflows it unless it stretches.
     *
     * @param child a child of the container being arranged
     * @param slot the area the child may use, margin included, in the host's coordinates
     */
    void arrange(Component<?> child, Rect slot);
}
