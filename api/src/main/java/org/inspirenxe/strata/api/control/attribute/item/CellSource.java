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

import org.inspirenxe.strata.api.component.Component;

import java.util.function.Function;

/**
 * A control that shows each item through a component built by a cell factory, such as a list, a tree or a grid, or a
 * table column, which builds the cells of its column.
 *
 * <p>A control may build cells only for the items in view and drop them when they scroll out, so keep nothing in a
 * cell that must outlive it; build it from the item. Events from inside a cell, such as a button's
 * {@code PRESSED}, go through the control and reach the containers around it.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <T> the type of the items
 * @param <S> the control's own type, which the setter returns
 */
public interface CellSource<T, S extends CellSource<T, S>> {

    /**
     * Sets what builds the component for an item, and builds the cells in view again.
     *
     * @param cellFactory makes a new component for an item each time its cell is built
     * @return this control
     */
    S setCellFactory(Function<? super T, ? extends Component<?>> cellFactory);
}
