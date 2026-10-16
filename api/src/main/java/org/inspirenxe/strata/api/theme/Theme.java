package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.text.TextStyle;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A set of style keys mapped to paints and text styles. A key may have a paint, a text style, or both.
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

    private Theme(final Builder builder) {
        this.name = builder.name;
        this.paints = Map.copyOf(builder.paints);
        this.textStyles = Map.copyOf(builder.textStyles);
    }

    /**
     * Starts an empty theme.
     *
     * @param name a name for messages and debugging
     * @return a builder
     * @throws NullPointerException if {@code name} is null
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
     * Returns whether a key has a paint or a text style.
     *
     * @param key the style key
     * @return true if the theme defines the key
     */
    public boolean has(final String key) {
        return this.paints.containsKey(key) || this.textStyles.containsKey(key);
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

        private Builder(final String name) {
            this.name = Objects.requireNonNull(name, "name");
            this.paints = new HashMap<>();
            this.textStyles = new HashMap<>();
        }

        /**
         * Sets a key's paint, replacing any earlier one.
         *
         * @param key the style key
         * @param paint the paint
         * @return this builder
         * @throws NullPointerException if either argument is null
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
         * @throws NullPointerException if either argument is null
         */
        public Builder textStyle(final String key, final TextStyle style) {
            this.textStyles.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(style, "style"));
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
