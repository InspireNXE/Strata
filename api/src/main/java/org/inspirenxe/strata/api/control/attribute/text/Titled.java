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
package org.inspirenxe.strata.api.control.attribute.text;

/**
 * A control or entry with a title over its content, such as a window, an expander or a table column.
 *
 * <p>Only Strata implements this interface.</p>
 *
 * @param <S> the control's own type, which the setter returns
 */
public interface Titled<S extends Titled<S>> {

    /**
     * Returns the title.
     *
     * @return the title, never null; empty when there is none
     */
    String title();

    /**
     * Replaces the title.
     *
     * @param title the title, or an empty string for none
     * @return this control
     */
    S setTitle(String title);
}
