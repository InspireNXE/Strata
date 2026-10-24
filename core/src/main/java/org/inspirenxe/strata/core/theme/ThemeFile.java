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
package org.inspirenxe.strata.core.theme;

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.FontId;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.text.TextStylePatch;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.spi.ThemeDocument;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One parsed theme file. The format is described on {@link ThemeResolver}.
 *
 * @param layer the layer the file makes, from its document
 * @param parent the theme the file builds on, if it names one
 * @param paints the paints it sets
 * @param text the text style changes it makes, applied over the inherited style of each key
 * @param insets the insets it sets
 * @param metrics the metrics it sets
 */
record ThemeFile(String layer, Optional<ThemeId> parent, Map<String, Paint> paints,
                 Map<String, TextStylePatch> text, Map<String, Insets> insets, Map<String, Float> metrics) {
    private static final Set<String> TOP_KEYS = Set.of("parent", "paints", "text", "insets", "metrics");
    private static final Set<String> SPRITE_KEYS = Set.of("sprite", "tint");
    private static final Set<String> NINE_SLICE_KEYS = Set.of("nine_slice", "border", "tint", "size", "tiled");
    private static final Set<String> TILED_KEYS = Set.of("tiled", "size", "tint");
    private static final Set<String> GRADIENT_KEYS = Set.of("gradient");
    private static final Set<String> COLOR_KEYS = Set.of("color", "radius");
    private static final Pattern DURATION = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)(ms|s)");
    private static final Set<String> TEXT_KEYS =
            Set.of("color", "bold", "italic", "underline", "strikethrough", "shadow", "monospace", "font", "size");

    /**
     * Parses a document.
     *
     * @param document the document
     * @return the file
     * @throws ThemeException if the document is not valid JSON or not a valid theme file, naming its origin
     */
    static ThemeFile parse(final ThemeDocument document) {
        try {
            final Map<String, Object> root = object(Json.parse(document.content()), "the file");
            checkKeys(root, TOP_KEYS, "the file");
            final Optional<ThemeId> parent = root.containsKey("parent")
                    ? Optional.of(ThemeId.parse(string(root.get("parent"), "parent")))
                    : Optional.empty();
            final Map<String, Paint> paints = new LinkedHashMap<>();
            if (root.containsKey("paints")) {
                for (final Map.Entry<String, Object> entry : object(root.get("paints"), "paints").entrySet()) {
                    paints.put(entry.getKey(), paint(entry.getValue(), "paints." + entry.getKey()));
                }
            }
            final Map<String, TextStylePatch> text = new LinkedHashMap<>();
            if (root.containsKey("text")) {
                for (final Map.Entry<String, Object> entry : object(root.get("text"), "text").entrySet()) {
                    text.put(entry.getKey(), textPatch(entry.getValue(), "text." + entry.getKey()));
                }
            }
            final Map<String, Insets> insets = new LinkedHashMap<>();
            if (root.containsKey("insets")) {
                for (final Map.Entry<String, Object> entry : object(root.get("insets"), "insets").entrySet()) {
                    insets.put(entry.getKey(), insets(entry.getValue(), "insets." + entry.getKey()));
                }
            }
            final Map<String, Float> metrics = new LinkedHashMap<>();
            if (root.containsKey("metrics")) {
                for (final Map.Entry<String, Object> entry : object(root.get("metrics"), "metrics").entrySet()) {
                    metrics.put(entry.getKey(), metric(entry.getValue(), "metrics." + entry.getKey()));
                }
            }
            return new ThemeFile(document.layer(), parent, paints, text, insets, metrics);
        } catch (final IllegalArgumentException e) {
            throw new ThemeException("Theme file " + document.origin() + " is invalid: " + e.getMessage());
        }
    }

    private static Paint paint(final Object value, final String where) {
        if (value instanceof final String text) {
            return "none".equals(text) ? Paint.NONE : Paint.solid(color(text, where));
        }
        final Map<String, Object> map = object(value, where);
        if (map.containsKey("sprite")) {
            checkKeys(map, SPRITE_KEYS, where);
            return new Paint.Sprite(SpriteId.parse(string(map.get("sprite"), where + ".sprite")), tint(map, where));
        }
        if (map.containsKey("nine_slice")) {
            checkKeys(map, NINE_SLICE_KEYS, where);
            if (!map.containsKey("border")) {
                throw new IllegalArgumentException(where + " needs a border");
            }
            final boolean tiled = Boolean.TRUE.equals(flag(map, "tiled", where));
            final Size size = map.containsKey("size") ? size(map.get("size"), where + ".size") : null;
            if (tiled && size == null) {
                throw new IllegalArgumentException(where + " is tiled and needs a size");
            }
            return new Paint.NineSlice(SpriteId.parse(string(map.get("nine_slice"), where + ".nine_slice")),
                    insets(map.get("border"), where + ".border"), tint(map, where), size, tiled);
        }
        if (map.containsKey("tiled")) {
            checkKeys(map, TILED_KEYS, where);
            if (!map.containsKey("size")) {
                throw new IllegalArgumentException(where + " needs a size");
            }
            return new Paint.Tiled(SpriteId.parse(string(map.get("tiled"), where + ".tiled")),
                    size(map.get("size"), where + ".size"), tint(map, where));
        }
        if (map.containsKey("color")) {
            checkKeys(map, COLOR_KEYS, where);
            final int argb = color(string(map.get("color"), where + ".color"), where + ".color");
            if (!map.containsKey("radius")) {
                return Paint.solid(argb);
            }
            final float radius = number(map.get("radius"), where + ".radius");
            if (radius < 0f) {
                throw new IllegalArgumentException(where + ".radius must not be negative");
            }
            return Paint.rounded(argb, radius);
        }
        if (map.containsKey("gradient")) {
            checkKeys(map, GRADIENT_KEYS, where);
            final List<?> colors = list(map.get("gradient"), where + ".gradient");
            if (colors.size() != 2) {
                throw new IllegalArgumentException(where + ".gradient needs two colors, top and bottom");
            }
            return Paint.gradient(color(string(colors.get(0), where + ".gradient"), where + ".gradient"),
                    color(string(colors.get(1), where + ".gradient"), where + ".gradient"));
        }
        throw new IllegalArgumentException(where
                + " needs a color, \"none\", or one of color, sprite, tiled, nine_slice, gradient");
    }

    private static TextStylePatch textPatch(final Object value, final String where) {
        final Map<String, Object> map = object(value, where);
        checkKeys(map, TEXT_KEYS, where);
        TextStylePatch patch = TextStylePatch.of();
        if (map.containsKey("color")) {
            patch = patch.color(color(string(map.get("color"), where + ".color"), where + ".color"));
        }
        final Boolean bold = flag(map, "bold", where);
        if (bold != null) {
            patch = patch.bold(bold);
        }
        final Boolean italic = flag(map, "italic", where);
        if (italic != null) {
            patch = patch.italic(italic);
        }
        final Boolean underline = flag(map, "underline", where);
        if (underline != null) {
            patch = patch.underline(underline);
        }
        final Boolean strikethrough = flag(map, "strikethrough", where);
        if (strikethrough != null) {
            patch = patch.strikethrough(strikethrough);
        }
        final Boolean shadow = flag(map, "shadow", where);
        if (shadow != null) {
            patch = patch.shadow(shadow);
        }
        final Boolean monospace = flag(map, "monospace", where);
        if (monospace != null) {
            patch = patch.monospace(monospace);
        }
        if (map.containsKey("font")) {
            // null names the platform's default font, so a child key can undo its parent's
            final Object font = map.get("font");
            try {
                patch = patch.font(font == Json.NULL ? null : FontId.parse(string(font, where + ".font")));
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(where + ".font: " + e.getMessage(), e);
            }
        }
        if (map.containsKey("size")) {
            final float size = number(map.get("size"), where + ".size");
            if (size < 0f) {
                throw new IllegalArgumentException(where + ".size must not be negative: " + size);
            }
            patch = patch.size(size);
        }
        return patch;
    }

    private static int tint(final Map<String, Object> map, final String where) {
        return map.containsKey("tint") ? color(string(map.get("tint"), where + ".tint"), where + ".tint") : 0xFFFFFFFF;
    }

    private static Insets insets(final Object value, final String where) {
        if (value instanceof Double) {
            return Insets.of(number(value, where));
        }
        final List<?> parts = list(value, where);
        return switch (parts.size()) {
            case 2 -> Insets.of(number(parts.get(0), where), number(parts.get(1), where));
            case 4 -> new Insets(number(parts.get(0), where), number(parts.get(1), where),
                    number(parts.get(2), where), number(parts.get(3), where));
            default -> throw new IllegalArgumentException(
                    where + " needs one number, [horizontal, vertical] or [left, top, right, bottom]");
        };
    }

    /**
     * Parses a metric: a number, {@code true} or {@code false} for a flag, stored as 1 or 0, or a duration with a
     * unit, {@code "500ms"} or {@code "0.5s"}, stored in milliseconds.
     */
    private static float metric(final Object value, final String where) {
        if (value instanceof final Double number && Float.isFinite(number.floatValue())) {
            return number.floatValue();
        }
        if (value instanceof final Boolean flag) {
            return flag ? 1f : 0f;
        }
        if (value instanceof final String text) {
            final Matcher matcher = DURATION.matcher(text);
            if (matcher.matches()) {
                final double amount = Double.parseDouble(matcher.group(1)) * ("s".equals(matcher.group(2)) ? 1000.0 : 1.0);
                if (Float.isFinite((float) amount)) {
                    return (float) amount;
                }
            }
        }
        throw new IllegalArgumentException(where + " must be a number, true or false, or a duration such as \"500ms\""
                + " or \"0.5s\"");
    }

    /**
     * Parses a size written as {@code [width, height]}.
     */
    private static Size size(final Object value, final String where) {
        final List<?> parts = list(value, where);
        if (parts.size() != 2) {
            throw new IllegalArgumentException(where + " needs [width, height]");
        }
        return new Size(number(parts.get(0), where), number(parts.get(1), where));
    }

    private static int color(final String text, final String where) {
        if (text.startsWith("#") && (text.length() == 7 || text.length() == 9)) {
            try {
                final int rgb = Integer.parseUnsignedInt(text.substring(1), 16);
                return text.length() == 7 ? 0xFF000000 | rgb : rgb;
            } catch (final NumberFormatException ignored) {
                // Falls through to the error below
            }
        }
        throw new IllegalArgumentException(where + " is not a color (#RRGGBB or #AARRGGBB): " + text);
    }

    private static Boolean flag(final Map<String, Object> map, final String key, final String where) {
        if (!map.containsKey(key)) {
            return null;
        }
        if (map.get(key) instanceof final Boolean value) {
            return value;
        }
        throw new IllegalArgumentException(where + "." + key + " must be true or false");
    }

    private static float number(final Object value, final String where) {
        if (value instanceof final Double number) {
            final float result = number.floatValue();
            if (!Float.isFinite(result)) {
                throw new IllegalArgumentException(where + " must hold finite numbers, not " + number);
            }
            return result;
        }
        throw new IllegalArgumentException(where + " must hold numbers");
    }

    private static String string(final Object value, final String where) {
        if (value instanceof final String text) {
            return text;
        }
        throw new IllegalArgumentException(where + " must be a string");
    }

    private static List<?> list(final Object value, final String where) {
        if (value instanceof final List<?> list) {
            return list;
        }
        throw new IllegalArgumentException(where + " must be an array");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(final Object value, final String where) {
        if (value instanceof final Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException(where + " must be an object");
    }

    private static void checkKeys(final Map<String, Object> map, final Set<String> allowed, final String where) {
        for (final String key : map.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("Unknown key '" + key + "' in " + where);
            }
        }
    }
}
