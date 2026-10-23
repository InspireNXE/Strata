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
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Event types are keys by identity, so two libraries that pick the same name never hear each other's events.
 */
class EventTypeIdentityTest {
    private static final EventType<ControlEvent<Gadget>> OURS = EventType.report("changed");
    private static final EventType<ControlEvent<Gadget>> THEIRS = EventType.report("changed");

    private final TestHost test = new TestHost(200, 200);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void typesWithTheSameNameAndClassAreNotEqual() {
        assertNotEquals(OURS, THEIRS);
        assertEquals(OURS, OURS);
    }

    @Test
    void aHandlerHearsOnlyItsOwnTypeWhenAnotherHasTheSameName() {
        final Gadget gadget = new Gadget(this.ui);
        this.test.show(this.ui.stackPanel().add(gadget));
        final List<String> heard = new ArrayList<>();
        gadget.onEvent(OURS, event -> heard.add("ours"));
        gadget.onEvent(THEIRS, event -> heard.add("theirs"));

        gadget.fire(THEIRS);

        assertEquals(List.of("theirs"), heard);
    }

    /**
     * A component of a library's own that reports whatever type it is told to.
     */
    static final class Gadget extends CustomComponent<Gadget> {
        Gadget(final ComponentFactory factory) {
            super(factory);
        }

        void fire(final EventType<ControlEvent<Gadget>> type) {
            this.report(type);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(20, 20);
        }
    }
}
