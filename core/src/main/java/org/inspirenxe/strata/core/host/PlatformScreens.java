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
package org.inspirenxe.strata.core.host;

import org.inspirenxe.strata.api.screen.ScreenController;

/**
 * Holds the platform's screen controller for the process: the one {@code StrataCore.setScreenController} was given,
 * which {@code Strata.screens()} returns and which handles of hosts without an opener of their own open screens
 * with. It may be set and read from any thread.
 */
public final class PlatformScreens {
    /**
     * What {@link #get()} throws with when no controller is set.
     */
    public static final String MISSING = "No screen controller is set. The engine adapter hands one to Strata when"
            + " it loads; on Swing, mark the panel that opens screens with StrataPanel.setDefault()";

    private static volatile ScreenController controller;

    private PlatformScreens() {
    }

    /**
     * Returns the platform's controller.
     *
     * @return the controller
     * @throws IllegalStateException if none is set, with {@link #MISSING} as the message
     */
    public static ScreenController get() {
        final ScreenController current = PlatformScreens.controller;
        if (current == null) {
            throw new IllegalStateException(MISSING);
        }
        return current;
    }

    /**
     * Sets the platform's controller, replacing any earlier one.
     *
     * @param controller the controller, or {@code null} to remove it
     */
    public static void set(final ScreenController controller) {
        PlatformScreens.controller = controller;
    }
}
