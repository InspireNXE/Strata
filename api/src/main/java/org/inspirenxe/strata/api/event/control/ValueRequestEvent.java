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
package org.inspirenxe.strata.api.event.control;

/**
 * A control's Pre event that carries the value a change is about to use: the text an edit would produce, the
 * slider value a drag reached, the row a press would select. Cancel it to refuse the change, or set another value
 * to rewrite it. The control checks the final value as it checks one set from code, so a rewritten value may still
 * be clamped, limited or ignored; each event type says how.
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 *
 * @param <C> the control type
 * @param <V> the value type
 */
public interface ValueRequestEvent<C, V> extends RequestEvent<C> {

    /**
     * Returns the value the change will use: the one the user's action produced, or the last one a handler set.
     *
     * @return the value
     */
    V value();

    /**
     * Replaces the value the change will use. Handlers after this one see the new value.
     *
     * @param value the value to use instead; each event type says whether null is allowed
     */
    void setValue(V value);
}
