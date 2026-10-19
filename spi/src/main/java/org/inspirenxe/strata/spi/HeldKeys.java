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

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tells key presses from repeats, for adapters whose engine reports a held key as more presses, such as AWT.
 * The first press of a key is a {@link KeyAction#PRESS}; every press after it, until the key is released, is a
 * {@link KeyAction#REPEAT}.
 *
 * <pre>{@code
 * void onEngineKeyDown(int code) {
 *     Key key = map(code);
 *     host.key(key, this.heldKeys.press(key), modifiers());
 * }
 *
 * void onEngineKeyUp(int code) {
 *     Key key = map(code);
 *     this.heldKeys.release(key);
 *     host.key(key, KeyAction.RELEASE, modifiers());
 * }
 * }</pre>
 *
 * <p>Call {@link #clear()} when the window or engine screen loses focus, or keys released elsewhere stay held and
 * their next press reads as a repeat. Not thread-safe; use it on the UI thread.</p>
 */
public final class HeldKeys {
    private final Set<Key> held;

    /**
     * Creates a tracker with no keys held.
     */
    public HeldKeys() {
        this.held = EnumSet.noneOf(Key.class);
    }

    /**
     * Records a key going down.
     *
     * @param key the key the engine reported
     * @return {@link KeyAction#PRESS} if the key was not held, otherwise {@link KeyAction#REPEAT}
     */
    public KeyAction press(final Key key) {
        return this.held.add(key) ? KeyAction.PRESS : KeyAction.REPEAT;
    }

    /**
     * Records a key coming up. Releasing a key that is not held does nothing.
     *
     * @param key the key the engine reported
     */
    public void release(final Key key) {
        this.held.remove(key);
    }

    /**
     * Returns whether a key is held.
     *
     * @param key the key
     * @return true between its press and its release
     */
    public boolean isHeld(final Key key) {
        return this.held.contains(key);
    }

    /**
     * Forgets every held key.
     */
    public void clear() {
        this.held.clear();
    }
}
