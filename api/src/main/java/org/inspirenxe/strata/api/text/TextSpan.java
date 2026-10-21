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

import java.util.Objects;

/**
 * A run of text in one style. Spans are single lines as far as engines are concerned: break text into lines (as
 * {@link org.inspirenxe.strata.api.control.display.Label} does) before drawing it.
 *
 * <p>A span can hold an {@link InlineElement} instead of characters. Its text is then the one character
 * {@link #OBJECT_REPLACEMENT}, so splitting, trimming and wrapping treat it as one character that is not whitespace,
 * and its style is that of the text around it. The text metrics and painter that Strata hands to components measure
 * and draw the element; an engine's own metrics and canvas never see one.</p>
 *
 * @param text the characters; may be empty, and is {@link #OBJECT_REPLACEMENT} when there is an element
 * @param style how they look
 * @param element what is drawn in place of the characters, or null for plain text
 */
public record TextSpan(String text, TextStyle style, InlineElement element) {
    /**
     * The text of a span that holds an element: U+FFFC, the Unicode object replacement character.
     */
    public static final String OBJECT_REPLACEMENT = "\uFFFC";

    /**
     * Checks the arguments.
     *
     * @throws IllegalArgumentException if there is an element and the text is not {@link #OBJECT_REPLACEMENT}
     */
    public TextSpan {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
        if (element != null && !OBJECT_REPLACEMENT.equals(text)) {
            throw new IllegalArgumentException("A span with an element must hold U+FFFC as its text");
        }
    }

    /**
     * Creates a span of characters.
     *
     * @param text the characters; may be empty
     * @param style how they look
     */
    public TextSpan(final String text, final TextStyle style) {
        this(text, style, null);
    }

    /**
     * Creates a span in {@link TextStyle#DEFAULT}.
     *
     * @param text the characters
     * @return the span
     */
    public static TextSpan of(final String text) {
        return new TextSpan(text, TextStyle.DEFAULT);
    }

    /**
     * Creates a span that holds an element.
     *
     * @param element what to draw
     * @param style the style of the text around it, which sizes a sprite that {@linkplain InlineSprite#fitsLine()
     *              fits the line}
     * @return the span
     */
    public static TextSpan of(final InlineElement element, final TextStyle style) {
        return new TextSpan(OBJECT_REPLACEMENT, style, Objects.requireNonNull(element, "element"));
    }

    /**
     * Returns whether the span holds an element.
     *
     * @return true when {@link #element()} is not null
     */
    public boolean isInline() {
        return this.element != null;
    }

    /**
     * Returns a copy with other characters and the same style. The copy keeps the element only when the new text is
     * still {@link #OBJECT_REPLACEMENT}.
     *
     * @param text the new characters
     * @return the new span
     */
    public TextSpan withText(final String text) {
        return new TextSpan(text, this.style, OBJECT_REPLACEMENT.equals(text) ? this.element : null);
    }

    /**
     * Returns a copy with the same characters and element in another style.
     *
     * @param style the new style
     * @return the new span
     */
    public TextSpan withStyle(final TextStyle style) {
        return new TextSpan(this.text, style, this.element);
    }
}
