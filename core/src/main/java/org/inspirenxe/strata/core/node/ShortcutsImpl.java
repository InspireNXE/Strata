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
import org.inspirenxe.strata.api.command.Shortcuts;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.core.control.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The {@link Shortcuts} of one root. It holds the commands added for the whole host and those added with a scope,
 * in the order they were added; {@link Root} asks it to match a key nothing on the focus path consumed.
 */
final class ShortcutsImpl implements Shortcuts {
    private final ComponentFactoryImpl ui;
    private final List<Entry> entries;

    ShortcutsImpl(final ComponentFactoryImpl ui) {
        this.ui = ui;
        this.entries = new ArrayList<>();
    }

    @Override
    public Subscription add(final Command command) {
        return this.register(null, Objects.requireNonNull(command, "command"));
    }

    @Override
    public Subscription add(final Component<?> scope, final Command command) {
        return this.register(this.ui.nodeOf(Objects.requireNonNull(scope, "scope")),
                Objects.requireNonNull(command, "command"));
    }

    private Subscription register(final Node scope, final Command command) {
        final Entry entry = new Entry(scope, command);
        this.entries.add(entry);
        return () -> this.entries.remove(entry);
    }

    /**
     * Finds the first enabled command scoped to a node on a path, innermost first, whose stroke matches a key.
     *
     * @param from the innermost node of the path
     * @param key the key
     * @param held the modifiers held
     * @param shortcutModifier the platform's shortcut modifier
     * @return the command, or null if none matches
     */
    Command matchScoped(final Node from, final Key key, final Modifiers held, final Modifier shortcutModifier) {
        for (Node node = from; node != null; node = node.parentNode()) {
            for (final Entry entry : this.entries) {
                if (entry.scope == node && matches(entry.command, key, held, shortcutModifier)) {
                    return entry.command;
                }
            }
        }
        return null;
    }

    /**
     * Finds the first enabled command added for the whole host whose stroke matches a key.
     *
     * @return the command, or null if none matches
     */
    Command matchHost(final Key key, final Modifiers held, final Modifier shortcutModifier) {
        for (final Entry entry : this.entries) {
            if (entry.scope == null && matches(entry.command, key, held, shortcutModifier)) {
                return entry.command;
            }
        }
        return null;
    }

    /**
     * Returns whether a command has a stroke that matches a key and is enabled now.
     *
     * @param command the command
     * @param key the key
     * @param held the modifiers held
     * @param shortcutModifier the platform's shortcut modifier
     * @return true if it would run
     */
    static boolean matches(final Command command, final Key key, final Modifiers held, final Modifier shortcutModifier) {
        final KeyStroke stroke = command.shortcut();
        return stroke != null && stroke.matches(key, held, shortcutModifier) && Commands.isEnabled(command);
    }

    private record Entry(Node scope, Command command) {
    }
}
