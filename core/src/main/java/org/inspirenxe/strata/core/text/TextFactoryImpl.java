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
package org.inspirenxe.strata.core.text;

import org.inspirenxe.strata.api.text.InlineElement;
import org.inspirenxe.strata.api.text.PluralCategory;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextFactory;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.spi.TextSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The core {@link TextFactory}. Text is a tree of immutable records that does its work when it is resolved; see
 * {@link Text} for what each kind does.
 */
public final class TextFactoryImpl implements TextFactory {
    /**
     * The one factory.
     */
    public static final TextFactoryImpl INSTANCE = new TextFactoryImpl();

    private static final Text EMPTY = new CoreText.Literal("", null);

    private TextFactoryImpl() {
    }

    @Override
    public Text empty() {
        return EMPTY;
    }

    @Override
    public Text of(final String text) {
        return text.isEmpty() ? EMPTY : new CoreText.Literal(text, null);
    }

    @Override
    public Text of(final String text, final TextStyle style) {
        return new CoreText.Literal(text, Objects.requireNonNull(style, "style"));
    }

    @Override
    public Text translatable(final String key, final Object... args) {
        for (final Object arg : args) {
            Objects.requireNonNull(arg, "arg");
        }
        return new CoreText.Translatable(key, Arrays.asList(args));
    }

    @Override
    public Text plural(final String key, final long count, final Object... args) {
        for (final Object arg : args) {
            Objects.requireNonNull(arg, "arg");
        }
        return new CoreText.Plural(key, count, Arrays.asList(args));
    }

    @Override
    public PluralCategory pluralCategory(final Locale locale, final long count) {
        return PluralRules.select(Objects.requireNonNull(locale, "locale"), count);
    }

    @Override
    public Text concat(final List<? extends Text> parts) {
        return parts.isEmpty() ? EMPTY : CoreText.Concat.of(parts);
    }

    @Override
    public Text join(final Text separator, final Iterable<? extends Text> parts) {
        Objects.requireNonNull(separator, "separator");
        final List<Text> joined = new ArrayList<>();
        for (final Text part : parts) {
            if (!joined.isEmpty()) {
                joined.add(separator);
            }
            joined.add(Objects.requireNonNull(part, "part"));
        }
        return this.concat(joined);
    }

    @Override
    public Text inline(final InlineElement element) {
        return new CoreText.Inline(element);
    }

    @Override
    public Text.Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Wraps an engine's own text.
     *
     * @param source the engine text
     * @return the text
     */
    public Text of(final TextSource source) {
        return new CoreText.Sourced(source);
    }

    private static final class BuilderImpl implements Text.Builder {
        private final List<Text> parts = new ArrayList<>();

        @Override
        public Text.Builder append(final Text text) {
            this.parts.add(Objects.requireNonNull(text, "text"));
            return this;
        }

        @Override
        public Text.Builder append(final String text) {
            return this.append(new CoreText.Literal(text, null));
        }

        @Override
        public Text.Builder append(final String text, final TextStyle style) {
            return this.append(new CoreText.Literal(text, Objects.requireNonNull(style, "style")));
        }

        @Override
        public Text.Builder color(final String text, final int argb) {
            return this.append(new CoreText.Literal(text, null).color(argb));
        }

        @Override
        public Text.Builder bold(final String text) {
            return this.append(new CoreText.Literal(text, null).bold());
        }

        @Override
        public Text.Builder italic(final String text) {
            return this.append(new CoreText.Literal(text, null).italic());
        }

        @Override
        public Text build() {
            return this.parts.isEmpty() ? EMPTY : CoreText.Concat.of(this.parts);
        }
    }
}
