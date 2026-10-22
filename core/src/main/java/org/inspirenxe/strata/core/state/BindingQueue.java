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
package org.inspirenxe.strata.core.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * The dirty bindings and watches of one root. Any thread may queue a binding; {@link #flush} evaluates them on the UI thread, in
 * rounds: a round evaluates every binding queued when it starts, and bindings those evaluations mark dirty wait for
 * the next round. Flushing stops when a round finds nothing queued.
 *
 * <p>A flush that is still finding dirty bindings after {@link #ROUND_LIMIT} rounds is taken to be a binding cycle,
 * such as two handlers that keep setting each other's states. It logs an error naming the bindings still dirty,
 * drops their updates, and returns false. The next change to one of their states queues them again.</p>
 */
public final class BindingQueue {
    /**
     * How many rounds a flush runs before it reports a cycle.
     */
    public static final int ROUND_LIMIT = 100;

    private static final System.Logger LOGGER = System.getLogger(BindingQueue.class.getName());

    private final Queue<Dependent> dirty;
    private final List<Dependent> round;
    private final Runnable queued;
    private boolean flushing;

    /**
     * Creates an empty queue.
     */
    public BindingQueue() {
        this(() -> { });
    }

    /**
     * Creates an empty queue that tells something whenever a binding is queued, as a root asks its host for a frame.
     *
     * @param queued runs after each binding is queued, on the thread that queued it, which can be any thread
     */
    public BindingQueue(final Runnable queued) {
        this.dirty = new ConcurrentLinkedQueue<>();
        this.round = new ArrayList<>();
        this.queued = queued;
    }

    void enqueue(final Dependent binding) {
        this.dirty.add(binding);
        this.queued.run();
    }

    /**
     * Returns whether no binding is waiting.
     *
     * @return true if a flush would evaluate nothing
     */
    public boolean isEmpty() {
        return this.dirty.isEmpty();
    }

    /**
     * Evaluates dirty bindings until none is left, as the class describes. A flush called while one runs, as when a
     * binding's write attaches more components, returns at once, and the running flush picks up what was queued.
     * If a source or a handler throws, the bindings not yet evaluated in that round stay queued.
     *
     * @return false if the round limit was reached and updates were dropped, true otherwise
     */
    public boolean flush() {
        if (this.flushing || this.dirty.isEmpty()) {
            return true;
        }
        this.flushing = true;
        try {
            for (int rounds = 0; ; rounds++) {
                Dependent binding;
                while ((binding = this.dirty.poll()) != null) {
                    this.round.add(binding);
                }
                if (this.round.isEmpty()) {
                    return true;
                }
                if (rounds == BindingQueue.ROUND_LIMIT) {
                    this.giveUp();
                    return false;
                }
                this.runRound();
            }
        } finally {
            this.flushing = false;
        }
    }

    private void runRound() {
        int next = 0;
        try {
            while (next < this.round.size()) {
                this.round.get(next++).evaluate();
            }
        } finally {
            for (int i = next; i < this.round.size(); i++) {
                this.dirty.add(this.round.get(i));
            }
            this.round.clear();
        }
    }

    private void giveUp() {
        final String names = this.round.stream().distinct().map(Object::toString).collect(Collectors.joining(", "));
        for (final Dependent binding : this.round) {
            binding.discard();
        }
        this.round.clear();
        LOGGER.log(System.Logger.Level.ERROR, "Binding cycle: bindings were still changing after "
                + BindingQueue.ROUND_LIMIT + " rounds, so their updates were dropped: " + names);
    }
}
