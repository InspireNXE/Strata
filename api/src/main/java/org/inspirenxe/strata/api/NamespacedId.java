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
package org.inspirenxe.strata.api;

import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.text.FontId;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.ThemeId;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * An id written as {@code namespace:path}: a {@link ComponentId}, a {@link ThemeId}, a {@link SpriteId} or a
 * {@link FontId}.
 *
 * <p>All four follow one grammar, which {@link #check} enforces and {@link #parse} reads. The namespace is one or
 * more characters with no colon, and the path one or more characters of any kind, colons included. Parsing splits
 * at the first colon, so an id's {@code toString()}, which is {@code namespace:path}, parses back to an equal id.</p>
 *
 * <p>Do not implement this interface. The four records are the only kinds of id. It is not sealed because a sealed
 * type in a jar without a module descriptor must keep its subtypes in its own package.</p>
 */
public interface NamespacedId {
    /**
     * Returns the namespace.
     *
     * @return the owner, usually the id of the application or library; not empty, and without a colon
     */
    String namespace();

    /**
     * Returns the path.
     *
     * @return the name within the namespace; not empty, and may hold colons
     */
    String path();

    /**
     * Checks the parts of an id against the grammar. The records call it from their constructors.
     *
     * @param kind what is named, such as {@code "Sprite"}, for the message
     * @param namespace the namespace
     * @param path the path
     * @throws IllegalArgumentException if either part is empty, or the namespace holds a colon
     */
    static void check(final String kind, final String namespace, final String path) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty() || path.isEmpty() || namespace.indexOf(':') >= 0) {
            throw new IllegalArgumentException(kind + " id needs a namespace without a colon and a path: "
                    + namespace + ":" + path);
        }
    }

    /**
     * Parses {@code namespace:path}, splitting at the first colon, and makes the id from the two parts. A missing
     * namespace is an error, so an id never lands in another namespace by accident.
     *
     * @param kind what is named, such as {@code "Sprite"}, for the message
     * @param id the text to parse
     * @param maker makes the id from its namespace and path, checking them
     * @param <I> the kind of id
     * @return the id
     * @throws IllegalArgumentException if there is no colon, or either side of it is empty
     */
    static <I extends NamespacedId> I parse(final String kind, final String id,
            final BiFunction<String, String, I> maker) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(maker, "maker");
        final int colon = id.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException(kind + " id needs a namespace: " + id);
        }
        return maker.apply(id.substring(0, colon), id.substring(colon + 1));
    }
}
