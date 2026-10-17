package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;

/**
 * A sprite or an engine {@link Visual} drawn at a size you choose, such as an icon, a picture or a game item.
 *
 * <p>Strata cannot ask the engine how big a sprite's image is, so the image's size in UI units is set with
 * {@link #setImageSize}; it is 16 by 16 by default. A visual brings its own natural size, which becomes the image
 * size when the visual is set. That size is the desired size, and the aspect ratio {@link Scaling#FIT} keeps. The
 * sprite is looked up as described in {@link SpriteId}; a missing sprite, or a visual the engine cannot draw, draws
 * the engine's placeholder. An image shows either a sprite or a visual, never both.</p>
 *
 * <p>An image is not focusable, reads no theme keys and has no default behavior for input, so clicks pass through to
 * its ancestors. A disabled image draws the same as an enabled one; tint it to show the difference.</p>
 *
 * <pre>{@code
 * Image icon = factory.image(SpriteId.of("examplemod", "icon/gear")).setImageSize(9, 9);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#image(SpriteId)} or
 * {@link org.inspirenxe.strata.api.ComponentFactory#image(Visual)}. Do not implement this interface.</p>
 */
public interface Image extends Component<Image> {

    /**
     * Returns the sprite.
     *
     * @return the sprite drawn, or {@code null} while the image shows a visual
     */
    SpriteId sprite();

    /**
     * Shows a sprite, replacing the sprite or visual shown before. Does not change the size.
     *
     * @param sprite the sprite to draw
     * @return this image
     */
    Image setSprite(SpriteId sprite);

    /**
     * Returns the visual.
     *
     * @return the visual drawn, or {@code null} while the image shows a sprite
     */
    Visual visual();

    /**
     * Shows an engine visual, replacing the sprite or visual shown before, and sets the image size to the visual's
     * natural size.
     *
     * @param visual the visual to draw
     * @return this image
     */
    Image setVisual(Visual visual);

    /**
     * Returns the tint.
     *
     * @return the ARGB color multiplied with the sprite; {@code 0xFFFFFFFF} by default
     */
    int tint();

    /**
     * Sets an ARGB color to multiply with the sprite or visual. Opaque white leaves it unchanged, and lower alpha
     * fades it. Engines that cannot tint a visual ignore it there.
     *
     * @param tint the tint
     * @return this image
     */
    Image setTint(int tint);

    /**
     * Returns the width of the image, in UI units.
     *
     * @return the image width; 16 by default
     */
    float imageWidth();

    /**
     * Returns the height of the image, in UI units.
     *
     * @return the image height; 16 by default
     */
    float imageHeight();

    /**
     * Sets the size the image is drawn at when nothing stretches it, which is also its desired size. A new layout
     * runs.
     *
     * @param width the width in UI units
     * @param height the height in UI units
     * @return this image
     * @throws IllegalArgumentException if either value is negative or not finite
     */
    Image setImageSize(float width, float height);

    /**
     * Returns how the sprite fills the image's bounds.
     *
     * @return the scaling; {@link Scaling#STRETCH} by default
     */
    Scaling scaling();

    /**
     * Sets how the sprite fills the image's bounds when they differ from the image size, for example when the image
     * is stretched.
     *
     * @param scaling the scaling
     * @return this image
     */
    Image setScaling(Scaling scaling);

    /**
     * How an {@link Image} draws its sprite in bounds of another size.
     */
    enum Scaling {
        /** Stretch the sprite over the whole bounds, changing its aspect ratio if the bounds have another. */
        STRETCH,
        /**
         * Draw the sprite as large as fits inside the bounds with the image size's aspect ratio, centered. Space
         * left over on one axis stays empty.
         */
        FIT
    }

    /**
     * An image's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param sprite the {@linkplain #sprite() sprite}, or null while a visual is shown
     * @param visual the {@linkplain #visual() visual}, or null while a sprite is shown
     * @param tint the {@linkplain #tint() tint}
     * @param imageWidth the {@linkplain #imageWidth() image width}
     * @param imageHeight the {@linkplain #imageHeight() image height}
     * @param scaling the {@linkplain #scaling() scaling}; never null
     */
    record State(SpriteId sprite, Visual visual, int tint, float imageWidth, float imageHeight, Scaling scaling) {

        /**
         * Checks that exactly one of the sprite and the visual is set, and the scaling.
         *
         * @throws IllegalArgumentException if both or neither of the sprite and the visual are set
         */
        public State {
            if ((sprite == null) == (visual == null)) {
                throw new IllegalArgumentException("Exactly one of sprite and visual must be set");
            }
            Objects.requireNonNull(scaling, "scaling");
        }
    }
}
