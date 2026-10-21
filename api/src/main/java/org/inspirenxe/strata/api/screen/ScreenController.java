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
package org.inspirenxe.strata.api.screen;

import org.inspirenxe.strata.api.theme.ThemeRef;

/**
 * Opens screens. {@link org.inspirenxe.strata.api.Strata#screens()} returns the platform's controller, so code that
 * opens a screen names no engine adapter:
 *
 * <pre>{@code
 * Strata.screens().open(new ExchangeScreen(exchange));
 * }</pre>
 *
 * <p>Each engine adapter decides where a screen opens. An engine with screens of its own shows it as one of them,
 * and a screen stack pushes it. The adapter's documentation says which.</p>
 *
 * <p>Strata and its engine adapters implement this interface. Do not implement it.</p>
 */
public interface ScreenController {

    /**
     * Opens a screen in the platform's default theme, the one {@link ThemeRef#platform()} names. The same as
     * {@code open(screen, ThemeRef.platform())}.
     *
     * @param screen the screen to open
     * @return the handle of the new opening; its methods do nothing once the screen closes
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme cannot be resolved or lacks keys the
     *     screen's tree needs, on platforms that build the screen at once
     */
    default ScreenHandle open(final Screen screen) {
        return this.open(screen, ThemeRef.platform());
    }

    /**
     * Opens a screen in a theme. The screen follows the theme's packs and overrides as long as it is open.
     *
     * @param screen the screen to open
     * @param theme the theme for its components
     * @return the handle of the new opening; its methods do nothing once the screen closes
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme cannot be resolved or lacks keys the
     *     screen's tree needs, on platforms that build the screen at once
     */
    ScreenHandle open(Screen screen, ThemeRef theme);
}
