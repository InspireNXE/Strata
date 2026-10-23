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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.geometry.Rect;

/**
 * One rectangle a control draws, kept from one frame to the next. Asking for the same place and size again returns
 * the rectangle made before, so painting allocates only when what is drawn moves or changes size.
 */
public final class RectCache {
    private Rect rect = Rect.ZERO;

    /**
     * Creates a cache holding {@link Rect#ZERO}.
     */
    public RectCache() {
    }

    /**
     * Returns a rectangle with a place and size, the one made last time if it matches. A negative width or height
     * counts as 0, as {@link Rect} clamps it.
     *
     * @param x the left edge
     * @param y the top edge
     * @param width the width
     * @param height the height
     * @return the rectangle
     */
    public Rect at(final float x, final float y, final float width, final float height) {
        final Rect last = this.rect;
        if (last.x() != x || last.y() != y || last.width() != Math.max(0f, width)
                || last.height() != Math.max(0f, height)) {
            this.rect = new Rect(x, y, width, height);
        }
        return this.rect;
    }
}
