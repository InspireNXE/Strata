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
package org.inspirenxe.strata.api.appearance;

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Optional;

/**
 * The appearances registered for the process, which controls ask how to show a value. Reach it with
 * {@link org.inspirenxe.strata.api.Strata#appearances()}, or
 * {@link org.inspirenxe.strata.api.ComponentFactory#appearances()} inside a screen; both return the same registry.
 *
 * <p>A value's appearance is the one registered for its class, else for its nearest superclass that has one, else
 * for the nearest of its interfaces that has one. Interfaces are searched breadth first: those the class and its
 * superclasses implement directly, then the interfaces those extend, and so on. Of two interfaces at the same
 * distance, the one whose type was first registered wins. A later registration for the
 * same type hides an earlier one until it is removed. A value with no appearance has no icon and no tooltip, and its
 * name is {@link String#valueOf(Object)}.</p>
 *
 * <p>Strata registers {@link Text}, named by itself, and {@link Visual}, whose icon is itself. Engine adapters
 * register their own types, such as an engine's items, when they start.</p>
 *
 * <p>Registration may happen on any thread. Lookups happen on the UI thread. Do not implement this interface.</p>
 */
public interface Appearances {

    /**
     * Registers an appearance for a type and its subtypes.
     *
     * @param <T> the type
     * @param type the type
     * @param appearance the appearance
     * @return a subscription that removes the registration, bringing back what it hid
     */
    <T> Subscription register(Class<T> type, Appearance<? super T> appearance);

    /**
     * Returns the appearance that values of a class take.
     *
     * @param <T> the type
     * @param type the class
     * @return the appearance, or empty if nothing is registered for the class or its supertypes
     */
    <T> Optional<Appearance<? super T>> find(Class<T> type);

    /**
     * Returns a value's icon.
     *
     * @param value the value
     * @return the icon, or empty for none
     */
    Optional<Visual> iconOf(Object value);

    /**
     * Returns a value's name.
     *
     * @param value the value
     * @return the name; {@link Text#of(String)} of {@link String#valueOf(Object)} for a value with no appearance
     */
    Text nameOf(Object value);

    /**
     * Returns a value's tooltip, asked anew on every call.
     *
     * @param value the value
     * @return the tooltip, or empty for none
     */
    Optional<Tooltip> tooltipOf(Object value);
}
