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

/**
 * The kind of device an input came from, the {@link CauseKeys#DEVICE} context value. Hosts put the device the
 * adapter named for the input, and {@link #MOUSE} for pointer input and {@link #KEYBOARD} for keys and typed
 * characters when it named none; the others are for adapters whose engine tells devices apart.
 */
public enum Device {
    /** A mouse, or a pointer the engine does not tell apart from one. */
    MOUSE,
    /** A keyboard, including key repeats Strata makes itself. */
    KEYBOARD,
    /** A game controller. */
    CONTROLLER,
    /** A touch screen. Touches do not hover. */
    TOUCH,
    /** A pen or stylus, which hovers as a mouse does and may report its pressure. */
    PEN,
    /** A screen reader or other assistive technology acting for the user. */
    NARRATOR
}
