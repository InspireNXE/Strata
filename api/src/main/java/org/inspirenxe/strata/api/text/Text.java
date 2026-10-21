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
package org.inspirenxe.strata.api.text;

import org.inspirenxe.strata.api.Strata;

import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Styled text that controls show: captions, titles, tooltips, messages. Text is a description that is read when a
 * control lays out, so translated text follows the current language, and text an engine adapter wraps follows the
 * engine's own text system.
 *
 * <pre>{@code
 * Text price = Text.translatable("shop.price", Text.of("64").color(0xFFFFAA00).bold());
 * Text shown = canAfford ? price : price.color(0xFFFF5555);
 * label.setText(shown);
 * }</pre>
 *
 * <h2>Styles</h2>
 *
 * <p>Text has no style of its own until it is {@linkplain #resolve resolved} against a base style, which is the one
 * the control would draw plain text in. {@link #of(String)} draws in the base. {@link #style}, {@link #color},
 * {@link #bold} and the like change the base for the text they are called on, so parts that set the same property
 * themselves keep theirs: a red line keeps its gold number. {@link #of(String, TextStyle)} ignores the base and
 * draws in exactly the style given. {@link #mapSpans} and {@link #styleMatches} work on the resolved spans, after
 * every part has its style, so they change parts that set their own style too.</p>
 *
 * <h2>Operations</h2>
 *
 * <p>Text is immutable; every operation returns new text. Operations run each time the text is resolved, so the
 * result of {@link #replace} or {@link #trim} on translated text still follows the language. The splits are the
 * exception: how many lines or words there are is decided when they are called, though each part still resolves
 * on its own.</p>
 *
 * <h2>Equality</h2>
 *
 * <p>Two texts are equal when they were built the same way from equal parts: literal text by its characters and
 * style, translated text by its key and arguments, not by what it reads in the current language. Operations that
 * take a function or a pattern are equal only to themselves.</p>
 *
 * <p>Text is made by Strata's core through {@link TextFactory}, and engine adapters can wrap their own text in it.
 * Do not implement this interface.</p>
 */
public interface Text {

    /**
     * Returns text with no characters.
     *
     * @return the empty text
     */
    static Text empty() {
        return Strata.texts().empty();
    }

    /**
     * Returns literal text drawn in the base style.
     *
     * @param text the characters, with {@code \n} for line breaks where the control allows them
     * @return the text
     */
    static Text of(final String text) {
        return Strata.texts().of(text);
    }

    /**
     * Returns literal text drawn in exactly the given style, whatever the base.
     *
     * @param text the characters
     * @param style the style
     * @return the text
     */
    static Text of(final String text, final TextStyle style) {
        return Strata.texts().of(text, style);
    }

    /**
     * Returns text that the platform translates into the current language each time it is resolved. The
     * translation is a pattern in which {@code %s} stands for the next argument, {@code %1$s} for the first, and
     * {@code %%} for a percent sign. A text argument is resolved in place against the same base, keeping its own
     * styles; any other argument is shown as {@link String#valueOf(Object)}. A key with no translation shows the key
     * itself.
     *
     * @param key the translation key, such as {@code shop.price}
     * @param args the arguments, text or any object; none may be null
     * @return the text
     */
    static Text translatable(final String key, final Object... args) {
        return Strata.texts().translatable(key, args);
    }

    /**
     * Returns the parts one after another.
     *
     * @param parts the parts
     * @return the text
     */
    static Text concat(final Text... parts) {
        return Strata.texts().concat(List.of(parts));
    }

    /**
     * Returns the parts one after another.
     *
     * @param parts the parts
     * @return the text
     */
    static Text concat(final List<? extends Text> parts) {
        return Strata.texts().concat(parts);
    }

    /**
     * Returns the parts with a separator between each two.
     *
     * <pre>{@code
     * Text names = Text.join(Text.of(", "), List.of(Text.of("Ana"), Text.of("Ben"), Text.of("Cleo")));
     * }</pre>
     *
     * @param separator the text between parts
     * @param parts the parts
     * @return the text, empty when there are no parts
     */
    static Text join(final Text separator, final Iterable<? extends Text> parts) {
        return Strata.texts().join(separator, parts);
    }

    /**
     * Makes text that shows an engine visual at its natural size, in the line where it lands. A label makes the line
     * tall enough for it, and the text beside it is centered on the line. In {@link #plain()} it is
     * {@link TextSpan#OBJECT_REPLACEMENT}.
     *
     * @param visual the visual
     * @return the text
     */
    static Text visual(final Visual visual) {
        return Strata.texts().inline(new InlineVisual(visual));
    }

    /**
     * Makes text that shows a sprite as a square as tall as the line height of the style where it lands, so it sits
     * in the text like a character. In {@link #plain()} it is {@link TextSpan#OBJECT_REPLACEMENT}.
     *
     * @param sprite the sprite
     * @return the text
     */
    static Text sprite(final SpriteId sprite) {
        return Strata.texts().inline(new InlineSprite(sprite));
    }

    /**
     * Makes text that shows a sprite at a given size. A label makes the line tall enough for it, and the text beside
     * it is centered on the line. In {@link #plain()} it is {@link TextSpan#OBJECT_REPLACEMENT}.
     *
     * @param sprite the sprite
     * @param width its width in UI units
     * @param height its height in UI units
     * @return the text
     * @throws IllegalArgumentException if the size is not positive and finite
     */
    static Text sprite(final SpriteId sprite, final float width, final float height) {
        if (width == 0f && height == 0f) {
            throw new IllegalArgumentException("Inline sprite size must be positive: 0 x 0");
        }
        return Strata.texts().inline(new InlineSprite(sprite, width, height));
    }

    /**
     * Starts building text from parts, which reads better than a chain of {@link #append} once a line has many.
     *
     * @return a new builder
     */
    static Builder builder() {
        return Strata.texts().builder();
    }

    /**
     * Reads the text as it is now: translated, with every operation applied and every part given its style.
     * Adjacent spans with equal styles are joined and empty spans are left out.
     *
     * @param base the style plain parts are drawn in, usually the control's text style
     * @return the spans, in order; empty for empty text
     */
    List<TextSpan> resolve(TextStyle base);

    /**
     * Returns the characters as they read now, without styles, for searching, sorting and narration.
     *
     * @return the characters
     */
    String plain();

    /**
     * Returns whether the text reads as no characters now.
     *
     * @return whether {@link #plain()} is empty
     */
    boolean isEmpty();

    /**
     * Returns this text followed by another.
     *
     * @param other the text to add
     * @return the new text
     */
    Text append(Text other);

    /**
     * Returns this text followed by literal text in the base style.
     *
     * @param other the characters to add
     * @return the new text
     */
    Text append(String other);

    /**
     * Returns this text followed by literal text in exactly the given style.
     *
     * @param other the characters to add
     * @param style the style
     * @return the new text
     */
    Text append(String other, TextStyle style);

    /**
     * Returns this text resolved against a changed base, so its parts that set the same properties keep theirs.
     *
     * <pre>{@code
     * Text dimmed = name.style(base -> base.withColor(0xFF808080).withItalic(true));
     * }</pre>
     *
     * @param change makes the base this text resolves against from the base it is given; a pure function
     * @return the new text
     */
    Text style(UnaryOperator<TextStyle> change);

    /**
     * Returns this text with the base color changed; see {@link #style}.
     *
     * @param argb the color, ARGB
     * @return the new text
     */
    Text color(int argb);

    /**
     * Returns this text with a bold base; see {@link #style}.
     *
     * @return the new text
     */
    Text bold();

    /**
     * Returns this text with an italic base; see {@link #style}.
     *
     * @return the new text
     */
    Text italic();

    /**
     * Returns this text with an underlined base; see {@link #style}.
     *
     * @return the new text
     */
    Text underline();

    /**
     * Returns this text with a struck-through base; see {@link #style}.
     *
     * @return the new text
     */
    Text strikethrough();

    /**
     * Returns this text with every resolved span changed, after every part has its style.
     *
     * <pre>{@code
     * Text allRed = price.mapSpans(span -> span.withStyle(span.style().withColor(0xFFFF5555)));
     * }</pre>
     *
     * @param mapper changes one span; a pure function that must not return null
     * @return the new text
     */
    Text mapSpans(UnaryOperator<TextSpan> mapper);

    /**
     * Returns this text with the characters a pattern matches restyled in place, after every part has its style.
     * Matches may cross parts. Empty matches are skipped.
     *
     * <pre>{@code
     * Pattern query = Pattern.compile(Pattern.quote(search.text()), Pattern.CASE_INSENSITIVE);
     * Text row = name.styleMatches(query, style -> style.withBold(true).withUnderline(true));
     * }</pre>
     *
     * @param pattern the pattern, matched against {@link #plain()}
     * @param change restyles a matched character's style; a pure function
     * @return the new text
     */
    Text styleMatches(Pattern pattern, UnaryOperator<TextStyle> change);

    /**
     * Returns this text with every occurrence of some characters replaced. The replacement resolves against the
     * style the first replaced character had, so it fits in where it lands.
     *
     * @param target the characters to find, matched exactly; must not be empty
     * @param replacement the text to put in their place
     * @return the new text
     * @throws IllegalArgumentException if {@code target} is empty
     */
    Text replace(String target, Text replacement);

    /**
     * Returns this text with every occurrence of some characters replaced by others; see
     * {@link #replace(String, Text)}.
     *
     * @param target the characters to find, matched exactly; must not be empty
     * @param replacement the characters to put in their place
     * @return the new text
     * @throws IllegalArgumentException if {@code target} is empty
     */
    Text replace(String target, String replacement);

    /**
     * Returns this text without whitespace at its start and end.
     *
     * @return the new text
     */
    Text trim();

    /**
     * Returns this text with each run of whitespace, line breaks included, turned into one space.
     *
     * @return the new text
     */
    Text normalizeWhitespace();

    /**
     * Splits this text at its line breaks. The number of lines is decided now; each line resolves on its own later,
     * and is empty if the text has fewer lines by then.
     *
     * @return the lines, at least one
     */
    List<Text> splitByLines();

    /**
     * Splits this text into its words, the runs of characters between whitespace. The number of words is decided
     * now; each word resolves on its own later, and is empty if the text has fewer words by then.
     *
     * @return the words; empty if the text is all whitespace
     */
    List<Text> splitByWords();

    /**
     * Builds text from parts. Do not implement this interface.
     */
    interface Builder {
        /**
         * Adds text.
         *
         * @param text the text
         * @return this builder
         */
        Builder append(Text text);

        /**
         * Adds literal text in the base style.
         *
         * @param text the characters
         * @return this builder
         */
        Builder append(String text);

        /**
         * Adds literal text in exactly the given style.
         *
         * @param text the characters
         * @param style the style
         * @return this builder
         */
        Builder append(String text, TextStyle style);

        /**
         * Adds literal text in the base style with another color.
         *
         * @param text the characters
         * @param argb the color, ARGB
         * @return this builder
         */
        Builder color(String text, int argb);

        /**
         * Adds literal text in the base style, bold.
         *
         * @param text the characters
         * @return this builder
         */
        Builder bold(String text);

        /**
         * Adds literal text in the base style, italic.
         *
         * @param text the characters
         * @return this builder
         */
        Builder italic(String text);

        /**
         * Returns the parts added so far as one text. The builder can go on adding.
         *
         * @return the text
         */
        Text build();
    }
}
