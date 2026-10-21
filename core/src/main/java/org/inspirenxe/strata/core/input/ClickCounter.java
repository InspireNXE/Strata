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
package org.inspirenxe.strata.core.input;

import org.inspirenxe.strata.api.input.MouseButton;

/**
 * Counts presses into single, double and triple clicks. Each screen's {@code Root} keeps one.
 *
 * <p>A press continues the run when it hits the same target with the same button, comes within
 * {@link #MULTI_CLICK_MILLIS} of the previous press, and lands within {@link #MULTI_CLICK_DISTANCE} of it on both
 * axes. After a triple click the next press starts again at one.</p>
 *
 * <p>The host delivers every press and release at once with its count, and controls react to the count they care
 * about (a text box selects a word on two and everything on three). No release waits to see whether a second click
 * follows, so single clicks act without delay.</p>
 */
public final class ClickCounter {
    /**
     * The longest gap, in milliseconds, between two presses of one run.
     */
    public static final long MULTI_CLICK_MILLIS = 500L;
    /**
     * The farthest, in UI units along either axis, a press may land from the previous one and still continue a run.
     */
    public static final float MULTI_CLICK_DISTANCE = 4f;
    /**
     * The highest count; the press after it starts a new run.
     */
    public static final int MAX_CLICKS = 3;

    private Object lastTarget;
    private MouseButton lastButton;
    private long lastTime;
    private float lastX;
    private float lastY;
    private int count;

    /**
     * Creates a counter with no run in progress.
     */
    public ClickCounter() {
    }

    /**
     * Records a press and returns its place in the current run.
     *
     * @param target the pressed component; targets compare by identity
     * @param button the pressed button
     * @param x the press's x, in the host's coordinates
     * @param y the press's y, in the host's coordinates
     * @param timeMillis the press time from the platform's clock
     * @return the click count, from 1 to {@link #MAX_CLICKS}
     */
    public int press(final Object target, final MouseButton button, final float x, final float y, final long timeMillis) {
        final boolean continues = this.count > 0
                && this.count < MAX_CLICKS
                && target == this.lastTarget
                && button == this.lastButton
                && timeMillis - this.lastTime <= MULTI_CLICK_MILLIS
                && Math.abs(x - this.lastX) <= MULTI_CLICK_DISTANCE
                && Math.abs(y - this.lastY) <= MULTI_CLICK_DISTANCE;

        this.count = continues ? this.count + 1 : 1;
        this.lastTarget = target;
        this.lastButton = button;
        this.lastTime = timeMillis;
        this.lastX = x;
        this.lastY = y;
        return this.count;
    }

    /**
     * Returns the count of the most recent press.
     *
     * @return the count, or 0 if there has been no press since the last reset
     */
    public int current() {
        return this.count;
    }

    /**
     * Ends the current run, so the next press counts as 1.
     */
    public void reset() {
        this.count = 0;
        this.lastTarget = null;
        this.lastButton = null;
    }
}
