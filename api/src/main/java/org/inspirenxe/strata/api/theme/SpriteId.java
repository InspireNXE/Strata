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
package org.inspirenxe.strata.api.theme;

import java.util.Objects;

/**
 * Names a sprite as {@code namespace:path}. The engine adapter decides where the image comes from. The Swing
 * adapter reads the class path resource {@code assets/<namespace>/sprites/<path>.png} by default, and takes a
 * resolver for any other layout. A missing image draws as magenta.
 *
 * <p>The namespace is one or more characters with no colon, and the path one or more characters of any kind, so
 * {@link #toString()} and {@link #parse} give back the same id.</p>
 *
 * @param namespace the owner, usually the id of the application or library; not empty, and without a colon
 * @param path the path inside the namespace, without extension; not empty
 */
public record SpriteId(String namespace, String path) {
    /**
     * Checks the parts.
     *
     * @throws IllegalArgumentException if either part is empty, or the namespace holds a colon
     */
    public SpriteId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty() || path.isEmpty() || namespace.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Sprite id needs a namespace without a colon and a path: "
                    + namespace + ":" + path);
        }
    }

    /**
     * Creates a sprite id.
     *
     * @param namespace the owner, usually the id of the application or library
     * @param path the path inside the namespace
     * @return the sprite id
     * @throws IllegalArgumentException if either part is empty, or the namespace holds a colon
     */
    public static SpriteId of(final String namespace, final String path) {
        return new SpriteId(namespace, path);
    }

    /**
     * Parses {@code namespace:path}. A missing namespace is an error, so a sprite never lands in another
     * namespace by accident.
     *
     * @param id the text to parse
     * @return the sprite id
     * @throws IllegalArgumentException if there is no colon, or either side of it is empty
     */
    public static SpriteId parse(final String id) {
        final int colon = id.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Sprite id needs a namespace: " + id);
        }
        return new SpriteId(id.substring(0, colon), id.substring(colon + 1));
    }

    /**
     * Returns the id as {@code namespace:path}, the form {@link #parse} accepts.
     *
     * @return the id as text
     */
    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }
}
