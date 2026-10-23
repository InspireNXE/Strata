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
package org.inspirenxe.strata.api.animation;

import org.inspirenxe.strata.api.state.PropertyKey;

import java.util.function.DoubleConsumer;

/**
 * Runs the animations of one host. Get it from {@code ComponentFactory.animator()}.
 *
 * <pre>{@code
 * ui.animator().animate(health, Ranged.VALUE, 0.4, 250, Easing.EASE_OUT);
 * ui.animator().run(200, Easing.EASE_IN_OUT, fraction -> gauge.setNeedle((float) fraction))
 *         .onFinished(() -> status.setText(Text.of("Ready")));
 * }</pre>
 *
 * <ul>
 *     <li>Animations step once a frame, by the platform's clock, on the UI thread: after the tasks handed to the UI
 *     thread and before data bindings, so a binding sees where an animation is. They step in a host's
 *     {@code flush}, which every layout and render runs, and the host asks for frames while one runs.</li>
 *     <li>An animation's time starts when it is made, and its first step comes on the next frame. Its last step is
 *     exactly 1, whatever the easing returns near the end. A duration of 0 takes that last step on the next
 *     frame.</li>
 *     <li>Each step runs inside a cause frame with the animation pushed and {@code Origin.CODE} put, so requests do
 *     not fire and reports say {@code Origin.CODE}. A two-way binding on an animated property does not write the
 *     property back to its state on every step; it writes once the animation ends or is closed.</li>
 *     <li>The animations of a screen stop where they are when it closes.</li>
 *     <li>Every method throws {@link IllegalStateException} when called off the host's UI thread.</li>
 * </ul>
 *
 * <p>Do not implement this interface.</p>
 */
public interface Animator {

    /**
     * Runs an animation that calls a step with the eased fraction of its time each frame, ending on exactly 1.
     *
     * @param millis the duration in milliseconds; 0 for a single step of 1 on the next frame
     * @param easing the easing
     * @param step called each frame with the eased fraction, which may go outside 0 to 1 before the end
     * @return the running animation
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    Animation run(long millis, Easing easing, DoubleConsumer step);

    /**
     * Animates a property of a target from what the key's reader returns now to a value, writing each step's value
     * with the key's writer, which works like the property's setter. The last step writes {@code to} itself. A
     * running animation of a key with the same name on the same target stops where it is first.
     *
     * @param <C> the target's type
     * @param <V> the type of the property's value
     * @param target the object whose property moves, usually a component
     * @param key the property
     * @param to the value to end on
     * @param millis the duration in milliseconds
     * @param easing the easing
     * @param interpolator how to find the values between
     * @return the running animation
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    <C, V> Animation animate(C target, PropertyKey<? super C, V> key, V to, long millis, Easing easing,
                             Interpolator<V> interpolator);

    /**
     * Animates a property whose value is a double, with {@link Interpolator#DOUBLE}, as
     * {@link #animate(Object, PropertyKey, Object, long, Easing, Interpolator)} does.
     *
     * @param <C> the target's type
     * @param target the object whose property moves
     * @param key the property
     * @param to the value to end on
     * @param millis the duration in milliseconds
     * @param easing the easing
     * @return the running animation
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    <C> Animation animate(C target, PropertyKey<? super C, Double> key, double to, long millis, Easing easing);
}
