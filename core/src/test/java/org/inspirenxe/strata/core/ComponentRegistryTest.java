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
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.component.registry.ComponentMaker;
import org.inspirenxe.strata.api.component.registry.ComponentRegistry;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.core.TestComponents.Gauge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The component registry and {@link ComponentFactory#create}: Strata's controls, a library found through the service
 * loader, and components registered from code, used by type, by id and through their attributes.
 */
class ComponentRegistryTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final ComponentRegistry registry = Strata.components();
    private final List<Subscription> registered = new ArrayList<>();

    /**
     * A second component type, registered from code by the tests that need one.
     */
    interface Dial extends Component<Dial>, Ranged<Dial> {
    }

    static final class DialImpl extends CustomComponent<Dial> implements Dial {
        private double value;

        DialImpl(final ComponentFactory factory) {
            super(factory);
        }

        @Override
        public double value() {
            return this.value;
        }

        @Override
        public Dial setValue(final double value) {
            this.value = value;
            return this;
        }

        @Override
        public double min() {
            return 0;
        }

        @Override
        public double max() {
            return 1;
        }

        @Override
        public Dial setRange(final double min, final double max) {
            return this;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(10, 10);
        }
    }

    @AfterEach
    void unregister() {
        this.registered.forEach(Subscription::unsubscribe);
    }

    private <T extends Component<?>> void register(final ComponentId id, final Class<T> type,
            final ComponentMaker<? extends T> maker) {
        this.registered.add(this.registry.register(id, type, maker));
    }

    // ------------------------------------------------------------------
    // Strata's own controls
    // ------------------------------------------------------------------

    @Test
    void strataControlsAreMadeByTypeAndById() {
        final Slider slider = this.ui.create(Slider.class);
        assertFalse(slider instanceof CustomComponent<?>, "the factory's own slider");
        assertInstanceOf(ListView.class, this.ui.create(ComponentId.of("strata", "list_view")));
        assertTrue(this.registry.ids().contains(ComponentId.of("strata", "button")));
        assertSame(this.registry, this.ui.components());
    }

    @Test
    void strataTypesAndNamespaceAreClosed() {
        assertThrows(IllegalArgumentException.class, () -> this.registry.register(Button.class, f -> f.button()));
        assertThrows(IllegalArgumentException.class,
                () -> this.registry.register(ComponentId.of("strata", "dial"), Dial.class, DialImpl::new));
    }

    // ------------------------------------------------------------------
    // Libraries
    // ------------------------------------------------------------------

    @Test
    void aLibraryOnTheClassPathIsRegisteredWithoutBeingCalled() {
        final Gauge gauge = this.ui.create(Gauge.class);
        assertInstanceOf(TestComponents.GaugeImpl.class, gauge);
        assertTrue(this.registry.has(TestComponents.GAUGE));
    }

    @Test
    void aLibraryIsRegisteredOncePerClass() {
        this.registry.ids();
        this.registry.load(ComponentRegistryTest.class.getClassLoader());
        this.registry.load(Thread.currentThread().getContextClassLoader());
        assertEquals(1, TestComponents.REGISTERED.get());
    }

    @Test
    void aLibraryThatThrowsLeavesNothingRegistered() {
        assertFalse(this.registry.has(TestComponents.BROKEN));
        assertTrue(this.registry.has(TestComponents.GAUGE), "the other library is still there");
    }

    // ------------------------------------------------------------------
    // Making by type and id
    // ------------------------------------------------------------------

    @Test
    void aComponentMadeByIdIsUsedThroughItsAttributes() {
        final Component<?> made = this.ui.create(TestComponents.GAUGE);
        final List<Double> changes = new ArrayList<>();
        if (made instanceof Ranged<?> ranged) {
            ranged.onEvent(Ranged.CHANGED, event -> changes.add(event.value()));
            ranged.setValue(0.5);
        }
        this.test.show(this.ui.anchorPanel().add(made));

        assertEquals(List.of(0.5), changes);
    }

    @Test
    void anAttributeFindsTheNewestRegisteredSubtype() {
        final Subscription dial = this.registry.register(ComponentId.of("test", "dial"), Dial.class, DialImpl::new);
        try {
            assertInstanceOf(Dial.class, this.ui.create(Ranged.class));
        } finally {
            dial.unsubscribe();
        }
        assertFalse(this.ui.create(Ranged.class) instanceof Dial, "removing it brings back what it hid");
    }

    @Test
    void aLaterRegistrationHidesAnEarlierOne() {
        final List<String> made = new ArrayList<>();
        this.register(ComponentId.of("test", "dial"), Dial.class, factory -> {
            made.add("first");
            return new DialImpl(factory);
        });
        final Subscription second = this.registry.register(ComponentId.of("test", "dial"), Dial.class, factory -> {
            made.add("second");
            return new DialImpl(factory);
        });
        this.ui.create(Dial.class);
        second.unsubscribe();
        this.ui.create(ComponentId.of("test", "dial"));

        assertEquals(List.of("second", "first"), made);
    }

    @Test
    void missingComponentsAreEmptyOrThrow() {
        assertTrue(this.ui.tryCreate(Dial.class).isEmpty());
        assertTrue(this.ui.tryCreate(ComponentId.of("test", "missing")).isEmpty());
        assertThrows(IllegalStateException.class, () -> this.ui.create(Dial.class));
        assertThrows(IllegalStateException.class, () -> this.ui.create(ComponentId.of("test", "missing")));
    }

    @Test
    void badMakersAreRejected() {
        final ComponentFactory other = new TestHost(100, 100).factory();
        this.register(ComponentId.of("test", "null"), Dial.class, factory -> null);
        this.register(ComponentId.of("test", "elsewhere"), Dial.class, factory -> new DialImpl(other));

        assertThrows(IllegalStateException.class, () -> this.ui.create(ComponentId.of("test", "null")));
        assertThrows(IllegalStateException.class, () -> this.ui.create(ComponentId.of("test", "elsewhere")));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void aMakerOfTheWrongTypeIsRejected() {
        this.register(ComponentId.of("test", "liar"), Dial.class, (ComponentMaker) (ComponentMaker<Gauge>) f -> f.create(Gauge.class));

        assertThrows(IllegalStateException.class, () -> this.ui.create(ComponentId.of("test", "liar")));
    }
}
