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

import org.inspirenxe.strata.api.text.TextStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A theme named by id, plus keys the developer sets on top of it. Hosts given a ref resolve it when they open and
 * again whenever the engine reloads its assets, so the screen follows the player's packs and keeps the
 * developer's overrides.
 *
 * <p>Resolving starts from the theme registered under {@link #id()}, or from the theme its files name as parent,
 * applies the theme files the engine finds for the id in pack order, and then applies the overrides held here.
 * Overrides always win over packs.</p>
 *
 * <p>A ref is immutable. {@link #override(String, Paint)} and {@link #override(String, TextStyle)} return a new
 * ref.</p>
 *
 * @param id the theme to start from
 * @param paints paint overrides by style key; copied
 * @param textStyles text style overrides by style key; copied
 */
public record ThemeRef(ThemeId id, Map<String, Paint> paints, Map<String, TextStyle> textStyles) {
    /**
     * Checks and copies the arguments.
     */
    public ThemeRef {
        Objects.requireNonNull(id, "id");
        paints = Map.copyOf(paints);
        textStyles = Map.copyOf(textStyles);
    }

    /**
     * Returns a ref to a theme with no overrides.
     *
     * @param id the theme
     * @return the ref
     */
    public static ThemeRef of(final ThemeId id) {
        return new ThemeRef(id, Map.of(), Map.of());
    }

    /**
     * Returns a copy of this ref that sets a paint after the packs are applied. A later override of the same key
     * replaces an earlier one.
     *
     * @param key the style key
     * @param paint the paint to use
     * @return the new ref
     */
    public ThemeRef override(final String key, final Paint paint) {
        final Map<String, Paint> paints = new HashMap<>(this.paints);
        paints.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(paint, "paint"));
        return new ThemeRef(this.id, paints, this.textStyles);
    }

    /**
     * Returns a copy of this ref that sets a text style after the packs are applied. A later override of the same
     * key replaces an earlier one.
     *
     * @param key the style key
     * @param style the text style to use
     * @return the new ref
     */
    public ThemeRef override(final String key, final TextStyle style) {
        final Map<String, TextStyle> styles = new HashMap<>(this.textStyles);
        styles.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(style, "style"));
        return new ThemeRef(this.id, this.paints, styles);
    }
}
