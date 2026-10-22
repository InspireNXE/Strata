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

import org.inspirenxe.strata.api.event.cause.Device;

import java.util.Objects;

/**
 * Where an input came from: the device, which pointer of it, and how hard it presses. An adapter whose engine tells
 * devices apart passes one with each input through the {@link InputSink} forms that take it; the other forms pass
 * {@link #MOUSE} or {@link #KEYBOARD}.
 *
 * <p>The device goes into the {@code CauseKeys.DEVICE} context of the input's frame. For pointer input, each pointer
 * id has its own presses, drags and click counts, as each mouse button does. Only {@link Device#MOUSE} and
 * {@link Device#PEN} pointers hover, so a touch shows no hover face and no tooltip. A long press of a first touch
 * opens the context menu, and two touches that move together scroll.</p>
 *
 * @param device the device; for keys, {@link Device#KEYBOARD}, {@link Device#CONTROLLER} for a controller's buttons
 *     mapped to keys, or {@link Device#NARRATOR}
 * @param pointerId which pointer of the device: 0 for the mouse and the first touch, and a number of the adapter's
 *     choosing, the same from press to release, for each further touch
 * @param pressure how hard the pointer presses, from 0 to 1; 1 for a device without pressure
 */
public record InputContext(Device device, int pointerId, float pressure) {
    /**
     * The mouse, with no pressure: what the forms without a context pass for pointer input.
     */
    public static final InputContext MOUSE = new InputContext(Device.MOUSE, 0, 1f);
    /**
     * The keyboard: what the forms without a context pass for keys and typed characters.
     */
    public static final InputContext KEYBOARD = new InputContext(Device.KEYBOARD, 0, 1f);

    /**
     * Checks the context.
     *
     * @param device the device
     * @param pointerId which pointer, not negative
     * @param pressure from 0 to 1
     * @throws IllegalArgumentException if the pointer id is negative or the pressure is outside 0 to 1 or NaN
     */
    public InputContext {
        Objects.requireNonNull(device, "device");
        if (pointerId < 0) {
            throw new IllegalArgumentException("A pointer id must not be negative: " + pointerId);
        }
        if (!(pressure >= 0f && pressure <= 1f)) {
            throw new IllegalArgumentException("Pressure must be from 0 to 1: " + pressure);
        }
    }

    /**
     * Returns the first touch, or a touch of another id, at full pressure.
     *
     * @param pointerId which touch
     * @return the context
     * @throws IllegalArgumentException if the pointer id is negative
     */
    public static InputContext touch(final int pointerId) {
        return new InputContext(Device.TOUCH, pointerId, 1f);
    }

    /**
     * Returns a pen at a pressure.
     *
     * @param pressure from 0 to 1
     * @return the context
     * @throws IllegalArgumentException if the pressure is outside 0 to 1 or NaN
     */
    public static InputContext pen(final float pressure) {
        return new InputContext(Device.PEN, 0, pressure);
    }
}
