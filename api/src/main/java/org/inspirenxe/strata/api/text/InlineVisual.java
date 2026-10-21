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
package org.inspirenxe.strata.api.text;

import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;

/**
 * An engine visual in a line of text, drawn untinted at its {@linkplain Visual#size() natural size}.
 *
 * @param visual the visual to draw
 */
public record InlineVisual(Visual visual) implements InlineElement {
    /**
     * Checks the visual.
     */
    public InlineVisual {
        Objects.requireNonNull(visual, "visual");
    }
}
