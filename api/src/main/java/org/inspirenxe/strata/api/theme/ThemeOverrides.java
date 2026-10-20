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
 * Keys to set on a theme: paints, text styles and insets by style key, with no theme id. The same overrides can
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
 * beneath it, where a theme file's text entry changes only the fields it names.</p>
 *
 * <p>Overrides are immutable. The {@code with} methods return new overrides.</p>
 *
 * @param paints paints by style key; copied
 * @param textStyles text styles by style key; copied
 * @param insets insets by style key, in UI units; copied
 */
public record ThemeOverrides(Map<String, Paint> paints, Map<String, TextStyle> textStyles,
                             Map<String, Insets> insets) {
    private static final ThemeOverrides EMPTY = new ThemeOverrides(Map.of(), Map.of(), Map.of());

    /**
     * Copies the maps.
     */
    public ThemeOverrides {
        paints = Map.copyOf(paints);
        textStyles = Map.copyOf(textStyles);
        insets = Map.copyOf(insets);
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
        return new ThemeOverrides(all, this.textStyles, this.insets);
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
        return new ThemeOverrides(this.paints, all, this.insets);
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
        return new ThemeOverrides(this.paints, this.textStyles, all);
    }
}
