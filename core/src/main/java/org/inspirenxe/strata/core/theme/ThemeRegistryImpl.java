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
package org.inspirenxe.strata.core.theme;

import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRegistry;
import org.inspirenxe.strata.api.theme.Themes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The core {@link ThemeRegistry}. It keeps each id's overrides in the order they were added and at most one
 * definition per id, and counts a version that changes whenever either changes, which hosts compare together with
 * their theme source's version. {@link ThemeResolver} reads it on every resolve. Every method is thread-safe.
 */
public final class ThemeRegistryImpl implements ThemeRegistry {
    /**
     * The one registry, which {@link org.inspirenxe.strata.api.Strata#themes()} returns and every host reads.
     */
    public static final ThemeRegistryImpl INSTANCE = new ThemeRegistryImpl();

    private final Map<ThemeId, List<Entry>> overrides;
    private final Map<ThemeId, Definition> definitions;
    private final AtomicLong version;

    /**
     * Creates an empty registry. Core uses {@link #INSTANCE}; tests can make their own and pass it to a
     * {@link ThemeResolver}.
     */
    public ThemeRegistryImpl() {
        this.overrides = new HashMap<>();
        this.definitions = new HashMap<>();
        this.version = new AtomicLong();
    }

    @Override
    public synchronized Subscription override(final ThemeId id, final ThemeOverrides overrides) {
        Objects.requireNonNull(id, "id");
        final Entry entry = new Entry(Objects.requireNonNull(overrides, "overrides"));
        this.overrides.computeIfAbsent(id, key -> new ArrayList<>()).add(entry);
        this.version.incrementAndGet();
        final AtomicBoolean open = new AtomicBoolean(true);
        return () -> {
            if (open.getAndSet(false)) {
                this.removeOverride(id, entry);
            }
        };
    }

    @Override
    public synchronized Subscription define(final ThemeId id, final ThemeId parent, final ThemeOverrides overrides) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(overrides, "overrides");
        if (id.equals(Themes.FLAT)) {
            throw new ThemeException("Theme " + id + " is built into Strata and cannot be defined in code");
        }
        if (id.equals(Themes.PLATFORM)) {
            throw new ThemeException("Theme " + id + " stands for the platform's default theme and cannot be"
                    + " defined in code");
        }
        if (this.definitions.containsKey(id)) {
            throw new ThemeException("Theme " + id + " is already defined in code, on "
                    + this.definitions.get(id).parent());
        }
        final Definition definition = new Definition(parent, overrides);
        this.definitions.put(id, definition);
        this.version.incrementAndGet();
        final AtomicBoolean open = new AtomicBoolean(true);
        return () -> {
            if (open.getAndSet(false)) {
                this.removeDefinition(id, definition);
            }
        };
    }

    /**
     * Returns the overrides added for an id that are still open.
     *
     * @param id the theme
     * @return the overrides, in the order they were added; empty if there are none
     */
    public synchronized List<ThemeOverrides> overrides(final ThemeId id) {
        final List<Entry> entries = this.overrides.get(id);
        if (entries == null) {
            return List.of();
        }
        final List<ThemeOverrides> all = new ArrayList<>(entries.size());
        for (final Entry entry : entries) {
            all.add(entry.overrides());
        }
        return all;
    }

    /**
     * Returns the open definition of an id.
     *
     * @param id the theme
     * @return the definition, or empty if code has not defined the id
     */
    public synchronized Optional<Definition> definition(final ThemeId id) {
        return Optional.ofNullable(this.definitions.get(id));
    }

    /**
     * Returns a number that changes whenever overrides or a definition are added or closed. Cheap enough to read on
     * every layout and render.
     *
     * @return the current version
     */
    public long version() {
        return this.version.get();
    }

    private synchronized void removeOverride(final ThemeId id, final Entry entry) {
        final List<Entry> entries = this.overrides.get(id);
        // Entries compare by identity, so equal overrides added twice are removed one at a time
        if (entries != null && entries.remove(entry)) {
            if (entries.isEmpty()) {
                this.overrides.remove(id);
            }
            this.version.incrementAndGet();
        }
    }

    private synchronized void removeDefinition(final ThemeId id, final Definition definition) {
        if (this.definitions.get(id) == definition) {
            this.definitions.remove(id);
            this.version.incrementAndGet();
        }
    }

    /**
     * A theme defined in code.
     *
     * @param parent the theme it starts from
     * @param overrides the keys it sets on the parent
     */
    public record Definition(ThemeId parent, ThemeOverrides overrides) {
    }

    /**
     * One call to {@link #override}. A class, not a record, so two entries holding equal overrides stay apart.
     */
    private static final class Entry {
        private final ThemeOverrides overrides;

        private Entry(final ThemeOverrides overrides) {
            this.overrides = overrides;
        }

        private ThemeOverrides overrides() {
            return this.overrides;
        }
    }
}
