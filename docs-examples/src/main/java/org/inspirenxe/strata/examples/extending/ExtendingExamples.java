package org.inspirenxe.strata.examples.extending;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.CharEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.MeasureNext;
import org.inspirenxe.strata.api.extension.Next;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ButtonBehavior;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.LabelBehavior;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.api.widget.TextBoxBehavior;

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
        final Button inbox = factory.button("Inbox", new ButtonBehavior() {
            @Override
            public void paint(final Button button, final Painter painter, final Rect bounds, final Next next) {
                next.proceed();
                if (unread[0]) {
                    painter.fill(new Rect(bounds.right() - 6f, bounds.y() + 2f, 4f, 4f), 0xFFE8A33D);
                }
            }
        });
        inbox.onAction(() -> unread[0] = false);
        // --8<-- [end:paint]
        return inbox;
    }

    static TextBox digitsOnly(final ComponentFactory factory) {
        // --8<-- [start:filter]
        final TextBox amount = factory.textBox(new TextBoxBehavior() {
            @Override
            public void onChar(final TextBox box, final CharEvent event, final Next next) {
                if (Character.isDigit(event.codePoint())) {
                    // Run the text box's own handling, which inserts the character
                    next.proceed();
                } else {
                    // Skip it, and keep the character from reaching anything else
                    event.consume();
                }
            }
        });
        // --8<-- [end:filter]
        return amount;
    }

    static Label minimumWidth(final ComponentFactory factory) {
        // --8<-- [start:measure]
        // A label that is never narrower than 80 units, whatever its text
        final Label score = factory.label("0", new LabelBehavior() {
            @Override
            public Size measure(final Label label, final MeasureContext context, final float availableWidth,
                                final float availableHeight, final MeasureNext next) {
                final Size natural = next.proceed();
                return new Size(Math.max(80f, natural.width()), natural.height());
            }
        });
        // --8<-- [end:measure]
        return score;
    }

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
     * The check box and wrap panel in use.
     *
     * @return the screen
     */
    public static Screen customComponents() {
        return factory -> {
            // --8<-- [start:use]
            final CheckBox music = new CheckBoxImpl(factory, "Music").setChecked(true);
            final CheckBox sounds = new CheckBoxImpl(factory, "Sound effects");
            final Label summary = factory.label("Music on");
            music.onChange(on -> summary.setText(on ? "Music on" : "Music off"));

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
     * A theme with the check box's keys added.
     *
     * @return the theme
     */
    public static Theme checkBoxTheme() {
        // --8<-- [start:theme]
        // The host refuses a tree whose theme lacks a required key, so add your component's keys to the theme
        return Themes.flat().toBuilder()
                .paint(CheckBox.STYLE_BOX, Paint.solid(0xFF1B1C1F))
                .paint(CheckBox.STYLE_CHECK, Paint.solid(0xFF6A9FFF))
                .build();
        // --8<-- [end:theme]
    }
}
