package org.inspirenxe.strata.sandbox;

import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.swing.StrataPanel;
import org.inspirenxe.strata.swing.SwingFonts;
import org.inspirenxe.strata.swing.SwingPlatform;
import org.inspirenxe.strata.swing.SwingSnapshot;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Opens the demo screen in a window. {@code --snapshot <file.png>} renders one frame to a PNG instead, without a
 * display.
 */
public final class Sandbox {
    private static final float SCALE = 2f;

    private Sandbox() {
    }

    public static void main(final String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--snapshot")) {
            final File out = new File(args[1]);
            ImageIO.write(SwingSnapshot.render(new DemoScreen(), Themes.flat(), 960, 600, SCALE), "png", out);
            System.out.println("Wrote " + out.getAbsolutePath());
            return;
        }
        SwingUtilities.invokeLater(Sandbox::openWindow);
    }

    private static void openWindow() {
        final SwingPlatform platform = new SwingPlatform(SwingFonts.defaults());
        final StrataPanel panel = new StrataPanel(platform, Themes.flat(), SCALE);
        panel.screens().push(new DemoScreen());

        final JFrame frame = new JFrame("Strata sandbox");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
    }
}
