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
package org.inspirenxe.strata.api.control.attribute.item;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A control whose rows the user can drag, to reorder them or to drop them somewhere else, such as a list.
 *
 * <p>Rows are dragged once a {@linkplain #setDragTransfer drag transfer} is set. A left drag that starts on a row
 * drags the selected rows when the row is selected, which a press selects, and that row alone otherwise; a plain
 * press on one of several selected rows keeps them selected until its release, so the drag carries them all. The
 * function makes the payload from their items, and the drag allows {@code MOVE} and {@code COPY}. Its drag image is
 * the first item's cell, built again from the cell factory. Over the control itself the drag shows a line where the
 * rows would land, and the drop asks {@link #MOVE_ITEMS}. Drops from elsewhere are the project's: handle
 * {@code InputEvents.DRAG_OVER} and {@code InputEvents.DROP} on the control. After a {@code MOVE} to another
 * component the source hears {@code InputEvents.TRANSFER_DONE}, and the project removes the rows it gave away with
 * {@code setItems}, since the items are its data.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <T> the type of the items
 * @param <S> the control's own type, which the setter returns
 */
public interface ItemDragging<T, S extends ItemDragging<T, S>> extends EventTarget<S> {
    /**
     * Asked before the user moves rows inside the control by dragging them there. The value says which rows move
     * and where. A handler may cancel it, or rewrite it to another move; a move whose indexes are not rows of the
     * control, or one that changes nothing, is ignored. If no handler cancels it, the control moves the items with
     * {@code setItems}, which keeps the selection on the moved items by key, so the project that holds the items in a
     * state sets the moved list there, from the request or from the control's items after it. Fires only for the
     * user's actions.
     */
    EventType<ValueRequestEvent<ItemDragging<?, ?>, ItemMove>> MOVE_ITEMS = EventType.valueRequest("item_dragging.move");

    /**
     * Returns what makes the payload of dragged rows.
     *
     * @return the function, or empty while rows are not dragged, the default
     */
    Optional<Function<? super List<T>, Transfer>> dragTransfer();

    /**
     * Sets what makes the payload of dragged rows from their items, in row order. A function that returns null or an
     * empty transfer starts no drag.
     *
     * @param transfer the function, or {@code null} to stop rows from being dragged
     * @return this control
     */
    S setDragTransfer(Function<? super List<T>, Transfer> transfer);

    /**
     * Rows moved inside a control: which rows, and where they go.
     *
     * @param indexes the rows' indexes in the items before the move, ascending and without repeats, never empty
     * @param before the index, in the items before the move, of the row the moved rows go in front of, or the
     *     number of items to put them at the end
     */
    record ItemMove(List<Integer> indexes, int before) {
        /**
         * Checks and copies the indexes.
         *
         * @param indexes the rows' indexes, ascending and without repeats, never empty
         * @param before where they go
         * @throws IllegalArgumentException if the indexes are empty, not ascending, repeat or are negative, or
         *     {@code before} is negative
         */
        public ItemMove {
            indexes = List.copyOf(Objects.requireNonNull(indexes, "indexes"));
            if (indexes.isEmpty()) {
                throw new IllegalArgumentException("A move needs at least one row");
            }
            int last = -1;
            for (final int index : indexes) {
                if (index <= last) {
                    throw new IllegalArgumentException("Indexes must ascend without repeats and not be negative: "
                            + indexes);
                }
                last = index;
            }
            if (before < 0) {
                throw new IllegalArgumentException("Where rows go must not be negative: " + before);
            }
        }

        /**
         * Returns the items after this move.
         *
         * @param <T> the type of the items
         * @param items the items before
         * @return a new list with the moved items in front of {@code before}, in their order
         * @throws IndexOutOfBoundsException if an index is not an item's, or {@code before} is past the end
         */
        public <T> List<T> apply(final List<T> items) {
            Objects.checkIndex(this.indexes.get(this.indexes.size() - 1), items.size());
            Objects.checkIndex(this.before, items.size() + 1);
            final List<T> moved = new ArrayList<>(this.indexes.size());
            final List<T> rest = new ArrayList<>(items.size());
            int at = 0;
            int insert = 0;
            for (int i = 0; i < items.size(); i++) {
                if (at < this.indexes.size() && this.indexes.get(at) == i) {
                    moved.add(items.get(i));
                    at++;
                } else {
                    rest.add(items.get(i));
                    if (i < this.before) {
                        insert++;
                    }
                }
            }
            rest.addAll(insert, moved);
            return rest;
        }
    }
}
