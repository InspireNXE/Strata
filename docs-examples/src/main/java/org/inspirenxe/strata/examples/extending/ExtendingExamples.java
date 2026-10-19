package org.inspirenxe.strata.examples.extending;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.ValueRequestEvent;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;

/**
 * The extending guide's examples.
 */
public final class ExtendingExamples {

    private ExtendingExamples() {
    }

    static Button badge(final ComponentFactory factory) {
        // --8<-- [start:paint]
        // The normal button, plus a dot in the corner while there is something new
        final boolean[] unread = {true};
        final Button inbox = factory.button("Inbox")
                .onEvent(Component.PAINTED, event -> {
                    if (unread[0]) {
                        final Rect bounds = event.bounds();
                        event.painter().fill(new Rect(bounds.right() - 6f, bounds.y() + 2f, 4f, 4f), 0xFFE8A33D);
                    }
                })
                .onEvent(Button.PRESSED, event -> unread[0] = false);
        // --8<-- [end:paint]
        return inbox;
    }

    static TextBox digitsOnly(final ComponentFactory factory) {
        // --8<-- [start:filter]
        // Typing, pasting and deleting all ask first, with the text the edit would produce
        final TextBox amount = factory.textBox().onEvent(TextBox.EDIT, event -> {
            if (!event.value().chars().allMatch(Character::isDigit)) {
                event.cancel();
            }
        });
        // --8<-- [end:filter]
        return amount;
    }

    static Slider cappedVolume(final ComponentFactory factory) {
        // --8<-- [start:rewrite]
        // The user cannot drag past the cap, but code can still set any value
        final double[] cap = {0.8};
        final Slider volume = factory.slider()
                .onEvent(Slider.CHANGE, event -> event.setValue(Math.min(event.value(), cap[0])));
        // --8<-- [end:rewrite]
        return volume;
    }

    static Window editor(final ComponentFactory factory, final boolean[] unsaved) {
        // --8<-- [start:veto]
        // Ask before closing a window with unsaved changes
        final Window window = factory.window("Notes");
        window.onEvent(Window.CLOSE, event -> {
            if (!unsaved[0]) {
                return;
            }
            event.cancel();
            factory.overlays().dialog(factory.messageBox("Notes", "Discard your changes?", MessageBox.Buttons.YES_NO)
                    .onEvent(MessageBox.ANSWERED, answer -> {
                        if (answer.value() == MessageBox.Result.YES) {
                            window.close();
                        }
                    }));
        });
        return window;
        // --8<-- [end:veto]
    }

    static Label minimumWidth(final ComponentFactory factory) {
        // --8<-- [start:measure]
        // A label that is never narrower than 80 units, whatever its text
        final Label score = factory.label("0")
                .onEvent(Component.MEASURED, event -> event.setWidth(Math.max(80f, event.width())));
        // --8<-- [end:measure]
        return score;
    }

    // --8<-- [start:reuse]
    /** Lets through only text made of digits. Shared by every amount field. */
    static final EventHandler<ValueRequestEvent<TextBox, String>> DIGITS_ONLY = event -> {
        if (!event.value().chars().allMatch(Character::isDigit)) {
            event.cancel();
        }
    };

    static TextBox amountField(final ComponentFactory factory) {
        return factory.textBox().setMaxLength(6).onEvent(TextBox.EDIT, DIGITS_ONLY);
    }
    // --8<-- [end:reuse]

    // --8<-- [start:compose]
    /**
     * A caption above a text box. Composition needs no Strata types of its own: build the parts in a container and
     * hand out the container.
     */
    public static final class LabeledField {
        private final StackPanel root;
        private final TextBox field;

        public LabeledField(final ComponentFactory factory, final String caption) {
            this.field = factory.textBox();
            this.field.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            this.root = factory.stackPanel().setSpacing(2).add(factory.label(caption)).add(this.field);
        }

        /** The component to add to a container. */
        public Component<?> component() {
            return this.root;
        }

        public String text() {
            return this.field.text();
        }
    }
    // --8<-- [end:compose]

    /**
     * The switch and wrap panel in use.
     *
     * @return the screen
     */
    public static Screen customComponents() {
        return factory -> {
            // --8<-- [start:use]
            final ToggleSwitch music = new ToggleSwitchImpl(factory, "Music").setOn(true);
            final ToggleSwitch sounds = new ToggleSwitchImpl(factory, "Sound effects");
            final Label summary = factory.label("Music on");
            music.onEvent(ToggleSwitch.TOGGLED, event -> summary.setText(event.value() ? "Music on" : "Music off"));

            final WrapPanel tags = new WrapPanel(factory, 4);
            for (final String tag : new String[] {"survival", "creative", "hardcore", "peaceful", "adventure"}) {
                tags.add(factory.button(tag));
            }
            tags.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

            final StackPanel column = factory.stackPanel().setSpacing(6).setPadding(8)
                    .add(music).add(sounds).add(summary).add(tags);
            // --8<-- [end:use]
            column.setWidth(200);
            column.setBackground(Paint.solid(0xFF2B2D31));
            final LabeledField seed = new LabeledField(factory, "World seed");
            column.add(seed.component());
            return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }

    /**
     * A theme with the switch's keys added.
     *
     * @return the theme
     */
    public static Theme checkBoxTheme() {
        // --8<-- [start:theme]
        // The host refuses a tree whose theme lacks a required key, so add your component's keys to the theme
        return Themes.flat().toBuilder()
                .paint(ToggleSwitch.STYLE_TRACK, Paint.solid(0xFF1B1C1F))
                .paint(ToggleSwitch.STYLE_TRACK_ON, Paint.solid(0xFF2E7D32))
                .paint(ToggleSwitch.STYLE_KNOB, Paint.solid(0xFFE0E0E0))
                .build();
        // --8<-- [end:theme]
    }
}
