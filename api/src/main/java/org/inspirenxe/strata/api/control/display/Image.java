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
package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;

/**
 * A sprite or an engine {@link Visual} drawn at a size you choose, such as an icon, a picture or a product photo.
 *
 * <p>Strata cannot ask the engine how big a sprite's image is, so the image's size in UI units is set with
 * {@link #setImageSize}; it is 16 by 16 by default. A visual brings its own natural size, which becomes the image
 * size when the visual is set. That size is the desired size, and the aspect ratio {@link Scaling#FIT} keeps. The
 * sprite is looked up as described in {@link SpriteId}; a missing sprite, or a visual the engine cannot draw, draws
 * the engine's placeholder. An image shows either a sprite or a visual, never both.</p>
 *
 * <p>An image is not focusable, reads no theme keys and has no default behavior for input, so clicks pass through to
 * its ancestors. A disabled image draws the same as an enabled one; tint it to show the difference. It fires no
 * events.</p>
 *
 * <pre>{@code
 * Image icon = factory.image(SpriteId.of("example", "icon/gear")).setImageSize(9, 9);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#image()},
 * {@link org.inspirenxe.strata.api.ComponentFactory#image(SpriteId)},
 * {@link org.inspirenxe.strata.api.ComponentFactory#image(Visual)} or
 * {@link org.inspirenxe.strata.api.ComponentFactory#image(Object)}. Do not implement this interface.</p>
 */
public interface Image extends Component<Image>, HasCursor<Image>, HasTooltip<Image> {
    /**
     * The value shown, as {@link #value()} and {@link #setValue} say, null for none. Binds one way.
     */
    PropertyKey<Image, Object> VALUE = PropertyKey.of("image.value", Image::value, Image::setValue);
    /**
     * The sprite, as {@link #sprite()} and {@link #setSprite} say. Binds one way. A null value throws as
     * {@code setSprite} does.
     */
    PropertyKey<Image, SpriteId> SPRITE = PropertyKey.of("image.sprite", Image::sprite, Image::setSprite);

    /**
     * Returns the sprite.
     *
     * @return the sprite drawn, or {@code null} while the image shows a visual or nothing
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
     * @return the visual drawn, or {@code null} while the image shows a sprite or nothing
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
     * Returns the value shown.
     *
     * @return the value set with {@link #setValue}, or {@code null} if none is
     */
    Object value();

    /**
     * Shows a value through its {@linkplain org.inspirenxe.strata.api.appearance.Appearances appearance}: its icon, at
     * the icon's natural size, or nothing if it has none, and its tooltip, asked anew each time the pointer arrives.
     * Setting a tooltip afterwards replaces the value's until the next {@code setValue}. A value with a tooltip
     * replaces the tooltip shown; one without, and {@code null}, remove the previous value's tooltip and keep a
     * tooltip set with {@code setTooltip}. Setting a sprite or a visual clears the value, and its tooltip the same
     * way.
     *
     * @param value the value, or {@code null} to show nothing
     * @return this image
     */
    Image setValue(Object value);

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
         * Checks that at most one of the sprite and the visual is set, and the scaling.
         *
         * @throws IllegalArgumentException if both the sprite and the visual are set
         */
        public State {
            if (sprite != null && visual != null) {
                throw new IllegalArgumentException("At most one of sprite and visual may be set");
            }
            Objects.requireNonNull(scaling, "scaling");
        }
    }
}
