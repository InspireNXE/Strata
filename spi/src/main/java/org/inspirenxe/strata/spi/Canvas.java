package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;

/**
 * What an engine draws for Strata. Calls may be recorded and replayed later, so an implementation is free to defer
 * them. Clip and transform pushes are always balanced by the caller within one frame.
 *
 * <p>Colors are ARGB. A color with alpha 0 draws nothing.</p>
 */
public interface Canvas {

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

    void pushClip(Rect rect);

    void popClip();

    void pushTranslation(float dx, float dy);

    void popTransform();

    /**
     * Starts a new layer: everything drawn after this call appears above everything drawn before it.
     */
    void nextLayer();

    void requestCursor(CursorKind cursor);
}
