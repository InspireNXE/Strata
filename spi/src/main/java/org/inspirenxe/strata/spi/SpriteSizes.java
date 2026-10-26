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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.Optional;

/**
 * The sizes of the sprites a host draws, in UI units, so that an image showing a sprite can take the sprite's size
 * until the developer sets one. An adapter gives a host its sizes with {@link ScreenHost#setSpriteSizes} or
 * {@link ScreenStack#setSpriteSizes}; a host without them sizes such images as if every sprite were unknown.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Return the size the sprite is drawn at by default: for sprite images drawn one image pixel to a unit, the
 *     plain image's width and height in pixels, not those of an image meant for a denser display.</li>
 *     <li>Return empty for a sprite the engine does not have or cannot size yet, such as one whose image has not
 *     loaded. Strata then uses 16 by 16. Never return null, and never a size that is negative or not finite.</li>
 *     <li>Core calls it on the UI thread while it measures, so keep it cheap after the first call for a sprite, as a
 *     sprite set that caches its images is. It may load the image the first time.</li>
 *     <li>When the sprites change, as after a reload, give the host the sizes again so that images measure again.</li>
 * </ul>
 */
@FunctionalInterface
public interface SpriteSizes {
    /**
     * Sizes that know no sprite.
     */
    SpriteSizes NONE = sprite -> Optional.empty();

    /**
     * Returns a sprite's size.
     *
     * @param sprite the sprite
     * @return its size in UI units, or empty when the engine does not know it
     */
    Optional<Size> size(SpriteId sprite);
}
