package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.Activatable;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.sandbox.almura.StoreData.Sort;
import org.inspirenxe.strata.sandbox.almura.StoreData.StoreItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A mock-up of Almura's {@code StoreScreen}: Buy and Sell tabs over a searchable list, four transaction buttons,
 * and for admins a second pane that browses every item to list.
 */
public final class StoreScreen implements Screen {
    private final StoreData data;
    private final boolean admin;
    private final Map<String, Supplier<List<Item>>> finders;
    private ComponentFactory ui;
    private ScreenHandle handle;

    private TabBar tabs;
    private TextBox search;
    private ComboBox<Sort> sort;
    private ListView<StoreItem> items;
    private Button one;
    private Button stack;
    private Button quantity;
    private Button all;

    private ComboBox<String> finder;
    private TextBox adminSearch;
    private ListView<Item> adminItems;
    private Button list;
    private Button unlist;
    private Label total;

    /**
     * Creates the screen over fresh mock data.
     *
     * @param admin true to show the admin pane
     */
    public StoreScreen(final boolean admin) {
        this.data = new StoreData("General Goods");
        this.admin = admin;
        this.finders = new LinkedHashMap<>();
        this.finders.put("Store", () -> {
            final List<Item> found = new ArrayList<>();
            for (final StoreItem item : this.data.buying) {
                found.add(item.item());
            }
            for (final StoreItem item : this.data.selling) {
                if (!found.contains(item.item())) {
                    found.add(item.item());
                }
            }
            return found;
        });
        this.finders.put("Inventory", () -> this.data.held.stream()
                .distinct()
                .toList());
        for (final String category : Item.CATEGORIES) {
            this.finders.put(category, () -> Item.inCategory(category));
        }
    }

    @Override
    public boolean hidesBackground() {
        return true;
    }

    @Override
    public void opened(final ScreenHandle handle) {
        this.handle = handle;
    }

    private boolean buySide() {
        return this.tabs.selectedIndex() == 0;
    }

    @Override
    public Component<?> build(final ComponentFactory ui) {
        this.ui = ui;
        final Window window = ui.window(this.data.name)
                .setSize(this.admin ? 481 : 257, 300)
                .onEvent(Closable.CLOSED, event -> {
                    if (this.handle != null) {
                        this.handle.close();
                    }
                });

        final GridLength[] widths = this.admin
                ? new GridLength[] {GridLength.fixed(251), GridLength.weighted(1)}
                : new GridLength[] {GridLength.weighted(1)};
        final GridPanel columns = Look.grid(ui, widths)
                .rows(GridLength.weighted(1))
                .setSpacing(4, 0)
                .add(this.buildStore(ui), 0, 0);
        if (this.admin) {
            columns.add(this.buildAdmin(ui), 0, 1);
        }
        window.add(columns);

        this.refresh(true);
        return ui.anchorPanel()
                .add(window)
                .setAnchor(Anchor.FILL);
    }

    private Component<?> buildStore(final ComponentFactory ui) {
        this.tabs = ui.tabBar()
                .addTab("Buy")
                .addTab("Sell")
                .setSpacing(4)
                .onEvent(SingleSelection.SELECTED, event -> this.refresh(false))
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        this.search = ui.textBox()
                .onEvent(TextBox.SUBMITTED, event -> this.refresh(false))
                .setAnchor(Anchor.FILL);
        this.sort = ui.<Sort>comboBox(sort -> sort.label)
                .setItems(List.of(Sort.values()))
                .onEvent(SingleSelection.SELECTED, e -> this.refresh(false))
                .setAnchor(Anchor.FILL);
        final Button go = Look.button(ui, "Search", 50)
                .onEvent(Button.PRESSED, event -> this.refresh(false))
                .setAnchor(Anchor.FILL);

        final Label itemNameLabel = Look.label(ui, "Item Name:", Look.WHITE)
                .setAnchor(Anchor.MIDDLE_LEFT);
        final GridPanel searchRow = Look.grid(ui, GridLength.auto(), GridLength.weighted(1), GridLength.fixed(78),
                        GridLength.auto())
                .rows(GridLength.auto())
                .setSpacing(2, 0)
                .add(itemNameLabel, 0, 0)
                .add(this.search, 0, 1)
                .add(this.sort, 0, 2)
                .add(go, 0, 3)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        this.items = ui.<StoreItem>listView(this::storeCell)
                .setRowHeight(24)
                .setRowSpacing(1)
                .setKeyFunction(StoreItem::record)
                .onEvent(SingleSelection.SELECTED, e -> this.updateControls())
                .onEvent(Activatable.ACTIVATED, e -> this.transact(this.items.items().get(e.value()), 1));
        Look.panel(this.items)
                .setAnchor(Anchor.FILL);

        this.one = Look.button(ui, "Buy 1", 60)
                .onEvent(Button.PRESSED, event -> this.selected()
                        .ifPresent(item -> this.transact(item, 1)));
        this.stack = Look.button(ui, "Buy 64", 60)
                .onEvent(Button.PRESSED, event -> this.selected()
                        .ifPresent(item -> this.transact(item, item.item().maxStack())));
        this.quantity = Look.button(ui, "Buy Qty", 60)
                .onEvent(Button.PRESSED, event -> this.selected()
                        .ifPresent(item -> StoreDialogs.transactQuantity(ui, item, this.buySide(), this.maxFor(item),
                                amount -> this.transact(item, amount))));
        this.all = Look.button(ui, "Buy All", 60)
                .onEvent(Button.PRESSED, event -> this.selected()
                        .ifPresent(item -> this.transact(item, this.maxFor(item))));
        final GridPanel buttons = Look.grid(ui, GridLength.auto(), GridLength.auto(), GridLength.weighted(1),
                        GridLength.auto(), GridLength.auto())
                .rows(GridLength.auto())
                .setSpacing(2, 0)
                .add(this.one, 0, 0)
                .add(this.stack, 0, 1)
                .add(this.quantity, 0, 3)
                .add(this.all, 0, 4)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final GridPanel store = Look.grid(ui, GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto(), GridLength.weighted(1), GridLength.auto())
                .setSpacing(0, 3)
                .add(this.tabs, 0, 0)
                .add(searchRow, 1, 0)
                .add(this.items, 2, 0)
                .add(buttons, 3, 0);
        Look.panel(store)
                .setPadding(3);
        return store;
    }

    private Component<?> buildAdmin(final ComponentFactory ui) {
        this.finder = ui.<String>comboBox(name -> name)
                .setItems(List.copyOf(this.finders.keySet()))
                .setMaxVisibleOptions(24)
                .onEvent(SingleSelection.SELECTED, e -> this.refreshAdmin())
                .setAnchor(Anchor.FILL);
        this.adminSearch = ui.textBox()
                .onEvent(TextInput.EDITED, event -> this.refreshAdmin())
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        this.adminItems = ui.<Item>listView(this::adminCell)
                .setRowHeight(24)
                .setRowSpacing(1)
                .setKeyFunction(Item::name)
                .onEvent(SingleSelection.SELECTED, e -> this.updateAdminControls())
                .onEvent(Activatable.ACTIVATED, e -> this.listOrModify(this.adminItems.items().get(e.value())));
        Look.panel(this.adminItems)
                .setAnchor(Anchor.FILL);
        this.list = Look.button(ui, "List", 50)
                .onEvent(Button.PRESSED, event -> this.adminItems.selected().ifPresent(this::listOrModify));
        this.unlist = Look.button(ui, "Unlist", 50)
                .onEvent(Button.PRESSED, event -> this.adminItems.selected().ifPresent(this::unlist));
        this.total = Look.label(ui, "Total: 0", Look.WHITE)
                .setAnchor(Anchor.MIDDLE_RIGHT);

        final Label typeLabel = Look.label(ui, "Type:", Look.WHITE)
                .setAnchor(Anchor.MIDDLE_LEFT);
        final GridPanel type = Look.grid(ui, GridLength.auto(), GridLength.weighted(1))
                .rows(GridLength.auto())
                .setSpacing(4, 0)
                .add(typeLabel, 0, 0)
                .add(this.finder, 0, 1)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final GridPanel bottom = Look.grid(ui, GridLength.auto(), GridLength.auto(), GridLength.weighted(1))
                .rows(GridLength.auto())
                .setSpacing(2, 0)
                .add(this.list, 0, 0)
                .add(this.unlist, 0, 1)
                .add(this.total, 0, 2)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final GridPanel pane = Look.grid(ui, GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto(), GridLength.weighted(1), GridLength.auto())
                .setSpacing(0, 3)
                .add(type, 0, 0)
                .add(this.adminSearch, 1, 0)
                .add(this.adminItems, 2, 0)
                .add(bottom, 3, 0);
        Look.panel(pane)
                .setPadding(3);
        return pane;
    }

    // ------------------------------------------------------------------
    // Cells
    // ------------------------------------------------------------------

    private Component<?> storeCell(final StoreItem item) {
        final Image icon = this.ui.image(Icons.of(item.item()))
                .setTooltip(item.item().tooltip())
                .setAnchor(Anchor.MIDDLE_LEFT);
        final Label name = Look.label(this.ui, item.item().name(), Look.WHITE)
                .setOverflow(TextOverflow.ELLIPSIS)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.MIDDLE);
        final String countText = item.quantity() > 0 ? " x " + Look.withSuffix(item.quantity()) : "";
        final Label count = Look.label(this.ui, countText, Look.GRAY)
                .setAnchor(Anchor.MIDDLE_LEFT);
        if (item.quantity() >= 1_000_000L) {
            count.setTooltip(Look.currency(BigDecimal.valueOf(item.quantity())));
        }
        final Label price = Look.rich(this.ui, Look.span(Look.withSuffix(item.price()), Look.GOLD),
                        Look.span("/ea", Look.GRAY))
                .setTextScale(0.8f)
                .setAnchor(Anchor.MIDDLE_RIGHT)
                .setMargin(new Insets(0, 0, 3, 0));
        if (item.quantity() > 0) {
            price.setTooltip("Total: " + Look.withSuffix(item.price().multiply(BigDecimal.valueOf(item.quantity()))));
        }
        final GridPanel row = Look.grid(this.ui, GridLength.fixed(16), GridLength.weighted(1), GridLength.auto(),
                        GridLength.auto())
                .rows(GridLength.weighted(1))
                .setSpacing(4, 0)
                .setPadding(Look.pad(3, 0))
                .add(icon, 0, 0)
                .add(name, 0, 1)
                .add(count, 0, 2)
                .add(price, 0, 3);

        final AnchorPanel cell = this.ui.anchorPanel()
                .add(row)
                .setAnchor(Anchor.FILL);
        if (item.quantity() == 0) {
            final Label soldOutLabel = Look.label(this.ui, "Sold Out!", Look.RED)
                    .setTextScale(2f)
                    .setAnchor(Anchor.CENTER);
            final AnchorPanel soldOut = this.ui.anchorPanel()
                    .add(soldOutLabel)
                    .setBackground(Paint.solid(0xB4555555))
                    .setAnchor(Anchor.FILL);
            cell.add(soldOut);
        }
        return cell;
    }

    private Component<?> adminCell(final Item item) {
        final Optional<StoreItem> buy = this.listing(true, item);
        final Optional<StoreItem> sell = this.listing(false, item);
        final Image icon = this.ui.image(Icons.of(item))
                .setTooltip(item.tooltip())
                .setAnchor(Anchor.MIDDLE_LEFT);
        final Label name = Look.label(this.ui, item.name(), Look.WHITE)
                .setOverflow(TextOverflow.ELLIPSIS)
                .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.MIDDLE);
        final StackPanel prices = this.ui.stackPanel()
                .setAnchor(Anchor.MIDDLE_RIGHT);
        buy.ifPresent(listing -> {
            final Label price = Look.rich(this.ui, Look.span("B: ", Look.GRAY),
                            Look.span(Look.withSuffix(listing.price()), Look.GOLD), Look.span("/ea", Look.GRAY))
                    .setTextScale(0.8f)
                    .setAnchor(Anchor.TOP_RIGHT);
            prices.add(price);
        });
        sell.ifPresent(listing -> {
            final Label price = Look.rich(this.ui, Look.span("S: ", Look.GRAY),
                            Look.span(Look.withSuffix(listing.price()), Look.GOLD), Look.span("/ea", Look.GRAY))
                    .setTextScale(0.8f)
                    .setAnchor(Anchor.TOP_RIGHT);
            prices.add(price);
        });
        final GridPanel row = Look.grid(this.ui, GridLength.fixed(16), GridLength.weighted(1), GridLength.auto(),
                        GridLength.fixed(5))
                .rows(GridLength.weighted(1))
                .setSpacing(4, 0)
                .setPadding(new Insets(3, 0, 0, 0))
                .add(icon, 0, 0)
                .add(name, 0, 1)
                .add(prices, 0, 2);
        if (buy.isPresent() || sell.isPresent()) {
            final AnchorPanel strip = this.ui.anchorPanel()
                    .setBackground(Paint.gradient(buy.isPresent() ? Look.DARK_GREEN : Look.DARK_RED,
                            sell.isPresent() ? Look.DARK_GREEN : Look.DARK_RED))
                    .setTooltip(Tooltip.text((buy.isPresent() ? "Sold here" : "Not sold here") + "\n"
                            + (sell.isPresent() ? "Bought here" : "Not bought here")))
                    .setAnchor(Anchor.FILL);
            row.add(strip, 0, 3);
        }
        return row;
    }

    // ------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------

    private Optional<StoreItem> selected() {
        return this.items.selected();
    }

    private Optional<StoreItem> listing(final boolean buy, final Item item) {
        return this.data.side(buy).stream()
                .filter(listing -> listing.item().equals(item))
                .findFirst();
    }

    private long maxFor(final StoreItem item) {
        final long available = item.infinite() ? 9999 : item.quantity();
        return this.buySide() ? available : Math.min(this.data.heldCount(item.item()), available);
    }

    private void transact(final StoreItem item, final long amount) {
        if (item.quantity() == 0 || amount <= 0) {
            return;
        }
        if (!item.infinite()) {
            ExchangeData.replace(this.data.side(this.buySide()), item, old -> new StoreItem(old.record(), old.item(),
                    Math.max(0, old.quantity() - amount), old.price()));
        }
        this.refresh(false);
    }

    private void refresh(final boolean createControls) {
        for (int side = 0; side < 2; side++) {
            final boolean empty = this.data.side(side == 0).isEmpty();
            this.tabs.setTabEnabled(side, !empty);
            this.tabs.setTabTooltip(side, empty ? Tooltip.text(side == 0 ? "There are no items for sale."
                    : "There are no items available for purchase.") : null);
        }
        final Sort order = this.sort.selected().orElse(Sort.NAME_ASC);
        this.items.setItems(this.data.search(this.buySide(), this.search.text(), order));
        if (this.items.selected().isEmpty() && !this.items.items().isEmpty()) {
            this.items.select(0);
        }
        if (createControls && this.admin) {
            this.refreshAdmin();
        }
        this.updateControls();
    }

    private void updateControls() {
        final String verb = this.buySide() ? "Buy" : "Sell";
        final Optional<StoreItem> selected = this.selected();
        final int max = selected.map(item -> item.item().maxStack()).orElse(64);
        this.one.setText(verb + " 1");
        this.stack.setText(verb + " " + max);
        this.quantity.setText(verb + " Qty");
        this.all.setText(verb + " All");
        if (selected.isEmpty()) {
            for (final Button button : List.of(this.one, this.stack, this.quantity, this.all)) {
                button.setEnabled(false);
            }
            return;
        }
        final StoreItem item = selected.get();
        if (this.buySide()) {
            this.one.setEnabled(item.infinite() || item.quantity() > 0);
            this.stack.setEnabled(item.infinite() || item.quantity() >= max);
            this.quantity.setEnabled(item.infinite() || item.quantity() > 0);
            this.all.setEnabled(item.quantity() >= 1);
        } else {
            final int held = this.data.heldCount(item.item());
            final boolean stock = item.infinite() || item.quantity() > 0;
            this.one.setEnabled(stock && held >= 1);
            this.stack.setEnabled(stock && held >= max);
            this.quantity.setEnabled(stock && held > 1);
            this.all.setEnabled(stock && held >= 1);
        }
    }

    private void refreshAdmin() {
        final List<Item> found = this.finders.get(this.finder.selected().orElse("Store")).get();
        final String text = this.adminSearch.text().toLowerCase(Locale.ROOT);
        final List<Item> shown = found.stream()
                .filter(item -> item.name().toLowerCase(Locale.ROOT).contains(text))
                .toList();
        this.adminItems.setItems(shown);
        if (this.adminItems.selected().isEmpty() && !shown.isEmpty()) {
            this.adminItems.select(0);
        }
        this.total.setText("Total: " + shown.size());
        this.updateAdminControls();
    }

    private void updateAdminControls() {
        final Optional<Item> selected = this.adminItems.selected();
        final boolean listed = selected
                .map(item -> this.listing(true, item).isPresent() || this.listing(false, item).isPresent())
                .orElse(false);
        this.list.setText(listed ? "Modify" : "List")
                .setEnabled(selected.isPresent());
        this.unlist.setEnabled(listed);
    }

    private void listOrModify(final Item item) {
        StoreDialogs.list(this.ui, item, this.listing(true, item), this.listing(false, item), (buy, sell) -> {
            this.apply(true, item, buy);
            this.apply(false, item, sell);
            this.adminItems.refresh();
            this.refresh(false);
            this.updateAdminControls();
        });
    }

    private void apply(final boolean buy, final Item item, final Optional<StoreDialogs.Side> side) {
        final List<StoreItem> list = this.data.side(buy);
        final Optional<StoreItem> existing = this.listing(buy, item);
        existing.ifPresent(list::remove);
        side.ifPresent(values -> list.add(new StoreItem(existing.map(StoreItem::record).orElseGet(this.data::newRecord),
                item, values.quantity(), values.price())));
    }

    private void unlist(final Item item) {
        this.listing(true, item).ifPresent(this.data.buying::remove);
        this.listing(false, item).ifPresent(this.data.selling::remove);
        this.adminItems.refresh();
        this.refresh(false);
        this.updateAdminControls();
    }

    // For snapshots

    ComboBox<String> finderBox() {
        return this.finder;
    }

    ListView<Item> adminList() {
        return this.adminItems;
    }

    void openList() {
        this.adminItems.selected().ifPresent(this::listOrModify);
    }
}
