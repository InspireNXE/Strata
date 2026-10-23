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
package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The style states of one component and the styles they pick. The states the node keeps, {@value #DISABLED},
 * {@value #PRESSED}, {@value #HOVERED} and {@value #FOCUSED}, come in as flags on each lookup; the component's own
 * states are named here, in the order they were first set. A lookup of a key tries {@code key.disabled}, then
 * {@code key.<state>} for each of the component's own states that is on, in that order, then {@code key.pressed},
 * {@code key.hovered} and {@code key.focused} for those that are on, and last the key itself.
 *
 * <p>The result of each key for each combination of states is kept until the theme changes, so a lookup allocates
 * nothing after the first with the same states.</p>
 */
final class StyleStates {
    /**
     * The state of a component that is disabled, itself or through an ancestor.
     */
    static final String DISABLED = "disabled";
    /**
     * The state of a component a pointer button went down on and is still held.
     */
    static final String PRESSED = "pressed";
    /**
     * The state of the deepest component under the pointer.
     */
    static final String HOVERED = "hovered";
    /**
     * The state of the component with keyboard focus.
     */
    static final String FOCUSED = "focused";

    private static final Set<String> BUILT_IN = Set.of(DISABLED, PRESSED, HOVERED, FOCUSED);
    // The node's states after the component's own, in the order a lookup tries them; their bits start at 2
    private static final String[] AFTER_OWN = {PRESSED, HOVERED, FOCUSED};
    // The node's states take the four low bits of a combination, the component's own the rest
    private static final int OWN_SHIFT = 4;
    private static final int MAX_OWN = Long.SIZE - OWN_SHIFT;
    // A cached lookup that found no key, so the caller's fallback applies
    private static final Object MISSING = new Object();

    private final List<String> own = new ArrayList<>();
    private long ownOn;
    private final Map<String, Cache> paints = new HashMap<>();
    private final Map<String, Cache> texts = new HashMap<>();
    private Theme cachedFor;

    /**
     * Turns one of the component's own states on or off.
     *
     * @param state the state's name
     * @param on whether the state is on
     * @throws IllegalArgumentException if the name is empty or one the node keeps, or the component already has as
     *     many states as a combination can hold
     */
    void set(final String state, final boolean on) {
        Objects.requireNonNull(state, "state");
        if (state.isEmpty() || BUILT_IN.contains(state)) {
            throw new IllegalArgumentException("Not a state a component can set: '" + state + "'");
        }
        int index = this.own.indexOf(state);
        if (index < 0) {
            if (!on) {
                return;
            }
            if (this.own.size() == MAX_OWN) {
                throw new IllegalArgumentException("A component can have at most " + MAX_OWN + " states");
            }
            this.own.add(state);
            index = this.own.size() - 1;
        }
        this.ownOn = on ? this.ownOn | 1L << index : this.ownOn & ~(1L << index);
    }

    /**
     * Returns the combination of states for the node's flags and the component's own states.
     *
     * @param disabled whether the component is disabled
     * @param pressed whether it is pressed
     * @param hovered whether it is hovered
     * @param focused whether it has focus
     * @return the combination
     */
    long combination(final boolean disabled, final boolean pressed, final boolean hovered, final boolean focused) {
        return (disabled ? 1L : 0L) | (pressed ? 2L : 0L) | (hovered ? 4L : 0L) | (focused ? 8L : 0L)
                | this.ownOn << OWN_SHIFT;
    }

    /**
     * Looks up a paint for a combination of states.
     *
     * @param theme the component's theme
     * @param key the style key
     * @param fallback returned when no key of the lookup has a paint
     * @param states the combination, from {@link #combination}
     * @return the paint
     */
    Paint paint(final Theme theme, final String key, final Paint fallback, final long states) {
        final Object found = this.lookup(this.paints, theme, key, states, true);
        return found == MISSING ? fallback : (Paint) found;
    }

    /**
     * Looks up a text style for a combination of states.
     *
     * @param theme the component's theme
     * @param key the style key
     * @param fallback returned when no key of the lookup has a text style
     * @param states the combination, from {@link #combination}
     * @return the text style
     */
    TextStyle text(final Theme theme, final String key, final TextStyle fallback, final long states) {
        final Object found = this.lookup(this.texts, theme, key, states, false);
        return found == MISSING ? fallback : (TextStyle) found;
    }

    private Object lookup(final Map<String, Cache> caches, final Theme theme, final String key, final long states,
                          final boolean paint) {
        if (theme != this.cachedFor) {
            this.paints.clear();
            this.texts.clear();
            this.cachedFor = theme;
        }
        Cache cache = caches.get(key);
        if (cache == null) {
            cache = new Cache();
            caches.put(key, cache);
        }
        final int index = cache.indexOf(states);
        if (index >= 0) {
            return cache.values[index];
        }
        final Object found = this.resolve(theme, key, states, paint);
        cache.add(states, found);
        return found;
    }

    private Object resolve(final Theme theme, final String key, final long states, final boolean paint) {
        if ((states & 1L) != 0L) {
            final Object found = find(theme, key + "." + DISABLED, paint);
            if (found != MISSING) {
                return found;
            }
        }
        for (int i = 0; i < this.own.size(); i++) {
            if ((states & 1L << (i + OWN_SHIFT)) != 0L) {
                final Object found = find(theme, key + "." + this.own.get(i), paint);
                if (found != MISSING) {
                    return found;
                }
            }
        }
        for (int i = 0; i < AFTER_OWN.length; i++) {
            if ((states & 2L << i) != 0L) {
                final Object found = find(theme, key + "." + AFTER_OWN[i], paint);
                if (found != MISSING) {
                    return found;
                }
            }
        }
        return find(theme, key, paint);
    }

    private static Object find(final Theme theme, final String key, final boolean paint) {
        final Optional<?> found = paint ? theme.paint(key) : theme.textStyle(key);
        return found.isPresent() ? found.get() : MISSING;
    }

    /**
     * The results of one key, by combination of states.
     */
    private static final class Cache {
        private long[] states = new long[2];
        private Object[] values = new Object[2];
        private int size;

        private int indexOf(final long combination) {
            for (int i = 0; i < this.size; i++) {
                if (this.states[i] == combination) {
                    return i;
                }
            }
            return -1;
        }

        private void add(final long combination, final Object value) {
            if (this.size == this.states.length) {
                this.states = Arrays.copyOf(this.states, this.size * 2);
                this.values = Arrays.copyOf(this.values, this.size * 2);
            }
            this.states[this.size] = combination;
            this.values[this.size] = value;
            this.size++;
        }
    }
}
