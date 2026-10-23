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
package org.inspirenxe.strata.core.node;

/**
 * A built-in menu bar that Alt pressed and released alone, or F10, moves to. {@link Root} finds the first visible,
 * enabled one in the top dialog, or in the content while no dialog is open, and gives it the keys with
 * {@link Root#grabKeys} until it lets go or the grab ends.
 */
public interface KeyboardMenu {
    /**
     * Highlights the bar's first title that opens a menu and takes the keys.
     *
     * @return true if the bar has such a title and took the keys
     */
    boolean enterKeyboard();
}
