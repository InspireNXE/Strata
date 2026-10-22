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

import java.util.Map;
import java.util.Objects;

/**
 * A theme named by id, plus keys the developer sets on top of it for one screen. Hosts given a ref resolve it when
 * they open, and again whenever the engine reloads its assets or the overrides in {@link ThemeRegistry} change, so
 * the screen follows the user's packs and keeps the developer's overrides.
 *
 * <p>A theme resolves from layers, lowest first. It starts from the id's base: a theme the engine adapter builds
 * in code, the adapter's base files, a theme defined with {@link ThemeRegistry#define}, or the theme the id's files
 * name as {@code parent}. Next come the theme files the engine finds for the id, in the order the adapter gives
 * them, then the overrides added with {@link ThemeRegistry#override} in the order they were added, and last the
 * overrides held here. The ref's overrides always win.</p>
 *
 * <p>A ref is immutable. {@link #override(String, Paint)}, {@link #override(String, TextStyle)},
 * {@link #override(String, Insets)}, {@link #override(String, float)} and {@link #override(ThemeOverrides)} return
 * a new ref.</p>
 *
 * @param id the theme to start from
 * @param overrides the keys set on top of the theme
 */
public record ThemeRef(ThemeId id, ThemeOverrides overrides) {
    /**
     * Checks the arguments.
     */
    public ThemeRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(overrides, "overrides");
    }

    /**
     * Creates a ref with paint, text style, insets and metric overrides.
     *
     * @param id the theme to start from
     * @param paints paint overrides by style key; copied
     * @param textStyles text style overrides by style key; copied
     * @param insets insets overrides by style key; copied
     * @param metrics metric overrides by style key; copied
     * @throws IllegalArgumentException if a metric is not finite
     */
    public ThemeRef(final ThemeId id, final Map<String, Paint> paints, final Map<String, TextStyle> textStyles,
                    final Map<String, Insets> insets, final Map<String, Float> metrics) {
        this(id, new ThemeOverrides(paints, textStyles, insets, metrics));
    }

    /**
     * Creates a ref with paint, text style and insets overrides and no metric overrides.
     *
     * @param id the theme to start from
     * @param paints paint overrides by style key; copied
     * @param textStyles text style overrides by style key; copied
     * @param insets insets overrides by style key; copied
     */
    public ThemeRef(final ThemeId id, final Map<String, Paint> paints, final Map<String, TextStyle> textStyles,
                    final Map<String, Insets> insets) {
        this(id, paints, textStyles, insets, Map.of());
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
        return new ThemeRef(id, ThemeOverrides.of());
    }

    /**
     * Returns a ref to the platform's default theme, {@link Themes#PLATFORM}, with no overrides. Screens opened
     * without a theme use it. Start from it to change a few keys of the default look on one screen:
     *
     * <pre>{@code
     * Strata.screens().open(screen, ThemeRef.platform().override(Label.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFFFD700)));
     * }</pre>
     *
     * @return the ref
     */
    public static ThemeRef platform() {
        return ThemeRef.of(Themes.PLATFORM);
    }

    /**
     * Returns the paint overrides, as {@link ThemeOverrides#paints()} on {@link #overrides()} does.
     *
     * @return paint overrides by style key; unmodifiable
     */
    public Map<String, Paint> paints() {
        return this.overrides.paints();
    }

    /**
     * Returns the text style overrides, as {@link ThemeOverrides#textStyles()} on {@link #overrides()} does.
     *
     * @return text style overrides by style key; unmodifiable
     */
    public Map<String, TextStyle> textStyles() {
        return this.overrides.textStyles();
    }

    /**
     * Returns the insets overrides, as {@link ThemeOverrides#insets()} on {@link #overrides()} does.
     *
     * @return insets overrides by style key; unmodifiable
     */
    public Map<String, Insets> insets() {
        return this.overrides.insets();
    }

    /**
     * Returns the metric overrides, as {@link ThemeOverrides#metrics()} on {@link #overrides()} does.
     *
     * @return metric overrides by style key; unmodifiable
     */
    public Map<String, Float> metrics() {
        return this.overrides.metrics();
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
        return new ThemeRef(this.id, this.overrides.with(key, paint));
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
        return new ThemeRef(this.id, this.overrides.with(key, style));
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
        return new ThemeRef(this.id, this.overrides.with(key, insets));
    }

    /**
     * Returns a copy of this ref that sets a metric after the packs are applied. A later override of the same key
     * replaces an earlier one.
     *
     * @param key the style key
     * @param value the metric to use, in the unit its key states
     * @return the new ref
     * @throws IllegalArgumentException if {@code value} is not finite
     */
    public ThemeRef override(final String key, final float value) {
        return new ThemeRef(this.id, this.overrides.with(key, value));
    }

    /**
     * Returns a copy of this ref that sets every key of some overrides after the packs and the overrides in
     * {@link ThemeRegistry} are applied. A key this ref already overrides is replaced.
     *
     * @param overrides the keys to set
     * @return the new ref
     */
    public ThemeRef override(final ThemeOverrides overrides) {
        return new ThemeRef(this.id, this.overrides.with(overrides));
    }
}
