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
package org.inspirenxe.strata.core.input;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Names key strokes for shortcut hints, as {@code ComponentFactory.keyText} describes.
 *
 * <p>Modifiers come first, in the order Control, Alt, Shift, Super, each the translation of
 * {@code strata.modifier.<name>}, joined by the translation of {@code strata.modifier.separator}. Letters, digits,
 * F1 to F12 and punctuation keys are named by what they show; keypad keys by {@code strata.key.keypad} with the
 * key's own name; every other key by the translation of {@code strata.key.<name>}, where the name is the
 * {@link Key} constant in lower case, as {@code strata.key.page_up}. Core's English has all of them.</p>
 */
public final class KeyNames {
    private KeyNames() {
    }

    /**
     * Returns how a stroke reads.
     *
     * @param stroke the stroke
     * @param shortcutModifier the platform's shortcut modifier, which a shortcut stroke holds
     * @return the text, translated as it is resolved
     */
    public static Text text(final KeyStroke stroke, final Modifier shortcutModifier) {
        final Modifiers held = stroke.resolve(shortcutModifier);
        final List<Text> parts = new ArrayList<>();
        final Text separator = Text.translatable("strata.modifier.separator");
        for (final Modifier modifier : new Modifier[] {Modifier.CONTROL, Modifier.ALT, Modifier.SHIFT, Modifier.SUPER}) {
            if (held.has(modifier)) {
                parts.add(Text.translatable("strata.modifier." + modifier.name().toLowerCase(Locale.ROOT)));
                parts.add(separator);
            }
        }
        parts.add(KeyNames.key(stroke.key()));
        return Text.concat(parts);
    }

    /**
     * Returns the name of a key alone.
     *
     * @param key the key
     * @return the name, translated as it is resolved where it is not a symbol
     */
    public static Text key(final Key key) {
        final String literal = KeyNames.literal(key);
        if (literal != null) {
            return Text.of(literal);
        }
        if (key.compareTo(Key.KP_0) >= 0 && key.compareTo(Key.KP_EQUALS) <= 0) {
            final String own = KeyNames.literal(KeyNames.keypadMain(key));
            return Text.translatable("strata.key.keypad", own != null ? Text.of(own)
                    : Text.translatable("strata.key." + key.name().substring(3).toLowerCase(Locale.ROOT)));
        }
        return Text.translatable("strata.key." + key.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the key a keypad key stands for, for its name.
     */
    private static Key keypadMain(final Key key) {
        return switch (key) {
            case KP_0 -> Key.NUM_0;
            case KP_1 -> Key.NUM_1;
            case KP_2 -> Key.NUM_2;
            case KP_3 -> Key.NUM_3;
            case KP_4 -> Key.NUM_4;
            case KP_5 -> Key.NUM_5;
            case KP_6 -> Key.NUM_6;
            case KP_7 -> Key.NUM_7;
            case KP_8 -> Key.NUM_8;
            case KP_9 -> Key.NUM_9;
            case KP_DECIMAL -> Key.PERIOD;
            case KP_DIVIDE -> Key.SLASH;
            case KP_SUBTRACT -> Key.MINUS;
            case KP_EQUALS -> Key.EQUALS;
            default -> key;
        };
    }

    /**
     * Returns what a key shows on a US keyboard, or null for a key named by a translation.
     */
    private static String literal(final Key key) {
        if (key.compareTo(Key.A) >= 0 && key.compareTo(Key.Z) <= 0) {
            return key.name();
        }
        if (key.compareTo(Key.NUM_0) >= 0 && key.compareTo(Key.NUM_9) <= 0) {
            return key.name().substring(4);
        }
        if (key.compareTo(Key.F1) >= 0 && key.compareTo(Key.F12) <= 0) {
            return key.name();
        }
        return switch (key) {
            case APOSTROPHE -> "'";
            case COMMA -> ",";
            case MINUS -> "-";
            case PERIOD -> ".";
            case SLASH -> "/";
            case SEMICOLON -> ";";
            case EQUALS -> "=";
            case LEFT_BRACKET -> "[";
            case BACKSLASH -> "\\";
            case RIGHT_BRACKET -> "]";
            case GRAVE -> "`";
            case KP_MULTIPLY -> "*";
            case KP_ADD -> "+";
            default -> null;
        };
    }
}
