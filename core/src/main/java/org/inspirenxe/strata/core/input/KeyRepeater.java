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

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * Makes key repeats for engines that do not send them. Only navigation and delete keys repeat, since text editing
 * needs nothing more. Each screen's {@code Root} feeds it every key event and polls it once a frame.
 *
 * <p>Only the most recently pressed repeatable key repeats, as on most operating systems.</p>
 */
public final class KeyRepeater {
    /**
     * How long, in milliseconds, a key is held before the first repeat.
     */
    public static final long INITIAL_DELAY_MILLIS = 400L;
    /**
     * The time, in milliseconds, between later repeats (about 30 a second).
     */
    public static final long INTERVAL_MILLIS = 33L;

    private Key key;
    private Modifiers modifiers;
    private long nextFire;

    /**
     * Creates a repeater with no key held.
     */
    public KeyRepeater() {
        this.modifiers = Modifiers.NONE;
    }

    /**
     * Returns whether holding a key makes repeats: the arrows, Backspace, Delete, Home, End, Page Up and Page Down.
     *
     * @param key the key
     * @return true if the key repeats
     */
    public static boolean isRepeatable(final Key key) {
        return switch (key) {
            case LEFT, RIGHT, UP, DOWN, BACKSPACE, DELETE, HOME, END, PAGE_UP, PAGE_DOWN -> true;
            default -> false;
        };
    }

    /**
     * Records a key event. A press of a repeatable key starts the countdown for that key, replacing any key already
     * held; releasing that key stops it. Other events change nothing.
     *
     * @param key the key
     * @param action the action
     * @param modifiers the modifiers held, which every repeat carries
     * @param nowMillis the event time from the platform's clock
     */
    public void onKey(final Key key, final KeyAction action, final Modifiers modifiers, final long nowMillis) {
        if (action == KeyAction.PRESS && isRepeatable(key)) {
            this.key = key;
            this.modifiers = modifiers;
            this.nextFire = nowMillis + INITIAL_DELAY_MILLIS;
        } else if (action == KeyAction.RELEASE && key == this.key) {
            this.stop();
        }
    }

    /**
     * Returns the key to repeat now, if one is due, and schedules the next repeat. At most one repeat fires per call,
     * so a slow frame drops repeats instead of sending a burst.
     *
     * @param nowMillis the current time from the platform's clock
     * @return the key to send a repeat for, or {@code null} if nothing is due
     */
    public Key poll(final long nowMillis) {
        if (this.key == null || nowMillis < this.nextFire) {
            return null;
        }
        this.nextFire = nowMillis + INTERVAL_MILLIS;
        return this.key;
    }

    /**
     * Returns the modifiers that were held when the repeating key was pressed.
     *
     * @return the modifiers, or {@link Modifiers#NONE} when no key is held
     */
    public Modifiers modifiers() {
        return this.modifiers;
    }

    /**
     * Stops repeating until the next press of a repeatable key.
     */
    public void stop() {
        this.key = null;
        this.modifiers = Modifiers.NONE;
    }
}
