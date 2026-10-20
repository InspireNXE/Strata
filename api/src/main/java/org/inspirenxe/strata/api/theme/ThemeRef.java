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

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A theme named by id, plus keys the developer sets on top of it for one screen. Hosts given a ref resolve it when
 * they open, and again whenever the engine reloads its assets or the overrides in {@link ThemeRegistry} change, so
 * the screen follows the player's packs and keeps the developer's overrides.
 *
 * <p>A theme resolves from layers, lowest first. It starts from the id's base: a theme the engine adapter builds
 * in code, the adapter's base files, a theme defined with {@link ThemeRegistry#define}, or the theme the id's files
 * name as {@code parent}. Next come the theme files the engine finds for the id, in the order the adapter gives
 * them, then the overrides added with {@link ThemeRegistry#override} in the order they were added, and last the
 * overrides held here. The ref's overrides always win.</p>
 *
 * <p>A ref is immutable. {@link #override(String, Paint)}, {@link #override(String, TextStyle)},
 * {@link #override(String, Insets)} and {@link #override(ThemeOverrides)} return a new ref.</p>
 *
 * @param id the theme to start from
 * @param paints paint overrides by style key; copied
 * @param textStyles text style overrides by style key; copied
 * @param insets insets overrides by style key; copied
 */
public record ThemeRef(ThemeId id, Map<String, Paint> paints, Map<String, TextStyle> textStyles,
                       Map<String, Insets> insets) {
    /**
     * Checks and copies the arguments.
     */
    public ThemeRef {
        Objects.requireNonNull(id, "id");
        paints = Map.copyOf(paints);
        textStyles = Map.copyOf(textStyles);
        insets = Map.copyOf(insets);
    }

    /**
     * Creates a ref with paint and text style overrides and no insets overrides.
     *
     * @param id the theme to start from
     * @param paints paint overrides by style key; copied
     * @param textStyles text style overrides by style key; copied
     */
    public ThemeRef(final ThemeId id, final Map<String, Paint> paints, final Map<String, TextStyle> textStyles) {
        this(id, paints, textStyles, Map.of());
    }

    /**
     * Returns a ref to a theme with no overrides.
     *
     * @param id the theme
     * @return the ref
     */
    public static ThemeRef of(final ThemeId id) {
        return new ThemeRef(id, Map.of(), Map.of(), Map.of());
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
        return new ThemeRef(this.id, paints, this.textStyles, this.insets);
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
        return new ThemeRef(this.id, this.paints, styles, this.insets);
    }

    /**
     * Returns a copy of this ref that sets insets after the packs are applied. A later override of the same key
     * replaces an earlier one.
     *
     * @param key the style key
     * @param insets the insets to use, in UI units
     * @return the new ref
     */
    public ThemeRef override(final String key, final Insets insets) {
        final Map<String, Insets> all = new HashMap<>(this.insets);
        all.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(insets, "insets"));
        return new ThemeRef(this.id, this.paints, this.textStyles, all);
    }

    /**
     * Returns a copy of this ref that sets every key of some overrides after the packs and the overrides in
     * {@link ThemeRegistry} are applied. A key this ref already overrides is replaced.
     *
     * @param overrides the keys to set
     * @return the new ref
     */
    public ThemeRef override(final ThemeOverrides overrides) {
        final Map<String, Paint> paints = new HashMap<>(this.paints);
        paints.putAll(overrides.paints());
        final Map<String, TextStyle> styles = new HashMap<>(this.textStyles);
        styles.putAll(overrides.textStyles());
        final Map<String, Insets> all = new HashMap<>(this.insets);
        all.putAll(overrides.insets());
        return new ThemeRef(this.id, paints, styles, all);
    }
}
