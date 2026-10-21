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

import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.Objects;

/**
 * A sprite in a line of text, drawn untinted. A width and height of 0 make a square as tall as the line height of
 * the span's style, so the sprite sits in the text like a character.
 *
 * @param sprite the sprite to draw
 * @param width the width it takes in the line, in UI units, or 0 with a height of 0 to fit the line
 * @param height the height it takes, in UI units, or 0 with a width of 0 to fit the line
 */
public record InlineSprite(SpriteId sprite, float width, float height) implements InlineElement {
    /**
     * Checks the arguments.
     *
     * @throws IllegalArgumentException if the size is not finite, either side is negative, or only one side is 0
     */
    public InlineSprite {
        Objects.requireNonNull(sprite, "sprite");
        final boolean fits = width == 0f && height == 0f;
        if (!fits && (!(width > 0f) || !(height > 0f) || Float.isInfinite(width) || Float.isInfinite(height))) {
            throw new IllegalArgumentException("Inline sprite size must be positive and finite, or 0 by 0: " + width
                    + " x " + height);
        }
    }

    /**
     * Creates a sprite as tall and wide as the line height of the style it lands in.
     *
     * @param sprite the sprite to draw
     */
    public InlineSprite(final SpriteId sprite) {
        this(sprite, 0f, 0f);
    }

    /**
     * Returns whether the sprite takes its size from the line.
     *
     * @return true when the width and height are 0
     */
    public boolean fitsLine() {
        return this.width == 0f && this.height == 0f;
    }
}
