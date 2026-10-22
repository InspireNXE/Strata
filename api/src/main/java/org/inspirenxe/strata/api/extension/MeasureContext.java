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
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * What a component may use while it measures. Valid only during the measure call.
 *
 * <p>Strata provides this object. Do not implement this interface.</p>
 */
public interface MeasureContext {

    /**
     * Returns the engine's text measurement.
     *
     * @return the text metrics
     */
    TextMetrics text();

    /**
     * Measures a child within the given space. Containers must call this for every child during their own
     * measure; the result is also available later from {@link ArrangeContext#outerSize}.
     *
     * @param child a child of the container being measured
     * @param availableWidth the width offered to the child, margin included; may be infinite
     * @param availableHeight the height offered to the child, margin included; may be infinite
     * @return the space the child needs: its desired size plus its margin, or zero if it is hidden
     * @throws IllegalArgumentException if {@code child} was made by another factory or has no Strata implementation
     */
    Size measure(Component<?> child, float availableWidth, float availableHeight);

    /**
     * Returns the smallest space a child can work in, for sharing space among children. It is the child's content
     * minimum ({@link CustomComponent#measureMinimum}), raised to its minimum size and capped by its maximum size, or
     * its fixed size on an axis that has one, plus its margin. Strata works it out when first asked after the child's
     * layout was invalidated and keeps it until the next time, so it may be read before or after the child is
     * measured.
     *
     * @param child a child of the component being measured
     * @return the child's outer minimum, or zero if it is hidden
     * @throws IllegalArgumentException if {@code child} was made by another factory or has no Strata implementation
     */
    Size minimumSize(Component<?> child);
}
