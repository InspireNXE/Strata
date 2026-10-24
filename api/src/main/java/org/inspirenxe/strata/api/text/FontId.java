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
package org.inspirenxe.strata.api.text;

import org.inspirenxe.strata.api.NamespacedId;

/**
 * Names a font family as {@code namespace:path}, for {@link TextStyle#font()}. The engine adapter decides where the
 * family comes from: an adapter may let the application register families under ids, or map ids to the engine's own
 * fonts. An engine without the named family draws and measures the text with its default font, at the style's size.
 *
 * <p>The namespace is one or more characters with no colon, and the path one or more characters of any kind, so
 * {@link #toString()} and {@link #parse} give back the same id. The grammar is {@link NamespacedId}'s, which the
 * other ids share.</p>
 *
 * @param namespace the owner, usually the id of the application or library; not empty, and without a colon
 * @param path the family's name inside the namespace; not empty
 */
public record FontId(String namespace, String path) implements NamespacedId {
    /**
     * Checks the parts.
     *
     * @throws IllegalArgumentException if either part is empty, or the namespace holds a colon
     */
    public FontId {
        NamespacedId.check("Font", namespace, path);
    }

    /**
     * Creates a font id.
     *
     * @param namespace the owner, usually the id of the application or library
     * @param path the family's name inside the namespace
     * @return the font id
     * @throws IllegalArgumentException if either part is empty, or the namespace holds a colon
     */
    public static FontId of(final String namespace, final String path) {
        return new FontId(namespace, path);
    }

    /**
     * Parses {@code namespace:path}. A missing namespace is an error, so a font never lands in another namespace by
     * accident.
     *
     * @param id the text to parse
     * @return the font id
     * @throws IllegalArgumentException if there is no colon, or either side of it is empty
     */
    public static FontId parse(final String id) {
        return NamespacedId.parse("Font", id, FontId::new);
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
