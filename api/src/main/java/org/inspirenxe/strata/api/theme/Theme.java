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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A set of style keys mapped to paints, text styles and insets. A key may have any of the three. Paints and text
 * styles say what a control looks like; insets, in UI units, say where it puts its content, such as the space
 * around a tab's text.
 *
 * <p>Themes are immutable values; make one with {@link #builder(String)}, or derive a variant of an existing theme
 * with {@link #toBuilder()}:</p>
 *
 * <pre>{@code
 * Theme dark = Themes.flat().toBuilder()
 *         .paint(Button.STYLE_NORMAL, Paint.solid(0xFF202020))
 *         .build();
 * }</pre>
 */
public final class Theme {
    private final String name;
    private final Map<String, Paint> paints;
    private final Map<String, TextStyle> textStyles;
    private final Map<String, Insets> insets;

    private Theme(final Builder builder) {
        this.name = builder.name;
        this.paints = Map.copyOf(builder.paints);
        this.textStyles = Map.copyOf(builder.textStyles);
        this.insets = Map.copyOf(builder.insets);
    }

    /**
     * Starts an empty theme.
     *
     * @param name a name for messages and debugging
     * @return a builder
     */
    public static Builder builder(final String name) {
        return new Builder(name);
    }

    /**
     * Returns the theme's name.
     *
     * @return the name given to the builder
     */
    public String name() {
        return this.name;
    }

    /**
     * Looks up a paint.
     *
     * @param key the style key
     * @return the paint, or empty if the key has none
     */
    public Optional<Paint> paint(final String key) {
        return Optional.ofNullable(this.paints.get(key));
    }

    /**
     * Looks up a paint, with a fallback.
     *
     * @param key the style key
     * @param fallback returned if the key has no paint
     * @return the paint, or {@code fallback}
     */
    public Paint paintOr(final String key, final Paint fallback) {
        return this.paints.getOrDefault(key, fallback);
    }

    /**
     * Looks up a text style.
     *
     * @param key the style key
     * @return the text style, or empty if the key has none
     */
    public Optional<TextStyle> textStyle(final String key) {
        return Optional.ofNullable(this.textStyles.get(key));
    }

    /**
     * Looks up a text style, with a fallback.
     *
     * @param key the style key
     * @param fallback returned if the key has no text style
     * @return the text style, or {@code fallback}
     */
    public TextStyle textStyleOr(final String key, final TextStyle fallback) {
        return this.textStyles.getOrDefault(key, fallback);
    }

    /**
     * Looks up insets.
     *
     * @param key the style key
     * @return the insets, or empty if the key has none
     */
    public Optional<Insets> insets(final String key) {
        return Optional.ofNullable(this.insets.get(key));
    }

    /**
     * Looks up insets, with a fallback.
     *
     * @param key the style key
     * @param fallback returned if the key has no insets
     * @return the insets, or {@code fallback}
     */
    public Insets insetsOr(final String key, final Insets fallback) {
        return this.insets.getOrDefault(key, fallback);
    }

    /**
     * Returns every paint this theme sets, by style key.
     *
     * @return an immutable map
     */
    public Map<String, Paint> paints() {
        return this.paints;
    }

    /**
     * Returns every text style this theme sets, by style key.
     *
     * @return an immutable map
     */
    public Map<String, TextStyle> textStyles() {
        return this.textStyles;
    }

    /**
     * Returns every set of insets this theme sets, by style key.
     *
     * @return an immutable map
     */
    public Map<String, Insets> insets() {
        return this.insets;
    }

    /**
     * Returns whether a key has a paint, a text style or insets.
     *
     * @param key the style key
     * @return true if the theme defines the key
     */
    public boolean has(final String key) {
        return this.paints.containsKey(key) || this.textStyles.containsKey(key) || this.insets.containsKey(key);
    }

    /**
     * Returns the keys this theme does not define.
     *
     * @param required the keys to check
     * @return the missing keys, in the order given; empty if none are missing
     */
    public Set<String> missing(final Collection<String> required) {
        final Set<String> missing = new LinkedHashSet<>();
        for (final String key : required) {
            if (!this.has(key)) {
                missing.add(key);
            }
        }
        return missing;
    }

    /**
     * Starts a builder holding all of this theme's keys and name, to make a variant.
     *
     * @return a builder
     */
    public Builder toBuilder() {
        final Builder builder = new Builder(this.name);
        builder.paints.putAll(this.paints);
        builder.textStyles.putAll(this.textStyles);
        builder.insets.putAll(this.insets);
        return builder;
    }

    /**
     * Collects keys for a new {@link Theme}. A builder is not thread-safe and can be reused after
     * {@link #build()}.
     */
    public static final class Builder {
        private final String name;
        private final Map<String, Paint> paints;
        private final Map<String, TextStyle> textStyles;
        private final Map<String, Insets> insets;

        private Builder(final String name) {
            this.name = Objects.requireNonNull(name, "name");
            this.paints = new HashMap<>();
            this.textStyles = new HashMap<>();
            this.insets = new HashMap<>();
        }

        /**
         * Sets a key's paint, replacing any earlier one.
         *
         * @param key the style key
         * @param paint the paint
         * @return this builder
         */
        public Builder paint(final String key, final Paint paint) {
            this.paints.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(paint, "paint"));
            return this;
        }

        /**
         * Sets a key's text style, replacing any earlier one.
         *
         * @param key the style key
         * @param style the text style
         * @return this builder
         */
        public Builder textStyle(final String key, final TextStyle style) {
            this.textStyles.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(style, "style"));
            return this;
        }

        /**
         * Sets a key's insets, replacing any earlier ones.
         *
         * @param key the style key
         * @param insets the insets, in UI units
         * @return this builder
         */
        public Builder insets(final String key, final Insets insets) {
            this.insets.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(insets, "insets"));
            return this;
        }

        /**
         * Creates the theme from the keys set so far.
         *
         * @return the theme
         */
        public Theme build() {
            return new Theme(this);
        }
    }
}
