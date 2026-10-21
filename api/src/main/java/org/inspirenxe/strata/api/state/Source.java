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

import java.util.Objects;
import java.util.function.Function;

/**
 * A value a component can be bound to with {@link org.inspirenxe.strata.api.component.Component#bind}: a
 * {@link State}, or a lambda that computes a value from states.
 *
 * <pre>{@code
 * save.bind(Component.ENABLED, () -> !model.name.get().isBlank());
 * }</pre>
 *
 * <p>A binding calls {@link #get()} on the UI thread and records every {@code State} whose {@code get()} runs during
 * that call, however deeply nested. When any of them changes, the binding calls {@code get()} again before the next
 * frame is laid out, and records again, so a source may read different states each time. A source must not set
 * states or change components, and should read nothing that changes without a state, since such a change is never
 * seen.</p>
 *
 * @param <T> the type of the value
 */
@FunctionalInterface
public interface Source<T> {

    /**
     * Returns the current value.
     *
     * @return the value; may be null only where the key it is bound to accepts null
     */
    T get();

    /**
     * Returns a source that applies a function to this source's value. The new source reads this one each time it
     * is evaluated, so it depends on whatever this one depends on. States the function itself reads are recorded
     * too, as in any computed source.
     *
     * <pre>{@code
     * total.bind(HasText.TEXT, cart.count.map(count -> Text.of(count + " items")));
     * }</pre>
     *
     * @param <R> the type of the new value
     * @param mapper computes the new value from this source's value
     * @return the new source
     */
    default <R> Source<R> map(final Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return () -> mapper.apply(this.get());
    }
}
