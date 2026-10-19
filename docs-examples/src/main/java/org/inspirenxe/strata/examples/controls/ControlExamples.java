package org.inspirenxe.strata.examples.controls;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.List;

/**
 * The controls guide's examples for images, check boxes, sliders and rows of controls in a list.
 */
public final class ControlExamples {

    private ControlExamples() {
    }

    /**
     * Settings a game might keep.
     */
    public static final class Settings {
        private boolean music = true;
        private double volume = 80;
        private int distance = 12;

        public boolean music() {
            return this.music;
        }

        public void setMusic(final boolean music) {
            this.music = music;
        }

        public double volume() {
            return this.volume;
        }

        public void setVolume(final double volume) {
            this.volume = volume;
        }

        public int distance() {
            return this.distance;
        }

        public void setDistance(final int distance) {
            this.distance = distance;
        }
    }

    static Image image(final ComponentFactory factory) {
        // --8<-- [start:image]
        // assets/examplemod/sprites/icon/star.png, drawn at 16 by 16 units
        final Image star = factory.image(SpriteId.of("examplemod", "icon/star"));

        // The same sprite as a large picture that keeps its shape however it is stretched
        final Image banner = factory.image(SpriteId.of("examplemod", "icon/star"))
                .setImageSize(16, 16)
                .setScaling(Image.Scaling.FIT);
        banner.setAnchor(Anchor.FILL);

        // Tints multiply the image: half transparent here
        final Image faded = factory.image(SpriteId.of("examplemod", "icon/star")).setTint(0x80FFFFFF);
        // --8<-- [end:image]
        return star;
    }

    static CheckBox checkBox(final ComponentFactory factory, final Settings settings) {
        // --8<-- [start:checkbox]
        final CheckBox music = factory.checkBox("Music")
                .setSelected(settings.music())
                .onEvent(Toggle.TOGGLED, event -> settings.setMusic(event.value()));
        // --8<-- [end:checkbox]
        return music;
    }

    static Slider slider(final ComponentFactory factory, final Settings settings) {
        // --8<-- [start:slider]
        final Slider volume = factory.slider()
                .setRange(0, 100)
                .setStep(1)
                .setValue(settings.volume())
                .setLabel(value -> "Volume: " + (int) value + "%")
                .onEvent(Ranged.CHANGED, event -> settings.setVolume(event.value()));
        volume.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        // --8<-- [end:slider]
        return volume;
    }

    static Slider integerSlider(final ComponentFactory factory, final Settings settings) {
        // --8<-- [start:integers]
        // A step of 1 keeps the value whole; cast it when storing
        final Slider distance = factory.slider()
                .setRange(2, 32)
                .setStep(1)
                .setValue(settings.distance())
                .setLabel(value -> "Render distance: " + (int) value + " chunks")
                .onEvent(Ranged.CHANGED, event -> settings.setDistance(event.value().intValue()));
        // --8<-- [end:integers]
        return distance;
    }

    static ListView<String> optionRows(final ComponentFactory factory, final Settings settings) {
        // --8<-- [start:optionRows]
        final ListView<String> options = factory.<String>listView(name -> {
                    // Each control starts from the model and writes back to it
                    final Component<?> control = switch (name) {
                        case "Music" -> factory.toggleSwitch()
                                .setSelected(settings.music())
                                .onEvent(Toggle.TOGGLED, event -> settings.setMusic(event.value()));
                        case "Volume" -> factory.slider()
                                .setRange(0, 100)
                                .setStep(1)
                                .setValue(settings.volume())
                                .setLabel(value -> (int) value + "%")
                                .onEvent(Ranged.CHANGED, event -> settings.setVolume(event.value()))
                                .setWidth(100);
                        default -> factory.button("Reset");
                    };
                    return factory.stackPanel(Orientation.HORIZONTAL)
                            .add(factory.label(name).setAnchor(Anchor.MIDDLE_LEFT))
                            .add(factory.spacer(), 1)
                            .add(control.setAnchor(Anchor.MIDDLE_LEFT))
                            .setAnchor(Anchor.FILL);
                })
                .setRowHeight(24)
                .setItems(List.of("Music", "Volume", "Defaults"))
                // The rows hold controls, so there is nothing to select
                .onEvent(SingleSelection.SELECT, event -> event.cancel());

        // One handler around the list hears every row's button
        options.onEvent(Button.PRESSED, event -> {
            settings.setMusic(true);
            settings.setVolume(80);
            options.refresh();
        });
        // --8<-- [end:optionRows]
        return options;
    }

    /**
     * An options list whose rows hold a switch, a slider and a button.
     *
     * @return the screen
     */
    public static Screen optionRows() {
        final Settings settings = new Settings();
        return factory -> {
            final ListView<String> options = optionRows(factory, settings);
            options.setSize(240, 74);
            options.setAnchor(Anchor.CENTER);
            return factory.anchorPanel().add(options).setAnchor(Anchor.FILL);
        };
    }

    /**
     * A settings panel with every new control.
     *
     * @return the screen
     */
    public static Screen controls() {
        final Settings settings = new Settings();
        return factory -> {
            final StackPanel heading = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(4)
                    .add(factory.image(SpriteId.of("examplemod", "icon/star")).setImageSize(9, 9)
                            .setAnchor(Anchor.MIDDLE_LEFT))
                    .add(factory.label("Audio and video").setAnchor(Anchor.MIDDLE_LEFT));

            final Label status = factory.label("");
            final CheckBox music = checkBox(factory, settings);
            final CheckBox subtitles = factory.checkBox("Subtitles");
            final CheckBox locked = factory.checkBox("Locked option").setSelected(true).setEnabled(false);
            music.onEvent(Toggle.TOGGLED, event -> status.setText(event.value() ? "Music on" : "Music off"));

            final Slider distance = integerSlider(factory, settings);
            distance.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

            final StackPanel column = factory.stackPanel().setSpacing(6).setPadding(8)
                    .add(heading)
                    .add(music)
                    .add(subtitles)
                    .add(locked)
                    .add(slider(factory, settings))
                    .add(distance)
                    .add(status);
            column.setWidth(220);
            column.setBackground(Paint.solid(0xFF2B2D31));
            return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }
}
