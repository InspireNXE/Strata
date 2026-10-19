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
 * A sprite placed in a line of text, such as an icon in a sentence. Reserved for rich text; no built-in control
 * draws sprite spans yet.
 *
 * @param sprite the sprite to draw
 * @param width the width it takes in the line, in UI units
 * @param height the height it takes, in UI units
 * @param tint an ARGB color multiplied with the sprite; opaque white leaves it unchanged
 */
public record SpriteSpan(SpriteId sprite, float width, float height, int tint) implements InlineElement {
    /**
     * Checks the sprite.
     */
    public SpriteSpan {
        Objects.requireNonNull(sprite, "sprite");
    }

    /**
     * Creates an untinted sprite span.
     *
     * @param sprite the sprite to draw
     * @param width the width it takes in the line
     * @param height the height it takes
     */
    public SpriteSpan(final SpriteId sprite, final float width, final float height) {
        this(sprite, width, height, 0xFFFFFFFF);
    }
}
