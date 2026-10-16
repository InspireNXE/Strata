package org.inspirenxe.strata.api.text;

import java.util.Objects;

/**
 * A run of text in one style. Spans are single lines as far as engines are concerned: break text into lines (as
 * {@link org.inspirenxe.strata.api.widget.Label} does) before drawing it.
 *
 * @param text the characters; may be empty, must not be null
 * @param style how they look
 */
public record TextSpan(String text, TextStyle style) implements InlineElement {
    /**
     * Checks the arguments.
     *
     * @throws NullPointerException if {@code text} or {@code style} is null
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
