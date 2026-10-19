package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.sandbox.almura.StoreData.StoreItem;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

/**
 * The Store's modal dialogs: the admin's list form for both sides of the store, and the buy or sell quantity
 * prompt.
 */
final class StoreDialogs {

    /**
     * One side of a listing, as the list form returns it.
     *
     * @param price the price of one
     * @param quantity the quantity, or {@link StoreData#INFINITE}
     */
    record Side(BigDecimal price, long quantity) {
    }

    private StoreDialogs() {
    }

    static void transactQuantity(final ComponentFactory ui, final StoreItem item, final boolean buy, final long max,
                                 final LongConsumer transact) {
        final TextBox amount = ui.textBox().setText("1").setSelectAllOnFocus(true);
        amount.setWidth(45);
        amount.setFilter(text -> {
            final String digits = text.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? "" : Long.toString(Math.min(Long.parseLong(digits.length() > 12 ? digits.substring(0, 12) : digits), max));
        });
        final Label total = Look.label(ui, Look.currency(item.price()), Look.WHITE);
        final Button confirm = Look.button(ui, buy ? "Buy" : "Sell", 40);
        final Button cancel = Look.button(ui, "Cancel", 40);
        final GridPanel grid = Look.grid(ui, GridLength.auto(), GridLength.weighted(1))
                .rows(GridLength.auto(), GridLength.auto(), GridLength.auto()).setSpacing(4, 3);
        grid.add(Look.label(ui, "Quantity:", Look.WHITE).setAnchor(Anchor.MIDDLE_LEFT), 0, 0)
                .add(amount.setAnchor(Anchor.MIDDLE_RIGHT), 0, 1)
                .add(Look.label(ui, "Per:", Look.WHITE), 1, 0)
                .add(Look.label(ui, Look.currency(item.price()), Look.WHITE).setAnchor(Anchor.TOP_RIGHT), 1, 1)
                .add(Look.label(ui, "Total:", Look.WHITE), 2, 0).add(total.setAnchor(Anchor.TOP_RIGHT), 2, 1);
        grid.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final Window window = ExchangeDialogs.window(ui, "Enter a Quantity", 130, grid, confirm, cancel);
        amount.onTextChanged(text -> {
            final long value = text.isEmpty() ? 0 : Long.parseLong(text);
            total.setText(Look.currency(item.price().multiply(BigDecimal.valueOf(value))));
            confirm.setEnabled(value >= 1 && value <= max);
        });
        final Runnable submit = () -> {
            if (confirm.isEnabled()) {
                transact.accept(Long.parseLong(amount.text()));
                window.close();
            }
        };
        amount.onSubmit(text -> submit.run());
        confirm.onAction(submit);
        cancel.onAction(window::close);
        ui.overlays().dialog(window);
    }

    static void list(final ComponentFactory ui, final Item item, final Optional<StoreItem> buying,
                     final Optional<StoreItem> selling, final BiConsumer<Optional<Side>, Optional<Side>> done) {
        final Column buy = new Column(ui, "Buy", buying, 1);
        final Column sell = new Column(ui, "Sell", selling, 3);
        final Button confirm = Look.button(ui, buying.isPresent() || selling.isPresent() ? "Modify" : "List", 50);
        final Button cancel = Look.button(ui, "Cancel", 50);
        final Runnable validate = () -> confirm.setEnabled(buy.value().isPresent() || sell.value().isPresent());
        buy.onChange(validate);
        sell.onChange(validate);
        validate.run();

        final GridPanel columns = Look.grid(ui, GridLength.weighted(1), GridLength.weighted(1))
                .rows(GridLength.weighted(1)).setSpacing(4, 0);
        columns.add(buy.panel, 0, 0).add(sell.panel, 0, 1);
        final Window window = ExchangeDialogs.window(ui, "List " + item.name(), 250, columns, confirm, cancel);
        confirm.onAction(() -> {
            done.accept(buy.value(), sell.value());
            window.close();
        });
        cancel.onAction(window::close);
        ui.overlays().dialog(window);
    }

    /**
     * One side of the list form: price, quantity, an infinite check box and the total.
     */
    private static final class Column {
        final GridPanel panel;
        private final TextBox price;
        private final TextBox quantity;
        private final CheckBox infinite;
        private final Label total;
        private Runnable listener;

        Column(final ComponentFactory ui, final String title, final Optional<StoreItem> existing, final int tabIndex) {
            this.price = ui.textBox().setFilter(text -> text.matches("\\d{0,8}(\\.\\d{0,2})?") ? text : null);
            this.price.setWidth(50);
            this.price.setTabIndex(tabIndex);
            this.quantity = ui.textBox().setFilter(text -> text.matches("\\d{0,8}") ? text : null);
            this.quantity.setWidth(50);
            this.quantity.setTabIndex(tabIndex + 1);
            this.infinite = ui.checkBox("Infinite");
            this.total = Look.label(ui, "", 0xFF999999);
            existing.ifPresent(item -> {
                this.price.setText(item.price().toPlainString());
                if (item.infinite()) {
                    this.infinite.setChecked(true);
                } else {
                    this.quantity.setText(Long.toString(item.quantity()));
                }
            });
            this.price.onTextChanged(text -> this.changed());
            this.quantity.onTextChanged(text -> this.changed());
            this.infinite.onCheckedChanged(checked -> this.changed());

            this.panel = Look.grid(ui, GridLength.weighted(1), GridLength.auto())
                    .rows(GridLength.auto(), GridLength.auto(), GridLength.auto(), GridLength.auto(), GridLength.auto(),
                            GridLength.weighted(1), GridLength.auto())
                    .setSpacing(2, 2);
            this.panel.add(Look.label(ui, title, Look.WHITE).setAnchor(Anchor.TOP_CENTER), 0, 0, 1, 2)
                    .add(ui.separator(Orientation.HORIZONTAL), 1, 0, 1, 2)
                    .add(this.right(Look.label(ui, "Price (per):", Look.WHITE)), 2, 0).add(this.price, 2, 1)
                    .add(this.right(Look.label(ui, "Quantity:", Look.WHITE)), 3, 0).add(this.quantity, 3, 1)
                    .add(this.infinite, 4, 1)
                    .add(this.row(ui), 6, 0, 1, 2);
            Look.panel(this.panel).setPadding(3);
            this.changed();
        }

        private Component<?> right(final Label label) {
            return label.setAnchor(Anchor.MIDDLE_RIGHT);
        }

        private Component<?> row(final ComponentFactory ui) {
            return Look.row(ui, 3, Look.label(ui, "Total:", Look.WHITE), this.total);
        }

        void onChange(final Runnable listener) {
            this.listener = listener;
        }

        private void changed() {
            final boolean endless = this.infinite.isChecked();
            this.quantity.setReadOnly(endless);
            if (endless) {
                this.quantity.setText("-1");
            } else if (this.quantity.text().equals("-1")) {
                this.quantity.setText("1");
            }
            this.total.setText(this.value().filter(side -> side.quantity() > 0)
                    .map(side -> Look.withSuffix(side.price().multiply(BigDecimal.valueOf(side.quantity())))).orElse(""));
            if (this.listener != null) {
                this.listener.run();
            }
        }

        Optional<Side> value() {
            if (this.price.text().isEmpty() || this.price.text().equals(".")) {
                return Optional.empty();
            }
            final BigDecimal price = new BigDecimal(this.price.text());
            if (price.signum() <= 0) {
                return Optional.empty();
            }
            if (this.infinite.isChecked()) {
                return Optional.of(new Side(price, StoreData.INFINITE));
            }
            if (this.quantity.text().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Side(price, Long.parseLong(this.quantity.text())));
        }
    }
}
