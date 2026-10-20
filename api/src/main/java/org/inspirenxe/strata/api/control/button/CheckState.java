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
package org.inspirenxe.strata.api.control.button;

/**
 * What a {@link CheckBox} shows.
 */
public enum CheckState {
    /**
     * An empty box.
     */
    UNCHECKED,
    /**
     * A checked box.
     */
    CHECKED,
    /**
     * Neither checked nor empty, drawn as a filled square: what a box that stands for several others shows while only
     * some of them are checked.
     */
    MIXED;

    /**
     * Returns whether this is {@link #CHECKED}, which is what {@link CheckBox#isSelected()} reports.
     *
     * @return whether checked
     */
    public boolean isChecked() {
        return this == CHECKED;
    }
}
