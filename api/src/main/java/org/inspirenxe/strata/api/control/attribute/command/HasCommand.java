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
package org.inspirenxe.strata.api.control.attribute.command;

import org.inspirenxe.strata.api.command.Command;

import java.util.Optional;

/**
 * A control or entry that can show a {@link Command}. It has no events of its own, so menu entries, which are not
 * components, can have it too.
 *
 * <p>A control with a command follows it. Its text becomes the command's text when the command is set. Its enabled
 * state follows the command's {@linkplain Command#enabled() enabled source}, as a binding to
 * {@link org.inspirenxe.strata.api.component.Component#ENABLED} would, and a menu entry follows it while its menu is
 * attached. A menu item shows the command's stroke as its shortcut hint. Pressing the control runs an
 * {@linkplain Command.Action action} after the control's own request and report, and a control showing a
 * {@linkplain Command.Toggle toggle} is bound to its state both ways, so a check item and a toolbar toggle share one
 * state.</p>
 *
 * <p>Clearing the command stops the enabled state and the toggle state following it and leaves the text as it is.
 * Setting the control's text afterwards replaces the command's text without clearing the command.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <C> the kind of command the control takes: {@link Command.Action} or {@link Command.Toggle}
 * @param <S> the control's own type, which the setter returns
 */
public interface HasCommand<C extends Command, S extends HasCommand<C, S>> {

    /**
     * Returns the command the control shows.
     *
     * @return the command, or empty for none
     */
    Optional<C> command();

    /**
     * Shows a command, replacing any other, or clears it.
     *
     * @param command the command, or {@code null} to clear it
     * @return this control
     */
    S setCommand(C command);
}
