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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.animation.Animation;
import org.inspirenxe.strata.api.animation.Animator;
import org.inspirenxe.strata.api.animation.Easing;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;

/**
 * One number a built-in control moves smoothly, such as a switch's knob or a list's scroll offset, through the host's
 * animator with {@link Easing#EASE_OUT}. The control applies each value itself, through the setter it gives, so its
 * own rules (clamping, reports, layout) still hold. Without an animator, as while the control is detached, or with a
 * duration of 0, a move applies its end value at once, which is how every control looked before motion existed.
 *
 * <p>Built-in motion is off unless a theme sets a duration (design 4.51); {@link #millis} reads one.</p>
 */
public final class Motion {
    private final Setter setter;
    private float destination;
    private Animation running;

    /**
     * Creates a motion that is still.
     *
     * @param setter applies each value
     */
    public Motion(final Setter setter) {
        this.setter = Objects.requireNonNull(setter, "setter");
    }

    /**
     * Reads a duration metric from a theme, in milliseconds, kept from going below 0.
     *
     * @param theme the theme
     * @param key the metric's key
     * @return the duration; 0 when the theme sets none
     */
    public static long millis(final Theme theme, final String key) {
        return Math.max(0L, (long) theme.metricOr(key, 0f));
    }

    /**
     * Returns whether a move is under way.
     *
     * @return true while the animation runs
     */
    public boolean isMoving() {
        return this.running != null && this.running.isRunning();
    }

    /**
     * Returns where the value is headed: the end of the move under way, or the given current value when still. A
     * further step, such as another wheel notch, adds to this, so quick input is not lost to the easing.
     *
     * @param current the value now
     * @return the destination
     */
    public float destination(final float current) {
        return this.isMoving() ? this.destination : current;
    }

    /**
     * Moves from one value to another over a time, stopping any move under way where it is first. The first value
     * applied comes on the next frame; with no animator or no time, the end value is applied now.
     *
     * @param animator the host's animator, or {@code null} to apply the end at once
     * @param millis the duration
     * @param from the value now
     * @param to the value to end on
     */
    public void move(final Animator animator, final long millis, final float from, final float to) {
        this.stop();
        this.destination = to;
        if (animator == null || millis <= 0L || from == to) {
            this.setter.set(to);
            return;
        }
        this.running = animator.run(millis, Easing.EASE_OUT, fraction -> this.setter.set(fraction >= 1d ? to
                : from + (to - from) * (float) fraction));
    }

    /**
     * Stops a move under way where it is. Does nothing when still.
     */
    public void stop() {
        final Animation current = this.running;
        this.running = null;
        if (current != null) {
            current.close();
        }
    }

    /**
     * Applies one value of a motion.
     */
    @FunctionalInterface
    public interface Setter {
        /**
         * Applies a value.
         *
         * @param value the value
         */
        void set(float value);
    }
}
