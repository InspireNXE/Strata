package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;

/**
 * The core {@link Image}. Its desired size is the image size, 16 by 16 by default or the visual's natural size. It
 * draws the sprite or visual stretched over its bounds, or, with {@link Image.Scaling#FIT}, at the largest size with
 * the image size's aspect ratio that fits, centered. An image size of zero on either axis draws nothing with
 * {@code FIT}.
 */
public final class ImageImpl extends CustomComponent<Image> implements Image {
    private SpriteId sprite;
    private Visual visual;
    private int tint;
    private float imageWidth;
    private float imageHeight;
    private Scaling scaling;

    /**
     * Creates an image that draws nothing, 16 by 16 units.
     *
     * @param ui the factory that owns the image
     */
    public ImageImpl(final ComponentFactory ui) {
        super(ui);
        this.tint = 0xFFFFFFFF;
        this.imageWidth = 16f;
        this.imageHeight = 16f;
        this.scaling = Scaling.STRETCH;
    }

    /**
     * Creates an image.
     *
     * @param ui the factory that owns the image
     * @param sprite the sprite to draw
     */
    public ImageImpl(final ComponentFactory ui, final SpriteId sprite) {
        super(ui);
        this.sprite = Objects.requireNonNull(sprite, "sprite");
        this.tint = 0xFFFFFFFF;
        this.imageWidth = 16f;
        this.imageHeight = 16f;
        this.scaling = Scaling.STRETCH;
    }

    /**
     * Creates an image of a visual, at the visual's natural size.
     *
     * @param ui the factory that owns the image
     * @param visual the visual to draw
     */
    public ImageImpl(final ComponentFactory ui, final Visual visual) {
        super(ui);
        this.visual = Objects.requireNonNull(visual, "visual");
        this.tint = 0xFFFFFFFF;
        this.imageWidth = visual.size().width();
        this.imageHeight = visual.size().height();
        this.scaling = Scaling.STRETCH;
    }

    @Override
    public SpriteId sprite() {
        return this.sprite;
    }

    @Override
    public Image setSprite(final SpriteId sprite) {
        this.sprite = Objects.requireNonNull(sprite, "sprite");
        this.visual = null;
        return this;
    }

    @Override
    public Visual visual() {
        return this.visual;
    }

    @Override
    public Image setVisual(final Visual visual) {
        this.visual = Objects.requireNonNull(visual, "visual");
        this.sprite = null;
        return this.setImageSize(visual.size().width(), visual.size().height());
    }

    @Override
    public int tint() {
        return this.tint;
    }

    @Override
    public Image setTint(final int tint) {
        this.tint = tint;
        return this;
    }

    @Override
    public float imageWidth() {
        return this.imageWidth;
    }

    @Override
    public float imageHeight() {
        return this.imageHeight;
    }

    @Override
    public Image setImageSize(final float width, final float height) {
        if (!(width >= 0f) || !(height >= 0f) || Float.isInfinite(width) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("Image size must be finite and not negative: " + width + " x " + height);
        }
        this.imageWidth = width;
        this.imageHeight = height;
        this.invalidateLayout();
        return this;
    }

    @Override
    public Scaling scaling() {
        return this.scaling;
    }

    @Override
    public Image setScaling(final Scaling scaling) {
        this.scaling = Objects.requireNonNull(scaling, "scaling");
        return this;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return new Size(this.imageWidth, this.imageHeight);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.scaling == Scaling.STRETCH) {
            this.draw(painter, bounds);
            return;
        }
        if (this.imageWidth <= 0f || this.imageHeight <= 0f) {
            return;
        }
        final float scale = Math.min(bounds.width() / this.imageWidth, bounds.height() / this.imageHeight);
        final float width = this.imageWidth * scale;
        final float height = this.imageHeight * scale;
        this.draw(painter, new Rect(bounds.x() + (bounds.width() - width) * 0.5f,
                bounds.y() + (bounds.height() - height) * 0.5f, width, height));
    }

    private void draw(final Painter painter, final Rect rect) {
        if (this.visual != null) {
            painter.visual(this.visual, rect, this.tint);
        } else if (this.sprite != null) {
            painter.sprite(this.sprite, rect, this.tint);
        }
    }

    @Override
    protected Object snapshotState() {
        return new Image.State(this.sprite, this.visual, this.tint, this.imageWidth, this.imageHeight, this.scaling);
    }
}
