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
package org.inspirenxe.strata.api.event.cause;

import java.util.List;
import java.util.Optional;

/**
 * What led to an event: a chain of objects and a context of keyed values. The chain holds what the change passed
 * through, such as a radio group that deselected a button or a network message the application applied, with the
 * most recently pushed object first. The context says who did it and how, such as the
 * {@linkplain CauseKeys#ORIGIN origin}, the input device and the button.
 *
 * <p>The shape is the one engines with a cause stack of their own commonly use, so that an adapter for such an engine
 * can answer from the engine's cause stack. A cause does not change once it is made.</p>
 *
 * <p>Strata and adapters implement this interface. Developers get causes from {@code Event.cause()} and
 * {@link CauseStack#current()}.</p>
 */
public interface Cause {

    /**
     * Returns the object pushed most recently, the direct cause.
     *
     * @return the first object of the chain, or empty if nothing was pushed; on an engine whose stack always holds
     *     something, never empty
     */
    Optional<Object> root();

    /**
     * Returns the first object of the chain that is an instance of a type, so the most recently pushed one.
     *
     * @param <T> the type
     * @param type the type to look for
     * @return the object, or empty if the chain holds none of that type
     */
    <T> Optional<T> first(Class<T> type);

    /**
     * Returns every object of the chain that is an instance of a type, most recently pushed first.
     *
     * @param <T> the type
     * @param type the type to look for
     * @return an unmodifiable list, empty if the chain holds none of that type
     */
    <T> List<T> all(Class<T> type);

    /**
     * Returns the whole chain, most recently pushed first.
     *
     * @return an unmodifiable list; may hold the engine's own objects
     */
    List<Object> all();

    /**
     * Returns a value of the context. An inner frame's value hides an outer one's for the same key.
     *
     * @param <V> the value type
     * @param key the key
     * @return the value, or empty if no frame put one for the key
     */
    <V> Optional<V> context(CauseKey<V> key);

    /**
     * Returns who caused the event: the {@linkplain CauseKeys#ORIGIN origin} in the context, or
     * {@link Origin#CODE} when none was put.
     *
     * @return the origin; never null
     */
    default Origin origin() {
        return this.context(CauseKeys.ORIGIN).orElse(Origin.CODE);
    }
}
