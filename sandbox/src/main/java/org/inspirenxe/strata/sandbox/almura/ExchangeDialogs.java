package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.sandbox.almura.ExchangeData.Listing;
import org.inspirenxe.strata.sandbox.almura.ExchangeData.Stock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * The Exchange's modal dialogs: buy a quantity, set a price, and move items between the inventory and the
 * exchange. Each opens with {@code Overlays.dialog}, so the market stays visible behind it.
 */
final class ExchangeDialogs {

    private ExchangeDialogs() {
    }

    /**
     * A small window with a content grid and a right-aligned row of buttons. The width is Almura's; the height
     * follows the content, since fonts differ between engines.
     */
    static Window window(final ComponentFactory ui, final String title, final float width,
                         final Component<?> content, final Button... buttons) {
        final Window window = ui.window(title);
        window.setWidth(width);
        final StackPanel row = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(2);
        for (int i = buttons.length - 1; i >= 0; i--) {
            row.add(buttons[i]);
        }
        row.setAlignment(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM);
        final GridPanel layout = Look.grid(ui, GridLength.weighted(1)).rows(GridLength.weighted(1), GridLength.auto())
                .setSpacing(0, 4);
        layout.add(content, 0, 0).add(row, 1, 0);
        window.add(layout);
        return window;
    }

    static void buyQuantity(final ComponentFactory ui, final Listing listing, final LongConsumer buy) {
        final TextBox amount = ui.textBox().setText("1").setSelectAllOnFocus(true);
        amount.setWidth(45);
        amount.onEvent(TextBox.EDIT, event -> {
            final String digits = event.value().replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                event.setValue("");
                return;
            }
            final long value = Long.parseLong(digits.length() > 12 ? digits.substring(0, 12) : digits);
            event.setValue(Long.toString(Math.min(value, listing.quantity())));
        });
        final Label per = Look.label(ui, Look.currency(listing.price()), Look.WHITE);
        final Label total = Look.label(ui, Look.currency(listing.price()), Look.WHITE);
        final Button confirm = Look.button(ui, "Buy", 40);
        final Button cancel = Look.button(ui, "Cancel", 40);

        final GridPanel grid = Look.grid(ui, GridLength.auto(), GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto(), GridLength.auto()).setSpacing(4, 3);
        grid.add(Look.label(ui, "Quantity:", Look.WHITE).setAnchor(Anchor.MIDDLE_LEFT), 0, 0)
                .add(amount.setAnchor(Anchor.MIDDLE_RIGHT), 0, 1)
                .add(Look.label(ui, "Per:", Look.WHITE), 1, 0).add(per.setAnchor(Anchor.TOP_RIGHT), 1, 1)
                .add(Look.label(ui, "Total:", Look.WHITE), 2, 0).add(total.setAnchor(Anchor.TOP_RIGHT), 2, 1);
        grid.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);

        final Window window = window(ui, "Enter a Quantity", 130, grid, confirm, cancel);
        final Runnable validate = () -> {
            final long value = amount.text().isEmpty() ? 0 : Long.parseLong(amount.text());
            total.setText(Look.currency(listing.price().multiply(BigDecimal.valueOf(value))));
            confirm.setEnabled(value > 0 && value <= listing.quantity());
        };
        amount.onEvent(TextBox.EDITED, event -> validate.run());
        final Runnable submit = () -> {
            if (confirm.isEnabled()) {
                buy.accept(Long.parseLong(amount.text()));
                window.close();
            }
        };
        amount.onEvent(TextBox.SUBMITTED, event -> submit.run());
        confirm.onEvent(Button.PRESSED, event -> submit.run());
        cancel.onEvent(Button.PRESSED, event -> window.close());
        ui.overlays().dialog(window);
    }

    static void listPrice(final ComponentFactory ui, final Stock stock, final Consumer<BigDecimal> list) {
        final TextBox price = ui.textBox().setSelectAllOnFocus(true);
        price.setText(stock.lastPrice() == null ? "" : stock.lastPrice().toPlainString());
        price.setWidth(65);
        price.onEvent(TextBox.EDIT, event -> {
            if (!event.value().matches("\\d{0,12}(\\.\\d{0,2})?")) {
                event.cancel();
            }
        });
        final Button confirm = Look.button(ui, "List", 40);
        final Button cancel = Look.button(ui, "Cancel", 40);
        final StackPanel row = Look.row(ui, 2, price, Look.label(ui, "/ea", Look.WHITE));
        final Window window = window(ui, "Enter a price", 124, row.setAnchor(Anchor.MIDDLE_LEFT), confirm, cancel);
        final Runnable validate = () -> {
            final BigDecimal value = parse(price.text());
            confirm.setEnabled(value != null && value.signum() > 0 && value.compareTo(BigDecimal.valueOf(1_000_000_000_000L)) <= 0);
        };
        price.onEvent(TextBox.EDITED, event -> validate.run());
        validate.run();
        final Runnable submit = () -> {
            if (confirm.isEnabled()) {
                list.accept(parse(price.text()));
                window.close();
            }
        };
        price.onEvent(TextBox.SUBMITTED, event -> submit.run());
        confirm.onEvent(Button.PRESSED, event -> submit.run());
        cancel.onEvent(Button.PRESSED, event -> window.close());
        ui.overlays().dialog(window);
    }

    private static BigDecimal parse(final String text) {
        try {
            return text.isEmpty() || text.equals(".") ? null : new BigDecimal(text);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Offer: move items between the inventory and the exchange
    // ------------------------------------------------------------------

    static void offer(final ComponentFactory ui, final ExchangeData data, final Runnable done) {
        new Offer(ui, data, done).open();
    }

    /**
     * The state of the offer dialog: copies of both sides, applied only on OK.
     */
    private static final class Offer {
        private static final int INVENTORY_SLOTS = 36;

        private final ComponentFactory ui;
        private final ExchangeData data;
        private final Runnable done;
        private final List<Stock> inventory;
        private final List<Stock> unlisted;
        private ListView<Stock> left;
        private ListView<Stock> right;
        private ProgressBar leftBar;
        private ProgressBar rightBar;
        private Label direction;
        private Button one;
        private Button stack;
        private Button type;
        private Button all;
        private boolean toRight;

        Offer(final ComponentFactory ui, final ExchangeData data, final Runnable done) {
            this.ui = ui;
            this.data = data;
            this.done = done;
            this.inventory = new ArrayList<>(data.inventory);
            this.unlisted = new ArrayList<>(data.mine.stream().filter(stock -> !stock.listed()).toList());
            this.toRight = true;
        }

        void open() {
            this.left = this.list();
            this.right = this.list();
            this.left.onEvent(ListView.SELECTED, e -> this.selected(true, this.left.selected()));
            this.right.onEvent(ListView.SELECTED, e -> this.selected(false, this.right.selected()));
            this.left.onEvent(ListView.ACTIVATED, e -> {
                final Stock stock = this.left.items().get(e.value());
                this.move(true, stock, stock.quantity());
            });
            this.right.onEvent(ListView.ACTIVATED, e -> {
                final Stock stock = this.right.items().get(e.value());
                this.move(false, stock, stock.quantity());
            });
            this.leftBar = this.ui.progressBar();
            this.rightBar = this.ui.progressBar();

            this.one = this.mover("1", "Move one");
            this.stack = this.mover("S", "Move the selected stack");
            this.type = this.mover("T", "Move every stack of this type");
            this.all = this.mover("A", "Move everything");
            this.one.onEvent(Button.PRESSED, event -> this.selection().ifPresent(stock -> this.move(this.toRight, stock, 1)));
            this.stack.onEvent(Button.PRESSED, event -> this.selection().ifPresent(stock -> this.move(this.toRight, stock, stock.quantity())));
            this.type.onEvent(Button.PRESSED, event -> this.selection().ifPresent(stock -> {
                for (final Stock same : List.copyOf(this.toRight ? this.inventory : this.unlisted)) {
                    if (same.item().equals(stock.item())) {
                        this.move(this.toRight, same, same.quantity());
                    }
                }
            }));
            this.all.onEvent(Button.PRESSED, event -> {
                for (final Stock each : List.copyOf(this.toRight ? this.inventory : this.unlisted)) {
                    this.move(this.toRight, each, each.quantity());
                }
            });
            this.direction = Look.label(this.ui, "->", Look.WHITE);

            final StackPanel middle = this.ui.stackPanel().setSpacing(4).add(this.one).add(this.stack)
                    .add(this.direction.setAnchor(Anchor.TOP_CENTER)).add(this.type).add(this.all);
            Look.panel(middle).setPadding(Look.pad(9, 6));
            middle.setAnchor(Anchor.CENTER);

            final GridPanel grid = Look.grid(this.ui, GridLength.weighted(1), GridLength.fixed(40), GridLength.weighted(1))
                    .rows(GridLength.auto(), GridLength.weighted(1), GridLength.auto()).setSpacing(4, 3);
            grid.add(Look.label(this.ui, "Inventory", Look.WHITE).setAnchor(Anchor.TOP_CENTER), 0, 0)
                    .add(Look.label(this.ui, "Unlisted Items", Look.WHITE).setAnchor(Anchor.TOP_CENTER), 0, 2)
                    .add(this.left, 1, 0).add(middle, 1, 1).add(this.right, 1, 2)
                    .add(this.leftBar.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP), 2, 0)
                    .add(this.rightBar.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP), 2, 2);

            final Button ok = Look.button(this.ui, "OK", 40);
            final Button cancel = Look.button(this.ui, "Cancel", 40);
            final Window window = window(this.ui, "Offer", 400, grid, ok, cancel);
            window.setHeight(325);
            ok.onEvent(Button.PRESSED, event -> {
                this.data.inventory.clear();
                this.data.inventory.addAll(this.inventory);
                this.data.mine.removeIf(stock -> !stock.listed());
                this.data.mine.addAll(this.unlisted);
                this.done.run();
                window.close();
            });
            cancel.onEvent(Button.PRESSED, event -> window.close());
            this.refresh();
            if (!this.inventory.isEmpty()) {
                this.left.select(0);
            } else if (!this.unlisted.isEmpty()) {
                this.right.select(0);
            }
            this.ui.overlays().dialog(window);
        }

        private ListView<Stock> list() {
            final ListView<Stock> list = this.ui.<Stock>listView(this::cell).setRowHeight(24).setRowSpacing(1)
                    .setKeyFunction(Stock::record);
            Look.panel(list).setAnchor(Anchor.FILL);
            return list;
        }

        private Component<?> cell(final Stock stock) {
            final GridPanel cell = Look.grid(this.ui, GridLength.fixed(16), GridLength.weighted(1), GridLength.auto())
                    .rows(GridLength.weighted(1)).setSpacing(4, 0);
            cell.setPadding(Look.pad(3, 0));
            cell.add(this.ui.image(Icons.of(stock.item())).setTooltip(stock.item().tooltip()).setAnchor(Anchor.MIDDLE_LEFT), 0, 0)
                    .add(Look.label(this.ui, stock.item().name(), Look.WHITE).setOverflow(TextOverflow.ELLIPSIS)
                            .setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.MIDDLE), 0, 1)
                    .add(Look.label(this.ui, " x " + Look.withSuffix(stock.quantity()), Look.GRAY).setAnchor(Anchor.MIDDLE_LEFT), 0, 2);
            return cell;
        }

        private Button mover(final String text, final String tooltip) {
            final Button button = Look.button(this.ui, text, 20);
            button.setHeight(20);
            button.setTooltip(tooltip);
            button.setEnabled(false);
            return button;
        }

        private Optional<Stock> selection() {
            return this.toRight ? this.left.selected() : this.right.selected();
        }

        private void selected(final boolean fromLeft, final Optional<Stock> selected) {
            if (selected.isEmpty()) {
                return;
            }
            this.toRight = fromLeft;
            (fromLeft ? this.right : this.left).select(-1);
            this.direction.setText(fromLeft ? "->" : "<-");
            final String target = fromLeft ? "the exchange" : "your inventory";
            this.one.setTooltip("Move one to " + target);
            this.stack.setTooltip("Move the selected stack to " + target);
            this.type.setTooltip("Move every stack of this type to " + target);
            this.all.setTooltip("Move everything to " + target);
            for (final Button button : List.of(this.one, this.stack, this.type, this.all)) {
                button.setEnabled(true);
            }
        }

        private void move(final boolean toRight, final Stock stock, final long amount) {
            final List<Stock> from = toRight ? this.inventory : this.unlisted;
            final List<Stock> to = toRight ? this.unlisted : this.inventory;
            final int toLimit = toRight ? this.data.limit - (int) this.data.listedCount() : INVENTORY_SLOTS;
            final long moved = Math.min(amount, stock.quantity());
            final Optional<Stock> merge = to.stream().filter(other -> other.item().equals(stock.item())).findFirst();
            if (merge.isEmpty() && to.size() >= toLimit) {
                return;
            }
            ExchangeData.replace(from, stock, old -> old.quantity() - moved <= 0 ? null
                    : new Stock(old.record(), old.item(), old.quantity() - moved, null, old.lastPrice()));
            if (merge.isPresent()) {
                ExchangeData.replace(to, merge.get(), old -> new Stock(old.record(), old.item(), old.quantity() + moved, null, old.lastPrice()));
            } else {
                to.add(new Stock(this.data.newRecord(), stock.item(), moved, null, stock.lastPrice()));
            }
            this.refresh();
        }

        private void refresh() {
            this.left.setItems(this.inventory);
            this.right.setItems(this.unlisted);
            final int rightLimit = this.data.limit - (int) this.data.listedCount();
            this.leftBar.setValue(this.inventory.size() / (float) INVENTORY_SLOTS).setText(this.inventory.size() + "/" + INVENTORY_SLOTS);
            this.rightBar.setValue(rightLimit <= 0 ? 1f : this.unlisted.size() / (float) rightLimit)
                    .setText(this.unlisted.size() + "/" + rightLimit);
        }
    }
}
