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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.core.node.Node;

/**
 * How controls show and run {@link Command}s. A control with a command takes its text and binds
 * {@link Component#ENABLED}, and a toggle's {@link Toggle#SELECTED} both ways, through its node; a cleared command
 * takes the bindings off again and leaves the text.
 */
public final class Commands {
    private Commands() {
    }

    /**
     * Makes a control follow a command, or stop following one.
     *
     * @param node the control's node; its facade must have {@link HasText}, and {@link Toggle} for a toggle
     * @param command the command, or null to stop following the previous one
     */
    public static void follow(final Node node, final Command command) {
        if (command == null) {
            node.unbind(Component.ENABLED);
            node.unbind(Toggle.SELECTED);
            return;
        }
        ((HasText<?>) node.facade()).setText(command.text());
        node.bind(Component.ENABLED, command.enabled());
        if (command instanceof Command.Toggle toggle) {
            node.bind(Toggle.SELECTED, toggle.selected());
        } else {
            node.unbind(Toggle.SELECTED);
        }
    }

    /**
     * Returns whether a command is enabled now.
     *
     * @param command the command
     * @return true when its enabled source gives true
     */
    public static boolean isEnabled(final Command command) {
        return Boolean.TRUE.equals(command.enabled().get());
    }

    /**
     * Runs a command if it is enabled: an action's {@code run}, or a toggle's flip, with the command pushed on the
     * cause chain inside whatever frame is open.
     *
     * @param causes the cause stack
     * @param command the command, or null to do nothing
     */
    public static void run(final CauseStack causes, final Command command) {
        if (command == null || !Commands.isEnabled(command)) {
            return;
        }
        try (CauseFrame _ = causes.pushFrame().push(command)) {
            switch (command) {
                case Command.Action action -> action.run().run();
                case Command.Toggle toggle -> toggle.selected().set(!Boolean.TRUE.equals(toggle.selected().get()));
            }
        }
    }
}
