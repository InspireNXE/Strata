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

import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * Something the user can do from a button, a menu item or a key, such as Save. A command is a value: the state it
 * shows lives in sources, so a button and a menu item given the same command enable and disable together.
 *
 * <pre>{@code
 * final State<Boolean> dirty = State.of(false);
 * final Command.Action save = Command.of(Text.translatable("editor.save"), this::save)
 *         .withShortcut(KeyStroke.shortcut(Key.S))
 *         .withEnabled(dirty);
 * toolbar.add(ui.button().setCommand(save));
 * fileMenu.addItem(ui.menuItem().setCommand(save));
 * ui.shortcuts().add(save);
 * }</pre>
 *
 * <p>A control that shows a command takes its text, follows its enabled source, and runs it when pressed, after
 * its own request and report. See {@link org.inspirenxe.strata.api.control.attribute.command.HasCommand}. A
 * command's shortcut does nothing until the command is added to
 * {@link org.inspirenxe.strata.api.ComponentFactory#shortcuts()} or shown in a menu bar's menus.</p>
 */
public sealed interface Command permits Command.Action, Command.Toggle {

    /**
     * Returns the caption that controls showing the command take.
     *
     * @return the text
     */
    Text text();

    /**
     * Returns the key stroke that runs the command once it is added to the host's shortcuts, and that menus show as
     * the item's hint.
     *
     * @return the stroke, or {@code null} for none
     */
    KeyStroke shortcut();

    /**
     * Returns whether the command can run now. Controls showing the command are enabled while it gives true, and a
     * shortcut runs only then.
     *
     * @return the source, read on the UI thread
     */
    Source<Boolean> enabled();

    /**
     * Creates an action with no shortcut that is always enabled.
     *
     * @param text the caption
     * @param run what the action does, on the UI thread
     * @return the action
     */
    static Action of(final Text text, final Runnable run) {
        return new Action(text, null, () -> Boolean.TRUE, run);
    }

    /**
     * Creates an action with no shortcut that is always enabled, captioned with plain text.
     *
     * @param text the caption
     * @param run what the action does, on the UI thread
     * @return the action
     */
    static Action of(final String text, final Runnable run) {
        return Command.of(Text.of(text), run);
    }

    /**
     * Creates a toggle with no shortcut that is always enabled.
     *
     * @param text the caption
     * @param selected the state the toggle shows and flips
     * @return the toggle
     */
    static Toggle toggle(final Text text, final State<Boolean> selected) {
        return new Toggle(text, null, () -> Boolean.TRUE, selected);
    }

    /**
     * Creates a toggle with no shortcut that is always enabled, captioned with plain text.
     *
     * @param text the caption
     * @param selected the state the toggle shows and flips
     * @return the toggle
     */
    static Toggle toggle(final String text, final State<Boolean> selected) {
        return Command.toggle(Text.of(text), selected);
    }

    /**
     * A command that does something when run, shown by a button or a menu item.
     *
     * @param text the caption
     * @param shortcut the key stroke, or {@code null} for none
     * @param enabled whether the action can run now
     * @param run what the action does, on the UI thread
     */
    record Action(Text text, KeyStroke shortcut, Source<Boolean> enabled, Runnable run) implements Command {
        /**
         * Checks the components.
         *
         * @param text the caption
         * @param shortcut the key stroke, or {@code null} for none
         * @param enabled whether the action can run now
         * @param run what the action does
         */
        public Action {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(enabled, "enabled");
            Objects.requireNonNull(run, "run");
        }

        /**
         * Returns this action with a shortcut.
         *
         * @param shortcut the key stroke
         * @return a new action
         */
        public Action withShortcut(final KeyStroke shortcut) {
            return new Action(this.text, Objects.requireNonNull(shortcut, "shortcut"), this.enabled, this.run);
        }

        /**
         * Returns this action enabled while a source gives true.
         *
         * @param enabled the source, such as a {@link State}
         * @return a new action
         */
        public Action withEnabled(final Source<Boolean> enabled) {
            return new Action(this.text, this.shortcut, enabled, this.run);
        }
    }

    /**
     * A command that flips a state when run, shown by a check box, a toggle switch or a check menu item. A control
     * showing it is bound to the state both ways, so every control showing the toggle shows the same state.
     *
     * @param text the caption
     * @param shortcut the key stroke, or {@code null} for none
     * @param enabled whether the toggle can flip now
     * @param selected the state the toggle shows and flips
     */
    record Toggle(Text text, KeyStroke shortcut, Source<Boolean> enabled, State<Boolean> selected)
            implements Command {
        /**
         * Checks the components.
         *
         * @param text the caption
         * @param shortcut the key stroke, or {@code null} for none
         * @param enabled whether the toggle can flip now
         * @param selected the state the toggle shows and flips
         */
        public Toggle {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(enabled, "enabled");
            Objects.requireNonNull(selected, "selected");
        }

        /**
         * Returns this toggle with a shortcut.
         *
         * @param shortcut the key stroke
         * @return a new toggle
         */
        public Toggle withShortcut(final KeyStroke shortcut) {
            return new Toggle(this.text, Objects.requireNonNull(shortcut, "shortcut"), this.enabled, this.selected);
        }

        /**
         * Returns this toggle enabled while a source gives true.
         *
         * @param enabled the source, such as a {@link State}
         * @return a new toggle
         */
        public Toggle withEnabled(final Source<Boolean> enabled) {
            return new Toggle(this.text, this.shortcut, enabled, this.selected);
        }
    }
}
