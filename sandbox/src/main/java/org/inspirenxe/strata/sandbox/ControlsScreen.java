package org.inspirenxe.strata.sandbox;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
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
import java.util.function.Function;
import javax.imageio.ImageIO;

/**
 * Shows the standard controls on one screen: a menu bar over a split pane, with a category tree on the left and the
 * other controls on the right. Under the tree are the range controls: a vertical slider, scroll bars and two busy
 * signs. Uses the API only.
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
        final Menu recent = ui.menu()
                .addItem("Exchange", () -> { })
                .addItem("Store", () -> { });
        final Menu file = ui.menu()
                .addItem("New listing", "Ctrl+N", () -> ui.overlays().toast("Listing created"))
                .addSubmenu("Open recent", recent)
                .addSeparator()
                .addCheckItem("Show prices", true, shown -> { })
                .addItem("Close", () -> this.handle.close());
        final Menu view = ui.menu()
                .addItem("Zoom in", () -> { })
                .addItem(ui.menuItem("Zoom out").setEnabled(false));
        this.menuBar = ui.menuBar()
                .addMenu("File", file)
                .addMenu("View", view)
                .setAnchor(Anchor.FILL);

        final Function<Category, Label> row = category -> ui.label(category.name())
                .setAnchor(Anchor.MIDDLE_LEFT);
        this.tree = ui.<Category>treeView(row)
                .setChildren(Category::children)
                .setItems(Category.ROOTS)
                .setAnchor(Anchor.FILL);
        this.tree.selectItem(Category.ROOTS.getFirst().children().get(1));

        final StackPanel right = ui.stackPanel()
                .setSpacing(6)
                .setPadding(Insets.of(8))
                .add(this.choices(ui))
                .add(this.numbers(ui))
                .add(this.offers(ui))
                .add(this.notes(ui))
                .setAnchor(Anchor.FILL);

        final Component<?> left = ui.gridPanel()
                .rows(GridLength.weighted(1), GridLength.auto())
                .columns(GridLength.weighted(1))
                .add(this.tree, 0, 0)
                .add(this.ranges(ui), 1, 0)
                .setAnchor(Anchor.FILL);

        final SplitPane split = ui.splitPane(Orientation.HORIZONTAL)
                .setFirst(left)
                .setSecond(right)
                .setPosition(0.24f)
                .setAnchor(Anchor.FILL);

        return ui.gridPanel()
                .rows(GridLength.auto(), GridLength.weighted(1))
                .columns(GridLength.weighted(1))
                .add(this.menuBar, 0, 0)
                .add(split, 1, 0)
                .setAnchor(Anchor.FILL)
                .setBackground(Paint.gradient(0xFF1E1F22, 0xFF111214));
    }

    private Component<?> ranges(final ComponentFactory ui) {
        final Slider level = ui.slider(Orientation.VERTICAL)
                .setRange(0, 10)
                .setStep(1)
                .setValue(7)
                .setHeight(64);
        final ScrollBar bar = ui.scrollBar()
                .setExtent(40, 120)
                .setValue(30)
                .setHeight(64);
        final ScrollBar slim = ui.scrollBar()
                .setSlim(true)
                .setExtent(40, 200)
                .setValue(120)
                .setHeight(64);
        final ScrollBar across = ui.scrollBar(Orientation.HORIZONTAL)
                .setExtent(50, 150)
                .setValue(25)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final ProgressBar loading = ui.progressBar()
                .setIndeterminate(true)
                .setText("Loading")
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final BusyIndicator joining = ui.busyIndicator("Joining")
                .setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.TOP);
        final StackPanel column = ui.stackPanel()
                .setSpacing(8)
                .add(across)
                .add(loading)
                .add(joining)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        return ui.gridPanel()
                .rows(GridLength.auto())
                .columns(GridLength.auto(), GridLength.auto(), GridLength.auto(), GridLength.weighted(1))
                .setSpacing(6, 0)
                .add(level, 0, 0)
                .add(bar, 0, 1)
                .add(slim, 0, 2)
                .add(column, 0, 3)
                .setPadding(Insets.of(8))
                .setAnchor(Anchor.FILL);
    }

    private Component<?> choices(final ComponentFactory ui) {
        final RadioGroup<String> sort = ui.radioGroup();
        final Label sortLabel = ui.label("Sort")
                .setAnchor(Anchor.MIDDLE_LEFT);
        final RadioButton byName = sort.add("Name", "name");
        final RadioButton byPrice = sort.add("Price", "price");
        final RadioButton bySeller = sort.add("Seller", "seller").setEnabled(false);
        final StackPanel radios = ui.stackPanel(Orientation.HORIZONTAL)
                .setSpacing(10)
                .add(sortLabel)
                .add(byName)
                .add(byPrice)
                .add(bySeller);
        sort.selectItem("price");

        final ToggleSwitch onlyMine = ui.toggleSwitch("Only mine")
                .setSelected(true);
        final ToggleSwitch hideSold = ui.toggleSwitch("Hide sold");
        final ToggleSwitch locked = ui.toggleSwitch("Locked")
                .setEnabled(false);
        final StackPanel toggles = ui.stackPanel(Orientation.HORIZONTAL)
                .setSpacing(10)
                .add(onlyMine)
                .add(hideSold)
                .add(locked);

        return ui.stackPanel()
                .setSpacing(6)
                .add(radios)
                .add(toggles);
    }

    private Component<?> numbers(final ComponentFactory ui) {
        final Function<String, Label> slot = name -> ui.label(name.substring(0, 1))
                .setAnchor(Anchor.CENTER);
        final GridView<String> hotbar = ui.<String>gridView(slot)
                .setColumns(9)
                .setTooltips(Tooltip::text)
                .setItems(Arrays.asList("Stone", "Iron", null, "Gold", null, null, "Wood", null, "Glass"))
                .select(3);

        final Label quantityLabel = ui.label("Quantity")
                .setAnchor(Anchor.MIDDLE_LEFT);
        final Spinner quantity = ui.spinner()
                .setRange(1, 64)
                .setValue(16);
        final Label priceLabel = ui.label("Price")
                .setAnchor(Anchor.MIDDLE_LEFT);
        final Spinner price = ui.spinner()
                .setRange(0, 1000)
                .setDecimals(2)
                .setStep(0.25)
                .setValue(12.5);
        final StackPanel spinners = ui.stackPanel(Orientation.HORIZONTAL)
                .setSpacing(10)
                .add(quantityLabel)
                .add(quantity)
                .add(priceLabel)
                .add(price);

        final Label hotbarLabel = ui.label("Hotbar")
                .setAnchor(Anchor.MIDDLE_LEFT);
        final StackPanel slots = ui.stackPanel(Orientation.HORIZONTAL)
                .setSpacing(10)
                .add(hotbarLabel)
                .add(hotbar);

        return ui.stackPanel()
                .setSpacing(6)
                .add(spinners)
                .add(slots);
    }

    private Component<?> offers(final ComponentFactory ui) {
        final TableColumn<Offer> price = ui.<Offer>tableColumn("Price")
                .setWidth(GridLength.fixed(60))
                .setCellText(offer -> String.format("%.2f", offer.price()))
                .setComparator(Comparator.comparingDouble(Offer::price));
        this.table = ui.<Offer>tableView()
                .addTextColumn("Item", GridLength.weighted(1), Offer::item)
                .addTextColumn("Seller", GridLength.fixed(70), Offer::seller)
                .addColumn(price)
                .setItems(Offer.SAMPLE)
                .sortBy(price, true)
                .select(1)
                .setHeight(84)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        return this.table;
    }

    private Component<?> notes(final ComponentFactory ui) {
        final TextArea notes = ui.textArea()
                .setText("Notes wrap at the edge of the box, and the box scrolls when they run past four lines.\n"
                        + "A blank line follows.\n\nThen the end.")
                .setVisibleLines(3)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final Expander expander = ui.expander("Seller notes")
                .setContent(notes)
                .setExpanded(true)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final Label collapsed = ui.label("Hidden until expanded");
        final Expander more = ui.expander("History")
                .setContent(collapsed)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        return ui.stackPanel()
                .setSpacing(2)
                .add(expander)
                .add(more)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
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
        ImageIO.write(SwingSnapshot.render(screen, theme, 960, 600, 2f, stack -> input.accept(screen, stack)),
                "png", out);
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
                new Category("Building Blocks",
                        new Category("Stone"),
                        new Category("Wood", new Category("Oak"), new Category("Birch")),
                        new Category("Glass")),
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
