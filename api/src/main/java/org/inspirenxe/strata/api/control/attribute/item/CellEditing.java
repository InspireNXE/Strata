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
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

/**
 * A control whose cells can be edited in place, such as a list, a tree, a table or a grid. A cell is editable once a
 * {@linkplain CellEditor cell editor} is set on the control, or on a table's column, with
 * {@link CellSource#setCellEditor}.
 *
 * <p>The user starts an edit of the current entry with F2, or of an entry with a double click on it unless the
 * control {@linkplain Activatable#setActivateOnClick activates on a single click}; on an editable control that double
 * click edits and does not activate. A typed character starts an edit too, on a control with no type-ahead, which a
 * list, a tree and a table turn off with {@code setTypeAhead(null)} and a grid never has; the character then goes to
 * the editor. The control asks {@link #EDIT_CELL}, and if no handler cancels it, puts the editor in place of the cell
 * and focuses it. Only one cell is edited at a time, and the edited cell stays built while it scrolls out of view.
 * New items, a refresh, a new sort or new columns cancel the edit.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <T> the type of the items
 * @param <S> the control's own type, which the methods return
 */
public interface CellEditing<T, S extends CellEditing<T, S>> extends EventTarget<S> {
    /**
     * Asked before the user starts editing a cell. The value is the item. A handler may cancel it to keep the cell
     * as it is; a value set by a handler is ignored. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<CellEditing<?, ?>, Object>> EDIT_CELL = EventType.valueRequest("cell_editing.edit");
    /**
     * Told after an edit was committed. The value is the item the editor committed and the previous value the item
     * that was edited. The control has put the cell back and has not changed its items.
     */
    EventType<ValueEvent<CellEditing<?, ?>, Object>> CELL_EDITED = EventType.valueReport("cell_editing.edited");

    /**
     * Returns whether a cell is being edited.
     *
     * @return true from the start of an edit until it is committed or canceled
     */
    boolean isEditing();

    /**
     * Ends the edit with no change and puts the cell back, as the editor's {@code cancel} does. Does nothing while no
     * cell is being edited.
     *
     * @return this control
     */
    S cancelEdit();
}
