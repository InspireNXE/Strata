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
package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The core {@link KeyEvent}, for a key press, repeat or release.
 */
public final class KeyEventImpl extends EventImpl implements KeyEvent {
    private final Key key;
    private final KeyAction action;
    private final Modifiers modifiers;

    /**
     * Creates the event.
     *
     * @param target the focused component, or the screen's root when nothing has focus
     * @param key the physical key
     * @param action whether the key went down, repeated or went up
     * @param modifiers the modifiers held at the time of the event
     */
    public KeyEventImpl(final Component<?> target, final Key key, final KeyAction action, final Modifiers modifiers) {
        super(target);
        this.key = key;
        this.action = action;
        this.modifiers = modifiers;
    }

    @Override
    public Key key() {
        return this.key;
    }

    @Override
    public KeyAction action() {
        return this.action;
    }

    @Override
    public Modifiers modifiers() {
        return this.modifiers;
    }
}
