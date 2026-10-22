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

import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.state.State;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Core's {@link State}. The value and a version that counts its changes are volatile, and {@link #set} stores both
 * under the state's lock, so any thread may read or set it. The bindings and watches that read the state last time
 * they were evaluated are its dependents; {@code set} marks each of them dirty after storing, outside the lock, and
 * then calls the listeners {@link #subscribe} added, in order, on the setting thread.
 *
 * @param <T> the type of the value
 */
public final class StateImpl<T> implements State<T> {
    private final Set<Dependent> dependents;
    private final List<Consumer<? super T>> listeners;
    private volatile T value;
    private volatile long version;

    /**
     * Creates a state.
     *
     * @param value the first value; may be null
     */
    StateImpl(final T value) {
        this.dependents = ConcurrentHashMap.newKeySet();
        this.listeners = new CopyOnWriteArrayList<>();
        this.value = value;
    }

    @Override
    public T get() {
        // Read the version before the value: set writes them the other way round, so a change between the two reads
        // shows as a newer version when the binding checks after subscribing
        final long version = this.version;
        Tracking.read(this, version);
        return this.value;
    }

    @Override
    public void set(final T value) {
        synchronized (this) {
            if (Objects.equals(this.value, value)) {
                return;
            }
            this.value = value;
            this.version = this.version + 1;
        }
        for (final Dependent dependent : this.dependents) {
            dependent.invalidate();
        }
        for (final Consumer<? super T> listener : this.listeners) {
            listener.accept(value);
        }
    }

    @Override
    public Subscription subscribe(final Consumer<? super T> listener) {
        Objects.requireNonNull(listener, "listener");
        // A new object per subscription, so closing one never removes another of the same listener
        final Consumer<T> entry = listener::accept;
        this.listeners.add(entry);
        return () -> this.listeners.remove(entry);
    }

    /**
     * Returns the value without recording a read.
     *
     * @return the value
     */
    T peek() {
        return this.value;
    }

    /**
     * Returns how many times the value has changed.
     *
     * @return the version, 0 for a new state
     */
    long version() {
        return this.version;
    }

    void addDependent(final Dependent dependent) {
        this.dependents.add(dependent);
    }

    void removeDependent(final Dependent dependent) {
        this.dependents.remove(dependent);
    }

    /**
     * Returns how many bindings and watches follow this state, for tests that check a detached component or a closed
     * screen let go of it.
     *
     * @return the number of bindings and watches that read it last time they were evaluated and are still live
     */
    public int dependentCount() {
        return this.dependents.size();
    }

    /**
     * Returns the value, for debugging.
     *
     * @return for example {@code State[true]}
     */
    @Override
    public String toString() {
        return "State[" + this.value + "]";
    }
}
