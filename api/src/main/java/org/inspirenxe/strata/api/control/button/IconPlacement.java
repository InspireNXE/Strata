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
package org.inspirenxe.strata.api.control.button;

/**
 * Where a button's icon sits beside its caption: before it, after it, above it or below it. The icon and the
 * caption are {@code 4} units apart and centered together in the button less its padding. A button with an icon and
 * no caption, or a caption and no icon, looks the same whatever the placement.
 */
public enum IconPlacement {
    /**
     * The icon comes before the caption, at its left. A new button's placement.
     */
    START,
    /**
     * The icon comes after the caption, at its right. A caption too wide for the button is cut before the icon, so
     * the icon stays in view.
     */
    END,
    /**
     * The icon sits above the caption, each centered across the button, as on a ribbon or a toolbar.
     */
    TOP,
    /**
     * The icon sits below the caption, each centered across the button.
     */
    BOTTOM;

    /**
     * Returns whether the icon and the caption are stacked, one above the other.
     *
     * @return true for {@link #TOP} and {@link #BOTTOM}
     */
    public boolean isVertical() {
        return this == TOP || this == BOTTOM;
    }
}
