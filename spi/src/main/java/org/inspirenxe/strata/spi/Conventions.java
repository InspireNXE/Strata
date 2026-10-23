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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.input.Modifier;

import java.util.Objects;

/**
 * How the engine's own controls behave, so Strata's behave the same way and users meet one set of habits. Looks
 * belong in the theme; these are the behaviors a theme does not reach.
 *
 * @param activateOnPress true when a button acts as soon as the pointer's left button goes down on it, as some
 *                        engines' buttons do. This covers buttons, cycle buttons, checkboxes, radio buttons, toggle
 *                        switches and tabs. When false, they act when the button comes back up still over them,
 *                        and moving off first cancels.
 * @param shortcutModifier the modifier that {@code KeyStroke.shortcut} strokes hold: {@link Modifier#CONTROL} on
 *                         most platforms, {@link Modifier#SUPER} where the platform's shortcuts use Command. An
 *                         adapter that cannot tell reports Control.
 * @param mnemonics true when holding Alt underlines the marked letters of captions, Alt with a letter acts on the
 *                  control whose caption has it, and Alt or F10 alone moves to a menu bar. An engine that binds Alt
 *                  for its own use reports false: captions then show their text with the markers taken out, and
 *                  Alt does nothing.
 */
public record Conventions(boolean activateOnPress, Modifier shortcutModifier, boolean mnemonics) {
    /**
     * The conventions a platform has unless it says otherwise: buttons act on release, shortcuts use Control and
     * mnemonics are on.
     */
    public static final Conventions DEFAULT = new Conventions(false, Modifier.CONTROL, true);

    /**
     * Checks the components.
     *
     * @param activateOnPress true when buttons act on press
     * @param shortcutModifier the platform's shortcut modifier
     * @param mnemonics true when Alt shows and uses mnemonics
     */
    public Conventions {
        Objects.requireNonNull(shortcutModifier, "shortcutModifier");
    }

    /**
     * Creates conventions that differ from {@link #DEFAULT} only in when buttons act, with Control as the shortcut
     * modifier and mnemonics on.
     *
     * @param activateOnPress true when buttons act on press
     */
    public Conventions(final boolean activateOnPress) {
        this(activateOnPress, Modifier.CONTROL, true);
    }
}
