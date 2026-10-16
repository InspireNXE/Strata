package org.inspirenxe.strata.api.text;

import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.Objects;

/**
 * A sprite drawn inline with text.
 */
public record SpriteSpan(SpriteId sprite, float width, float height, int tint) implements InlineElement {
    public SpriteSpan {
        Objects.requireNonNull(sprite, "sprite");
    }

    public SpriteSpan(final SpriteId sprite, final float width, final float height) {
        this(sprite, width, height, 0xFFFFFFFF);
    }
}
