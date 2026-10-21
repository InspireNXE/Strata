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
package org.inspirenxe.strata.api.component.registry;

/**
 * A library of components that registers itself, so the people who use it never call an init method. List the
 * implementation in {@code META-INF/services/org.inspirenxe.strata.api.component.registry.ComponentLibrary} (or in a
 * {@code provides} clause of {@code module-info.java}):
 *
 * <pre>{@code
 * public final class FancyLibrary implements ComponentLibrary {
 *     public void register(ComponentRegistry registry) {
 *         registry.register(ComponentId.of("fancy", "gauge"), Gauge.class, GaugeImpl::new);
 *     }
 * }
 * }</pre>
 *
 * <p>Strata finds libraries with {@link java.util.ServiceLoader} the first time the registry is used, and again in
 * every class loader passed to {@link ComponentRegistry#load}. Each library class is registered once per process,
 * however many loaders can see it.</p>
 *
 * <p>Requirements for implementations: a public constructor with no parameters, as the service loader needs. Do
 * nothing in {@link #register} but register. If it throws, Strata removes whatever it registered, logs the error and
 * goes on with the next library. It may run on any thread.</p>
 */
public interface ComponentLibrary {

    /**
     * Registers the library's components.
     *
     * @param registry the registry to register them with
     */
    void register(ComponentRegistry registry);
}
