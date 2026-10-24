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

import org.inspirenxe.strata.api.event.Subscription;

/**
 * Runs repeating tasks for screens, on the engine's terms. Core hands it the tasks of
 * {@code ScreenHandle.every} and keeps the subscriptions; the engine decides the loop, the clock and the
 * timing: a game loop's scheduler, a toolkit's timer or the engine's tick.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Run tasks on the UI thread: the thread that renders the platform's hosts and sends them input.</li>
 *     <li>Run a task first about one interval after {@link #every} is called, then about one interval apart. An
 *     engine with a coarser beat, such as a tick, rounds the interval up to whole beats. Runs the engine missed, such
 *     as while it was paused, may be dropped rather than run one after another.</li>
 *     <li>Never run a task after its subscription closed, even one already due. Closing twice does nothing, and
 *     closing from inside the task stops the next run.</li>
 *     <li>A task that throws is the engine's to report; it may stop the task or keep running it, but must keep
 *     running other tasks.</li>
 *     <li>{@link #every} is called on the UI thread.</li>
 * </ul>
 */
@FunctionalInterface
public interface Scheduler {

    /**
     * Runs a task repeatedly until its subscription closes.
     *
     * @param millis the interval in milliseconds; above 0
     * @param task what to run
     * @return the subscription that stops the task
     * @throws IllegalArgumentException if {@code millis} is 0 or less
     */
    Subscription every(long millis, Runnable task);
}
