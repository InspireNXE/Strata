package org.inspirenxe.strata.sandbox.almura;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pretend Store data. "Buying" items are what the player buys from the store (the Buy tab); "selling" items are
 * what the store buys from the player (the Sell tab), as Almura names them.
 */
final class StoreData {
    /**
     * A quantity that never runs out.
     */
    static final long INFINITE = -1;

    final String name;
    final List<StoreItem> buying;
    final List<StoreItem> selling;
    final List<Item> held;
    private int nextRecord;

    /**
     * One listing on one side of the store.
     *
     * @param record the server's record number
     * @param item the item
     * @param quantity how many are left, or {@link #INFINITE}
     * @param price the price of one
     */
    record StoreItem(int record, Item item, long quantity, BigDecimal price) {
        boolean infinite() {
            return this.quantity == INFINITE;
        }
    }

    /**
     * How the store's list can be sorted.
     */
    enum Sort {
        NAME_ASC("Name ascending", Comparator.comparing((StoreItem item) -> item.item().name())),
        NAME_DESC("Name descending", Comparator.comparing((StoreItem item) -> item.item().name()).reversed()),
        PRICE_ASC("Price ascending", Comparator.comparing(StoreItem::price)),
        PRICE_DESC("Price descending", Comparator.comparing(StoreItem::price).reversed());

        final String label;
        final Comparator<StoreItem> order;

        Sort(final String label, final Comparator<StoreItem> order) {
            this.label = label;
            this.order = order;
        }
    }

    StoreData(final String name) {
        this.name = name;
        this.buying = new ArrayList<>();
        this.selling = new ArrayList<>();
        this.held = new ArrayList<>(List.of(Item.ALL.get(9), Item.ALL.get(9), Item.ALL.get(11), Item.ALL.get(14)));
        this.add(this.buying, 13, INFINITE, "120");
        this.add(this.buying, 14, 64, "2.50");
        this.add(this.buying, 15, 0, "7.25");
        this.add(this.buying, 20, INFINITE, "0.75");
        this.add(this.buying, 21, 12, "18");
        this.add(this.buying, 16, 3, "45");
        this.add(this.buying, 18, 5, "350");
        this.add(this.buying, 10, 1_250_000, "1.10");
        this.add(this.selling, 9, INFINITE, "1.25");
        this.add(this.selling, 11, INFINITE, "0.10");
        this.add(this.selling, 14, 128, "1.75");
    }

    private void add(final List<StoreItem> side, final int item, final long quantity, final String price) {
        side.add(new StoreItem(this.nextRecord++, Item.ALL.get(item), quantity, new BigDecimal(price)));
    }

    int newRecord() {
        return this.nextRecord++;
    }

    List<StoreItem> side(final boolean buy) {
        return buy ? this.buying : this.selling;
    }

    List<StoreItem> search(final boolean buy, final String name, final Sort sort) {
        final String lower = name.toLowerCase(Locale.ROOT);
        return this.side(buy).stream()
                .filter(item -> item.item().name().toLowerCase(Locale.ROOT).contains(lower))
                .sorted(sort.order)
                .toList();
    }

    int heldCount(final Item item) {
        return (int) this.held.stream().filter(item::equals).count() * 32;
    }
}
