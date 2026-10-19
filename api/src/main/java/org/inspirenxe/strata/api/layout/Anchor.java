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
 * A pair of alignments under one name, as MalisisCore's anchors were. {@code setAnchor(Anchor.BOTTOM_RIGHT)} is the
 * same as {@code setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM)}.
 */
public enum Anchor {
    /** Left and top. */
    TOP_LEFT(HorizontalAlignment.LEFT, VerticalAlignment.TOP),
    /** Centered horizontally, at the top. */
    TOP_CENTER(HorizontalAlignment.CENTER, VerticalAlignment.TOP),
    /** Right and top. */
    TOP_RIGHT(HorizontalAlignment.RIGHT, VerticalAlignment.TOP),
    /** Left, centered vertically. */
    MIDDLE_LEFT(HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE),
    /** Centered both ways. */
    CENTER(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE),
    /** Right, centered vertically. */
    MIDDLE_RIGHT(HorizontalAlignment.RIGHT, VerticalAlignment.MIDDLE),
    /** Left and bottom. */
    BOTTOM_LEFT(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM),
    /** Centered horizontally, at the bottom. */
    BOTTOM_CENTER(HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM),
    /** Right and bottom. */
    BOTTOM_RIGHT(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM),
    /** Stretched both ways to fill the slot. */
    FILL(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);

    private final HorizontalAlignment horizontal;
    private final VerticalAlignment vertical;

    Anchor(final HorizontalAlignment horizontal, final VerticalAlignment vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    /**
     * Returns the horizontal half of the anchor.
     *
     * @return the horizontal alignment
     */
    public HorizontalAlignment horizontal() {
        return this.horizontal;
    }

    /**
     * Returns the vertical half of the anchor.
     *
     * @return the vertical alignment
     */
    public VerticalAlignment vertical() {
        return this.vertical;
    }
}
