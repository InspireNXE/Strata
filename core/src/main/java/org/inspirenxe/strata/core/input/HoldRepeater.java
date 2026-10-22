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

import java.util.Objects;

/**
 * Repeats an action while a pointer button is held on a part of a control, such as a spinner's step button or a
 * scroll bar's track, on the key repeat timing: once {@link KeyRepeater#INITIAL_DELAY_MILLIS} after the press, then
 * every {@link KeyRepeater#INTERVAL_MILLIS}. The control acts once for the press itself and starts the repeat; each
 * screen's {@code Root} polls it once a frame and runs the action when it is due. Only one hold repeats at a time, as
 * a pointer holds one press.
 */
public final class HoldRepeater {
    private Object owner;
    private Runnable action;
    private long nextFire;

    /**
     * Creates a repeater with nothing held.
     */
    public HoldRepeater() {
    }

    /**
     * Starts repeating an action, in place of any hold repeating already.
     *
     * @param owner what holds the repeat, so it can stop only its own
     * @param action what to run on each repeat
     * @param nowMillis the press's time from the platform's clock
     */
    public void start(final Object owner, final Runnable action, final long nowMillis) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.action = Objects.requireNonNull(action, "action");
        this.nextFire = nowMillis + KeyRepeater.INITIAL_DELAY_MILLIS;
    }

    /**
     * Stops the repeat if an owner holds it.
     *
     * @param owner the owner that started it
     */
    public void stop(final Object owner) {
        if (this.owner == owner) {
            this.owner = null;
            this.action = null;
        }
    }

    /**
     * Returns what holds the repeat.
     *
     * @return the owner, or {@code null} while nothing repeats
     */
    public Object owner() {
        return this.owner;
    }

    /**
     * Returns the action to run now, if a repeat is due, and schedules the next. At most one repeat is due per call,
     * so a slow frame drops repeats instead of running a burst.
     *
     * @param nowMillis the current time from the platform's clock
     * @return the action, or {@code null} if nothing is due
     */
    public Runnable poll(final long nowMillis) {
        if (this.action == null || nowMillis < this.nextFire) {
            return null;
        }
        this.nextFire = nowMillis + KeyRepeater.INTERVAL_MILLIS;
        return this.action;
    }

    /**
     * Returns when the next repeat is due.
     *
     * @return the platform clock's milliseconds at which {@link #poll} next returns the action, or
     *     {@link Long#MAX_VALUE} while nothing repeats
     */
    public long nextFire() {
        return this.action == null ? Long.MAX_VALUE : this.nextFire;
    }
}
