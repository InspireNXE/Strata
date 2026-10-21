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
 * Names a theme as {@code namespace:path}, the way a {@link SpriteId} names a sprite. Pass one to
 * {@link ThemeRef#of} to get a theme that the engine's packs can restyle and that follows them when they reload.
 *
 * <p>Strata registers {@link Themes#FLAT} everywhere. Adapters register their engine themes under ids of their
 * own, and code can define more with {@link ThemeRegistry#define}. Any other id names a theme made only from theme
 * files, which the adapter finds in its asset roots.</p>
 *
 * @param namespace the owner, usually the id of the application, mod or plugin; not empty
 * @param path the theme's name within the namespace; not empty
 */
public record ThemeId(String namespace, String path) {
    /**
     * Checks the parts.
     *
     * @throws IllegalArgumentException if either part is empty
     */
    public ThemeId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException("Theme id needs a namespace and a path: " + namespace + ":" + path);
        }
    }

    /**
     * Returns a theme id.
     *
     * @param namespace the owner
     * @param path the theme's name within the namespace
     * @return the id
     * @throws IllegalArgumentException if either part is empty
     */
    public static ThemeId of(final String namespace, final String path) {
        return new ThemeId(namespace, path);
    }

    /**
     * Parses {@code namespace:path}, splitting at the first colon.
     *
     * @param id the text to parse
     * @return the id
     * @throws IllegalArgumentException if there is no colon or either part is empty
     */
    public static ThemeId parse(final String id) {
        final int colon = id.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Theme id needs a namespace: " + id);
        }
        return new ThemeId(id.substring(0, colon), id.substring(colon + 1));
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
