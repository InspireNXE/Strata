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
 * Whether a {@link MultiSelection} control selects one entry or several.
 */
public enum SelectionMode {
    /**
     * At most one entry, the current one.
     */
    SINGLE,
    /**
     * Any number of entries. A click selects one, Control and a click adds or removes one, Shift and a click selects
     * the range from the anchor, Shift and the arrow keys extend the range, and Control+A selects every entry.
     */
    MULTIPLE
}
