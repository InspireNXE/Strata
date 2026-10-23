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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A modifier key's own event carries the same modifiers whether or not the engine counted the key in them.
 */
class KeyModifiersTest {
    private static final Modifiers SHIFT = Modifiers.NONE.with(Modifier.SHIFT);

    private final TestHost test = new TestHost(200, 200);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aModifierKeysPressIncludesItWhenTheEngineLeftItOut() {
        final List<Modifiers> heard = this.listen();

        this.test.host().key(Key.LEFT_SHIFT, KeyAction.PRESS, Modifiers.NONE);

        assertEquals(List.of(SHIFT), heard);
    }

    @Test
    void aModifierKeysReleaseLeavesItOutWhenTheEngineCountedIt() {
        final List<Modifiers> heard = this.listen();
        this.test.host().key(Key.LEFT_SHIFT, KeyAction.PRESS, SHIFT);
        heard.clear();

        this.test.host().key(Key.LEFT_SHIFT, KeyAction.RELEASE, SHIFT);

        assertEquals(List.of(Modifiers.NONE), heard);
    }

    @Test
    void theCauseCarriesTheSameModifiersAsTheEvent() {
        final StackPanel panel = this.ui.stackPanel();
        this.test.show(panel);
        final List<Optional<Modifiers>> heard = new ArrayList<>();
        panel.onEvent(InputEvents.KEY, event -> heard.add(event.cause().context(CauseKeys.MODIFIERS)));

        this.test.host().key(Key.LEFT_SHIFT, KeyAction.PRESS, Modifiers.NONE);

        assertEquals(List.of(Optional.of(SHIFT)), heard);
    }

    private List<Modifiers> listen() {
        final StackPanel panel = this.ui.stackPanel();
        this.test.show(panel);
        final List<Modifiers> heard = new ArrayList<>();
        panel.onEvent(InputEvents.KEY, event -> heard.add(event.modifiers()));
        return heard;
    }
}
