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
package org.inspirenxe.strata.spi;

import java.util.Objects;

/**
 * One theme file, read by a {@link ThemeSource}.
 *
 * @param origin where the file came from, such as a pack name and path; used only in messages
 * @param content the file's JSON text
 * @param layer the name of the layer the file makes, usually the id of the pack it came from; a resolved theme
 *     reports it for each key the file set, through {@link org.inspirenxe.strata.api.theme.Theme#layer}
 */
public record ThemeDocument(String origin, String content, String layer) {
    /**
     * Checks the arguments.
     */
    public ThemeDocument {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(layer, "layer");
    }

    /**
     * Creates a document whose layer is named after its origin.
     *
     * @param origin where the file came from, such as a pack name and path; also the layer's name
     * @param content the file's JSON text
     */
    public ThemeDocument(final String origin, final String content) {
        this(origin, content, origin);
    }
}
