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

import org.inspirenxe.strata.api.state.Source;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Something that reads a {@link Source} on the UI thread and is evaluated again when a state it read changes: a
 * {@link Binding} or a {@link Watch}. While live it sits on a root's {@link BindingQueue} whenever it is dirty, and is
 * a dependent of every {@link StateImpl} its source read the last time it ran.
 */
abstract class Dependent {
    private final AtomicBoolean dirty;
    private final Set<StateImpl<?>> dependencies;
    private volatile BindingQueue queue;

    Dependent() {
        this.dirty = new AtomicBoolean();
        this.dependencies = new HashSet<>();
    }

    /**
     * Returns whether the dependent follows its states.
     */
    final boolean isLive() {
        return this.queue != null;
    }

    /**
     * Starts following: marks the dependent dirty on the queue. Does nothing if already live.
     *
     * @return false if it was already live
     */
    final boolean join(final BindingQueue queue) {
        if (this.queue != null) {
            return false;
        }
        this.queue = Objects.requireNonNull(queue, "queue");
        this.invalidate();
        return true;
    }

    /**
     * Stops following: forgets the queue and removes itself from every state it read. Does nothing if not live.
     *
     * @return false if it was not live
     */
    final boolean leave() {
        if (this.queue == null) {
            return false;
        }
        this.queue = null;
        for (final StateImpl<?> dependency : this.dependencies) {
            dependency.removeDependent(this);
        }
        this.dependencies.clear();
        this.dirty.set(false);
        return true;
    }

    /**
     * Marks the dependent dirty and queues it, unless it is not live or already dirty. Any thread may call this.
     */
    final void invalidate() {
        final BindingQueue queue = this.queue;
        if (queue != null && this.dirty.compareAndSet(false, true)) {
            queue.enqueue(this);
        }
    }

    /**
     * Forgets that the dependent is dirty without evaluating it, when a flush gives up on a cycle.
     */
    final void discard() {
        this.dirty.set(false);
    }

    /**
     * Takes the dirty mark, if the dependent is live and dirty.
     *
     * @return true if the caller should evaluate now
     */
    final boolean takeDirty() {
        return this.queue != null && this.dirty.compareAndSet(true, false);
    }

    /**
     * Reads a source inside a tracking frame and follows the states it read. If the source throws, the dependent still
     * follows the states it read before throwing, and the exception goes on to the caller.
     */
    final <V> V read(final Source<V> source) {
        final Tracking.Frame frame = Tracking.begin();
        final V value;
        try {
            value = source.get();
        } finally {
            Tracking.end(frame);
            this.follow(frame.reads());
        }
        return value;
    }

    /**
     * Replaces the dependencies with the states the source just read. A state that changed after the source read
     * it, before this dependent was its dependent, marks the dependent dirty again, so no change is missed.
     */
    private void follow(final Map<StateImpl<?>, Long> reads) {
        if (this.queue == null) {
            return;
        }
        for (final Iterator<StateImpl<?>> it = this.dependencies.iterator(); it.hasNext(); ) {
            final StateImpl<?> dependency = it.next();
            if (!reads.containsKey(dependency)) {
                dependency.removeDependent(this);
                it.remove();
            }
        }
        for (final Map.Entry<StateImpl<?>, Long> read : reads.entrySet()) {
            final StateImpl<?> state = read.getKey();
            if (this.dependencies.add(state)) {
                state.addDependent(this);
            }
            if (state.version() != read.getValue()) {
                this.invalidate();
            }
        }
    }

    /**
     * Evaluates the source and acts on its value, if the dependent is dirty and live. Called on the UI thread.
     */
    abstract void evaluate();
}
