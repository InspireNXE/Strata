package org.inspirenxe.strata.examples.layout;

import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;

/**
 * The layout guide's examples. Each method returns a screen; the documentation images are rendered from them.
 */
public final class LayoutExamples {
    private static final Paint PANEL = Paint.solid(0xFF2B2D31);
    private static final Paint INSET = Paint.solid(0xFF3A3D43);

    private LayoutExamples() {
    }

    /**
     * Children of an anchor panel overlap and pin themselves to edges, corners or the center.
     *
     * @return the screen
     */
    public static Screen anchorPanel() {
        return factory -> {
            // --8<-- [start:anchor]
            final AnchorPanel root = factory.anchorPanel().setPadding(6);
            root.setAnchor(Anchor.FILL);

            root.add(factory.label("Inventory").setAnchor(Anchor.TOP_CENTER));
            root.add(factory.button("X").setAnchor(Anchor.TOP_RIGHT));
            root.add(factory.label("v1.0").setAnchor(Anchor.BOTTOM_LEFT));

            final StackPanel dialog = factory.stackPanel().setPadding(8).setSpacing(4)
                    .add(factory.label("Nothing here yet"))
                    .add(factory.button("OK").setAnchor(Anchor.TOP_CENTER));
            dialog.setBackground(PANEL);
            root.add(dialog.setAnchor(Anchor.CENTER));
            // --8<-- [end:anchor]
            return root;
        };
    }

    /**
     * A column of stretched rows above a right-aligned row of buttons.
     *
     * @return the screen
     */
    public static Screen stackPanel() {
        return factory -> {
            // --8<-- [start:stack]
            final StackPanel buttons = factory.stackPanel(Orientation.HORIZONTAL).setSpacing(4)
                    .add(factory.button("Cancel"))
                    .add(factory.button("Apply"));
            // In a column, horizontal alignment places each child across the width
            buttons.setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP);

            final Label heading = factory.label("Video settings");
            final TextBox search = factory.textBox();
            search.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            final Label centered = factory.label("Centered");
            centered.setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.TOP);

            final StackPanel column = factory.stackPanel().setSpacing(6).setPadding(8)
                    .add(heading)
                    .add(search)
                    .add(centered)
                    .add(buttons);
            column.setWidth(220);
            column.setBackground(PANEL);
            // --8<-- [end:stack]
            return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }

    /**
     * A two-column form, with a button row spanning both columns.
     *
     * @return the screen
     */
    public static Screen gridPanel() {
        return factory -> {
            // --8<-- [start:grid]
            final TextBox name = factory.textBox();
            name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            final TextBox password = factory.textBox().setPassword(true);
            password.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

            final GridPanel form = factory.gridPanel()
                    // Labels take what they need; the fields share the rest
                    .columns(GridLength.auto(), GridLength.weighted(1))
                    .rows(GridLength.auto(), GridLength.auto(), GridLength.auto())
                    .setSpacing(6, 4)
                    .setPadding(8);
            form.add(factory.label("Name").setAnchor(Anchor.MIDDLE_LEFT), 0, 0)
                    .add(name, 0, 1)
                    .add(factory.label("Password").setAnchor(Anchor.MIDDLE_LEFT), 1, 0)
                    .add(password, 1, 1)
                    // Row 2, column 0, one row tall, two columns wide
                    .add(factory.button("Sign in").setAnchor(Anchor.TOP_RIGHT), 2, 0, 1, 2);
            form.setWidth(240);
            form.setBackground(PANEL);
            // --8<-- [end:grid]
            return factory.anchorPanel().add(form.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }

    /**
     * Weighted grid tracks: a sidebar, a main area twice as wide, and a fixed status bar.
     *
     * @return the screen
     */
    public static Screen weightedGrid() {
        return factory -> {
            // --8<-- [start:weighted]
            final GridPanel shell = factory.gridPanel()
                    .columns(GridLength.weighted(1), GridLength.weighted(2))
                    .rows(GridLength.weighted(1), GridLength.fixed(16))
                    .setSpacing(4, 4)
                    .setPadding(4);
            shell.setAnchor(Anchor.FILL);

            final Label sidebar = factory.label("Sidebar (weight 1)");
            sidebar.setBackground(INSET).setAnchor(Anchor.FILL);
            final Label main = factory.label("Main (weight 2)");
            main.setBackground(INSET).setAnchor(Anchor.FILL);
            final Label status = factory.label("Status bar (fixed 16)");
            status.setBackground(PANEL).setAnchor(Anchor.FILL);

            shell.add(sidebar, 0, 0).add(main, 0, 1).add(status, 1, 0, 1, 2);
            // --8<-- [end:weighted]
            return shell;
        };
    }

    /**
     * A toolbar whose middle column soaks up the free width, pushing the last button to the right edge.
     *
     * @return the screen
     */
    public static Screen spacer() {
        return factory -> {
            // --8<-- [start:spacer]
            final GridPanel toolbar = factory.gridPanel()
                    .columns(GridLength.auto(), GridLength.auto(), GridLength.weighted(1), GridLength.auto())
                    .setSpacing(4, 0)
                    .setPadding(4);
            toolbar.add(factory.button("New"), 0, 0)
                    .add(factory.button("Open"), 0, 1)
                    // Column 2 stays empty and takes the leftover width
                    .add(factory.button("Help"), 0, 3);
            toolbar.setBackground(PANEL);
            toolbar.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            // --8<-- [end:spacer]
            return factory.anchorPanel().add(toolbar).setAnchor(Anchor.FILL);
        };
    }

    /**
     * Free placement with an absolute panel.
     *
     * @return the screen
     */
    public static Screen absolutePanel() {
        return factory -> {
            // --8<-- [start:absolute]
            final AbsolutePanel board = factory.absolutePanel();
            board.add(factory.button("A"), 10, 10)
                    .add(factory.button("B"), 60, 30)
                    .add(factory.button("C overlaps B"), 80, 40);
            board.setBackground(PANEL);
            // --8<-- [end:absolute]
            return factory.anchorPanel().add(board.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }

    /**
     * Margin outside, padding inside.
     *
     * @return the screen
     */
    public static Screen marginAndPadding() {
        return factory -> {
            // --8<-- [start:spacing]
            final Label inner = factory.label("Label with a margin of 10");
            inner.setMargin(10).setBackground(INSET);

            final StackPanel outer = factory.stackPanel()
                    .setPadding(Insets.of(20, 6))
                    .add(inner);
            // The background covers the padding, but not the child's margin
            outer.setBackground(PANEL);
            // --8<-- [end:spacing]
            return factory.anchorPanel().add(outer.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }

    /**
     * Wrapping text needs a width to wrap to.
     *
     * @return the screen
     */
    public static Screen wrapping() {
        return factory -> {
            // --8<-- [start:wrap]
            final Label body = factory.label("A wrapping label breaks between words to fit the width it is "
                    + "offered. Offered unlimited width, it only breaks at line breaks.\nThis is a second paragraph.")
                    .setWrap(true);
            // Stretching gives it the column's width; setWidth or setMaxSize would work too
            body.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

            final StackPanel column = factory.stackPanel().setPadding(8).setSpacing(4)
                    .add(factory.label("About").setStyle(TextStyle.DEFAULT.withBold(true)))
                    .add(body);
            column.setWidth(180);
            column.setBackground(PANEL);
            // --8<-- [end:wrap]
            return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }

    /**
     * Size limits and fixed sizes.
     *
     * @return the screen
     */
    public static Screen sizes() {
        return factory -> {
            // --8<-- [start:sizes]
            final Button natural = factory.button("Natural size");
            final Button fixed = factory.button("Fixed 150 x 30").setSize(150, 30);
            final Button stretched = factory.button("Stretched, at most 200 wide");
            stretched.setMaxSize(200, Float.POSITIVE_INFINITY)
                    .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
            final Button tall = factory.button("At least 40 tall").setMinSize(0, 40);

            final StackPanel column = factory.stackPanel().setPadding(8).setSpacing(4)
                    .add(natural).add(fixed).add(stretched).add(tall);
            column.setWidth(260);
            column.setBackground(PANEL);
            // --8<-- [end:sizes]
            return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
        };
    }
}
