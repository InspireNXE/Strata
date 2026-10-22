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
package org.inspirenxe.strata.core.appearance;

import org.inspirenxe.strata.api.appearance.Appearance;
import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The core {@link Appearances}. Each type keeps a stack of registrations, newest on top, and an order number from its
 * first registration, which breaks ties between interfaces at the same distance. Lookups walk the class, its
 * superclasses, then its interfaces breadth first, and are cached per class until a registration changes.
 *
 * <p>It starts with {@link Text}, named by itself, and {@link Visual}, whose icon is itself.</p>
 */
public final class AppearancesImpl implements Appearances {
    /**
     * The one registry.
     */
    public static final AppearancesImpl INSTANCE = new AppearancesImpl();

    private static final Appearance<Object> NONE = new Appearance<>() { };

    private final Map<Class<?>, Deque<Appearance<?>>> registered = new HashMap<>();
    private final Map<Class<?>, Integer> order = new HashMap<>();
    private final Map<Class<?>, Appearance<?>> cache = new ConcurrentHashMap<>();

    private AppearancesImpl() {
        this.register(Text.class, Appearance.<Text>builder().name(text -> text).build());
        this.register(Visual.class, Appearance.<Visual>builder().icon(visual -> visual).build());
    }

    @Override
    public synchronized <T> Subscription register(final Class<T> type, final Appearance<? super T> appearance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(appearance, "appearance");
        this.registered.computeIfAbsent(type, key -> new ArrayDeque<>()).push(appearance);
        this.order.putIfAbsent(type, this.order.size());
        this.cache.clear();
        return () -> {
            synchronized (AppearancesImpl.this) {
                final Deque<Appearance<?>> stack = this.registered.get(type);
                if (stack != null && stack.removeFirstOccurrence(appearance)) {
                    this.cache.clear();
                }
            }
        };
    }

    /**
     * Registers an appearance beneath every other registration for its type, so any registration, earlier or later,
     * hides it. Engine adapters register their defaults this way, so it does not matter whether they register before
     * or after other code.
     *
     * @param <T> the type
     * @param type the type
     * @param appearance the appearance
     */
    public synchronized <T> void registerDefault(final Class<T> type, final Appearance<? super T> appearance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(appearance, "appearance");
        this.registered.computeIfAbsent(type, key -> new ArrayDeque<>()).addLast(appearance);
        this.order.putIfAbsent(type, this.order.size());
        this.cache.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Appearance<? super T>> find(final Class<T> type) {
        final Appearance<?> found = this.lookup(Objects.requireNonNull(type, "type"));
        return found == NONE ? Optional.empty() : Optional.of((Appearance<? super T>) found);
    }

    @Override
    public Optional<Visual> iconOf(final Object value) {
        return this.of(value).icon(value);
    }

    @Override
    public Text nameOf(final Object value) {
        return this.of(value).name(value);
    }

    @Override
    public Optional<Tooltip> tooltipOf(final Object value) {
        return this.of(value).tooltip(value);
    }

    @SuppressWarnings("unchecked")
    private Appearance<Object> of(final Object value) {
        Objects.requireNonNull(value, "value");
        return (Appearance<Object>) this.lookup(value.getClass());
    }

    private Appearance<?> lookup(final Class<?> type) {
        final Appearance<?> cached = this.cache.get(type);
        if (cached != null) {
            return cached;
        }
        // Searched and cached under the lock, so a registration that clears the cache cannot be followed by a stale put
        synchronized (this) {
            final Appearance<?> found = this.search(type);
            this.cache.put(type, found);
            return found;
        }
    }

    /**
     * Finds the nearest registration: the class and its superclasses first, then interfaces breadth first. Called
     * with the lock held.
     */
    private Appearance<?> search(final Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            final Appearance<?> top = this.top(current);
            if (top != null) {
                return top;
            }
        }
        List<Class<?>> level = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            level.addAll(List.of(current.getInterfaces()));
        }
        final Set<Class<?>> seen = new HashSet<>();
        while (!level.isEmpty()) {
            Class<?> best = null;
            for (final Class<?> candidate : level) {
                if (this.top(candidate) != null
                        && (best == null || this.order.get(candidate) < this.order.get(best))) {
                    best = candidate;
                }
            }
            if (best != null) {
                return this.top(best);
            }
            final List<Class<?>> next = new ArrayList<>();
            for (final Class<?> candidate : level) {
                if (seen.add(candidate)) {
                    next.addAll(List.of(candidate.getInterfaces()));
                }
            }
            level = next;
        }
        return NONE;
    }

    private Appearance<?> top(final Class<?> type) {
        final Deque<Appearance<?>> stack = this.registered.get(type);
        return stack == null ? null : stack.peekFirst();
    }
}
