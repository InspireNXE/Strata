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
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.spi.TextSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core's text: a tree of records, one per way text is made or changed, each resolving its children and then doing its
 * own part. Every operation from {@link Text} is a default method here that wraps the receiver in another record,
 * so nothing is worked out until {@link #resolve} is called.
 */
sealed interface CoreText extends Text {

    @Override
    default String plain() {
        return Spans.plain(this.resolve(TextStyle.DEFAULT));
    }

    @Override
    default boolean isEmpty() {
        return this.plain().isEmpty();
    }

    @Override
    default Text append(final Text other) {
        return Concat.of(List.of(this, Objects.requireNonNull(other, "other")));
    }

    @Override
    default Text append(final String other) {
        return this.append(new Literal(other, null));
    }

    @Override
    default Text append(final String other, final TextStyle style) {
        return this.append(new Literal(other, Objects.requireNonNull(style, "style")));
    }

    @Override
    default Text style(final UnaryOperator<TextStyle> change) {
        return new Restyled(this, change);
    }

    @Override
    default Text color(final int argb) {
        return new Patched(this, TextStyle.DEFAULT.withColor(argb), Set.of(TextStyle.Property.COLOR));
    }

    @Override
    default Text bold() {
        return new Patched(this, TextStyle.DEFAULT.withBold(true), Set.of(TextStyle.Property.BOLD));
    }

    @Override
    default Text italic() {
        return new Patched(this, TextStyle.DEFAULT.withItalic(true), Set.of(TextStyle.Property.ITALIC));
    }

    @Override
    default Text underline() {
        return new Patched(this, TextStyle.DEFAULT.withUnderline(true), Set.of(TextStyle.Property.UNDERLINE));
    }

    @Override
    default Text strikethrough() {
        return new Patched(this, TextStyle.DEFAULT.withStrikethrough(true), Set.of(TextStyle.Property.STRIKETHROUGH));
    }

    @Override
    default Text mapSpans(final UnaryOperator<TextSpan> mapper) {
        return new Mapped(this, mapper);
    }

    @Override
    default Text styleMatches(final Pattern pattern, final UnaryOperator<TextStyle> change) {
        return new Matched(this, pattern, change);
    }

    @Override
    default Text replace(final String target, final Text replacement) {
        return new Replaced(this, target, replacement);
    }

    @Override
    default Text replace(final String target, final String replacement) {
        return new Replaced(this, target, new Literal(replacement, null));
    }

    @Override
    default Text trim() {
        return new Trimmed(this);
    }

    @Override
    default Text normalizeWhitespace() {
        return new Normalized(this);
    }

    @Override
    default List<Text> splitByLines() {
        final String plain = this.plain();
        int count = 1;
        for (int i = 0; i < plain.length(); i++) {
            if (plain.charAt(i) == '\n') {
                count++;
            }
        }
        final List<Text> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(new Line(this, i));
        }
        return List.copyOf(lines);
    }

    @Override
    default List<Text> splitByWords() {
        final int count = Word.ranges(this.plain()).size() / 2;
        final List<Text> words = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            words.add(new Word(this, i));
        }
        return List.copyOf(words);
    }

    /**
     * Literal characters, in the base style or, when {@code style} is set, in exactly that style.
     *
     * @param text the characters
     * @param style the fixed style, or null for the base
     */
    record Literal(String text, TextStyle style) implements CoreText {
        public Literal {
            Objects.requireNonNull(text, "text");
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            return this.text.isEmpty() ? List.of() : List.of(new TextSpan(this.text, this.style != null ? this.style : base));
        }

        @Override
        public String plain() {
            return this.text;
        }
    }

    /**
     * One inline element, in the base style.
     *
     * @param element the element
     */
    record Inline(InlineElement element) implements CoreText {
        public Inline {
            Objects.requireNonNull(element, "element");
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            return List.of(TextSpan.of(this.element, base));
        }

        @Override
        public String plain() {
            return TextSpan.OBJECT_REPLACEMENT;
        }
    }

    /**
     * A translation key and its arguments, formatted anew on every resolve.
     *
     * @param key the key
     * @param args the arguments, text or any object
     */
    record Translatable(String key, List<Object> args) implements CoreText {
        public Translatable {
            Objects.requireNonNull(key, "key");
            args = List.copyOf(args);
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final String pattern = Translations.translate(this.key);
            final List<TextSpan> out = new ArrayList<>();
            final StringBuilder literal = new StringBuilder();
            int next = 0;
            for (int i = 0; i < pattern.length(); i++) {
                final char c = pattern.charAt(i);
                if (c != '%' || i + 1 >= pattern.length()) {
                    literal.append(c);
                    continue;
                }
                final char kind = pattern.charAt(i + 1);
                final int index;
                final int specifierEnd;
                if (kind == '%') {
                    literal.append('%');
                    i++;
                    continue;
                } else if (kind == 's') {
                    index = next++;
                    specifierEnd = i + 2;
                } else if (Character.isDigit(kind)) {
                    int j = i + 1;
                    while (j < pattern.length() && Character.isDigit(pattern.charAt(j))) {
                        j++;
                    }
                    if (j + 1 >= pattern.length() || pattern.charAt(j) != '$' || pattern.charAt(j + 1) != 's') {
                        literal.append(c);
                        continue;
                    }
                    index = Integer.parseInt(pattern, i + 1, j, 10) - 1;
                    specifierEnd = j + 2;
                } else {
                    literal.append(c);
                    continue;
                }
                if (index < 0 || index >= this.args.size()) {
                    literal.append(pattern, i, specifierEnd);
                } else {
                    if (!literal.isEmpty()) {
                        out.add(new TextSpan(literal.toString(), base));
                        literal.setLength(0);
                    }
                    final Object arg = this.args.get(index);
                    if (arg instanceof final Text text) {
                        out.addAll(text.resolve(base));
                    } else {
                        out.add(new TextSpan(String.valueOf(arg), base));
                    }
                }
                i = specifierEnd - 1;
            }
            if (!literal.isEmpty()) {
                out.add(new TextSpan(literal.toString(), base));
            }
            return Spans.normalize(out);
        }
    }

    /**
     * Parts one after another.
     *
     * @param parts the parts
     */
    record Concat(List<Text> parts) implements CoreText {
        public Concat {
            parts = List.copyOf(parts);
        }

        static Text of(final List<? extends Text> parts) {
            final List<Text> flat = new ArrayList<>(parts.size());
            for (final Text part : parts) {
                if (part instanceof final Concat concat) {
                    flat.addAll(concat.parts);
                } else {
                    flat.add(Objects.requireNonNull(part, "part"));
                }
            }
            return flat.size() == 1 ? flat.getFirst() : new Concat(flat);
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final List<TextSpan> out = new ArrayList<>();
            for (final Text part : this.parts) {
                out.addAll(part.resolve(base));
            }
            return Spans.normalize(out);
        }
    }

    /**
     * Text resolved against a base with some properties set.
     *
     * @param inner the text
     * @param values the style holding the values to set
     * @param properties the properties to take from {@code values}
     */
    record Patched(Text inner, TextStyle values, Set<TextStyle.Property> properties) implements CoreText {
        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            return this.inner.resolve(base.merge(this.values, this.properties));
        }

        @Override
        public String plain() {
            return this.inner.plain();
        }
    }

    /**
     * Text resolved against a base a function makes.
     *
     * @param inner the text
     * @param change makes the inner base from the outer one
     */
    record Restyled(Text inner, UnaryOperator<TextStyle> change) implements CoreText {
        public Restyled {
            Objects.requireNonNull(change, "change");
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            return this.inner.resolve(Objects.requireNonNull(this.change.apply(base), "restyled base"));
        }

        @Override
        public String plain() {
            return this.inner.plain();
        }
    }

    /**
     * Text whose resolved spans are each changed.
     *
     * @param inner the text
     * @param mapper the change to each span
     */
    record Mapped(Text inner, UnaryOperator<TextSpan> mapper) implements CoreText {
        public Mapped {
            Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final List<TextSpan> spans = this.inner.resolve(base);
            final List<TextSpan> out = new ArrayList<>(spans.size());
            for (final TextSpan span : spans) {
                out.add(Objects.requireNonNull(this.mapper.apply(span), "mapped span"));
            }
            return Spans.normalize(out);
        }
    }

    /**
     * Text with the characters a pattern matches restyled.
     *
     * @param inner the text
     * @param pattern the pattern
     * @param change the change to each matched character's style
     */
    record Matched(Text inner, Pattern pattern, UnaryOperator<TextStyle> change) implements CoreText {
        public Matched {
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(change, "change");
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            List<TextSpan> spans = this.inner.resolve(base);
            final Matcher matcher = this.pattern.matcher(Spans.plain(spans));
            while (matcher.find()) {
                if (matcher.end() > matcher.start()) {
                    spans = Spans.restyle(spans, matcher.start(), matcher.end(), this.change);
                }
            }
            return Spans.normalize(spans);
        }

        @Override
        public String plain() {
            return this.inner.plain();
        }
    }

    /**
     * Text with every occurrence of some characters replaced.
     *
     * @param inner the text
     * @param target the characters to find
     * @param replacement the text to put in their place
     */
    record Replaced(Text inner, String target, Text replacement) implements CoreText {
        public Replaced {
            if (target.isEmpty()) {
                throw new IllegalArgumentException("Cannot replace empty text");
            }
            Objects.requireNonNull(replacement, "replacement");
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final List<TextSpan> spans = this.inner.resolve(base);
            final String plain = Spans.plain(spans);
            int found = plain.indexOf(this.target);
            if (found < 0) {
                return spans;
            }
            final List<TextSpan> out = new ArrayList<>();
            int done = 0;
            while (found >= 0) {
                out.addAll(Spans.slice(spans, done, found));
                out.addAll(this.replacement.resolve(Spans.styleAt(spans, found, base)));
                done = found + this.target.length();
                found = plain.indexOf(this.target, done);
            }
            out.addAll(Spans.slice(spans, done, plain.length()));
            return Spans.normalize(out);
        }
    }

    /**
     * Text without whitespace at its ends.
     *
     * @param inner the text
     */
    record Trimmed(Text inner) implements CoreText {
        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final List<TextSpan> spans = this.inner.resolve(base);
            final String plain = Spans.plain(spans);
            int start = 0;
            int end = plain.length();
            while (start < end && Character.isWhitespace(plain.charAt(start))) {
                start++;
            }
            while (end > start && Character.isWhitespace(plain.charAt(end - 1))) {
                end--;
            }
            return start == 0 && end == plain.length() ? spans : Spans.normalize(Spans.slice(spans, start, end));
        }
    }

    /**
     * Text with each run of whitespace turned into one space, in the style of the run's first character.
     *
     * @param inner the text
     */
    record Normalized(Text inner) implements CoreText {
        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final List<TextSpan> out = new ArrayList<>();
            boolean inSpace = false;
            for (final TextSpan span : this.inner.resolve(base)) {
                final String text = span.text();
                final StringBuilder kept = new StringBuilder(text.length());
                for (int i = 0; i < text.length(); i++) {
                    final char c = text.charAt(i);
                    if (Character.isWhitespace(c)) {
                        if (!inSpace) {
                            kept.append(' ');
                            inSpace = true;
                        }
                    } else {
                        kept.append(c);
                        inSpace = false;
                    }
                }
                out.add(span.withText(kept.toString()));
            }
            return Spans.normalize(out);
        }
    }

    /**
     * One line of a text, by its index among the line breaks.
     *
     * @param inner the text
     * @param index the line, from 0
     */
    record Line(Text inner, int index) implements CoreText {
        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final List<TextSpan> spans = this.inner.resolve(base);
            final String plain = Spans.plain(spans);
            int start = 0;
            for (int line = 0; line < this.index; line++) {
                final int brk = plain.indexOf('\n', start);
                if (brk < 0) {
                    return List.of();
                }
                start = brk + 1;
            }
            final int brk = plain.indexOf('\n', start);
            return Spans.normalize(Spans.slice(spans, start, brk < 0 ? plain.length() : brk));
        }
    }

    /**
     * One word of a text, by its index among the runs of characters between whitespace.
     *
     * @param inner the text
     * @param index the word, from 0
     */
    record Word(Text inner, int index) implements CoreText {
        /**
         * Returns the start and end of each word, flattened: start, end, start, end and so on.
         */
        static List<Integer> ranges(final String plain) {
            final List<Integer> ranges = new ArrayList<>();
            int i = 0;
            while (i < plain.length()) {
                while (i < plain.length() && Character.isWhitespace(plain.charAt(i))) {
                    i++;
                }
                if (i >= plain.length()) {
                    break;
                }
                final int start = i;
                while (i < plain.length() && !Character.isWhitespace(plain.charAt(i))) {
                    i++;
                }
                ranges.add(start);
                ranges.add(i);
            }
            return ranges;
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            final List<TextSpan> spans = this.inner.resolve(base);
            final List<Integer> ranges = Word.ranges(Spans.plain(spans));
            if (this.index * 2 + 1 >= ranges.size()) {
                return List.of();
            }
            return Spans.normalize(Spans.slice(spans, ranges.get(this.index * 2), ranges.get(this.index * 2 + 1)));
        }
    }

    /**
     * An engine's own text.
     *
     * @param source the engine text
     */
    record Sourced(TextSource source) implements CoreText {
        public Sourced {
            Objects.requireNonNull(source, "source");
        }

        @Override
        public List<TextSpan> resolve(final TextStyle base) {
            return Spans.normalize(this.source.resolve(base));
        }
    }
}
