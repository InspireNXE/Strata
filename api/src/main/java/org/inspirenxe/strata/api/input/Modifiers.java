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
package org.inspirenxe.strata.api.input;

import java.util.StringJoiner;

/**
 * An immutable set of held {@link Modifier}s. It is packed into an int, so input events carry it without
 * allocating. Two sets are equal when they hold the same modifiers.
 *
 * @param bits one bit per {@link Modifier}, by ordinal; use the factory methods instead of building bits by hand
 */
public record Modifiers(int bits) {
    /**
     * The empty set: no modifier held.
     */
    public static final Modifiers NONE = new Modifiers(0);

    /**
     * Creates a set holding the given modifiers.
     *
     * @param modifiers the modifiers to hold; repeats are ignored
     * @return the set
     */
    public static Modifiers of(final Modifier... modifiers) {
        int bits = 0;
        for (final Modifier modifier : modifiers) {
            bits |= modifier.bit();
        }
        return new Modifiers(bits);
    }

    /**
     * Returns whether a modifier is in the set.
     *
     * @param modifier the modifier to look for
     * @return true if it is held
     */
    public boolean has(final Modifier modifier) {
        return (this.bits & modifier.bit()) != 0;
    }

    /**
     * Returns whether Shift is held.
     *
     * @return true if {@link Modifier#SHIFT} is in the set
     */
    public boolean shift() {
        return this.has(Modifier.SHIFT);
    }

    /**
     * Returns whether Control is held.
     *
     * @return true if {@link Modifier#CONTROL} is in the set
     */
    public boolean control() {
        return this.has(Modifier.CONTROL);
    }

    /**
     * Returns whether Alt is held.
     *
     * @return true if {@link Modifier#ALT} is in the set
     */
    public boolean alt() {
        return this.has(Modifier.ALT);
    }

    /**
     * Returns whether Super is held.
     *
     * @return true if {@link Modifier#SUPER} is in the set
     */
    public boolean superKey() {
        return this.has(Modifier.SUPER);
    }

    /**
     * Returns whether no modifier is held.
     *
     * @return true for the empty set
     */
    public boolean isEmpty() {
        return this.bits == 0;
    }

    /**
     * Returns this set with a modifier added.
     *
     * @param modifier the modifier to add
     * @return the new set; equal to this one if the modifier was already held
     */
    public Modifiers with(final Modifier modifier) {
        return new Modifiers(this.bits | modifier.bit());
    }

    /**
     * Returns this set with a modifier removed.
     *
     * @param modifier the modifier to remove
     * @return the new set; equal to this one if the modifier was not held
     */
    public Modifiers without(final Modifier modifier) {
        return new Modifiers(this.bits & ~modifier.bit());
    }

    /**
     * Returns whether this set holds every modifier in another.
     *
     * @param other the modifiers that must be held
     * @return true if each of them is in this set
     */
    public boolean containsAll(final Modifiers other) {
        return (this.bits & other.bits) == other.bits;
    }

    /**
     * Lists the held modifiers by name, for debugging.
     *
     * @return for example {@code Modifiers[SHIFT+CONTROL]}
     */
    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner("+", "Modifiers[", "]");
        for (final Modifier modifier : Modifier.values()) {
            if (this.has(modifier)) {
                joiner.add(modifier.name());
            }
        }
        return joiner.toString();
    }
}
