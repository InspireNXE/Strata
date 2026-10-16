package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;

/**
 * What a component draws with. Colors are ARGB; a color with alpha 0 draws nothing.
 *
 * <p>Clipping and translation are scoped to a callback, so a component can never leave them unbalanced.</p>
 */
public interface Painter {

    void fill(Rect rect, int argb);

    void fillGradient(Rect rect, int topArgb, int bottomArgb);

    void sprite(SpriteId sprite, Rect rect, int tint);

    /**
     * Draws a sprite stretched over {@code rect} with its borders kept at their native size. An engine that knows
     * the sprite's slicing natively may ignore {@code border}.
     */
    void nineSlice(SpriteId sprite, Rect rect, Insets border, int tint);

    /**
     * Draws a span with its top-left corner at the given point.
     */
    void text(TextSpan span, float x, float y);

    /**
     * Runs {@code draw} with drawing limited to {@code rect}.
     */
    void clipped(Rect rect, Runnable draw);

    /**
     * Runs {@code draw} with everything shifted by the given offset.
     */
    void translated(float dx, float dy, Runnable draw);
}
