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
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKey;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

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
 * <p>When the source is a state and the key has reports, the binding also handles each report on the component,
 * at the highest priority, so after the component's other handlers and before the handlers of the ancestors the
 * report bubbles to, and writes the property's value to the state unless the two are equal. It writes inside a
 * cause frame whose origin is an {@link Origin.Binding} of its source, so the control's requests do not fire for
 * the write. It skips reports with that origin, so a value it set never comes back to the state, and
 * reports that fire after attach before its first evaluation, so a value the control had before it read the source,
 * such as a combo box selecting its first item when the items arrive, never replaces the state's.</p>
 *
 * <p>A report fired by a step of an animation that has not reached its end, which puts {@link #IN_MOTION}, is not
 * written back either. The binding hands itself to the animation instead, which calls {@link #writeBack()} once it
 * ends or stops, so the state hears where the property came to rest and not every value on the way.</p>
 *
 * @param <V> the type of the property's value
 */
public final class Binding<V> extends Dependent {
    /**
     * Put by every step of an animation but its last, with the animation as the value, so bindings leave its
     * reports until it ends.
     */
    public static final CauseKey<Deferral> IN_MOTION = CauseKey.of("strata:in_motion", Deferral.class);
    private final Component<?> component;
    private final PropertyKey<Object, V> key;
    private final Source<V> source;
    private final StateImpl<V> writable;
    private final List<Subscription> reportSubscriptions;
    private final Supplier<CauseStack> causes;
    private final Origin.Binding origin;
    // Whether the source was read since the last attach; reports are not written back until it was
    private boolean evaluated;

    /**
     * Creates a binding that does nothing until attached.
     *
     * @param component the component whose property it sets
     * @param key the property; must apply to the component's type, which {@code Component.bind} checked
     * @param source the source
     * @param causes the cause stack of the component's factory, where the binding opens its frame while it writes
     */
    @SuppressWarnings("unchecked")
    public Binding(final Component<?> component, final PropertyKey<?, V> key, final Source<V> source,
                   final Supplier<CauseStack> causes) {
        this.component = Objects.requireNonNull(component, "component");
        this.key = (PropertyKey<Object, V>) Objects.requireNonNull(key, "key");
        this.source = Objects.requireNonNull(source, "source");
        this.causes = Objects.requireNonNull(causes, "causes");
        this.origin = new Origin.Binding(source);
        this.writable = source instanceof StateImpl<V> state && !key.reports().isEmpty() ? state : null;
        this.reportSubscriptions = new ArrayList<>(key.reports().size());
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
        return this.isLive();
    }

    /**
     * Starts following the source: handles the key's reports if the binding is two way, and marks the binding dirty
     * on the queue. The caller evaluates it once the tree it joined is attached. Does nothing if already attached.
     * Call on the UI thread.
     *
     * @param queue the queue of the root the component joined
     */
    public void attach(final BindingQueue queue) {
        if (this.isLive()) {
            return;
        }
        this.evaluated = false;
        if (this.writable != null) {
            for (final EventType<? extends ControlEvent<?>> report : this.key.reports()) {
                this.reportSubscriptions.add(this.subscribe(report));
            }
        }
        this.join(queue);
    }

    private <E extends ControlEvent<?>> Subscription subscribe(final EventType<E> report) {
        // After the control's own handlers, so they see the change before the state does; ancestors' handlers run later
        return this.component.subscribe(report, Integer.MAX_VALUE, this::reported);
    }

    /**
     * Stops following the source: drops the report handlers and every dependency, so no state refers to this
     * binding any more. Does nothing if not attached. Call on the UI thread.
     */
    public void detach() {
        if (!this.leave()) {
            return;
        }
        for (final Subscription subscription : this.reportSubscriptions) {
            subscription.unsubscribe();
        }
        this.reportSubscriptions.clear();
    }

    /**
     * Evaluates the source and sets the property, if the binding is dirty and attached. Call on the UI thread. If the
     * source throws, the binding still follows the states it read before throwing, so a change to one of them
     * evaluates it again, and the exception goes on to the caller.
     */
    @Override
    public void evaluate() {
        if (!this.takeDirty()) {
            return;
        }
        final V value = this.read(this.source);
        this.evaluated = true;
        this.write(value);
    }

    private void write(final V value) {
        if (Objects.equals(this.key.reader().apply(this.component), value)) {
            return;
        }
        // Its own reports carry this origin, so reported() leaves them alone, and the control's requests do not fire
        try (CauseFrame _ = this.causes.get().pushFrame().put(CauseKeys.ORIGIN, this.origin)) {
            this.key.writer().accept(this.component, value);
        }
    }

    private void reported(final ControlEvent<?> event) {
        if (!this.evaluated || !this.isLive() || event.target() != this.component
                || event.origin() instanceof Origin.Binding(final Object written) && written == this.source) {
            return;
        }
        final Optional<Deferral> motion = this.causes.get().context(IN_MOTION);
        if (motion.isPresent()) {
            motion.get().defer(this);
            return;
        }
        this.writeBack();
    }

    /**
     * Writes the property's value to the state unless they are equal, as a report does, for a report an animation
     * deferred. Does nothing for a one-way binding, or one that is detached or not evaluated since it attached. Call on
     * the UI thread.
     */
    public void writeBack() {
        if (this.writable == null || !this.evaluated || !this.isLive()) {
            return;
        }
        final V value = this.key.reader().apply(this.component);
        if (!Objects.equals(this.writable.peek(), value)) {
            this.writable.set(value);
        }
    }

    /**
     * Something running that bindings leave their write-backs with until it ends: an animation.
     */
    public interface Deferral {
        /**
         * Keeps a binding to write back once the animation ends or stops. A binding deferred twice is written back
         * once.
         *
         * @param binding the binding
         */
        void defer(Binding<?> binding);
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
