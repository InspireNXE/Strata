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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;

import java.util.function.Consumer;

/**
 * Builds the component that edits an item where its cell is shown, for a control that {@linkplain CellEditing edits
 * cells in place}. Set one with {@link CellSource#setCellEditor}.
 *
 * <p>The control puts the component in place of the cell, laid out in the cell's area, and focuses its first tab
 * stop, or the component itself when it has none. The editor ends the edit: it calls {@code commit} with the new item,
 * usually on Enter and when it loses focus, or {@code cancel}, usually on Escape. Only the first of the two counts, and
 * neither counts once the edit ended another way, such as by {@link CellEditing#cancelEdit()} or new items. The
 * control then puts the cell back and leaves its items alone; store the new item in the project's data and call
 * {@link ItemSource#refresh(Object)} or {@link ItemSource#setItems}.</p>
 *
 * <pre>{@code
 * factory.<Setting>tableColumn("Value").setCellText(Setting::value)
 *         .setCellEditor((ui, setting, commit, cancel) -> ui.textBox().setText(setting.value())
 *                 .onEvent(TextBox.SUBMITTED, e -> commit.accept(setting.withValue(e.value())))
 *                 .onEvent(Focusable.FOCUS_LOST, e -> cancel.run()));
 * }</pre>
 *
 * @param <T> the type of the items
 */
@FunctionalInterface
public interface CellEditor<T> {
    /**
     * Builds the editor for an item. Called on the UI thread each time an edit starts.
     *
     * @param factory the factory to build the editor with
     * @param item the item being edited
     * @param commit ends the edit with a new item, which the control reports with {@link CellEditing#CELL_EDITED}
     * @param cancel ends the edit with no change
     * @return a new component, not yet added anywhere
     */
    Component<?> edit(ComponentFactory factory, T item, Consumer<? super T> commit, Runnable cancel);
}
