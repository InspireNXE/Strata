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
package org.inspirenxe.strata.api.input;

import java.util.Objects;

/**
 * A key with the exact modifiers held for it, such as Control+S, as a shortcut names it.
 *
 * <p>A stroke made with {@link #shortcut} also holds the platform's shortcut modifier, which is Control on most
 * platforms and Command ({@link Modifier#SUPER}) where the platform's conventions say so, so one stroke reads
 * Control+S on one platform and Command+S on another. The modifier is added when the stroke is matched or shown, by
 * {@link #resolve}, and is not in {@link #modifiers()}.</p>
 *
 * <pre>{@code
 * final KeyStroke save = KeyStroke.shortcut(Key.S);                        // Control+S, or Command+S
 * final KeyStroke saveAll = KeyStroke.shortcut(Key.S, Modifier.SHIFT);     // and Shift
 * final KeyStroke help = KeyStroke.of(Key.F1);
 * }</pre>
 *
 * @param key the key
 * @param modifiers the modifiers held besides the shortcut modifier
 * @param shortcut true to hold the platform's shortcut modifier too
 */
public record KeyStroke(Key key, Modifiers modifiers, boolean shortcut) {
    /**
     * Checks the components.
     *
     * @param key the key
     * @param modifiers the modifiers held besides the shortcut modifier
     * @param shortcut true to hold the platform's shortcut modifier too
     */
    public KeyStroke {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(modifiers, "modifiers");
    }

    /**
     * Creates a stroke of a key with exactly the given modifiers.
     *
     * @param key the key
     * @param modifiers the modifiers; none for the key alone
     * @return the stroke
     */
    public static KeyStroke of(final Key key, final Modifier... modifiers) {
        return new KeyStroke(key, Modifiers.of(modifiers), false);
    }

    /**
     * Creates a stroke of a key with the platform's shortcut modifier and any others.
     *
     * @param key the key
     * @param more the modifiers held besides the shortcut modifier
     * @return the stroke
     */
    public static KeyStroke shortcut(final Key key, final Modifier... more) {
        return new KeyStroke(key, Modifiers.of(more), true);
    }

    /**
     * Returns the modifiers this stroke holds on a platform.
     *
     * @param shortcutModifier the platform's shortcut modifier
     * @return the modifiers, with the shortcut modifier added when this is a {@linkplain #shortcut shortcut}
     */
    public Modifiers resolve(final Modifier shortcutModifier) {
        Objects.requireNonNull(shortcutModifier, "shortcutModifier");
        return this.shortcut ? this.modifiers.with(shortcutModifier) : this.modifiers;
    }

    /**
     * Returns whether a key with some modifiers held is this stroke on a platform: the same key with exactly the
     * modifiers {@link #resolve} gives.
     *
     * @param key the key
     * @param held the modifiers held
     * @param shortcutModifier the platform's shortcut modifier
     * @return true if they match
     */
    public boolean matches(final Key key, final Modifiers held, final Modifier shortcutModifier) {
        return this.key == key && held.equals(this.resolve(shortcutModifier));
    }
}
