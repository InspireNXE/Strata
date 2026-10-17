package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.swing.SwingVisuals;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Stand-in item icons for the sandbox: 16 by 16 images drawn from a color and a shape, shown through Swing visuals
 * the way Minecraft would show item stacks through its own.
 */
final class Icons {
    private static final Map<String, Visual> CACHE = new HashMap<>();

    private Icons() {
    }

    /**
     * Returns the icon for an item, made once per name.
     *
     * @param item the item
     * @return its icon
     */
    static Visual of(final Item item) {
        return CACHE.computeIfAbsent(item.name(), name -> SwingVisuals.image(draw(item.color(), item.shape())));
    }

    private static BufferedImage draw(final int color, final Item.Shape shape) {
        final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        final int dark = shade(color, 0.55f);
        final int light = shade(color, 1.35f);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                final boolean inside;
                final boolean edge;
                switch (shape) {
                    case BLOCK -> {
                        inside = x >= 1 && x <= 14 && y >= 1 && y <= 14;
                        edge = x == 1 || x == 14 || y == 1 || y == 14;
                    }
                    case GEM -> {
                        final int d = Math.abs(x - 8) + Math.abs(y - 8);
                        inside = d <= 7;
                        edge = d == 7;
                    }
                    case INGOT -> {
                        inside = y >= 5 && y <= 11 && x >= 1 + (11 - y) / 2 && x <= 14 - (11 - y) / 2;
                        edge = inside && (y == 5 || y == 11 || x == 1 + (11 - y) / 2 || x == 14 - (11 - y) / 2);
                    }
                    default -> {
                        final int dx = x - 8;
                        final int dy = y - 8;
                        inside = dx * dx + dy * dy <= 42;
                        edge = inside && dx * dx + dy * dy > 30;
                    }
                }
                if (inside) {
                    final boolean shine = !edge && x + y < 12;
                    image.setRGB(x, y, edge ? dark : shine ? light : color);
                }
            }
        }
        return image;
    }

    private static int shade(final int argb, final float factor) {
        final int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * factor));
        final int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * factor));
        final int b = Math.min(255, (int) ((argb & 0xFF) * factor));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
