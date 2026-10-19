package org.inspirenxe.strata.sandbox;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.sandbox.almura.AlmuraMocks;

import java.util.function.Consumer;

/**
 * Exercises the layouts, buttons, focus, labels, a decorated built-in and a custom component, using the API only.
 */
public final class DemoScreen implements Screen {
    private final Consumer<Screen> open;
    private int clicks;

    /**
     * Creates the demo without the buttons that open the Almura mock-ups, for snapshots.
     */
    public DemoScreen() {
        this(null);
    }

    /**
     * Creates the demo.
     *
     * @param open pushes a screen over this one, or {@code null} to leave out the mock-up buttons
     */
    public DemoScreen(final Consumer<Screen> open) {
        this.open = open;
    }

    @Override
    public boolean hidesBackground() {
        return true;
    }

    @Override
    public Component<?> build(final ComponentFactory ui) {
        final Label counter = ui.label("Clicked 0 times");

        final Button increment = ui.button("Click me")
                .setAnchor(Anchor.FILL)
                .onEvent(Button.PRESSED, event -> {
                    this.clicks++;
                    counter.setText("Clicked " + this.clicks + " times");
                });
        final Button disabled = ui.button("Disabled")
                .setEnabled(false)
                .setAnchor(Anchor.FILL);
        final Button toggle = ui.button("Toggle disabled")
                .setAnchor(Anchor.FILL)
                .onEvent(Button.PRESSED, event -> disabled.setEnabled(!disabled.isEnabled()));

        // A built-in button with a corner marker drawn over it after it paints
        final Button marked = ui.button("Decorated")
                .setAnchor(Anchor.FILL)
                .onEvent(Component.PAINTED, event -> {
                    final Rect bounds = event.bounds();
                    event.painter().fill(new Rect(bounds.right() - 6f, bounds.y() + 2f, 4f, 4f), 0xFFE8A33D);
                });

        final GridPanel buttons = ui.gridPanel()
                .columns(GridLength.weighted(1), GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto())
                .setSpacing(4, 4)
                .add(increment, 0, 0)
                .add(toggle, 0, 1)
                .add(disabled, 1, 0)
                .add(marked, 1, 1)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final TextStyle titleStyle = TextStyle.DEFAULT
                .withColor(0xFFFFFFFF)
                .withBold(true)
                .withShadow(true);
        final Label title = ui.label("Strata sandbox")
                .setStyle(titleStyle);
        final String about = "This screen is written against the Strata API only and runs on the Java2D "
                + "adapter. Tab and Shift+Tab move focus between buttons; Enter or Space presses the focused one; "
                + "the text boxes take typing, selection and Ctrl+C/X/V. "
                + "The swatches below are a custom component.";
        final Label body = ui.label(about)
                .setWrap(true)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final Swatches swatches = new Swatches(ui, 0xFFE06C75, 0xFF98C379, 0xFFE5C07B, 0xFF61AFEF, 0xFFC678DD);

        final Label greeting = ui.label("Hello, Strata");
        final TextBox name = ui.textBox()
                .setText("Strata")
                .setMaxLength(24)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP)
                .onEvent(TextInput.EDITED,
                        event -> greeting.setText(event.value().isEmpty() ? "Hello?" : "Hello, " + event.value()));
        final TextBox secret = ui.textBox()
                .setPassword(true)
                .setText("hunter2")
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final Label nameLabel = ui.label("Name")
                .setAnchor(Anchor.MIDDLE_LEFT);
        final Label passwordLabel = ui.label("Password")
                .setAnchor(Anchor.MIDDLE_LEFT);
        final GridPanel fields = ui.gridPanel()
                .columns(GridLength.auto(), GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto())
                .setSpacing(6, 4)
                .add(nameLabel, 0, 0)
                .add(name, 0, 1)
                .add(passwordLabel, 1, 0)
                .add(secret, 1, 1)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final StackPanel column = ui.stackPanel()
                .setSpacing(6)
                .setPadding(10)
                .add(title)
                .add(body)
                .add(fields)
                .add(greeting)
                .add(buttons)
                .add(swatches)
                .add(counter)
                .setBackground(Paint.solid(0xFF2B2D31))
                .setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
        if (this.open != null) {
            final Label mockups = ui.label("Almura mock-ups");
            final Component<?> mockupButtons = AlmuraMocks.buttons(ui, this.open);
            final Button controls = ui.button("Standard controls")
                    .onEvent(Button.PRESSED, event -> this.open.accept(new ControlsScreen()));
            column.add(mockups)
                    .add(mockupButtons)
                    .add(controls)
                    .setWidth(400);
        } else {
            column.setWidth(320);
        }

        return ui.anchorPanel()
                .add(column)
                .setAnchor(Anchor.FILL)
                .setBackground(Paint.gradient(0xFF1E1F22, 0xFF111214));
    }
}
