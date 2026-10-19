package org.inspirenxe.strata.core.theme;

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.TextStyle;
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

/**
 * One parsed theme file. The format is described on {@link ThemeResolver}.
 *
 * @param parent the theme the file builds on, if it names one
 * @param paints the paints it sets
 * @param text the text style changes it makes, applied over the inherited style of each key
 */
record ThemeFile(Optional<ThemeId> parent, Map<String, Paint> paints, Map<String, TextPatch> text) {
    private static final Set<String> TOP_KEYS = Set.of("parent", "paints", "text");
    private static final Set<String> SPRITE_KEYS = Set.of("sprite", "tint");
    private static final Set<String> NINE_SLICE_KEYS = Set.of("nine_slice", "border", "tint");
    private static final Set<String> GRADIENT_KEYS = Set.of("gradient");
    private static final Set<String> TEXT_KEYS =
            Set.of("color", "bold", "italic", "underline", "strikethrough", "shadow");

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
            final Map<String, TextPatch> text = new LinkedHashMap<>();
            if (root.containsKey("text")) {
                for (final Map.Entry<String, Object> entry : object(root.get("text"), "text").entrySet()) {
                    text.put(entry.getKey(), textPatch(entry.getValue(), "text." + entry.getKey()));
                }
            }
            return new ThemeFile(parent, paints, text);
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
            return new Paint.NineSlice(SpriteId.parse(string(map.get("nine_slice"), where + ".nine_slice")),
                    insets(map.get("border"), where + ".border"), tint(map, where));
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
        throw new IllegalArgumentException(where + " needs a color, \"none\", or one of sprite, nine_slice, gradient");
    }

    private static TextPatch textPatch(final Object value, final String where) {
        final Map<String, Object> map = object(value, where);
        checkKeys(map, TEXT_KEYS, where);
        return new TextPatch(
                map.containsKey("color") ? color(string(map.get("color"), where + ".color"), where + ".color") : null,
                flag(map, "bold", where),
                flag(map, "italic", where),
                flag(map, "underline", where),
                flag(map, "strikethrough", where),
                flag(map, "shadow", where));
    }

    private static int tint(final Map<String, Object> map, final String where) {
        return map.containsKey("tint") ? color(string(map.get("tint"), where + ".tint"), where + ".tint") : 0xFFFFFFFF;
    }

    private static Insets insets(final Object value, final String where) {
        if (value instanceof final Double all) {
            return Insets.of(all.floatValue());
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
     * Parses {@code #RRGGBB} (opaque) or {@code #AARRGGBB}.
     */
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
            return number.floatValue();
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

    /**
     * Text style fields a file sets. A null field keeps the inherited value.
     *
     * @param color the color, or null
     * @param bold bold, or null
     * @param italic italic, or null
     * @param underline underline, or null
     * @param strikethrough strikethrough, or null
     * @param shadow shadow, or null
     */
    record TextPatch(Integer color, Boolean bold, Boolean italic, Boolean underline, Boolean strikethrough,
                     Boolean shadow) {
        /**
         * Applies the set fields over a style.
         *
         * @param base the inherited style
         * @return the changed style
         */
        TextStyle apply(final TextStyle base) {
            return new TextStyle(
                    this.color != null ? this.color : base.color(),
                    this.bold != null ? this.bold : base.bold(),
                    this.italic != null ? this.italic : base.italic(),
                    this.underline != null ? this.underline : base.underline(),
                    this.strikethrough != null ? this.strikethrough : base.strikethrough(),
                    this.shadow != null ? this.shadow : base.shadow());
        }
    }
}
