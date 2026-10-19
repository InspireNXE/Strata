package org.inspirenxe.strata.examples.events;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.ModifierPolicy;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * The events guide's examples.
 */
public final class EventExamples {

    private EventExamples() {
    }

    static void veto(final ComponentFactory factory, final TextBox confirmation) {
        // --8<-- [start:veto]
        final Button delete = factory.button("Delete world").onAction(() -> System.out.println("Deleted"));
        delete.on(EventTypes.CLICK, event -> {
            if (!confirmation.text().equals("DELETE")) {
                // BEFORE is the default phase: canceling here skips the button's own click handling
                event.cancel();
            }
        });
        // --8<-- [end:veto]
    }

    static void after(final TextBox name, final Label preview) {
        // --8<-- [start:after]
        // AFTER handlers see the result of the default behavior: here, the character is already inserted
        name.on(EventTypes.CHAR, Phase.AFTER, event -> preview.setText(name.text().toUpperCase()));
        // --8<-- [end:after]
    }

    static StackPanel delegated(final ComponentFactory factory) {
        // --8<-- [start:delegate]
        final Label selected = factory.label("Pick a color");
        final StackPanel palette = factory.stackPanel().setSpacing(2);
        for (final String color : new String[] {"Red", "Green", "Blue"}) {
            palette.add(factory.label(color).setId(color));
        }
        // Labels have no click behavior, so their clicks bubble up to the panel.
        // target() is the label that was clicked; current() is the panel running this handler.
        palette.on(EventTypes.CLICK, event -> {
            if (event.target() != palette) {
                selected.setText("Picked " + event.target().id());
                event.consume();
            }
        });
        // --8<-- [end:delegate]
        return palette;
    }

    static void rightClick(final Button button) {
        // --8<-- [start:buttons]
        button.on(EventTypes.CLICK, event -> {
            if (event.button() == MouseButton.RIGHT) {
                System.out.println("Context menu at " + event.x() + ", " + event.y());
                event.consume();
            } else if (event.clicks() == 2) {
                System.out.println("Double click");
            }
        });
        // --8<-- [end:buttons]
    }

    static void shortcut(final AnchorPanel root, final Runnable save) {
        // --8<-- [start:shortcut]
        // Key events go to the focused component first and bubble to the root,
        // so a handler on the root sees every key nothing else consumed.
        root.on(EventTypes.KEY, event -> {
            if (event.is(Key.S, Modifiers.of(Modifier.CONTROL), ModifierPolicy.EXACT)) {
                save.run();
                event.consume();
            }
        });
        // --8<-- [end:shortcut]
    }

    static void submit(final TextBox name, final java.util.function.Consumer<String> submit) {
        // --8<-- [start:submit]
        // The text box does not consume Enter, so it reaches handlers on the box and its ancestors
        name.on(EventTypes.KEY, event -> {
            if (event.key().isEnter() && event.action() == KeyAction.PRESS) {
                submit.accept(name.text());
                event.consume();
            }
        });
        // --8<-- [end:submit]
    }

    static void subscriptions(final Button button, final Object screen) {
        // --8<-- [start:remove]
        // Each registration returns a subscription that removes it
        final Subscription subscription = button.on(EventTypes.POINTER_ENTER, event -> System.out.println("Hi"));
        subscription.unsubscribe();

        // Or tag handlers with an owner and remove them all at once
        button.on(EventTypes.POINTER_ENTER, Phase.BEFORE, event -> System.out.println("Enter"), screen);
        button.on(EventTypes.POINTER_EXIT, Phase.BEFORE, event -> System.out.println("Exit"), screen);
        button.removeHandlers(screen);
        // --8<-- [end:remove]
    }

    /**
     * A box that can be dragged around an absolute panel.
     *
     * @return the screen
     */
    public static Screen dragging() {
        // --8<-- [start:drag]
        return factory -> {
            final AbsolutePanel board = factory.absolutePanel();
            board.setAnchor(Anchor.FILL);

            final Label handle = factory.label("Drag me");
            handle.setBackground(Paint.solid(0xFF3A3D43)).setMinSize(60, 20);
            final float[] position = {20f, 20f};
            board.add(handle, position[0], position[1]);

            // Drag events keep going to the component the press started on, wherever the pointer goes
            handle.on(EventTypes.DRAG, event -> {
                position[0] += event.deltaX();
                position[1] += event.deltaY();
                board.move(handle, position[0], position[1]);
                event.consume();
            });
            return board;
        };
        // --8<-- [end:drag]
    }

    /**
     * Keeps the handle from a screen's {@code opened} call to close the screen from a button.
     */
    // --8<-- [start:handle]
    public static final class ConfirmScreen implements Screen {
        private ScreenHandle handle;

        @Override
        public Component<?> build(final ComponentFactory factory) {
            final Button close = factory.button("Close").onAction(() -> this.handle.close());
            return factory.anchorPanel().add(close.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        }

        @Override
        public void opened(final ScreenHandle handle) {
            this.handle = handle;
        }
    }
    // --8<-- [end:handle]
}
