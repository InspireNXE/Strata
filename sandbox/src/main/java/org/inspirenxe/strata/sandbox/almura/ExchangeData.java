package org.inspirenxe.strata.sandbox.almura;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;

/**
 * Pretend Exchange data, standing in for what Almura's server sends: items for sale on the market, and the
 * player's own items that can be listed.
 */
final class ExchangeData {
    static final String PLAYER = "Zidane";
    static final int PAGE_SIZE = 10;

    final String name;
    final int limit;
    final List<Listing> market;
    final List<Stock> mine;
    final List<Stock> inventory;
    private int nextRecord;

    /**
     * An item for sale.
     *
     * @param record the server's record number, which keeps the selection across refreshes
     * @param item the item
     * @param quantity how many are for sale
     * @param price the price of one
     * @param seller who sells it
     * @param created when it was listed, as an order
     */
    record Listing(int record, Item item, long quantity, BigDecimal price, String seller, int created) {
    }

    /**
     * One of the player's listable items.
     *
     * @param record the server's record number
     * @param item the item
     * @param quantity how many the player has put in
     * @param price the price it is listed at, or null when it is not for sale
     * @param lastPrice the price it was last listed at, or null
     */
    record Stock(int record, Item item, long quantity, BigDecimal price, BigDecimal lastPrice) {
        boolean listed() {
            return this.price != null;
        }
    }

    /**
     * How the market can be sorted.
     */
    enum Sort {
        CREATED_ASC("Created ascending", Comparator.comparingInt(Listing::created)),
        CREATED_DESC("Created descending", Comparator.comparingInt(Listing::created).reversed()),
        PRICE_ASC("Price ascending", Comparator.comparing(Listing::price)),
        PRICE_DESC("Price descending", Comparator.comparing(Listing::price).reversed()),
        NAME_ASC("Item name ascending", Comparator.comparing((Listing listing) -> listing.item().name())),
        NAME_DESC("Item name descending", Comparator.comparing((Listing listing) -> listing.item().name()).reversed()),
        SELLER_ASC("Seller ascending", Comparator.comparing(Listing::seller)),
        SELLER_DESC("Seller descending", Comparator.comparing(Listing::seller).reversed());

        final String label;
        final Comparator<Listing> order;

        Sort(final String label, final Comparator<Listing> order) {
            this.label = label;
            this.order = order;
        }
    }

    ExchangeData(final String name, final int limit) {
        this.name = name;
        this.limit = limit;
        this.market = new ArrayList<>();
        this.mine = new ArrayList<>();
        this.inventory = new ArrayList<>();
        final Random random = new Random(1337);
        final String[] sellers = {"Dockter", "Grinch", "Zidane", "Mumfrey", "Steve", "Alex", "Notch"};
        for (int i = 0; i < 34; i++) {
            final Item item = Item.ALL.get(random.nextInt(Item.ALL.size()));
            final long quantity = i == 3 ? 2_500_000L : 1 + random.nextInt(item.maxStack() * 4);
            final BigDecimal price = BigDecimal.valueOf(5 + random.nextInt(40_000), 2);
            final String seller = sellers[random.nextInt(sellers.length)];
            this.market.add(new Listing(this.nextRecord++, item, quantity, price, seller, i));
        }
        this.mine.add(new Stock(this.nextRecord++, Item.ALL.get(0), 12, new BigDecimal("125.50"),
                new BigDecimal("125.50")));
        this.mine.add(new Stock(this.nextRecord++, Item.ALL.get(9), 128, null, new BigDecimal("2.00")));
        this.mine.add(new Stock(this.nextRecord++, Item.ALL.get(13), 3, null, null));
        this.mine.add(new Stock(this.nextRecord++, Item.ALL.get(19), 1, new BigDecimal("4999.99"), null));
        for (final int index : new int[] {2, 3, 7, 8, 11, 14, 16, 18, 20}) {
            final Item item = Item.ALL.get(index);
            final long quantity = Math.min(item.maxStack(), 1 + random.nextInt(64));
            this.inventory.add(new Stock(this.nextRecord++, item, quantity, null, null));
        }
    }

    int newRecord() {
        return this.nextRecord++;
    }

    /**
     * Filters and sorts the market, as the server does for a search.
     */
    List<Listing> search(final String itemName, final String seller, final Sort sort) {
        final String name = itemName.toLowerCase(Locale.ROOT);
        final String who = seller.toLowerCase(Locale.ROOT);
        return this.market.stream()
                .filter(listing -> listing.item().name().toLowerCase(Locale.ROOT).contains(name))
                .filter(listing -> listing.seller().toLowerCase(Locale.ROOT).contains(who))
                .sorted(sort.order)
                .toList();
    }

    long listedCount() {
        return this.mine.stream().filter(Stock::listed).count();
    }

    static <T> void replace(final List<T> list, final T old, final Function<T, T> change) {
        final int index = list.indexOf(old);
        if (index >= 0) {
            final T next = change.apply(old);
            if (next == null) {
                list.remove(index);
            } else {
                list.set(index, next);
            }
        }
    }
}
