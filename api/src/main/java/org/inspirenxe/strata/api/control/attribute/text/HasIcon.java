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
package org.inspirenxe.strata.api.control.attribute.text;

import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Optional;

/**
 * A control or entry that shows an icon beside its caption: a sprite, or an engine {@link Visual}, drawn at a size
 * the caller gives. {@link org.inspirenxe.strata.api.control.button.Button},
 * {@link org.inspirenxe.strata.api.control.button.DropDownButton} and
 * {@link org.inspirenxe.strata.api.control.pane.NavigationItem} have it; each says where it draws the icon.
 *
 * <p>The icon is a sprite, a visual or nothing, and setting one replaces the other. While disabled, a control or
 * entry draws its icon tinted gray. The attribute has no events and does not extend
 * {@link org.inspirenxe.strata.api.event.EventTarget}, so entries, which are not event targets, can have it.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the control's or entry's own type, which the setters return
 */
public interface HasIcon<S extends HasIcon<S>> {
    /**
     * Returns the icon's sprite.
     *
     * @return the sprite, or empty while the icon is a visual or there is none
     */
    Optional<SpriteId> iconSprite();

    /**
     * Returns the icon's visual.
     *
     * @return the visual, or empty while the icon is a sprite or there is none
     */
    Optional<Visual> iconVisual();

    /**
     * Shows a sprite as the icon, replacing any icon. The size stays what it was, 16 by 16 by default. A new layout
     * runs.
     *
     * @param sprite the sprite; see {@link SpriteId} for where engines look for it
     * @return this
     */
    S setIcon(SpriteId sprite);

    /**
     * Shows a visual as the icon, replacing any icon, at the visual's natural size, which {@link #iconWidth()} and
     * {@link #iconHeight()} then return. A new layout runs.
     *
     * @param visual the visual
     * @return this
     */
    S setIcon(Visual visual);

    /**
     * Removes the icon. The size stays for the next one. A new layout runs.
     *
     * @return this
     */
    S clearIcon();

    /**
     * Returns the width the icon is drawn at.
     *
     * @return the width in UI units; 16 by default
     */
    float iconWidth();

    /**
     * Returns the height the icon is drawn at.
     *
     * @return the height in UI units; 16 by default
     */
    float iconHeight();

    /**
     * Sets the size the icon is drawn at. Strata cannot ask an engine for a sprite's size, so the caller gives it.
     * A new layout runs.
     *
     * @param width the width in UI units, finite and not negative
     * @param height the height in UI units, finite and not negative
     * @return this
     * @throws IllegalArgumentException if either size is negative, infinite or not a number
     */
    S setIconSize(float width, float height);
}
