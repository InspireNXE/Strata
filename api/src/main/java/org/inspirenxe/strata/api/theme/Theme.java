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
 * Maps semantic style keys ({@code button.normal}, {@code text.primary}) to paints and text styles. Immutable; use
 * {@link #builder()} or {@link #toBuilder()} to make a variant.
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

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public String name() {
        return this.name;
    }

    public Optional<Paint> paint(final String key) {
        return Optional.ofNullable(this.paints.get(key));
    }

    public Paint paintOr(final String key, final Paint fallback) {
        return this.paints.getOrDefault(key, fallback);
    }

    public Optional<TextStyle> textStyle(final String key) {
        return Optional.ofNullable(this.textStyles.get(key));
    }

    public TextStyle textStyleOr(final String key, final TextStyle fallback) {
        return this.textStyles.getOrDefault(key, fallback);
    }

    public boolean has(final String key) {
        return this.paints.containsKey(key) || this.textStyles.containsKey(key);
    }

    /**
     * Returns every key from {@code required} that this theme does not define.
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

    public Builder toBuilder() {
        final Builder builder = new Builder(this.name);
        builder.paints.putAll(this.paints);
        builder.textStyles.putAll(this.textStyles);
        return builder;
    }

    public static final class Builder {
        private final String name;
        private final Map<String, Paint> paints;
        private final Map<String, TextStyle> textStyles;

        private Builder(final String name) {
            this.name = Objects.requireNonNull(name, "name");
            this.paints = new HashMap<>();
            this.textStyles = new HashMap<>();
        }

        public Builder paint(final String key, final Paint paint) {
            this.paints.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(paint, "paint"));
            return this;
        }

        public Builder textStyle(final String key, final TextStyle style) {
            this.textStyles.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(style, "style"));
            return this;
        }

        public Theme build() {
            return new Theme(this);
        }
    }
}
