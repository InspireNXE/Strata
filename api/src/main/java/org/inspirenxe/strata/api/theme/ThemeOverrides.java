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
import org.inspirenxe.strata.api.text.TextStylePatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Keys to set on a theme: paints, text styles, insets and metrics by style key, with no theme id. The same overrides can
 * change a theme for every screen with {@link ThemeRegistry#override}, start a theme of your own with
 * {@link ThemeRegistry#define}, or change a theme for one screen with {@link ThemeRef#override(ThemeOverrides)}.
 *
 * <pre>{@code
 * ThemeOverrides green = ThemeOverrides.of()
 *         .with(Button.STYLE_NORMAL, Paint.solid(0xFF2E7D32))
 *         .with(Button.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFFFFFFF));
 * }</pre>
 *
 * <p>Each value replaces the whole value of its key. A text style set here replaces every field of the style
 * beneath it. A {@link TextStylePatch} changes only the fields it sets, as a theme file's text entry does, so
 * {@code with(Label.STYLE_TEXT, TextStylePatch.of().color(0xFFE0C060))} recolors labels and keeps the shadow the
 * theme gives them. A key given both a style and a patch takes the style, then the patch over it.</p>
 *
 * <p>Overrides are immutable. The {@code with} methods return new overrides.</p>
 *
 * @param paints paints by style key; copied
 * @param textStyles text styles by style key; copied
 * @param insets insets by style key, in UI units; copied
 * @param metrics metrics by style key, each in the unit its key states; copied
 * @param textPatches text style patches by style key, applied over the style beneath each key; copied
 */
public record ThemeOverrides(Map<String, Paint> paints, Map<String, TextStyle> textStyles,
                             Map<String, Insets> insets, Map<String, Float> metrics,
                             Map<String, TextStylePatch> textPatches) {
    private static final ThemeOverrides EMPTY = new ThemeOverrides(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

    /**
     * Copies the maps.
     *
     * @throws IllegalArgumentException if a metric is not finite
     */
    public ThemeOverrides {
        paints = Map.copyOf(paints);
        textStyles = Map.copyOf(textStyles);
        insets = Map.copyOf(insets);
        metrics = Map.copyOf(metrics);
        textPatches = Map.copyOf(textPatches);
        metrics.forEach((key, value) -> {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Metric " + key + " is not finite: " + value);
            }
        });
    }

    /**
     * Creates overrides that set no text style patches.
     *
     * @param paints paints by style key; copied
     * @param textStyles text styles by style key; copied
     * @param insets insets by style key, in UI units; copied
     * @param metrics metrics by style key, each in the unit its key states; copied
     * @throws IllegalArgumentException if a metric is not finite
     */
    public ThemeOverrides(final Map<String, Paint> paints, final Map<String, TextStyle> textStyles,
                          final Map<String, Insets> insets, final Map<String, Float> metrics) {
        this(paints, textStyles, insets, metrics, Map.of());
    }

    /**
     * Creates overrides that set no metrics and no text style patches.
     *
     * @param paints paints by style key; copied
     * @param textStyles text styles by style key; copied
     * @param insets insets by style key, in UI units; copied
     */
    public ThemeOverrides(final Map<String, Paint> paints, final Map<String, TextStyle> textStyles,
                          final Map<String, Insets> insets) {
        this(paints, textStyles, insets, Map.of());
    }

    /**
     * Returns overrides that set no keys.
     *
     * @return the empty overrides
     */
    public static ThemeOverrides of() {
        return EMPTY;
    }

    /**
     * Returns a copy of these overrides that also sets a paint, replacing any paint they set for the key.
     *
     * @param key the style key
     * @param paint the paint
     * @return the new overrides
     */
    public ThemeOverrides with(final String key, final Paint paint) {
        final Map<String, Paint> all = new HashMap<>(this.paints);
        all.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(paint, "paint"));
        return new ThemeOverrides(all, this.textStyles, this.insets, this.metrics, this.textPatches);
    }

    /**
     * Returns a copy of these overrides that also sets a text style, replacing any style they set for the key.
     *
     * @param key the style key
     * @param style the text style
     * @return the new overrides
     */
    public ThemeOverrides with(final String key, final TextStyle style) {
        final Map<String, TextStyle> all = new HashMap<>(this.textStyles);
        all.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(style, "style"));
        return new ThemeOverrides(this.paints, all, this.insets, this.metrics, this.textPatches);
    }

    /**
     * Returns a copy of these overrides that also patches a text style, changing only the properties the patch sets
     * on the style beneath the key. A patch these overrides already hold for the key applies first, so the new one
     * wins where both set a property.
     *
     * @param key the style key
     * @param patch the patch
     * @return the new overrides
     */
    public ThemeOverrides with(final String key, final TextStylePatch patch) {
        Objects.requireNonNull(patch, "patch");
        final Map<String, TextStylePatch> all = new HashMap<>(this.textPatches);
        all.merge(Objects.requireNonNull(key, "key"), patch, TextStylePatch::then);
        return new ThemeOverrides(this.paints, this.textStyles, this.insets, this.metrics, all);
    }

    /**
     * Returns a copy of these overrides that also sets insets, replacing any insets they set for the key.
     *
     * @param key the style key
     * @param insets the insets, in UI units
     * @return the new overrides
     */
    public ThemeOverrides with(final String key, final Insets insets) {
        final Map<String, Insets> all = new HashMap<>(this.insets);
        all.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(insets, "insets"));
        return new ThemeOverrides(this.paints, this.textStyles, all, this.metrics, this.textPatches);
    }

    /**
     * Returns a copy of these overrides that also sets a metric, replacing any metric they set for the key.
     *
     * @param key the style key
     * @param value the metric, in the unit its key states
     * @return the new overrides
     * @throws IllegalArgumentException if {@code value} is not finite
     */
    public ThemeOverrides with(final String key, final float value) {
        final Map<String, Float> all = new HashMap<>(this.metrics);
        all.put(Objects.requireNonNull(key, "key"), value);
        return new ThemeOverrides(this.paints, this.textStyles, this.insets, all, this.textPatches);
    }

    /**
     * Returns a copy of these overrides that also sets a flag, a metric of 1 for true or 0 for false, as
     * {@link Theme#flagOr} reads it, replacing any metric they set for the key.
     *
     * @param key the style key
     * @param flag the flag
     * @return the new overrides
     */
    public ThemeOverrides with(final String key, final boolean flag) {
        return this.with(key, flag ? 1f : 0f);
    }

    /**
     * Returns a copy of these overrides that also sets every key of others, replacing what these set for the same
     * key and kind of value. Text style patches for the same key combine, the patch of others applying second.
     *
     * @param others the keys to add
     * @return the new overrides
     */
    public ThemeOverrides with(final ThemeOverrides others) {
        final Map<String, Paint> paints = new HashMap<>(this.paints);
        paints.putAll(others.paints);
        final Map<String, TextStyle> styles = new HashMap<>(this.textStyles);
        styles.putAll(others.textStyles);
        final Map<String, Insets> insets = new HashMap<>(this.insets);
        insets.putAll(others.insets);
        final Map<String, Float> metrics = new HashMap<>(this.metrics);
        metrics.putAll(others.metrics);
        final Map<String, TextStylePatch> patches = new HashMap<>(this.textPatches);
        others.textPatches.forEach((key, patch) -> patches.merge(key, patch, TextStylePatch::then));
        return new ThemeOverrides(paints, styles, insets, metrics, patches);
    }
}
