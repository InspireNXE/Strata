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

import java.util.Objects;

/**
 * A typed key for a value in a cause's context. Strata's own keys are in {@link CauseKeys}; a library or application
 * can make its own. Keys are compared by identity, so keep each in a constant.
 *
 * @param <V> the type of the value
 */
public final class CauseKey<V> {
    private final String name;
    private final Class<V> type;

    private CauseKey(final String name, final Class<V> type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a key.
     *
     * @param <V> the type of the value
     * @param name a readable name, such as {@code myapp:listing}, for debugging
     * @param type the type of the value
     * @return the new key
     */
    public static <V> CauseKey<V> of(final String name, final Class<V> type) {
        return new CauseKey<>(name, type);
    }

    /**
     * Returns the key's name.
     *
     * @return the name given to {@link #of}
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns the type of the key's value.
     *
     * @return the type given to {@link #of}
     */
    public Class<V> type() {
        return this.type;
    }

    /**
     * Returns the key's name, for debugging.
     *
     * @return for example {@code CauseKey[strata:origin]}
     */
    @Override
    public String toString() {
        return "CauseKey[" + this.name + "]";
    }
}
