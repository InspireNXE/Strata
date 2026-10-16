package org.inspirenxe.strata.examples;

import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.examples.events.EventExamples;
import org.inspirenxe.strata.examples.extending.ExtendingExamples;
import org.inspirenxe.strata.examples.layout.LayoutExamples;
import org.inspirenxe.strata.examples.quickstart.SettingsScreen;
import org.inspirenxe.strata.examples.screens.ScreenExamples;
import org.inspirenxe.strata.examples.theming.ThemeExamples;
import org.inspirenxe.strata.swing.SwingSnapshot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Renders the documentation's example screens with the Swing adapter, at 2 pixels per unit, onto the sandbox's
 * background color. Run it with {@code ./gradlew :docs-examples:renderDocImages}.
 */
public final class RenderDocImages {
    private static final float SCALE = 2f;
    private static final Color BACKDROP = new Color(0xFF1E1F22, true);

    private RenderDocImages() {
    }

    public static void main(final String[] args) throws IOException {
        final File out = new File(args[0]);
        if (!out.isDirectory() && !out.mkdirs()) {
            throw new IOException("Cannot create " + out);
        }
        final Theme flat = Themes.flat();
        render(out, "quickstart", new SettingsScreen(), flat, 320, 160);
        render(out, "layout-anchor", LayoutExamples.anchorPanel(), flat, 320, 160);
        render(out, "layout-stack", LayoutExamples.stackPanel(), flat, 320, 140);
        render(out, "layout-grid", LayoutExamples.gridPanel(), flat, 320, 120);
        render(out, "layout-weighted", LayoutExamples.weightedGrid(), flat, 320, 140);
        render(out, "layout-spacer", LayoutExamples.spacer(), flat, 320, 40);
        render(out, "layout-absolute", LayoutExamples.absolutePanel(), flat, 320, 110);
        render(out, "layout-spacing", LayoutExamples.marginAndPadding(), flat, 320, 80);
        render(out, "layout-wrap", LayoutExamples.wrapping(), flat, 320, 140);
        render(out, "layout-sizes", LayoutExamples.sizes(), flat, 320, 150);
        render(out, "events-drag", EventExamples.dragging(), flat, 320, 80);
        render(out, "extending-custom", ExtendingExamples.customComponents(), ExtendingExamples.checkBoxTheme(), 320, 190);
        render(out, "screens-list", new ScreenExamples.ShoppingList(), flat, 320, 100);
        render(out, "theme-flat", ThemeExamples.sampler(0xFF2B2D31), flat, 200, 120);
        render(out, "theme-derived", ThemeExamples.sampler(0xFF2B2D31), ThemeExamples.derived(), 200, 120);
        render(out, "theme-light", ThemeExamples.sampler(0xFFF5F5F5), ThemeExamples.fromScratch(), 200, 120);
    }

    private static void render(final File dir, final String name, final Screen screen, final Theme theme,
                               final int width, final int height) throws IOException {
        final BufferedImage frame = SwingSnapshot.render(screen, theme, (int) (width * SCALE), (int) (height * SCALE), SCALE);
        final BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor(BACKDROP);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(frame, 0, 0, null);
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", new File(dir, name + ".png"));
    }
}
