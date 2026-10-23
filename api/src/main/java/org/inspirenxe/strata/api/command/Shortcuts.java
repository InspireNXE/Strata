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
package org.inspirenxe.strata.api.command;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.Subscription;

/**
 * The commands a host runs from their {@linkplain Command#shortcut() shortcuts}, from
 * {@link org.inspirenxe.strata.api.ComponentFactory#shortcuts()}.
 *
 * <p>A key goes along the focus path first, so a text box keeps Control+A, Control+C and its other keys. If nothing
 * consumed a press or repeat, the host tries the commands added with a scope, for the focused component and then
 * each of its ancestors, innermost first, then those added for the whole host, then those of the menu bars in the
 * top layer. The first enabled command whose stroke matches runs and uses up the key. While a modal dialog is open,
 * only commands scoped to something inside it match; popups, flyouts and menus are not modal, so a host's shortcut
 * still runs while one has focus.</p>
 *
 * <p>A command runs on the UI thread inside the key's input frame with the command pushed on the cause chain, so
 * what it changes reports {@link org.inspirenxe.strata.api.event.cause.Origin.User} as the origin. An action runs its
 * {@link Command.Action#run() run}; a toggle flips its state.</p>
 *
 * <p>Strata provides this. Do not implement this interface.</p>
 */
public interface Shortcuts {

    /**
     * Runs a command from its shortcut anywhere in the host, while no modal dialog is open. A command without a
     * shortcut is held and never matches.
     *
     * @param command the command
     * @return a subscription that removes the command again
     */
    Subscription add(Command command);

    /**
     * Runs a command from its shortcut while focus is on a component or inside it. The command matches before
     * commands added for the whole host, and before those of any component further out.
     *
     * @param scope the component focus must be in
     * @param command the command
     * @return a subscription that removes the command again
     * @throws IllegalArgumentException if the scope was made by another factory
     */
    Subscription add(Component<?> scope, Command command);
}
