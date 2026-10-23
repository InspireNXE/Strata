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

import org.inspirenxe.strata.api.command.Command;

import java.util.List;

/**
 * A built-in component whose commands run from their shortcuts while it is attached and shown, as a menu bar's do.
 * {@link Root} asks the visible, enabled sources in the top dialog, or in the content while no dialog is open, after
 * the commands of the host's {@code Shortcuts}.
 */
public interface CommandSource {
    /**
     * Adds the commands whose shortcuts should run, in the order they should be tried.
     *
     * @param out the list to add to
     */
    void collectCommands(List<Command> out);
}
