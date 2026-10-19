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

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * How a span of text looks. Immutable; the {@code with} methods return modified copies, and
 * {@link #merge(TextStyle, Property, Property...)} takes chosen properties from another style.
 *
 * <p>Engines draw what they can: an engine with one font weight may fake or ignore bold. An adapter may use
 * the engine's own bold, italic, underline, strikethrough and shadow; the Swing adapter derives bold and italic
 * fonts and draws the lines and shadow itself.</p>
 *
 * @param color the color as ARGB; alpha 0 draws nothing
 * @param bold whether the text is bold
 * @param italic whether the text is italic
 * @param underline whether a line is drawn under the text
 * @param strikethrough whether a line is drawn through the text
 * @param shadow whether a darker copy is drawn one unit down and to the right
 */
public record TextStyle(int color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean shadow) {
    /**
     * Opaque white, no decoration, no shadow.
     */
    public static final TextStyle DEFAULT = new TextStyle(0xFFFFFFFF, false, false, false, false, false);

    /**
     * Returns a copy with another color.
     *
     * @param color the color as ARGB
     * @return the new style
     */
    public TextStyle withColor(final int color) {
        return new TextStyle(color, this.bold, this.italic, this.underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with bold set or cleared.
     *
     * @param bold true for bold
     * @return the new style
     */
    public TextStyle withBold(final boolean bold) {
        return new TextStyle(this.color, bold, this.italic, this.underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with italic set or cleared.
     *
     * @param italic true for italic
     * @return the new style
     */
    public TextStyle withItalic(final boolean italic) {
        return new TextStyle(this.color, this.bold, italic, this.underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with underline set or cleared.
     *
     * @param underline true to underline
     * @return the new style
     */
    public TextStyle withUnderline(final boolean underline) {
        return new TextStyle(this.color, this.bold, this.italic, underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with strikethrough set or cleared.
     *
     * @param strikethrough true to strike through
     * @return the new style
     */
    public TextStyle withStrikethrough(final boolean strikethrough) {
        return new TextStyle(this.color, this.bold, this.italic, this.underline, strikethrough, this.shadow);
    }

    /**
     * Returns a copy with the shadow set or cleared.
     *
     * @param shadow true to draw a shadow
     * @return the new style
     */
    public TextStyle withShadow(final boolean shadow) {
        return new TextStyle(this.color, this.bold, this.italic, this.underline, this.strikethrough, shadow);
    }

    /**
     * Returns a copy that takes the given properties from another style and keeps the rest of this one. The
     * properties are named because a style cannot say which of its values were meant to be set: {@code false} is a
     * value like any other, so naming them is what lets a merge turn bold or a shadow off as well as on.
     *
     * <pre>{@code
     * // A theme's field style with a highlighter's color and weight, keeping the theme's shadow
     * TextStyle keyword = base.merge(KEYWORD, TextStyle.Property.COLOR, TextStyle.Property.BOLD);
     * }</pre>
     *
     * <p>Merging every property returns a style equal to {@code other}; merging the same property twice has no
     * further effect.</p>
     *
     * @param other the style to take the properties from
     * @param first a property to take
     * @param rest more properties to take
     * @return the new style
     */
    public TextStyle merge(final TextStyle other, final Property first, final Property... rest) {
        return this.merge(other, EnumSet.of(first, rest));
    }

    /**
     * Returns a copy that takes the given properties from another style and keeps the rest of this one; see
     * {@link #merge(TextStyle, Property, Property...)}.
     *
     * @param other the style to take the properties from
     * @param properties the properties to take; an empty set gives a style equal to this one
     * @return the new style
     */
    public TextStyle merge(final TextStyle other, final Set<Property> properties) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(properties, "properties");
        return new TextStyle(
                properties.contains(Property.COLOR) ? other.color : this.color,
                properties.contains(Property.BOLD) ? other.bold : this.bold,
                properties.contains(Property.ITALIC) ? other.italic : this.italic,
                properties.contains(Property.UNDERLINE) ? other.underline : this.underline,
                properties.contains(Property.STRIKETHROUGH) ? other.strikethrough : this.strikethrough,
                properties.contains(Property.SHADOW) ? other.shadow : this.shadow);
    }

    /**
     * A property of a {@link TextStyle}, for naming the ones a {@linkplain #merge(TextStyle, Property, Property...)
     * merge} takes.
     */
    public enum Property {
        /**
         * {@link TextStyle#color()}.
         */
        COLOR,
        /**
         * {@link TextStyle#bold()}.
         */
        BOLD,
        /**
         * {@link TextStyle#italic()}.
         */
        ITALIC,
        /**
         * {@link TextStyle#underline()}.
         */
        UNDERLINE,
        /**
         * {@link TextStyle#strikethrough()}.
         */
        STRIKETHROUGH,
        /**
         * {@link TextStyle#shadow()}.
         */
        SHADOW
    }
}
