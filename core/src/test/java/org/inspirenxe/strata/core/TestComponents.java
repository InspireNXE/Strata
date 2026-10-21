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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.component.registry.ComponentLibrary;
import org.inspirenxe.strata.api.component.registry.ComponentRegistry;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A component library written the way a library author would write one, listed in this source set's
 * {@code META-INF/services} so the registry finds it, and a library that fails halfway through registering.
 */
public final class TestComponents {
    /**
     * The id the library registers its gauge under.
     */
    public static final ComponentId GAUGE = ComponentId.of("test", "gauge");

    /**
     * The id the broken library registers before it throws.
     */
    public static final ComponentId BROKEN = ComponentId.of("test", "broken");

    /**
     * How many times the library has registered.
     */
    public static final AtomicInteger REGISTERED = new AtomicInteger();

    private TestComponents() {
    }

    /**
     * What the library publishes.
     */
    public interface Gauge extends Component<Gauge>, Ranged<Gauge> {
        /**
         * Sets the needle's color.
         *
         * @param color the color, as ARGB
         * @return this gauge
         */
        Gauge setNeedleColor(int color);

        /**
         * Returns the needle's color.
         *
         * @return the color, as ARGB
         */
        int needleColor();
    }

    /**
     * The library's implementation.
     */
    static final class GaugeImpl extends CustomComponent<Gauge> implements Gauge {
        private double value;
        private double min;
        private double max = 1;
        private int needleColor = 0xFFFF0000;

        GaugeImpl(final ComponentFactory factory) {
            super(factory);
        }

        @Override
        public double value() {
            return this.value;
        }

        @Override
        public Gauge setValue(final double value) {
            final double clamped = Math.clamp(value, this.min, this.max);
            final double previous = this.value;
            if (clamped != previous) {
                this.value = clamped;
                this.report(Ranged.CHANGED, clamped, previous);
            }
            return this;
        }

        @Override
        public double min() {
            return this.min;
        }

        @Override
        public double max() {
            return this.max;
        }

        @Override
        public Gauge setRange(final double min, final double max) {
            this.min = min;
            this.max = max;
            return this.setValue(this.value);
        }

        @Override
        public Gauge setNeedleColor(final int color) {
            this.needleColor = color;
            return this;
        }

        @Override
        public int needleColor() {
            return this.needleColor;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(40, 40);
        }
    }

    /**
     * The library, found through the service loader.
     */
    public static final class Library implements ComponentLibrary {
        @Override
        public void register(final ComponentRegistry registry) {
            TestComponents.REGISTERED.incrementAndGet();
            registry.register(GAUGE, Gauge.class, GaugeImpl::new);
        }
    }

    /**
     * A library that registers one component and then throws.
     */
    public static final class BrokenLibrary implements ComponentLibrary {
        @Override
        public void register(final ComponentRegistry registry) {
            registry.register(BROKEN, Gauge.class, GaugeImpl::new);
            throw new IllegalStateException("broken on purpose");
        }
    }
}
