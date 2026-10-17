package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ComboBox;
import org.inspirenxe.strata.api.widget.Image;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.ListView;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.api.widget.Window;
import org.inspirenxe.strata.sandbox.almura.ExchangeData.Listing;
import org.inspirenxe.strata.sandbox.almura.ExchangeData.Sort;
import org.inspirenxe.strata.sandbox.almura.ExchangeData.Stock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * A mock-up of Almura's {@code ExchangeScreen}: search the market, page through it, buy, and manage your own
 * listings. Every control works against {@link ExchangeData}; nothing talks to a server.
 */
public final class ExchangeScreen implements Screen {
    private final ExchangeData data;
    private ComponentFactory ui;
    private ScreenHandle handle;

    private TextBox itemName;
    private TextBox seller;
    private ComboBox<Sort> sort;
    private ListView<Listing> forSale;
    private ListView<Stock> listable;
    private Label page;
    private Label limitLabel;
    private Button first;
    private Button previous;
    private Button next;
    private Button last;
    private Button buyOne;
    private Button buyStack;
    private Button buyQuantity;
    private Button buyAll;
    private Button list;
    private boolean control;

    private List<Listing> results;
    private int pageIndex;

    /**
     * Creates the screen over fresh mock data.
     */
    public ExchangeScreen() {
        this.data = new ExchangeData("Global Market", 10);
        this.results = List.of();
    }

    @Override
    public boolean hidesBackground() {
        return true;
    }

    @Override
    public Component<?> build(final ComponentFactory ui) {
        this.ui = ui;
        final Window window = ui.window("Exchange: " + this.data.name);
        window.setSize(600, 370);
        window.onClose(() -> {
            if (this.handle != null) {
                this.handle.close();
            }
        });

        final GridPanel columns = Look.grid(ui, GridLength.weighted(1), GridLength.weighted(1))
                .rows(GridLength.weighted(1))
                .setSpacing(6, 0);
        columns.add(this.buildMarket(ui), 0, 0).add(this.buildListable(ui), 0, 1);
        window.add(columns);

        final AnchorPanel screen = ui.anchorPanel().add(window);
        screen.setAnchor(Anchor.FILL);
        screen.on(EventTypes.MODIFIERS_CHANGED, event -> {
            this.control = event.modifiers().control();
            this.updateControls();
        });
        this.search();
        this.refreshListable();
        return screen;
    }

    @Override
    public void opened(final ScreenHandle handle) {
        this.handle = handle;
        this.itemName.requestFocus();
    }

    // ------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------

    private Component<?> buildMarket(final ComponentFactory ui) {
        this.itemName = ui.textBox().onSubmit(text -> this.search());
        this.itemName.setTabIndex(0);
        this.seller = ui.textBox().onSubmit(text -> this.search());
        this.seller.setTabIndex(1);
        this.sort = ui.<Sort>comboBox(sort -> sort.label)
                .setOptions(List.of(Sort.values()))
                .onSelectionChanged(sort -> this.search());
        final Button search = Look.button(ui, "Search", 80).onAction(this::search);

        final GridPanel fields = Look.grid(ui, GridLength.auto(), GridLength.fixed(120), GridLength.weighted(1),
                GridLength.fixed(90)).rows(GridLength.auto(), GridLength.auto()).setSpacing(4, 3);
        fields.add(Look.label(ui, "Item Name:", Look.WHITE).setAnchor(Anchor.MIDDLE_RIGHT), 0, 0)
                .add(this.itemName.setAnchor(Anchor.FILL), 0, 1)
                .add(this.sort.setAnchor(Anchor.FILL), 0, 3)
                .add(Look.label(ui, "Seller:", Look.WHITE).setAnchor(Anchor.MIDDLE_RIGHT), 1, 0)
                .add(this.seller.setAnchor(Anchor.FILL), 1, 1)
                .add(search.setAnchor(Anchor.FILL), 1, 3);
        fields.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        this.forSale = ui.<Listing>listView(this::forSaleCell)
                .setRowHeight(24)
                .setRowSpacing(1)
                .setKeyFunction(Listing::record)
                .setPlaceholder("No results found.")
                .onSelectionChanged(selected -> this.updateControls())
                .onActivate(listing -> this.buy(listing, 1));
        this.forSale.setAnchor(Anchor.FILL);

        this.first = Look.button(ui, "|<", 20).onAction(() -> this.setPage(0));
        this.previous = Look.button(ui, "<", 20).onAction(() -> this.setPage(this.pageIndex - 1));
        this.next = Look.button(ui, ">", 20).onAction(() -> this.setPage(this.pageIndex + 1));
        this.last = Look.button(ui, ">|", 20).onAction(() -> this.setPage(Integer.MAX_VALUE));
        this.page = ui.label("1/1").setTextAlignment(HorizontalAlignment.CENTER);
        final GridPanel paging = Look.grid(ui, GridLength.auto(), GridLength.auto(), GridLength.weighted(1),
                GridLength.auto(), GridLength.auto()).rows(GridLength.auto()).setSpacing(2, 0);
        paging.add(this.first, 0, 0).add(this.previous, 0, 1).add(this.page.setAnchor(Anchor.CENTER), 0, 2)
                .add(this.next, 0, 3).add(this.last, 0, 4);
        paging.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final GridPanel searching = Look.grid(ui, GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto(), GridLength.weighted(1), GridLength.auto(), GridLength.auto())
                .setSpacing(0, 3);
        searching.add(fields, 0, 0)
                .add(ui.separator(Orientation.HORIZONTAL), 1, 0)
                .add(this.forSale, 2, 0)
                .add(ui.separator(Orientation.HORIZONTAL), 3, 0)
                .add(paging, 4, 0);
        Look.panel(searching).setPadding(4);

        this.buyOne = Look.button(ui, "Buy 1", 60).onAction(() -> this.selectedListing().ifPresent(l -> this.buy(l, 1)));
        this.buyStack = Look.button(ui, "Buy 64", 60).onAction(() -> this.selectedListing()
                .ifPresent(l -> this.buy(l, l.item().maxStack())));
        this.buyQuantity = Look.button(ui, "Buy Qty", 60).onAction(() -> this.selectedListing()
                .ifPresent(l -> ExchangeDialogs.buyQuantity(ui, l, quantity -> this.buy(l, quantity))));
        this.buyAll = Look.button(ui, "Buy All", 50).onAction(() -> this.selectedListing()
                .ifPresent(l -> this.buy(l, l.quantity())));
        final GridPanel buying = Look.grid(ui, GridLength.auto(), GridLength.auto(), GridLength.weighted(1),
                GridLength.auto(), GridLength.auto()).rows(GridLength.auto()).setSpacing(6, 0);
        buying.add(this.buyOne, 0, 0).add(this.buyStack, 0, 1).add(this.buyQuantity, 0, 3).add(this.buyAll, 0, 4);
        Look.panel(buying).setPadding(Look.pad(3, 2));
        buying.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final GridPanel market = Look.grid(ui, GridLength.weighted(1)).rows(GridLength.weighted(1), GridLength.auto())
                .setSpacing(0, 2);
        return market.add(searching, 0, 0).add(buying, 1, 0);
    }

    private Component<?> buildListable(final ComponentFactory ui) {
        this.listable = ui.<Stock>listView(this::listableCell)
                .setRowHeight(24)
                .setRowSpacing(1)
                .setKeyFunction(Stock::record)
                .setPlaceholder("Nothing to list.")
                .onSelectionChanged(selected -> this.updateControls())
                .onActivate(this::toggleListing);
        this.listable.setAnchor(Anchor.FILL);

        this.list = Look.button(ui, "List", 46).onAction(() -> this.selectedStock().ifPresent(this::toggleListing));
        this.limitLabel = ui.label("0/0").setTextAlignment(HorizontalAlignment.CENTER);
        final Button offer = Look.button(ui, "", 30).onAction(() -> ExchangeDialogs.offer(ui, this.data, this::refreshListable));
        offer.setText("+/-");
        offer.setTooltip("Move items between your inventory and the exchange");

        final GridPanel bottom = Look.grid(ui, GridLength.auto(), GridLength.weighted(1), GridLength.auto())
                .rows(GridLength.auto());
        bottom.add(this.list, 0, 0).add(this.limitLabel.setAnchor(Anchor.CENTER), 0, 1).add(offer, 0, 2);
        bottom.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final GridPanel listing = Look.grid(ui, GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto(), GridLength.weighted(1), GridLength.auto(), GridLength.auto())
                .setSpacing(0, 3);
        listing.add(Look.label(ui, "Listable Items", Look.WHITE).setAnchor(Anchor.TOP_CENTER), 0, 0)
                .add(ui.separator(Orientation.HORIZONTAL), 1, 0)
                .add(this.listable, 2, 0)
                .add(ui.separator(Orientation.HORIZONTAL), 3, 0)
                .add(bottom, 4, 0);
        Look.panel(listing).setPadding(4);
        return listing;
    }

    private Component<?> forSaleCell(final Listing listing) {
        final Image icon = this.ui.image(Icons.of(listing.item())).setTooltip(listing.item().tooltip());
        final Label name = Look.label(this.ui, listing.item().name(), Look.WHITE).setOverflow(TextOverflow.ELLIPSIS);
        final Label quantity = Look.rich(this.ui, Look.span(" x ", Look.GRAY), Look.span(Look.withSuffix(listing.quantity()), Look.GRAY));
        if (listing.quantity() >= 1_000_000L) {
            quantity.setTooltip(Look.currency(BigDecimal.valueOf(listing.quantity())));
        }
        final Label seller = this.ui.label(listing.seller()).setStyle(Look.text(Look.GRAY).withItalic(true));
        final Label price = Look.rich(this.ui, Look.span(Look.withSuffix(listing.price()), Look.GOLD), Look.span("/ea", Look.GRAY))
                .setTextScale(0.8f);
        price.setTooltip("Total: " + Look.withSuffix(listing.price().multiply(BigDecimal.valueOf(listing.quantity()))));

        final GridPanel cell = Look.grid(this.ui, GridLength.fixed(16), GridLength.weighted(1), GridLength.auto(),
                GridLength.auto(), GridLength.auto()).rows(GridLength.weighted(1)).setSpacing(4, 0);
        cell.setPadding(Look.pad(3, 0));
        cell.add(icon.setAnchor(Anchor.MIDDLE_LEFT), 0, 0)
                .add(name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.MIDDLE), 0, 1)
                .add(quantity.setAnchor(Anchor.MIDDLE_LEFT), 0, 2)
                .add(price.setAnchor(Anchor.MIDDLE_RIGHT), 0, 4);
        if (!listing.seller().equals(ExchangeData.PLAYER)) {
            cell.add(seller.setAnchor(Anchor.MIDDLE_RIGHT), 0, 3);
        } else {
            cell.setBorder(Border.of(Look.AQUA, 1f));
        }
        return cell;
    }

    private Component<?> listableCell(final Stock stock) {
        final Image icon = this.ui.image(Icons.of(stock.item())).setTooltip(stock.item().tooltip());
        final Label name = Look.label(this.ui, stock.item().name(), Look.WHITE).setOverflow(TextOverflow.ELLIPSIS);
        final Label quantity = Look.rich(this.ui, Look.span(" x " + Look.withSuffix(stock.quantity()), Look.GRAY));
        final GridPanel cell = Look.grid(this.ui, GridLength.fixed(16), GridLength.weighted(1), GridLength.auto(),
                GridLength.auto(), GridLength.fixed(5)).rows(GridLength.weighted(1)).setSpacing(4, 0);
        cell.setPadding(new Insets(3, 0, 0, 0));
        cell.add(icon.setAnchor(Anchor.MIDDLE_LEFT), 0, 0)
                .add(name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.MIDDLE), 0, 1)
                .add(quantity.setAnchor(Anchor.MIDDLE_LEFT), 0, 2);
        if (stock.listed()) {
            final Label price = Look.rich(this.ui, Look.span(Look.withSuffix(stock.price()), Look.GOLD), Look.span("/ea", Look.GRAY))
                    .setTextScale(0.8f);
            price.setTooltip("Total: " + Look.withSuffix(stock.price().multiply(BigDecimal.valueOf(stock.quantity()))));
            final AnchorPanel strip = this.ui.anchorPanel();
            strip.setBackground(Paint.solid(Look.DARK_GREEN));
            strip.setTooltip("Listed for sale");
            cell.add(price.setAnchor(Anchor.MIDDLE_RIGHT), 0, 3).add(strip.setAnchor(Anchor.FILL), 0, 4);
        }
        return cell;
    }

    // ------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------

    private Optional<Listing> selectedListing() {
        return this.forSale.selected();
    }

    private Optional<Stock> selectedStock() {
        return this.listable.selected();
    }

    private void search() {
        this.results = this.data.search(this.itemName.text(), this.seller.text(), this.sort.selected().orElse(Sort.CREATED_ASC));
        this.setPage(0);
    }

    private int pageCount() {
        return Math.max(1, (this.results.size() + ExchangeData.PAGE_SIZE - 1) / ExchangeData.PAGE_SIZE);
    }

    private void setPage(final int index) {
        this.pageIndex = Math.max(0, Math.min(index, this.pageCount() - 1));
        final int from = this.pageIndex * ExchangeData.PAGE_SIZE;
        this.forSale.setItems(this.results.subList(Math.min(from, this.results.size()),
                Math.min(from + ExchangeData.PAGE_SIZE, this.results.size())));
        this.forSale.setScrollY(0);
        this.page.setText((this.pageIndex + 1) + "/" + this.pageCount());
        this.updateControls();
    }

    private void buy(final Listing listing, final long amount) {
        if (this.control) {
            // Ctrl held: an admin delist instead of a purchase
            this.data.market.remove(listing);
        } else {
            ExchangeData.replace(this.data.market, listing, old -> old.quantity() - amount <= 0 ? null
                    : new Listing(old.record(), old.item(), old.quantity() - amount, old.price(), old.seller(), old.created()));
        }
        this.results = this.data.search(this.itemName.text(), this.seller.text(), this.sort.selected().orElse(Sort.CREATED_ASC));
        this.setPage(this.pageIndex);
    }

    private void toggleListing(final Stock stock) {
        if (stock.listed()) {
            ExchangeData.replace(this.data.mine, stock, old -> new Stock(old.record(), old.item(), old.quantity(), null, old.price()));
            this.data.market.removeIf(listing -> listing.record() == stock.record());
            this.refreshListable();
            this.search();
            return;
        }
        if (this.data.listedCount() >= this.data.limit) {
            return;
        }
        ExchangeDialogs.listPrice(this.ui, stock, price -> {
            ExchangeData.replace(this.data.mine, stock, old -> new Stock(old.record(), old.item(), old.quantity(), price, price));
            this.data.market.add(new Listing(stock.record(), stock.item(), stock.quantity(), price, ExchangeData.PLAYER,
                    this.data.market.size()));
            this.refreshListable();
            this.search();
        });
    }

    private void refreshListable() {
        this.listable.setItems(this.data.mine);
        this.limitLabel.setText(this.data.listedCount() + "/" + this.data.limit);
        this.updateControls();
    }

    private void updateControls() {
        if (this.buyOne == null) {
            return;
        }
        final Optional<Listing> selected = this.selectedListing();
        final long quantity = selected.map(Listing::quantity).orElse(0L);
        final int stack = selected.map(listing -> listing.item().maxStack()).orElse(64);
        this.buyOne.setText(this.control ? "Delist" : "Buy 1").setEnabled(selected.isPresent());
        this.buyStack.setText("Buy " + stack).setEnabled(quantity > 1 && quantity >= stack);
        this.buyQuantity.setEnabled(quantity > 1);
        this.buyAll.setEnabled(quantity > 1);

        final Optional<Stock> stock = this.selectedStock();
        this.list.setText(stock.map(Stock::listed).orElse(false) ? "Unlist" : "List")
                .setEnabled(stock.isPresent() && (stock.get().listed() || this.data.listedCount() < this.data.limit));

        final boolean firstPage = this.pageIndex == 0;
        final boolean lastPage = this.pageIndex >= this.pageCount() - 1;
        this.first.setEnabled(!firstPage);
        this.previous.setEnabled(!firstPage);
        this.next.setEnabled(!lastPage);
        this.last.setEnabled(!lastPage);
    }

    // For snapshots

    ListView<Listing> forSaleList() {
        return this.forSale;
    }

    ComboBox<Sort> sortBox() {
        return this.sort;
    }

    Button buyQuantityButton() {
        return this.buyQuantity;
    }

    void openOffer() {
        ExchangeDialogs.offer(this.ui, this.data, this::refreshListable);
    }
}
