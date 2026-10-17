package org.inspirenxe.strata.examples.widgets;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.widget.CheckBox;
import org.inspirenxe.strata.api.widget.Image;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.Slider;

/**
 * The widgets guide's examples for images, check boxes and sliders.
 */
public final class WidgetExamples {

    private WidgetExamples() {
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
                .setChecked(settings.music())
                .onCheckedChanged(settings::setMusic);
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
                .onValueChanged(settings::setVolume);
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
                .onValueChanged(value -> settings.setDistance((int) value));
        // --8<-- [end:integers]
        return distance;
    }

    /**
     * A settings panel with every new widget.
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
            final CheckBox locked = factory.checkBox("Locked option").setChecked(true).setEnabled(false);
            music.onCheckedChanged(on -> status.setText(on ? "Music on" : "Music off"));

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
