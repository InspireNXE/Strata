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

import java.util.Objects;

/**
 * Names a registered component as {@code namespace:path}, the way a
 * {@link org.inspirenxe.strata.api.theme.ThemeId} names a theme. Code that does not compile against a library can
 * still make its components by id, and use them through the attributes they implement.
 *
 * <p>Strata registers its own controls and panels in the {@value #STRATA} namespace, under the path their factory
 * method is named for in snake case: {@code strata:button}, {@code strata:list_view},
 * {@code strata:anchor_panel}.</p>
 *
 * @param namespace the owner, usually a library or mod id; not empty
 * @param path the component's name within the namespace; not empty
 */
public record ComponentId(String namespace, String path) {
    /**
     * The namespace of Strata's own components, which no one else may register in.
     */
    public static final String STRATA = "strata";

    /**
     * Checks the parts.
     *
     * @throws IllegalArgumentException if either part is empty
     */
    public ComponentId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException("Component id needs a namespace and a path: " + namespace + ":" + path);
        }
    }

    /**
     * Returns a component id.
     *
     * @param namespace the owner
     * @param path the component's name within the namespace
     * @return the id
     * @throws IllegalArgumentException if either part is empty
     */
    public static ComponentId of(final String namespace, final String path) {
        return new ComponentId(namespace, path);
    }

    /**
     * Parses {@code namespace:path}, splitting at the first colon.
     *
     * @param id the text to parse
     * @return the id
     * @throws IllegalArgumentException if there is no colon or either part is empty
     */
    public static ComponentId parse(final String id) {
        final int colon = id.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Component id needs a namespace: " + id);
        }
        return new ComponentId(id.substring(0, colon), id.substring(colon + 1));
    }

    /**
     * Returns {@code namespace:path}.
     *
     * @return the id as text
     */
    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }
}
