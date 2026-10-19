package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.spi.ScreenStack;
import org.inspirenxe.strata.swing.SwingSnapshot;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.imageio.ImageIO;

/**
 * Entry points for the Almura mock-ups: buttons that open them, and a set of snapshots of each in interesting
 * states for checking without a display.
 */
public final class AlmuraMocks {

    private AlmuraMocks() {
    }

    /**
     * Returns the buttons that open each mock-up, for the sandbox's demo screen.
     *
     * @param ui the factory
     * @param open pushes a screen
     * @return a row of buttons
     */
    public static Component<?> buttons(final ComponentFactory ui, final Consumer<Screen> open) {
        final Button exchange = ui.button("Exchange")
                .onEvent(Button.PRESSED, event -> open.accept(new ExchangeScreen()));
        final Button store = ui.button("Store")
                .onEvent(Button.PRESSED, event -> open.accept(new StoreScreen(false)));
        final Button storeAdmin = ui.button("Store (admin)")
                .onEvent(Button.PRESSED, event -> open.accept(new StoreScreen(true)));
        final Button management = ui.button("Management")
                .onEvent(Button.PRESSED, event -> open.accept(new ManagementScreen("Exchange")));
        return Look.row(ui, 4, exchange, store, storeAdmin, management);
    }

    /**
     * A screen to render, and the input that puts it in the state to show.
     */
    private record Scenario<S extends Screen>(String name, Supplier<S> screen, BiConsumer<S, ScreenStack> input) {
        void render(final File directory, final Theme theme) throws IOException {
            final S screen = this.screen.get();
            final File out = new File(directory, this.name + ".png");
            ImageIO.write(SwingSnapshot.render(screen, theme, 1280, 800, 2f, stack -> this.input.accept(screen, stack)),
                    "png", out);
            System.out.println("Wrote " + out.getAbsolutePath());
        }
    }

    /**
     * Renders every scenario to a PNG in a directory.
     *
     * @param directory where to write the images
     * @param theme the theme to render with
     * @throws IOException if an image cannot be written
     */
    public static void snapshots(final File directory, final Theme theme) throws IOException {
        directory.mkdirs();
        final List<Scenario<?>> scenarios = List.of(
                new Scenario<>("exchange", ExchangeScreen::new, (screen, stack) -> {
                    layout(stack);
                    screen.forSaleList().select(1);
                }),
                new Scenario<>("exchange-tooltip", ExchangeScreen::new, (screen, stack) -> {
                    layout(stack);
                    final Rect list = screen.forSaleList().bounds();
                    stack.pointerMoved(list.x() + 12, list.y() + 26 + 12, Modifiers.NONE);
                }),
                new Scenario<>("exchange-sort", ExchangeScreen::new, (screen, stack) -> {
                    layout(stack);
                    click(stack, screen.sortBox().bounds());
                }),
                new Scenario<>("exchange-control", ExchangeScreen::new, (screen, stack) -> {
                    layout(stack);
                    screen.forSaleList().select(0);
                    stack.key(Key.LEFT_CONTROL, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
                }),
                new Scenario<>("exchange-quantity", ExchangeScreen::new, (screen, stack) -> {
                    layout(stack);
                    screen.forSaleList().select(0);
                    click(stack, screen.buyQuantityButton().bounds());
                }),
                new Scenario<>("exchange-offer", ExchangeScreen::new, (screen, stack) -> {
                    layout(stack);
                    screen.openOffer();
                }),
                new Scenario<>("store", () -> new StoreScreen(false), (screen, stack) -> layout(stack)),
                new Scenario<>("store-admin", () -> new StoreScreen(true), (screen, stack) -> layout(stack)),
                new Scenario<>("store-finder", () -> new StoreScreen(true), (screen, stack) -> {
                    layout(stack);
                    screen.finderBox().selectItem("Building Blocks");
                    layout(stack);
                    screen.adminList().select(30);
                    click(stack, screen.finderBox().bounds());
                }),
                new Scenario<>("store-list", () -> new StoreScreen(true), (screen, stack) -> {
                    layout(stack);
                    screen.openList();
                }),
                new Scenario<>("management", () -> new ManagementScreen("Exchange"), (screen, stack) -> layout(stack)),
                new Scenario<>("management-confirm", () -> new ManagementScreen("Store"), (screen, stack) -> {
                    layout(stack);
                    screen.openDeleteConfirmation();
                }));
        for (final Scenario<?> scenario : scenarios) {
            scenario.render(directory, theme);
        }
    }

    /**
     * Makes the stack lay out, and settle rows a list built during the first layout, by moving the pointer to a
     * corner.
     */
    private static void layout(final ScreenStack stack) {
        stack.pointerMoved(0, 0, Modifiers.NONE);
        stack.pointerMoved(0, 0, Modifiers.NONE);
    }

    private static void click(final ScreenStack stack, final Rect bounds) {
        final float x = bounds.x() + bounds.width() / 2f;
        final float y = bounds.y() + bounds.height() / 2f;
        stack.pointerMoved(x, y, Modifiers.NONE);
        stack.pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
        stack.pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
        stack.pointerMoved(x, y, Modifiers.NONE);
    }
}
