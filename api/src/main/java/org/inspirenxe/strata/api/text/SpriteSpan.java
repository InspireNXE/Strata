package org.inspirenxe.strata.api.text;

import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.Objects;

/**
 * A sprite placed in a line of text, such as an icon in a sentence. Reserved for rich text; no built-in widget
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
     *
     * @throws NullPointerException if {@code sprite} is null
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
