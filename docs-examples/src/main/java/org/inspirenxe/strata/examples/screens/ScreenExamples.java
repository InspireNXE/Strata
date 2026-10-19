package org.inspirenxe.strata.examples.screens;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.swing.StrataPanel;
import org.inspirenxe.strata.swing.SwingPlatform;
import org.inspirenxe.strata.swing.SwingTextMetrics;

import java.util.ArrayList;
import java.util.List;

/**
 * The screens guide's examples.
 */
public final class ScreenExamples {

    private ScreenExamples() {
    }

    // --8<-- [start:rebuild]
    /**
     * A list whose rows change. The screen object keeps the data; each rebuild makes a new tree from it.
     */
    public static final class ShoppingList implements Screen {
        private final List<String> items = new ArrayList<>(List.of("Bread", "Milk"));
        private ScreenHandle handle;

        @Override
        public Component<?> build(final ComponentFactory factory) {
            final StackPanel rows = factory.stackPanel().setSpacing(2).setPadding(8);
            for (final String item : this.items) {
                rows.add(factory.button("Remove " + item).onEvent(Button.PRESSED, event -> {
                    this.items.remove(item);
                    // The structure changed: ask for a new tree before the next frame
                    this.handle.invalidate();
                }));
            }
            rows.add(factory.button("Add eggs").onEvent(Button.PRESSED, event -> {
                this.items.add("Eggs");
                this.handle.invalidate();
            }));
            rows.setBackground(Paint.solid(0xFF2B2D31));
            return factory.anchorPanel().add(rows.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        }

        @Override
        public void opened(final ScreenHandle handle) {
            this.handle = handle;
        }

        @Override
        public void closed() {
            this.handle = null;
        }
    }
    // --8<-- [end:rebuild]

    static void dialog() {
        // --8<-- [start:stack]
        final StrataPanel panel = new StrataPanel(new SwingPlatform(SwingTextMetrics.defaults()), Themes.flat(), 2f);
        panel.screens().push(new ShoppingList());

        // A dialog on top: it gets the input, and the list shows through behind it
        panel.screens().push(factory -> {
            final StackPanel dialog = factory.stackPanel().setPadding(8).setSpacing(4)
                    .add(factory.label("Really quit?"))
                    .add(factory.button("No").onEvent(Button.PRESSED, event -> panel.screens().pop()));
            dialog.setBackground(Paint.solid(0xFF3A3D43));
            return factory.anchorPanel().add(dialog.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        });
        // --8<-- [end:stack]
    }

    // --8<-- [start:opaque]
    /**
     * A screen that covers everything, so a stack skips drawing the screens below it.
     */
    public static final class TitleScreen implements Screen {
        @Override
        public Component<?> build(final ComponentFactory factory) {
            final var root = factory.anchorPanel().add(factory.label("My Game").setAnchor(Anchor.CENTER));
            root.setBackground(Paint.solid(0xFF000000));
            return root.setAnchor(Anchor.FILL);
        }

        @Override
        public boolean hidesBackground() {
            return true;
        }
    }
    // --8<-- [end:opaque]
}
