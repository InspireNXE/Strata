package org.inspirenxe.strata.examples.theming;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.examples.quickstart.SettingsScreen;
import org.inspirenxe.strata.swing.SwingSnapshot;

/**
 * The theming guide's examples.
 */
public final class ThemeExamples {

    private ThemeExamples() {
    }

    /**
     * A variant of the flat theme with green buttons.
     *
     * @return the theme
     */
    public static Theme derived() {
        // --8<-- [start:derive]
        final Theme green = Themes.flat().toBuilder()
                .paint(Button.STYLE_NORMAL, Paint.solid(0xFF2E7D32))
                .paint(Button.STYLE_HOVERED, Paint.solid(0xFF388E3C))
                .paint(Button.STYLE_PRESSED, Paint.solid(0xFF1B5E20))
                .textStyle(Button.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFFFFFFF).withBold(true))
                .build();
        // --8<-- [end:derive]
        return green;
    }

    /**
     * A theme built from nothing, with every required key and a few optional ones.
     *
     * @return the theme
     */
    public static Theme fromScratch() {
        // --8<-- [start:scratch]
        final Theme light = Theme.builder("light")
                // Required by Button
                .paint(Button.STYLE_NORMAL, Paint.gradient(0xFFFFFFFF, 0xFFE0E0E0))
                .paint(Button.STYLE_HOVERED, Paint.gradient(0xFFFFFFFF, 0xFFD0E4FF))
                .paint(Button.STYLE_PRESSED, Paint.solid(0xFFC0D4F0))
                .paint(Button.STYLE_DISABLED, Paint.solid(0xFFEEEEEE))
                // Required by TextBox
                .paint(TextBox.STYLE_NORMAL, Paint.solid(0xFFFFFFFF))
                .paint(TextBox.STYLE_FOCUSED, Paint.solid(0xFFF4F8FF))
                .paint(TextBox.STYLE_DISABLED, Paint.solid(0xFFEEEEEE))
                // Optional keys
                .paint(Button.STYLE_FOCUS, Paint.solid(0xFF3B78E7))
                .textStyle(Button.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFF202020))
                .textStyle(Button.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF9E9E9E))
                .textStyle(Label.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFF202020))
                .textStyle(TextBox.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFF202020))
                .paint(TextBox.STYLE_CARET, Paint.solid(0xFF202020))
                .build();
        // --8<-- [end:scratch]
        return light;
    }

    static Theme sprites() {
        // --8<-- [start:sprites]
        // assets/examplemod/textures/gui/sprites/button.png, with a 4-pixel frame
        final SpriteId button = SpriteId.of("examplemod", "button");
        final SpriteId buttonHover = SpriteId.parse("examplemod:button_hover");
        final Theme textured = Themes.flat().toBuilder()
                .paint(Button.STYLE_NORMAL, Paint.nineSlice(button, Insets.of(4f)))
                .paint(Button.STYLE_HOVERED, Paint.nineSlice(buttonHover, Insets.of(4f)))
                .paint(Button.STYLE_PRESSED, new Paint.NineSlice(buttonHover, Insets.of(4f), 0xFFC0C0C0))
                .build();
        // --8<-- [end:sprites]
        return textured;
    }

    static void missing() {
        // --8<-- [start:missing]
        try {
            // The settings screen has buttons and a text box, whose required keys an empty theme lacks
            SwingSnapshot.render(new SettingsScreen(), Theme.builder("empty").build(), 320, 200, 1f);
        } catch (final ThemeException e) {
            // "Theme 'empty' is missing style keys: button.normal, textbox.focused, ..." with every missing key
            System.out.println(e.getMessage());
            final java.util.Set<String> keys = e.missingKeys();
        }
        // --8<-- [end:missing]
    }

    static void perComponent(final ComponentFactory factory) {
        // --8<-- [start:local]
        // Backgrounds and label styles can be set per component, outside the theme
        final StackPanel card = factory.stackPanel().setPadding(6);
        card.setBackground(Paint.gradient(0xFF3A3D43, 0xFF2B2D31));
        card.add(factory.label("Warning").setStyle(TextStyle.DEFAULT.withColor(0xFFFFB74D).withBold(true)));
        // --8<-- [end:local]
    }

    /**
     * The same controls, to render with several themes.
     *
     * @param background the panel color behind the controls
     * @return the screen
     */
    public static Screen sampler(final int background) {
        return factory -> {
            final TextBox field = factory.textBox().setText("Some text");
            final StackPanel column = factory.stackPanel().setSpacing(4).setPadding(8)
                    .add(factory.label("A label"))
                    .add(factory.button("A button"))
                    .add(factory.button("Disabled").setEnabled(false))
                    .add(field);
            column.setBackground(Paint.solid(background));
            return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }
}
