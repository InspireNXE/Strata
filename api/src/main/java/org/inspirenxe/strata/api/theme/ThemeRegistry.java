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
package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.event.Subscription;

/**
 * Themes and overrides added in code, for every host in the process. Get it from
 * {@link org.inspirenxe.strata.api.Strata#themes()}. Do not implement this interface.
 *
 * <pre>{@code
 * // Every screen that shows the engine's theme gets these buttons
 * Subscription buttons = Strata.themes().override(engineTheme, ThemeOverrides.of()
 *         .with(Button.STYLE_NORMAL, Paint.nineSlice(SpriteId.of("mymod", "button"), Insets.of(3f))));
 *
 * // A theme of the mod's own that follows packs for every key it leaves alone
 * Strata.themes().define(ThemeId.of("mymod", "dark"), engineTheme, ThemeOverrides.of()
 *         .with(Window.STYLE_BACKGROUND, Paint.solid(0xF0101010)));
 * }</pre>
 *
 * <p>Nothing here is written to a theme, a theme file or the engine's packs. The keys apply each time a theme
 * resolves, and once a subscription is closed, the keys it set resolve as they did before it. Every change here
 * makes each host that shows a {@link ThemeRef} resolve it again at the start of its next layout or render, so
 * open screens pick the change up. Every method may be called from any thread.</p>
 */
public interface ThemeRegistry {

    /**
     * Sets keys on a theme for every screen that uses it, until the subscription is closed. The keys apply above
     * the base and every theme file for the id, so code has the final say over packs. Overrides for one id apply
     * in the order they were added, and a {@link ThemeRef}'s own overrides apply after all of them. The keys
     * report the layer {@code code} from {@link Theme#layer}.
     *
     * <p>The id need not resolve yet; the overrides wait until it does.</p>
     *
     * @param id the theme to change
     * @param overrides the keys to set
     * @return a subscription that removes the overrides when closed
     */
    Subscription override(ThemeId id, ThemeOverrides overrides);

    /**
     * Defines a theme that starts from another and sets the overrides' keys, until the subscription is closed. The
     * defined theme resolves as one whose base is its parent, resolved in full, with the overrides above it. Theme
     * files for the id still apply on top, as for any theme, but their {@code parent} is ignored. The overrides'
     * keys report the layer {@code code} from {@link Theme#layer}.
     *
     * <p>An id that the engine adapter already gives a base, in code or as base files, cannot be defined. That is
     * found when the theme resolves, which then throws {@link ThemeException}.</p>
     *
     * @param id the theme to define
     * @param parent the theme it starts from
     * @param overrides the keys it sets on the parent
     * @return a subscription that removes the definition when closed, after which the id can be defined again
     * @throws ThemeException if the id is already defined in code and that definition is still open, or is
     *     {@link Themes#FLAT}, which Strata builds in code
     */
    Subscription define(ThemeId id, ThemeId parent, ThemeOverrides overrides);
}
