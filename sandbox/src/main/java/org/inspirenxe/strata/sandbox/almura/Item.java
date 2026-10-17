package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.text.TextSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A pretend item type, standing in for a Minecraft {@code ItemStack} with a count of one.
 *
 * @param name the display name
 * @param color the icon's main color
 * @param shape the icon's outline
 * @param maxStack the largest stack
 * @param category the creative tab it belongs to
 */
record Item(String name, int color, Shape shape, int maxStack, String category) {

    /**
     * An icon outline.
     */
    enum Shape { BLOCK, GEM, INGOT, ROUND }

    /**
     * The item's tooltip, laid out like vanilla's: the name, then the mod in blue italics.
     *
     * @return the tooltip
     */
    Tooltip tooltip() {
        final List<List<TextSpan>> lines = new ArrayList<>();
        lines.add(List.of(Look.span(this.name, Look.WHITE)));
        if (this.maxStack < 64) {
            lines.add(List.of(Look.span("Stacks to " + this.maxStack, Look.GRAY)));
        }
        lines.add(List.of(new TextSpan("Minecraft", Look.text(0xFF5555FF).withItalic(true))));
        return Tooltip.lines(lines);
    }

    static final List<Item> ALL = List.of(
            new Item("Diamond", 0xFF4AEDD9, Shape.GEM, 64, "Ingredients"),
            new Item("Emerald", 0xFF17DD62, Shape.GEM, 64, "Ingredients"),
            new Item("Iron Ingot", 0xFFD8D8D8, Shape.INGOT, 64, "Ingredients"),
            new Item("Gold Ingot", 0xFFFAD64A, Shape.INGOT, 64, "Ingredients"),
            new Item("Netherite Ingot", 0xFF4D494D, Shape.INGOT, 64, "Ingredients"),
            new Item("Copper Ingot", 0xFFE77C56, Shape.INGOT, 64, "Ingredients"),
            new Item("Redstone Dust", 0xFFC81E1E, Shape.ROUND, 64, "Redstone"),
            new Item("Lapis Lazuli", 0xFF2E5BD8, Shape.GEM, 64, "Ingredients"),
            new Item("Coal", 0xFF2B2B2B, Shape.ROUND, 64, "Ingredients"),
            new Item("Oak Log", 0xFF9C7A48, Shape.BLOCK, 64, "Building Blocks"),
            new Item("Spruce Planks", 0xFF7A5A34, Shape.BLOCK, 64, "Building Blocks"),
            new Item("Cobblestone", 0xFF8A8A8A, Shape.BLOCK, 64, "Building Blocks"),
            new Item("Glass", 0xFFCDE8F0, Shape.BLOCK, 64, "Building Blocks"),
            new Item("Golden Apple", 0xFFF5C64C, Shape.ROUND, 64, "Food & Drinks"),
            new Item("Bread", 0xFFC79A4E, Shape.ROUND, 64, "Food & Drinks"),
            new Item("Cooked Beef", 0xFF8E4A2B, Shape.ROUND, 64, "Food & Drinks"),
            new Item("Ender Pearl", 0xFF1F7A6C, Shape.ROUND, 16, "Ingredients"),
            new Item("Diamond Sword", 0xFF4AEDD9, Shape.GEM, 1, "Combat"),
            new Item("Iron Pickaxe", 0xFFD8D8D8, Shape.INGOT, 1, "Tools & Utilities"),
            new Item("Enchanted Book of Mending With a Long Name", 0xFF9B4FD8, Shape.BLOCK, 1, "Tools & Utilities"),
            new Item("Torch", 0xFFFFC34D, Shape.ROUND, 64, "Decoration Blocks"),
            new Item("Lantern", 0xFF7D6F5A, Shape.BLOCK, 64, "Decoration Blocks"),
            new Item("Hopper", 0xFF4A4A4A, Shape.BLOCK, 64, "Redstone"),
            new Item("Shulker Box", 0xFF9E6C9E, Shape.BLOCK, 1, "Decoration Blocks"));

    /**
     * Returns every item in a creative tab, padded with numbered variants so large tabs exercise list
     * virtualization.
     *
     * @param category the tab
     * @return the tab's items, sorted by name
     */
    static List<Item> inCategory(final String category) {
        final List<Item> items = new ArrayList<>(ALL.stream().filter(item -> item.category.equals(category)).toList());
        if (category.equals("Building Blocks")) {
            final Random random = new Random(26);
            final String[] stones = {"Andesite", "Basalt", "Blackstone", "Brick", "Calcite", "Deepslate", "Diorite",
                    "Granite", "Mud Brick", "Prismarine", "Purpur", "Quartz", "Sandstone", "Terracotta", "Tuff"};
            final String[] forms = {"", " Slab", " Stairs", " Wall", " Bricks", " Tiles", " Pillar"};
            for (final String stone : stones) {
                for (final String form : forms) {
                    final int shade = 0x60 + random.nextInt(0x80);
                    items.add(new Item(stone + form, 0xFF000000 | (shade << 16) | ((shade - 10) << 8) | (shade - 20),
                            Shape.BLOCK, 64, category));
                }
            }
        }
        items.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return items;
    }

    static final List<String> CATEGORIES = List.of("Building Blocks", "Decoration Blocks", "Redstone",
            "Tools & Utilities", "Combat", "Food & Drinks", "Ingredients");
}
