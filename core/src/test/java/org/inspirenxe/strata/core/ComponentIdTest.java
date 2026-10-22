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
import org.inspirenxe.strata.api.control.button.Button;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Components carry a free id that Strata stores and never checks.
 */
class ComponentIdTest {
    private final TestHost test = new TestHost(200, 200);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aBuiltInControlHasNoIdUntilOneIsSet() {
        final Button buy = this.ui.button("Buy");
        assertEquals(Optional.empty(), buy.id());

        assertSame(buy, buy.setId("buy"));
        assertEquals(Optional.of("buy"), buy.id());

        buy.setId(null);
        assertEquals(Optional.empty(), buy.id());
    }

    @Test
    void aCustomComponentKeepsItsIdThroughItsPeer() {
        final Box box = new Box(this.ui, 10, 10).setId("total");

        assertEquals(Optional.of("total"), box.id());
    }

    @Test
    void idsNeedNotBeUniqueAndStayWhenTheTreeChanges() {
        final Button first = this.ui.button("One").setId("row");
        final Button second = this.ui.button("Two").setId("row");
        this.test.show(this.ui.stackPanel().add(first).add(second));
        this.test.host().close();

        assertEquals(Optional.of("row"), first.id());
        assertEquals(Optional.of("row"), second.id());
    }
}
