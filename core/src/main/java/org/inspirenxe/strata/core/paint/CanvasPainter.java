package org.inspirenxe.strata.core.paint;

import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.spi.Canvas;

/**
 * The developer-facing {@link Painter} over an engine's {@link Canvas}. Scoped clip and translation calls keep the
 * canvas stacks balanced even if the callback throws. Reused across frames.
 */
public final class CanvasPainter implements Painter {
    private Canvas canvas;

    public CanvasPainter begin(final Canvas canvas) {
        this.canvas = canvas;
        return this;
    }

    public Canvas canvas() {
        return this.canvas;
    }

    @Override
    public void fill(final Rect rect, final int argb) {
        this.canvas.fill(rect, argb);
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        this.canvas.fillGradient(rect, topArgb, bottomArgb);
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        this.canvas.sprite(sprite, rect, tint);
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        this.canvas.nineSlice(sprite, rect, border, tint);
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        this.canvas.text(span, x, y);
    }

    @Override
    public void clipped(final Rect rect, final Runnable draw) {
        this.canvas.pushClip(rect);
        try {
            draw.run();
        } finally {
            this.canvas.popClip();
        }
    }

    @Override
    public void translated(final float dx, final float dy, final Runnable draw) {
        this.canvas.pushTranslation(dx, dy);
        try {
            draw.run();
        } finally {
            this.canvas.popTransform();
        }
    }
}
