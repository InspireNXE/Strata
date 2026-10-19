package org.inspirenxe.strata.sandbox;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.extension.Next;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ButtonBehavior;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.TextBox;
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

        final Button increment = ui.button("Click me").onAction(() -> {
            this.clicks++;
            counter.setText("Clicked " + this.clicks + " times");
        });
        final Button disabled = ui.button("Disabled").setEnabled(false);
        final Button toggle = ui.button("Toggle disabled");
        toggle.onAction(() -> disabled.setEnabled(!disabled.isEnabled()));

        // A built-in button decorated with a behavior: the normal look plus a corner marker
        final Button marked = ui.button("Decorated", new ButtonBehavior() {
            @Override
            public void paint(final Button button, final Painter painter, final Rect bounds, final Next next) {
                next.proceed();
                painter.fill(new Rect(bounds.right() - 6f, bounds.y() + 2f, 4f, 4f), 0xFFE8A33D);
            }
        });

        final GridPanel buttons = ui.gridPanel()
                .columns(GridLength.weighted(1), GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto())
                .setSpacing(4, 4);
        buttons.add(increment.setAnchor(Anchor.FILL), 0, 0)
                .add(toggle.setAnchor(Anchor.FILL), 0, 1)
                .add(disabled.setAnchor(Anchor.FILL), 1, 0)
                .add(marked.setAnchor(Anchor.FILL), 1, 1);
        buttons.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final Label title = ui.label("Strata sandbox")
                .setStyle(TextStyle.DEFAULT.withColor(0xFFFFFFFF).withBold(true).withShadow(true));
        final Label body = ui.label("This screen is written against the Strata API only and runs on the Java2D "
                + "adapter. Tab and Shift+Tab move focus between buttons; Enter or Space presses the focused one; the text boxes take typing, selection and Ctrl+C/X/V. "
                + "The swatches below are a custom component.")
                .setWrap(true);
        body.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final Swatches swatches = new Swatches(ui, 0xFFE06C75, 0xFF98C379, 0xFFE5C07B, 0xFF61AFEF, 0xFFC678DD);

        final Label greeting = ui.label("Hello, Strata");
        final TextBox name = ui.textBox().setText("Strata").setMaxLength(24)
                .onTextChanged(text -> greeting.setText(text.isEmpty() ? "Hello?" : "Hello, " + text));
        name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final TextBox secret = ui.textBox().setPassword(true).setText("hunter2");
        secret.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final GridPanel fields = ui.gridPanel()
                .columns(GridLength.auto(), GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto())
                .setSpacing(6, 4);
        fields.add(ui.label("Name").setAnchor(Anchor.MIDDLE_LEFT), 0, 0)
                .add(name, 0, 1)
                .add(ui.label("Password").setAnchor(Anchor.MIDDLE_LEFT), 1, 0)
                .add(secret, 1, 1);
        fields.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final var column = ui.stackPanel()
                .setSpacing(6)
                .setPadding(10)
                .add(title)
                .add(body)
                .add(fields)
                .add(greeting)
                .add(buttons)
                .add(swatches)
                .add(counter);
        if (this.open != null) {
            column.add(ui.label("Almura mock-ups"));
            column.add(AlmuraMocks.buttons(ui, this.open));
            column.add(ui.button("Standard controls").onAction(() -> this.open.accept(new ControlsScreen())));
            column.setWidth(400);
        }
        column.setBackground(Paint.solid(0xFF2B2D31));
        column.setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
        if (this.open == null) {
            column.setWidth(320);
        }

        final var screen = ui.anchorPanel().add(column);
        screen.setAnchor(Anchor.FILL);
        screen.setBackground(Paint.gradient(0xFF1E1F22, 0xFF111214));
        return screen;
    }
}
