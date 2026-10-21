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
package org.inspirenxe.strata.api.event.cause;

import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.screen.Screen;

/**
 * The context keys Strata puts. A host puts {@link #ORIGIN}, {@link #DEVICE}, {@link #MODIFIERS} and {@link #SCREEN}
 * for every input it dispatches, and {@link #BUTTON} and {@link #CLICKS} for pointer presses and releases, so every
 * event one input causes shares them.
 */
public final class CauseKeys {

    /**
     * Who caused the change. Read it through {@link Cause#origin()} or {@code Event.origin()}, which give
     * {@link Origin#CODE} when nothing was put.
     */
    public static final CauseKey<Origin> ORIGIN = CauseKey.of("strata:origin", Origin.class);

    /**
     * The kind of device the input came from.
     */
    public static final CauseKey<Device> DEVICE = CauseKey.of("strata:device", Device.class);

    /**
     * The mouse button of a pointer press or release, and of the clicks and drags it leads to.
     */
    public static final CauseKey<MouseButton> BUTTON = CauseKey.of("strata:button", MouseButton.class);

    /**
     * The modifiers held when the input arrived.
     */
    public static final CauseKey<Modifiers> MODIFIERS = CauseKey.of("strata:modifiers", Modifiers.class);

    /**
     * The click count of a pointer press or release: 1 for a single click, 2 for a double click.
     */
    public static final CauseKey<Integer> CLICKS = CauseKey.of("strata:clicks", Integer.class);

    /**
     * The screen showing in the host the input arrived at. Absent for a host with no screen open, such as one whose
     * content was set directly.
     */
    public static final CauseKey<Screen> SCREEN = CauseKey.of("strata:screen", Screen.class);

    private CauseKeys() {
    }
}
