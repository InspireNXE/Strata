package org.inspirenxe.strata.api.text;

import java.util.Objects;

public record TextSpan(String text, TextStyle style) implements InlineElement {
    public TextSpan {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
    }

    public static TextSpan of(final String text) {
        return new TextSpan(text, TextStyle.DEFAULT);
    }

    public TextSpan withText(final String text) {
        return new TextSpan(text, this.style);
    }

    public TextSpan withStyle(final TextStyle style) {
        return new TextSpan(this.text, style);
    }
}
