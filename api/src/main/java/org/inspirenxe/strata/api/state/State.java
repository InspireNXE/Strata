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
package org.inspirenxe.strata.api.state;

import org.inspirenxe.strata.api.Strata;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A cell that holds one value and tells the bindings that read it when it changes. State lives in a plain class,
 * with no base class:
 *
 * <pre>{@code
 * final class SettingsModel {
 *     final State<Boolean> music = State.of(true);
 *     final State<Double> volume = State.of(0.8);
 *     final State<String> name = State.of("Ana");
 * }
 *
 * musicBox.bind(Toggle.SELECTED, model.music);
 * }</pre>
 *
 * <p>A component bound to a state with a key that has a report, such as {@link
 * org.inspirenxe.strata.api.control.attribute.value.Toggle#SELECTED}, also writes the user's accepted changes back
 * to the state. See {@link org.inspirenxe.strata.api.component.Component#bind}.</p>
 *
 * <p>{@link #get()} and {@link #set} may be called from any thread. A change reaches the components bound to the
 * state before the next frame is laid out, or when the host is flushed, on the UI thread; several changes in one
 * frame cost one update.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <T> the type of the value
 */
public interface State<T> extends Source<T> {

    /**
     * Creates a state.
     *
     * @param <T> the type of the value
     * @param value the first value; may be null
     * @return the new state
     * @throws IllegalStateException if Strata's core is not on the class path
     */
    static <T> State<T> of(final T value) {
        return Strata.states().of(value);
    }

    /**
     * Returns a source that combines the values of two sources. It reads both each time it is evaluated, so a
     * change to either evaluates it again.
     *
     * <pre>{@code
     * buy.bind(Component.ENABLED, State.combine(model.price, model.balance, (price, balance) -> price <= balance));
     * }</pre>
     *
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     * @param <R> the type of the combined value
     * @param first the first source
     * @param second the second source
     * @param combiner computes the combined value
     * @return the new source
     */
    static <A, B, R> Source<R> combine(final Source<? extends A> first, final Source<? extends B> second,
                                       final BiFunction<? super A, ? super B, ? extends R> combiner) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(combiner, "combiner");
        return () -> combiner.apply(first.get(), second.get());
    }

    /**
     * Returns the value. Called while a binding evaluates a source, it records this state as one the source
     * depends on.
     *
     * @return the value; may be null
     */
    @Override
    T get();

    /**
     * Stores a value and marks every binding that read this state for an update. Does nothing if the value equals
     * the current one, as {@link Object#equals} says. The bound components change before the next frame is laid
     * out, not during this call.
     *
     * @param value the new value; may be null
     */
    void set(T value);
}
