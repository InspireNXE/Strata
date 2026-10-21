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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.Source;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One property of one component kept in step with a {@link Source}, made by {@code Component.bind}. The component's
 * node holds it and calls {@link #attach} and {@link #detach} as the component joins and leaves a shown tree.
 *
 * <p>While attached, the binding is a dependent of every {@link StateImpl} its source read the last time it was
 * evaluated. A change to any of them marks it dirty, from any thread, and puts it on its root's
 * {@link BindingQueue}, which evaluates it on the UI thread: the source runs inside a {@link Tracking} frame, the
 * dependencies are replaced by what it read, and the value is written to the property unless the property already
 * equals it.</p>
 *
 * <p>When the source is a state and the key has a report, the binding also handles that report on the component,
 * after every other handler, and writes the property's value to the state unless the two are equal. It skips
 * reports that fire while it writes the property itself, so a value it set never comes back to the state, and
 * reports that fire after attach before its first evaluation, so a value the control had before it read the source,
 * such as a combo box selecting its first item when the items arrive, never replaces the state's.</p>
 *
 * @param <V> the type of the property's value
 */
public final class Binding<V> {
    private final Component<?> component;
    private final PropertyKey<Object, V> key;
    private final Source<V> source;
    private final StateImpl<V> writable;
    private final AtomicBoolean dirty;
    private final Set<StateImpl<?>> dependencies;
    private volatile BindingQueue queue;
    private Subscription reportSubscription;
    private boolean writing;
    // Whether the source was read since the last attach; reports are not written back until it was
    private boolean evaluated;

    /**
     * Creates a binding that does nothing until attached.
     *
     * @param component the component whose property it sets
     * @param key the property; must apply to the component's type, which {@code Component.bind} checked
     * @param source the source
     */
    @SuppressWarnings("unchecked")
    public Binding(final Component<?> component, final PropertyKey<?, V> key, final Source<V> source) {
        this.component = Objects.requireNonNull(component, "component");
        this.key = (PropertyKey<Object, V>) Objects.requireNonNull(key, "key");
        this.source = Objects.requireNonNull(source, "source");
        this.writable = source instanceof StateImpl<V> state && key.report().isPresent() ? state : null;
        this.dirty = new AtomicBoolean();
        this.dependencies = new HashSet<>();
    }

    /**
     * Returns the property this binding sets.
     *
     * @return the key
     */
    public PropertyKey<?, V> key() {
        return this.key;
    }

    /**
     * Returns whether the binding is attached.
     *
     * @return true between {@link #attach} and {@link #detach}
     */
    public boolean isAttached() {
        return this.queue != null;
    }

    /**
     * Starts following the source: handles the key's report if the binding is two way, and marks the binding dirty
     * on the queue. The caller evaluates it once the tree it joined is attached. Does nothing if already attached.
     * Call on the UI thread.
     *
     * @param queue the queue of the root the component joined
     */
    public void attach(final BindingQueue queue) {
        if (this.queue != null) {
            return;
        }
        this.queue = Objects.requireNonNull(queue, "queue");
        this.evaluated = false;
        if (this.writable != null) {
            this.reportSubscription = this.subscribe(this.key.report().orElseThrow());
        }
        this.invalidate();
    }

    private <E extends ControlEvent<?>> Subscription subscribe(final EventType<E> report) {
        // After every other handler, so they all see the change before the state does
        return this.component.subscribe(report, Integer.MAX_VALUE, this::reported);
    }

    /**
     * Stops following the source: drops the report handler and every dependency, so no state refers to this
     * binding any more. Does nothing if not attached. Call on the UI thread.
     */
    public void detach() {
        if (this.queue == null) {
            return;
        }
        this.queue = null;
        if (this.reportSubscription != null) {
            this.reportSubscription.unsubscribe();
            this.reportSubscription = null;
        }
        for (final StateImpl<?> dependency : this.dependencies) {
            dependency.removeDependent(this);
        }
        this.dependencies.clear();
        this.dirty.set(false);
    }

    /**
     * Marks the binding dirty and queues it, unless it is detached or already dirty. Any thread may call this.
     */
    void invalidate() {
        final BindingQueue queue = this.queue;
        if (queue != null && this.dirty.compareAndSet(false, true)) {
            queue.enqueue(this);
        }
    }

    /**
     * Forgets that the binding is dirty without evaluating it, when a flush gives up on a cycle.
     */
    void discard() {
        this.dirty.set(false);
    }

    /**
     * Evaluates the source and sets the property, if the binding is dirty and attached. Call on the UI thread.
     */
    public void evaluate() {
        if (this.queue == null || !this.dirty.compareAndSet(true, false)) {
            return;
        }
        final Tracking.Frame frame = Tracking.begin();
        final V value;
        try {
            value = this.source.get();
        } finally {
            Tracking.end(frame);
        }
        this.evaluated = true;
        this.follow(frame.reads());
        this.write(value);
    }

    /**
     * Replaces the dependencies with the states the source just read. A state that changed after the source read
     * it, before this binding was its dependent, marks the binding dirty again, so no change is missed.
     */
    private void follow(final Map<StateImpl<?>, Long> reads) {
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

    private void write(final V value) {
        if (Objects.equals(this.key.reader().apply(this.component), value)) {
            return;
        }
        // TODO(causes, design 4.14): once causes are built, set the origin to Origin.Binding here and have reported()
        //  skip events whose origin is this binding, instead of the flag
        this.writing = true;
        try {
            this.key.writer().accept(this.component, value);
        } finally {
            this.writing = false;
        }
    }

    private void reported(final ControlEvent<?> event) {
        if (this.writing || !this.evaluated || this.queue == null || event.target() != this.component) {
            return;
        }
        final V value = this.key.reader().apply(this.component);
        if (!Objects.equals(this.writable.peek(), value)) {
            this.writable.set(value);
        }
    }

    /**
     * Names the component and key, for the cycle report.
     *
     * @return for example {@code CheckBoxImpl@1f2a.toggle.selected}
     */
    @Override
    public String toString() {
        return this.component.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(
                this.component)) + "." + this.key.name();
    }
}
