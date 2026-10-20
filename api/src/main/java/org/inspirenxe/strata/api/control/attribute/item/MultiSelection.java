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

import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

import java.util.Collection;
import java.util.List;

/**
 * A control that can select several entries, such as a list, a table or a tree. In {@link SelectionMode#SINGLE} mode,
 * the default, it selects at most one, as a {@link SingleSelection} does. In {@link SelectionMode#MULTIPLE} mode it
 * holds a set of selected entries beside the {@linkplain Selection current} one.
 *
 * <p>Indexes are the same as {@link Selection}'s. Every change of the set fires {@link #SELECTED_MANY}, and then
 * {@link Selection#SELECTED} if the current entry moved too. In {@code MULTIPLE} mode the user's changes ask
 * {@link #SELECT_MANY}; in {@code SINGLE} mode they ask {@link Selection#SELECT}, as before.</p>
 *
 * <pre>{@code
 * list.setSelectionMode(SelectionMode.MULTIPLE)
 *         .onEvent(MultiSelection.SELECTED_MANY, event -> remove.setEnabled(!event.value().isEmpty()));
 * }</pre>
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <T> the type of the entries
 * @param <S> the control's own type, which the setters return
 */
public interface MultiSelection<T, S extends MultiSelection<T, S>> extends Selection<T, S> {
    /**
     * Asked before the user changes the selected set in {@link SelectionMode#MULTIPLE} mode. The value is the indexes
     * about to be selected, in order. A handler may rewrite it to other indexes; the control ignores indexes it does
     * not show. Fires only for the user's actions, and only when the set would change.
     */
    EventType<ValueRequestEvent<MultiSelection<?, ?>, List<Integer>>> SELECT_MANY = EventType.valueRequest("selection.select_many");
    /**
     * Told after the selected set changed, in either mode, from the user or code. The value is the selected indexes
     * in order and the previous value the indexes before.
     */
    EventType<ValueEvent<MultiSelection<?, ?>, List<Integer>>> SELECTED_MANY = EventType.valueReport("selection.selected_many");

    /**
     * Returns the selection mode.
     *
     * @return the mode; {@link SelectionMode#SINGLE} by default
     */
    SelectionMode selectionMode();

    /**
     * Sets the selection mode. Going to {@link SelectionMode#SINGLE} keeps only the current entry selected.
     *
     * @param mode the mode
     * @return this control
     */
    S setSelectionMode(SelectionMode mode);

    /**
     * Returns the selected indexes.
     *
     * @return the indexes, in order; empty when nothing is selected
     */
    List<Integer> selectedIndexes();

    /**
     * Returns the selected entries.
     *
     * @return the entries at {@link #selectedIndexes()}, in order
     */
    List<T> selectedItems();

    /**
     * Replaces the selected set. The current entry stays if it is in the set, and is otherwise the last index in it,
     * or none. Fires {@link #SELECTED_MANY} if the set changed and {@link Selection#SELECTED} if the current entry
     * did, and never a request.
     *
     * @param indexes the indexes to select; duplicates count once
     * @return this control
     * @throws IndexOutOfBoundsException if an index is not an entry's index
     * @throws IllegalArgumentException if more than one index is given in {@link SelectionMode#SINGLE} mode
     */
    S selectIndexes(Collection<Integer> indexes);

    /**
     * Selects every entry in {@link SelectionMode#MULTIPLE} mode. Does nothing in {@link SelectionMode#SINGLE} mode.
     *
     * @return this control
     */
    S selectAll();
}
