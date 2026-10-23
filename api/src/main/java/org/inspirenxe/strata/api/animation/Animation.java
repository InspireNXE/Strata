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

import org.inspirenxe.strata.api.event.Subscription;

/**
 * An animation an {@link Animator} runs. It runs from the moment it is made until its time is up, it is
 * {@linkplain #finish() finished}, or it is closed. Closing it, with {@link #unsubscribe()} or {@link #close()},
 * stops it where it is: the property keeps the value of its last step and the {@link #onFinished} actions never
 * run. Starting another animation of the same property of the same target closes it too.
 *
 * <p>An animation is on the chain of the cause of every event its steps fire, so a handler can tell motion from
 * other changes with {@code event.cause().first(Animation.class)}.</p>
 *
 * <p>Use it on the UI thread. Do not implement this interface.</p>
 */
public interface Animation extends Subscription {

    /**
     * Returns whether the animation is still running.
     *
     * @return false once it reached its end, was finished or was closed
     */
    boolean isRunning();

    /**
     * Jumps to the end: runs the last step, exactly 1, at once, and then the {@link #onFinished} actions. Does
     * nothing if the animation is not running.
     *
     * @return this animation
     */
    Animation finish();

    /**
     * Adds an action to run once the animation ends by reaching its end or by {@link #finish()}, after its last
     * step. Actions run in the order they were added, on the UI thread. An action added after the animation ended
     * that way runs at once; one added after it was closed never runs.
     *
     * @param action the action
     * @return this animation
     */
    Animation onFinished(Runnable action);
}
