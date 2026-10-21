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

/**
 * A control that only ever selects one entry, such as a combo box, a grid, a tab bar, a menu bar or a radio group.
 * It adds nothing to {@link Selection}; it says that the current entry is the whole selection. Controls that can
 * select several entries implement {@link MultiSelection} instead.
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <T> the type of the entries
 * @param <S> the control's own type, which the setters return
 */
public interface SingleSelection<T, S extends SingleSelection<T, S>> extends Selection<T, S> {
}
