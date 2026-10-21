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

/**
 * Makes {@link State}s. {@link State#of} calls the factory that {@link org.inspirenxe.strata.api.Strata} finds, so
 * code rarely uses this interface directly. Implementations may be called from any thread.
 *
 * <p>Strata's core implements this interface. Do not implement it.</p>
 */
public interface StateFactory {

    /**
     * See {@link State#of}.
     *
     * @param <T> the type of the value
     * @param value the first value; may be null
     * @return the new state
     */
    <T> State<T> of(T value);
}
