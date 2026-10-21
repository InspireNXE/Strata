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
package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.event.cause.Cause;
import org.inspirenxe.strata.api.event.cause.CauseKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A cause as {@link CauseStackImpl} makes it: a copy of the chain, most recent first, and of the merged context. It
 * does not change once made.
 */
final class CauseImpl implements Cause {
    private final List<Object> chain;
    private final Map<CauseKey<?>, Object> context;

    /**
     * Creates a cause from copies the caller no longer changes.
     *
     * @param chain the chain, most recent first, unmodifiable
     * @param context the merged context, by key identity, unmodifiable
     */
    CauseImpl(final List<Object> chain, final Map<CauseKey<?>, Object> context) {
        this.chain = chain;
        this.context = context;
    }

    @Override
    public Optional<Object> root() {
        return this.chain.isEmpty() ? Optional.empty() : Optional.of(this.chain.getFirst());
    }

    @Override
    public <T> Optional<T> first(final Class<T> type) {
        for (final Object cause : this.chain) {
            if (type.isInstance(cause)) {
                return Optional.of(type.cast(cause));
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> List<T> all(final Class<T> type) {
        final List<T> found = new ArrayList<>();
        for (final Object cause : this.chain) {
            if (type.isInstance(cause)) {
                found.add(type.cast(cause));
            }
        }
        return List.copyOf(found);
    }

    @Override
    public List<Object> all() {
        return this.chain;
    }

    @Override
    public <V> Optional<V> context(final CauseKey<V> key) {
        final Object value = this.context.get(key);
        return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
    }

    /**
     * Lists the chain and context, for debugging.
     *
     * @return for example {@code Cause[chain=[RadioGroupImpl@1f2a], context={CauseKey[strata:origin]=User[...]}]}
     */
    @Override
    public String toString() {
        return "Cause[chain=" + this.chain + ", context=" + this.context + "]";
    }
}
