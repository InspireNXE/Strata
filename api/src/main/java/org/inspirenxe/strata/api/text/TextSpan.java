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
 * @param text the characters; may be empty, must not be null
 * @param style how they look
 */
public record TextSpan(String text, TextStyle style) implements InlineElement {
    /**
     * Checks the arguments.
     */
    public TextSpan {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
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
     * Returns a copy with other characters and the same style.
     *
     * @param text the new characters
     * @return the new span
     */
    public TextSpan withText(final String text) {
        return new TextSpan(text, this.style);
    }

    /**
     * Returns a copy with the same characters in another style.
     *
     * @param style the new style
     * @return the new span
     */
    public TextSpan withStyle(final TextStyle style) {
        return new TextSpan(this.text, style);
    }
}
