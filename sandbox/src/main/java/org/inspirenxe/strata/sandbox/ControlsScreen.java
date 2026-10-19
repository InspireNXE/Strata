package org.inspirenxe.strata.sandbox;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.collection.SlotGrid;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.spi.ScreenStack;
import org.inspirenxe.strata.swing.SwingSnapshot;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;

/**
 * Shows the standard controls on one screen: a menu bar over a split pane, with a category tree on the left and the
 * other controls on the right. Uses the API only.
 */
public final class ControlsScreen implements Screen {
    private ComponentFactory ui;
    private ScreenHandle handle;
    private MenuBar menuBar;
    private TreeView<Category> tree;
    private TableView<Offer> table;

    @Override
    public void opened(final ScreenHandle handle) {
        this.handle = handle;
    }

    @Override
    public boolean hidesBackground() {
        return true;
    }

    @Override
    public Component<?> build(final ComponentFactory ui) {
        this.ui = ui;
        final Menu recent = ui.menu().addItem("Exchange", () -> { }).addItem("Store", () -> { });
        final Menu file = ui.menu()
                .addItem("New listing", "Ctrl+N", () -> ui.overlays().toast("Listing created"))
                .addSubmenu("Open recent", recent)
                .addSeparator()
                .addCheckItem("Show prices", true, shown -> { })
                .addItem("Close", () -> this.handle.close());
        final Menu view = ui.menu().addItem("Zoom in", () -> { }).addItem("Zoom out", () -> { }).setItemEnabled(1, false);
        this.menuBar = ui.menuBar().addMenu("File", file).addMenu("View", view);
        this.menuBar.setAnchor(Anchor.FILL);

        this.tree = ui.<Category>treeView(category -> ui.label(category.name()).setAnchor(Anchor.MIDDLE_LEFT))
                .setChildren(Category::children)
                .setRoots(Category.ROOTS);
        this.tree.setAnchor(Anchor.FILL);
        this.tree.select(Category.ROOTS.getFirst().children().get(1));

        final var right = ui.stackPanel().setSpacing(6).setPadding(Insets.of(8));
        right.add(this.choices(ui)).add(this.numbers(ui)).add(this.offers(ui)).add(this.notes(ui));
        right.setAnchor(Anchor.FILL);

        final var split = ui.splitPane(Orientation.HORIZONTAL).setFirst(this.tree).setSecond(right).setPosition(0.24f);
        split.setAnchor(Anchor.FILL);

        final GridPanel page = ui.gridPanel().rows(GridLength.auto(), GridLength.weighted(1)).columns(GridLength.weighted(1));
        page.add(this.menuBar, 0, 0).add(split, 1, 0);
        page.setAnchor(Anchor.FILL);
        page.setBackground(Paint.gradient(0xFF1E1F22, 0xFF111214));
        return page;
    }

    private Component<?> choices(final ComponentFactory ui) {
        final RadioGroup<String> sort = ui.radioGroup();
        final var radios = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(10);
        radios.add(ui.label("Sort").setAnchor(Anchor.MIDDLE_LEFT))
                .add(sort.add("Name", "name"))
                .add(sort.add("Price", "price"))
                .add(sort.add("Seller", "seller").setEnabled(false));
        sort.select("price");
        final var toggles = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(10);
        toggles.add(ui.toggleSwitch("Only mine").setOn(true))
                .add(ui.toggleSwitch("Hide sold"))
                .add(ui.toggleSwitch("Locked").setEnabled(false));
        return ui.stackPanel().setSpacing(6).add(radios).add(toggles);
    }

    private Component<?> numbers(final ComponentFactory ui) {
        final SlotGrid<String> hotbar = ui.<String>slotGrid(name -> ui.label(name.substring(0, 1)).setAnchor(Anchor.CENTER))
                .setColumns(9)
                .setTooltips(Tooltip::text)
                .setItems(Arrays.asList("Stone", "Iron", null, "Gold", null, null, "Wood", null, "Glass"))
                .select(3);
        final var spinners = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(10);
        spinners.add(ui.label("Quantity").setAnchor(Anchor.MIDDLE_LEFT))
                .add(ui.spinner().setRange(1, 64).setValue(16))
                .add(ui.label("Price").setAnchor(Anchor.MIDDLE_LEFT))
                .add(ui.spinner().setRange(0, 1000).setDecimals(2).setStep(0.25).setValue(12.5));
        final var slots = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(10);
        slots.add(ui.label("Hotbar").setAnchor(Anchor.MIDDLE_LEFT)).add(hotbar);
        return ui.stackPanel().setSpacing(6).add(spinners).add(slots);
    }

    private Component<?> offers(final ComponentFactory ui) {
        this.table = ui.<Offer>tableView()
                .addTextColumn("Item", GridLength.weighted(1), Offer::item)
                .addTextColumn("Seller", GridLength.fixed(70), Offer::seller)
                .addTextColumn("Price", GridLength.fixed(60), offer -> String.format("%.2f", offer.price()))
                .setColumnComparator(2, Comparator.comparingDouble(Offer::price))
                .setItems(Offer.SAMPLE)
                .sortBy(2, true)
                .select(1);
        this.table.setHeight(84);
        this.table.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        return this.table;
    }

    private Component<?> notes(final ComponentFactory ui) {
        final var notes = ui.textArea()
                .setText("Notes wrap at the edge of the box, and the box scrolls when they run past four lines.\n"
                        + "A blank line follows.\n\nThen the end.")
                .setVisibleLines(3);
        notes.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final var expander = ui.expander("Seller notes").setContent(notes).setExpanded(true);
        expander.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final Label collapsed = ui.label("Hidden until expanded");
        final var more = ui.expander("History").setContent(collapsed);
        more.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        return ui.stackPanel().setSpacing(2).add(expander).add(more).setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
    }

    /**
     * Renders the screen as it opens, with the File menu and its submenu open, and with a toast.
     *
     * @param directory where to write the images
     * @param theme the theme to render with
     * @throws IOException if an image cannot be written
     */
    public static void snapshots(final File directory, final Theme theme) throws IOException {
        directory.mkdirs();
        render(directory, theme, "controls", (screen, stack) -> layout(stack));
        render(directory, theme, "controls-menu", (screen, stack) -> {
            layout(stack);
            final Rect bar = screen.menuBar.bounds();
            click(stack, bar.x() + 20, bar.y() + bar.height() / 2f);
            stack.key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
            stack.key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
            stack.key(Key.RIGHT, KeyAction.PRESS, Modifiers.NONE);
        });
        render(directory, theme, "controls-toast", (screen, stack) -> {
            layout(stack);
            screen.ui.overlays().toast("Listing created");
            screen.ui.overlays().toast("Sold 16 Iron for 200.00, paid into your account");
            layout(stack);
        });
    }

    private static void render(final File directory, final Theme theme, final String name,
                               final BiConsumer<ControlsScreen, ScreenStack> input) throws IOException {
        final ControlsScreen screen = new ControlsScreen();
        final File out = new File(directory, name + ".png");
        ImageIO.write(SwingSnapshot.render(screen, theme, 960, 600, 2f, stack -> input.accept(screen, stack)), "png", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }

    private static void layout(final ScreenStack stack) {
        stack.pointerMoved(0, 0, Modifiers.NONE);
        stack.pointerMoved(0, 0, Modifiers.NONE);
    }

    private static void click(final ScreenStack stack, final float x, final float y) {
        stack.pointerMoved(x, y, Modifiers.NONE);
        stack.pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
        stack.pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    /**
     * A store category.
     *
     * @param name the shown name
     * @param children the subcategories
     */
    record Category(String name, List<Category> children) {
        static final List<Category> ROOTS = List.of(
                new Category("Building Blocks", new Category("Stone"), new Category("Wood", new Category("Oak"),
                        new Category("Birch")), new Category("Glass")),
                new Category("Ores", new Category("Iron"), new Category("Gold")),
                new Category("Tools"),
                new Category("Food"));

        Category(final String name, final Category... children) {
            this(name, List.of(children));
        }
    }

    /**
     * A sale offer.
     *
     * @param item what is for sale
     * @param seller who sells it
     * @param price the price per item
     */
    record Offer(String item, String seller, double price) {
        static final List<Offer> SAMPLE = List.of(
                new Offer("Iron Ingot", "Ana", 12.5),
                new Offer("Gold Ingot", "Ben", 40),
                new Offer("Oak Log", "Ana", 1.25),
                new Offer("Glass", "Cleo", 2),
                new Offer("Diamond", "Dev", 250));
    }
}
