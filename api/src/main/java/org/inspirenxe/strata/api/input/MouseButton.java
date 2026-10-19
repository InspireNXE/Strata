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
package org.inspirenxe.strata.api.input;

/**
 * Mouse buttons, independent of any engine. Adapters map their engine's button numbers to these constants and map
 * anything else to {@link #UNKNOWN}.
 */
public enum MouseButton {
    /** The primary button, usually the left one. */
    LEFT,
    /** The secondary button, usually the right one. */
    RIGHT,
    /** The middle button or wheel click. */
    MIDDLE,
    /** The "back" side button (button 4). */
    BACK,
    /** The "forward" side button (button 5). */
    FORWARD,
    /** A button the adapter could not map. */
    UNKNOWN
}
