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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.geometry.Rect;

/**
 * Keeps the rectangles a field paints from one frame to the next, so a frame that draws what the last one drew
 * allocates none.
 */
final class Rects {

    private Rects() {
    }

    /**
     * Returns the cached rectangle if it has these edges, or a new one if it does not.
     *
     * @param cached the rectangle drawn last time, or null
     * @param width the width; zero if negative or NaN, as {@link Rect} makes it
     * @param height the height; zero if negative or NaN, as {@link Rect} makes it
     * @return a rectangle with these edges
     */
    static Rect reuse(final Rect cached, final float x, final float y, final float width, final float height) {
        final float w = width > 0f ? width : 0f;
        final float h = height > 0f ? height : 0f;
        if (cached != null && cached.x() == x && cached.y() == y && cached.width() == w && cached.height() == h) {
            return cached;
        }
        return new Rect(x, y, w, h);
    }
}
