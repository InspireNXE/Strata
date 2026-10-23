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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A listener that follows a {@link Source} the way a {@link Binding} does, made by {@code ComponentFactory.watch}.
 * Its root keeps it and stops it when the screen closes or rebuilds.
 *
 * <p>{@link #start} reads the source at once, on the UI thread, to learn which states it depends on and the value it
 * starts from, and calls nothing. From then on a change to any of those states queues the watch on the root's
 * {@link BindingQueue}, beside the bindings, and the flush reads the source again and calls the listener with the new
 * value if it differs from the last one, as {@link Objects#equals} says. Like a binding it reads again on every
 * evaluation, so a source may read different states each time.</p>
 *
 * @param <T> the type of the value
 */
public final class Watch<T> extends Dependent {
    private final Source<T> source;
    private final Consumer<? super T> listener;
    private T last;

    /**
     * Creates a watch that does nothing until started.
     *
     * @param source the source
     * @param listener called with each new value
     */
    public Watch(final Source<T> source, final Consumer<? super T> listener) {
        this.source = Objects.requireNonNull(source, "source");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Starts following the source: reads it once to record its states and the value to compare with, without calling
     * the listener. Does nothing if already started. Call on the UI thread.
     *
     * @param queue the queue of the root the watch belongs to
     */
    public void start(final BindingQueue queue) {
        if (!this.join(queue)) {
            return;
        }
        this.takeDirty();
        this.last = this.read(this.source);
    }

    /**
     * Stops following the source and lets go of every state it read. Does nothing if not started. Any later change
     * calls nothing.
     */
    public void stop() {
        this.leave();
    }

    /**
     * Returns whether the watch follows its source.
     *
     * @return true between {@link #start} and {@link #stop}
     */
    public boolean isStarted() {
        return this.isLive();
    }

    @Override
    void evaluate() {
        if (!this.takeDirty()) {
            return;
        }
        final T value = this.read(this.source);
        if (Objects.equals(value, this.last)) {
            return;
        }
        this.last = value;
        this.listener.accept(value);
    }

    /**
     * Names the watch and its listener, for the cycle report.
     *
     * @return for example {@code Watch[MyScreen$$Lambda@1f2a]}
     */
    @Override
    public String toString() {
        return "Watch[" + this.listener + "]";
    }
}
