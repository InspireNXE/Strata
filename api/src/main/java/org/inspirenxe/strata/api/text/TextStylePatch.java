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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A change to some properties of a {@link TextStyle}, applied over a style that comes from elsewhere. A theme
 * file's {@code text} entry is one, and code can give one to {@code ThemeOverrides} or {@code ThemeRef} to recolor
 * a key while keeping the rest of the style the theme gives it, such as its shadow:
 *
 * <pre>{@code
 * ThemeRef.platform().override(Label.STYLE_TEXT, TextStylePatch.of().color(0xFFE0C060));
 * }</pre>
 *
 * <p>The patch pairs a style holding the values with the set of properties it changes, so it can turn bold or a
 * shadow off as well as on. Patches are immutable; the setters return new patches. The values of properties the
 * patch does not change are kept as those of {@link TextStyle#DEFAULT}, so two patches that change the same
 * properties to the same values are equal.</p>
 *
 * @param values the values of the properties the patch changes
 * @param properties the properties the patch changes; copied
 */
public record TextStylePatch(TextStyle values, Set<TextStyle.Property> properties) {
    private static final TextStylePatch EMPTY = new TextStylePatch(TextStyle.DEFAULT, Set.of());

    /**
     * Copies the properties and resets the values of the others to those of {@link TextStyle#DEFAULT}.
     */
    public TextStylePatch {
        Objects.requireNonNull(values, "values");
        final EnumSet<TextStyle.Property> copy = EnumSet.noneOf(TextStyle.Property.class);
        copy.addAll(properties);
        properties = Collections.unmodifiableSet(copy);
        values = TextStyle.DEFAULT.merge(values, copy);
    }

    /**
     * Returns a patch that changes nothing.
     *
     * @return the empty patch
     */
    public static TextStylePatch of() {
        return EMPTY;
    }

    /**
     * Returns a patch that changes every property to those of a style, so applying it gives that style.
     *
     * @param style the style
     * @return the patch
     */
    public static TextStylePatch of(final TextStyle style) {
        return new TextStylePatch(style, EnumSet.allOf(TextStyle.Property.class));
    }

    /**
     * Returns whether the patch changes nothing.
     *
     * @return true if no property is set
     */
    public boolean isEmpty() {
        return this.properties.isEmpty();
    }

    /**
     * Returns a copy that also sets the color.
     *
     * @param argb the color as ARGB
     * @return the new patch
     */
    public TextStylePatch color(final int argb) {
        return this.with(this.values.withColor(argb), TextStyle.Property.COLOR);
    }

    /**
     * Returns a copy that also sets bold.
     *
     * @param bold true for bold
     * @return the new patch
     */
    public TextStylePatch bold(final boolean bold) {
        return this.with(this.values.withBold(bold), TextStyle.Property.BOLD);
    }

    /**
     * Returns a copy that also sets italic.
     *
     * @param italic true for italic
     * @return the new patch
     */
    public TextStylePatch italic(final boolean italic) {
        return this.with(this.values.withItalic(italic), TextStyle.Property.ITALIC);
    }

    /**
     * Returns a copy that also sets the underline.
     *
     * @param underline true to underline
     * @return the new patch
     */
    public TextStylePatch underline(final boolean underline) {
        return this.with(this.values.withUnderline(underline), TextStyle.Property.UNDERLINE);
    }

    /**
     * Returns a copy that also sets the strikethrough.
     *
     * @param strikethrough true to strike through
     * @return the new patch
     */
    public TextStylePatch strikethrough(final boolean strikethrough) {
        return this.with(this.values.withStrikethrough(strikethrough), TextStyle.Property.STRIKETHROUGH);
    }

    /**
     * Returns a copy that also sets the shadow.
     *
     * @param shadow true to draw a shadow
     * @return the new patch
     */
    public TextStylePatch shadow(final boolean shadow) {
        return this.with(this.values.withShadow(shadow), TextStyle.Property.SHADOW);
    }

    /**
     * Returns a copy that also sets the monospaced font.
     *
     * @param monospace true for the monospaced font
     * @return the new patch
     */
    public TextStylePatch monospace(final boolean monospace) {
        return this.with(this.values.withMonospace(monospace), TextStyle.Property.MONOSPACE);
    }

    /**
     * Returns a copy that also sets the font family.
     *
     * @param font the family, or {@code null} for the platform's default font
     * @return the new patch
     */
    public TextStylePatch font(final FontId font) {
        return this.with(this.values.withFont(font), TextStyle.Property.FONT);
    }

    /**
     * Returns a copy that also sets the size.
     *
     * @param size the size in UI units; 0 for the platform's default size
     * @return the new patch
     * @throws IllegalArgumentException if the size is negative, infinite or NaN
     */
    public TextStylePatch size(final float size) {
        return this.with(this.values.withSize(size), TextStyle.Property.SIZE);
    }

    /**
     * Returns a patch that makes this patch's changes and then another's, so where both set a property the other's
     * value wins.
     *
     * @param after the patch applied second
     * @return the combined patch
     */
    public TextStylePatch then(final TextStylePatch after) {
        if (after.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return after;
        }
        final EnumSet<TextStyle.Property> all = EnumSet.noneOf(TextStyle.Property.class);
        all.addAll(this.properties);
        all.addAll(after.properties);
        return new TextStylePatch(this.values.merge(after.values, after.properties), all);
    }

    /**
     * Applies the patch over a style: the properties the patch sets take its values, the rest keep the style's.
     *
     * @param base the style to change
     * @return the changed style; equal to {@code base} for an empty patch
     */
    public TextStyle apply(final TextStyle base) {
        return this.properties.isEmpty() ? base : base.merge(this.values, this.properties);
    }

    private TextStylePatch with(final TextStyle values, final TextStyle.Property property) {
        final EnumSet<TextStyle.Property> all = EnumSet.of(property);
        all.addAll(this.properties);
        return new TextStylePatch(values, all);
    }
}
