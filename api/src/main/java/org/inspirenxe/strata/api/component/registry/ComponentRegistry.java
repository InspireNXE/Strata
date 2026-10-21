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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.Subscription;

import java.util.Optional;
import java.util.Set;

/**
 * The components registered for the process, which {@link org.inspirenxe.strata.api.ComponentFactory#create} makes
 * by type or id. Reach it with {@link org.inspirenxe.strata.api.Strata#components()}, or
 * {@link org.inspirenxe.strata.api.ComponentFactory#components()} inside a screen; both return the same registry.
 *
 * <p>A library registers each of its components under the type callers see it as, usually an interface it
 * publishes, and optionally an id:</p>
 *
 * <pre>{@code
 * Strata.components().register(ComponentId.of("fancy", "gauge"), Gauge.class, GaugeImpl::new);
 * }</pre>
 *
 * <p>A library that lists a {@link ComponentLibrary} is registered without anyone calling it.</p>
 *
 * <p>Lookup by type takes the registration for that exact type. If there is none, it takes the newest registration
 * whose type is a subtype of the one asked for, so callers can ask for an interface the registered type extends,
 * including an attribute such as {@link org.inspirenxe.strata.api.control.attribute.value.Ranged}. A later
 * registration for the same type or id hides the earlier one until its subscription is removed.</p>
 *
 * <p>Strata registers every control and panel its factory makes, under the control's interface and an id in the
 * {@value ComponentId#STRATA} namespace, with the defaults of the factory method that takes no arguments. Those
 * types and that namespace are closed: registering in them throws.</p>
 *
 * <p>Registration and lookup may happen on any thread. Do not implement this interface.</p>
 */
public interface ComponentRegistry {

    /**
     * Registers a component under a type, with no id.
     *
     * @param <T> the type
     * @param type the type callers ask for, which every component the maker makes must be an instance of
     * @param maker makes the component
     * @return a subscription that removes the registration, bringing back what it hid
     * @throws IllegalArgumentException if {@code type} is one of Strata's own control or panel types
     */
    <T extends Component<?>> Subscription register(Class<T> type, ComponentMaker<? extends T> maker);

    /**
     * Registers a component under a type and an id.
     *
     * @param <T> the type
     * @param id the id, which code that does not compile against the type can make it by
     * @param type the type callers ask for, which every component the maker makes must be an instance of
     * @param maker makes the component
     * @return a subscription that removes the registration, bringing back what it hid
     * @throws IllegalArgumentException if {@code type} is one of Strata's own control or panel types, or the id is
     *     in the {@value ComponentId#STRATA} namespace
     */
    <T extends Component<?>> Subscription register(ComponentId id, Class<T> type, ComponentMaker<? extends T> maker);

    /**
     * Returns the maker that {@code create(type)} would use.
     *
     * @param <T> the type
     * @param type the type asked for
     * @return the maker registered for the type, else the newest registered for a subtype of it, or empty if there
     *     is none
     */
    <T> Optional<ComponentMaker<? extends Component<?>>> find(Class<T> type);

    /**
     * Returns the maker registered under an id.
     *
     * @param id the id
     * @return the newest maker registered under the id, or empty if there is none
     */
    Optional<ComponentMaker<? extends Component<?>>> find(ComponentId id);

    /**
     * Returns whether a component can be made as a type.
     *
     * @param type the type asked for
     * @return {@code true} if {@link #find(Class)} would find a maker
     */
    boolean has(Class<?> type);

    /**
     * Returns whether a component is registered under an id.
     *
     * @param id the id
     * @return {@code true} if {@link #find(ComponentId)} would find a maker
     */
    boolean has(ComponentId id);

    /**
     * Returns the ids of every registration, Strata's own included.
     *
     * @return a snapshot of the ids, which later registrations do not change
     */
    Set<ComponentId> ids();

    /**
     * Registers the {@link ComponentLibrary} services that a class loader can see and that are not registered yet.
     * Strata looks in its own class loader and the thread's context class loader by itself. Engine adapters call
     * this for each plugin whose classes those loaders cannot see, when the plugin loads.
     *
     * @param loader the class loader to look in
     */
    void load(ClassLoader loader);
}
