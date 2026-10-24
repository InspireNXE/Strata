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
package org.inspirenxe.strata.testing;

import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.spi.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The tester's scheduler: tasks run when the tester's clock moves past them, on the test's thread, every run the
 * move passed over in order, so advancing a second runs a 100 ms task ten times.
 */
final class TesterScheduler implements Scheduler {
    private final List<Task> tasks = new ArrayList<>();
    private long nowMillis;

    @Override
    public Subscription every(final long millis, final Runnable task) {
        Objects.requireNonNull(task, "task");
        if (millis <= 0L) {
            throw new IllegalArgumentException("A task's interval must be above 0 ms: " + millis);
        }
        final Task scheduled = new Task(millis, this.nowMillis + millis, task);
        this.tasks.add(scheduled);
        return scheduled;
    }

    /**
     * Moves time to a new reading and runs every task due by then.
     *
     * @param nowMillis the clock's reading in milliseconds
     */
    void advanceTo(final long nowMillis) {
        this.nowMillis = nowMillis;
        // Each pass runs the earliest due run, so runs of different tasks keep their order in time
        while (true) {
            Task next = null;
            for (final Task task : this.tasks) {
                if (task.dueMillis <= nowMillis && (next == null || task.dueMillis < next.dueMillis)) {
                    next = task;
                }
            }
            if (next == null) {
                return;
            }
            next.dueMillis += next.intervalMillis;
            next.task.run();
        }
    }

    private final class Task implements Subscription {
        private final long intervalMillis;
        private final Runnable task;
        private long dueMillis;

        Task(final long intervalMillis, final long dueMillis, final Runnable task) {
            this.intervalMillis = intervalMillis;
            this.dueMillis = dueMillis;
            this.task = task;
        }

        @Override
        public void unsubscribe() {
            TesterScheduler.this.tasks.remove(this);
        }
    }
}
