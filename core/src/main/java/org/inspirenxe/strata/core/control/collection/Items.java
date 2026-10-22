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
package org.inspirenxe.strata.core.control.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * The item handling the collection controls share: copying the items they are given, finding an item by key,
 * working out where the selection moves when the items change, and checking the indexes requests end with.
 */
final class Items {
    private static final IntUnaryOperator NOWHERE = row -> -1;

    private Items() {
    }

    /**
     * Copies items that must not hold null.
     *
     * @throws NullPointerException naming the first null item's index
     */
    static <T> List<T> copyOf(final List<? extends T> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == null) {
                throw new NullPointerException("Item " + i + " is null; this control holds no null items");
            }
        }
        return List.copyOf(items);
    }

    /**
     * Finds the first item whose key equals a key. Null items, which only a grid holds, never match.
     *
     * @return the item's index, or -1 if no item has the key
     */
    static <T> int indexOfKey(final List<? extends T> items, final Function<? super T, ?> keys, final Object key) {
        for (int i = 0; i < items.size(); i++) {
            final T candidate = items.get(i);
            if (candidate != null && Objects.equals(key, keys.apply(candidate))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the first item with the same key as a given one. A null item matches nothing, so it clears the selection
     * of the control that looks it up.
     *
     * @return the item's index, or -1 for null or if no item has the key
     */
    static <T> int indexOf(final List<? extends T> items, final Function<? super T, ?> keys, final T item) {
        return item == null ? -1 : Items.indexOfKey(items, keys, keys.apply(item));
    }

    /**
     * Maps each key to the first index that has it.
     */
    static <T> Map<Object, Integer> firstIndexes(final List<? extends T> items, final Function<? super T, ?> keys) {
        final Map<Object, Integer> first = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            first.putIfAbsent(keys.apply(items.get(i)), i);
        }
        return first;
    }

    /**
     * Works out where some selected rows move when the items change, matching by key. Items with equal keys are
     * matched in order: the second item with a key moves to the second item with that key, and to the last one when
     * there are fewer. A row whose key no new item has moves nowhere. The key function runs here, before the control
     * changes anything, so a key function that throws leaves the control as it was.
     *
     * @param before the items the rows index
     * @param after the new items
     * @param keys the key function
     * @param rows the selected rows
     * @return the new index of each of the rows, or -1; -1 for any other row
     */
    static <T> IntUnaryOperator moves(final List<? extends T> before, final List<? extends T> after,
                                      final Function<? super T, ?> keys, final Collection<Integer> rows) {
        if (rows.isEmpty()) {
            return NOWHERE;
        }
        final Set<Integer> wanted = new HashSet<>(rows);
        int last = -1;
        for (final int row : wanted) {
            last = Math.max(last, row);
        }
        // The key of each selected row and how many earlier rows share it
        final Map<Object, Integer> counts = new HashMap<>();
        final Map<Integer, Object> keyOf = new HashMap<>();
        final Map<Integer, Integer> rankOf = new HashMap<>();
        for (int i = 0; i <= last; i++) {
            final Object key = keys.apply(before.get(i));
            final int rank = counts.merge(key, 1, Integer::sum) - 1;
            if (wanted.contains(i)) {
                keyOf.put(i, key);
                rankOf.put(i, rank);
            }
        }
        final Set<Object> keysWanted = new HashSet<>(keyOf.values());
        final Map<Object, List<Integer>> found = new HashMap<>();
        for (int i = 0; i < after.size(); i++) {
            final Object key = keys.apply(after.get(i));
            if (keysWanted.contains(key)) {
                found.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }
        final Map<Integer, Integer> moved = new HashMap<>();
        for (final int row : wanted) {
            final List<Integer> candidates = found.get(keyOf.get(row));
            moved.put(row, candidates == null ? -1 : candidates.get(Math.min(rankOf.get(row), candidates.size() - 1)));
        }
        return row -> moved.getOrDefault(row, -1);
    }

    /**
     * Returns whether a value a request ended with is one of a count of entries.
     */
    static boolean isIndex(final Integer index, final int count) {
        return index != null && index >= 0 && index < count;
    }

    /**
     * Returns whether a value a request ended with is one of a count of entries, or -1 for none.
     */
    static boolean isIndexOrNone(final Integer index, final int count) {
        return index != null && index >= -1 && index < count;
    }
}
