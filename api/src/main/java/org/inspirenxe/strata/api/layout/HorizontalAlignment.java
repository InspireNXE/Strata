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
 * Where a component sits across the width of the slot its parent gives it. See
 * {@link org.inspirenxe.strata.api.component.Component#setAlignment}.
 */
public enum HorizontalAlignment {
    /** Against the slot's left edge (after the margin), at the desired width. */
    LEFT,
    /** Centered in the slot, at the desired width. */
    CENTER,
    /** Against the slot's right edge (before the margin), at the desired width. */
    RIGHT,
    /** As wide as the slot minus the margin, unless the component has a fixed width; the minimum and maximum still apply. */
    STRETCH
}
