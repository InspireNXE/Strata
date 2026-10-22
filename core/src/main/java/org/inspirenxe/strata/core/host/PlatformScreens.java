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

import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.core.frame.UiThreadImpl;
import org.inspirenxe.strata.spi.Platform;

import java.util.Objects;

/**
 * Holds the platform's screen controller for the process: the one {@code StrataCore.setScreenController} was given,
 * which {@code Strata.screens()} returns and which handles of hosts without an opener of their own open screens
 * with, and the UI thread of the controller's platform, which {@code Strata.uiThread()} returns. Both may be set and
 * read from any thread.
 */
public final class PlatformScreens {
    /**
     * What {@link #get()} throws with when no controller is set.
     */
    public static final String MISSING = "No screen controller is set. The engine adapter hands one to Strata when"
            + " it loads; on Swing, mark the panel that opens screens with StrataPanel.setDefault()";

    /**
     * What {@link #uiThread()} throws with when no platform is known.
     */
    public static final String NO_UI_THREAD = "No platform is known for Strata.uiThread(). The engine adapter names"
            + " it when it hands over its screen controller; on Swing, mark the panel that opens screens with"
            + " StrataPanel.setDefault()";

    private static volatile Current current = new Current(null, null);

    private PlatformScreens() {
    }

    /**
     * Returns the platform's controller.
     *
     * @return the controller
     * @throws IllegalStateException if none is set, with {@link #MISSING} as the message
     */
    public static ScreenController get() {
        final ScreenController controller = PlatformScreens.current.controller();
        if (controller == null) {
            throw new IllegalStateException(MISSING);
        }
        return controller;
    }

    /**
     * Returns the UI thread of the controller's platform.
     *
     * @return the UI thread
     * @throws IllegalStateException if no controller is set, or its platform is not known, with
     *     {@link #NO_UI_THREAD} as the message
     */
    public static UiThread uiThread() {
        final UiThread uiThread = PlatformScreens.current.uiThread();
        if (uiThread == null) {
            throw new IllegalStateException(NO_UI_THREAD);
        }
        return uiThread;
    }

    /**
     * Sets the platform's controller, replacing any earlier one. When the controller is core's own
     * {@link ScreenStackImpl}, its platform's UI thread becomes the one {@link #uiThread()} returns; for any other
     * controller no UI thread is known.
     *
     * @param controller the controller, or {@code null} to remove it
     */
    public static void set(final ScreenController controller) {
        final UiThread uiThread = switch (controller) {
            case final ScreenStackImpl stack -> stack.uiThread();
            case null, default -> null;
        };
        PlatformScreens.current = new Current(controller, uiThread);
    }

    /**
     * Sets the platform's controller and the platform it opens screens on, replacing any earlier ones.
     *
     * @param controller the controller
     * @param platform the platform whose UI thread {@link #uiThread()} returns
     */
    public static void set(final ScreenController controller, final Platform platform) {
        PlatformScreens.current = new Current(Objects.requireNonNull(controller, "controller"),
                UiThreadImpl.of(platform));
    }

    /**
     * The controller and its platform's UI thread, swapped together.
     *
     * @param controller the controller, or null
     * @param uiThread the UI thread, or null when not known
     */
    private record Current(ScreenController controller, UiThread uiThread) {
    }
}
