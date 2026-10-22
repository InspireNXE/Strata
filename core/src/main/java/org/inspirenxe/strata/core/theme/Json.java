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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader for theme files, so core needs no library. Objects become {@link LinkedHashMap}s, arrays
 * become {@link ArrayList}s, numbers become {@link Double}s, and {@code null} becomes {@link #NULL}.
 */
final class Json {
    /**
     * Stands in for JSON {@code null}, so maps never hold Java null.
     */
    static final Object NULL = new Object();

    private final String text;
    private int pos;

    private Json(final String text) {
        this.text = text;
    }

    /**
     * Parses one JSON value that fills the whole text.
     *
     * @param text the JSON
     * @return the value
     * @throws IllegalArgumentException if the text is not valid JSON, with the line and column
     */
    static Object parse(final String text) {
        final Json json = new Json(text);
        json.skipSpace();
        final Object value = json.value();
        json.skipSpace();
        if (json.pos != text.length()) {
            throw json.error("Unexpected text after the value");
        }
        return value;
    }

    private Object value() {
        if (this.pos >= this.text.length()) {
            throw this.error("Unexpected end of text");
        }
        final char c = this.text.charAt(this.pos);
        return switch (c) {
            case '{' -> this.object();
            case '[' -> this.array();
            case '"' -> this.string();
            case 't' -> this.literal("true", Boolean.TRUE);
            case 'f' -> this.literal("false", Boolean.FALSE);
            case 'n' -> this.literal("null", NULL);
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield this.number();
                }
                throw this.error("Unexpected character '" + c + "'");
            }
        };
    }

    private Map<String, Object> object() {
        final Map<String, Object> map = new LinkedHashMap<>();
        this.pos++;
        this.skipSpace();
        if (this.peek('}')) {
            this.pos++;
            return map;
        }
        while (true) {
            this.skipSpace();
            if (!this.peek('"')) {
                throw this.error("Expected a key in quotes");
            }
            final String key = this.string();
            this.skipSpace();
            this.expect(':');
            this.skipSpace();
            map.put(key, this.value());
            this.skipSpace();
            if (this.peek(',')) {
                this.pos++;
                continue;
            }
            this.expect('}');
            return map;
        }
    }

    private List<Object> array() {
        final List<Object> list = new ArrayList<>();
        this.pos++;
        this.skipSpace();
        if (this.peek(']')) {
            this.pos++;
            return list;
        }
        while (true) {
            this.skipSpace();
            list.add(this.value());
            this.skipSpace();
            if (this.peek(',')) {
                this.pos++;
                continue;
            }
            this.expect(']');
            return list;
        }
    }

    private String string() {
        final StringBuilder out = new StringBuilder();
        this.pos++;
        while (true) {
            if (this.pos >= this.text.length()) {
                throw this.error("Unclosed string");
            }
            final char c = this.text.charAt(this.pos++);
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                if (this.pos >= this.text.length()) {
                    throw this.error("Unclosed string");
                }
                final char escaped = this.text.charAt(this.pos++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (this.pos + 4 > this.text.length()) {
                            throw this.error("Short \\u escape");
                        }
                        int unit = 0;
                        for (int i = this.pos; i < this.pos + 4; i++) {
                            // Exactly four ASCII hex digits; Integer.parseInt would take a sign too
                            final char hex = this.text.charAt(i);
                            final int digit = hex >= '0' && hex <= '9' ? hex - '0'
                                    : hex >= 'a' && hex <= 'f' ? hex - 'a' + 10
                                    : hex >= 'A' && hex <= 'F' ? hex - 'A' + 10 : -1;
                            if (digit < 0) {
                                throw this.error("Bad \\u escape");
                            }
                            unit = unit * 16 + digit;
                        }
                        out.append((char) unit);
                        this.pos += 4;
                    }
                    default -> throw this.error("Unknown escape '\\" + escaped + "'");
                }
            } else if (c < 0x20) {
                throw this.error("Control character in string");
            } else {
                out.append(c);
            }
        }
    }

    private Double number() {
        final int start = this.pos;
        while (this.pos < this.text.length() && "+-0123456789.eE".indexOf(this.text.charAt(this.pos)) >= 0) {
            this.pos++;
        }
        try {
            return Double.valueOf(this.text.substring(start, this.pos));
        } catch (final NumberFormatException e) {
            this.pos = start;
            throw this.error("Bad number");
        }
    }

    private Object literal(final String word, final Object value) {
        if (!this.text.startsWith(word, this.pos)) {
            throw this.error("Unexpected text");
        }
        this.pos += word.length();
        return value;
    }

    private boolean peek(final char c) {
        return this.pos < this.text.length() && this.text.charAt(this.pos) == c;
    }

    private void expect(final char c) {
        if (!this.peek(c)) {
            throw this.error("Expected '" + c + "'");
        }
        this.pos++;
    }

    private void skipSpace() {
        while (this.pos < this.text.length()) {
            final char c = this.text.charAt(this.pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '﻿') {
                return;
            }
            this.pos++;
        }
    }

    private IllegalArgumentException error(final String message) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < Math.min(this.pos, this.text.length()); i++) {
            if (this.text.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new IllegalArgumentException(message + " at line " + line + ", column " + column);
    }
}
