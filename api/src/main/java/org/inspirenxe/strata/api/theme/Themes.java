package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.Label;

/**
 * Built-in themes.
 */
public final class Themes {

    private Themes() {
    }

    /**
     * A flat theme made only of solid colors, so it works on any engine without sprites.
     */
    public static Theme flat() {
        return Theme.builder("flat")
                .paint(Button.STYLE_NORMAL, Paint.solid(0xFF3C3F44))
                .paint(Button.STYLE_HOVERED, Paint.solid(0xFF4E525A))
                .paint(Button.STYLE_PRESSED, Paint.solid(0xFF2B2D31))
                .paint(Button.STYLE_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(Button.STYLE_FOCUS, Paint.solid(0xFF6A9FFF))
                .textStyle(Button.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFF0F0F0))
                .textStyle(Button.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF808080))
                .textStyle(Label.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .build();
    }
}
