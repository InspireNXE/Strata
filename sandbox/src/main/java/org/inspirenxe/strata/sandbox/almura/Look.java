package org.inspirenxe.strata.sandbox.almura;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.Label;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Almura's look and its number formats, shared by the Exchange and Store mock-ups. The colors follow Almura's
 * MalisisCore forms: black panels at alpha 215 with a white border at alpha 185, and a 16-color text palette.
 */
final class Look {
    static final int WHITE = 0xFFFFFFFF;
    static final int GRAY = 0xFFAAAAAA;
    static final int DARK_GRAY = 0xFF555555;
    static final int GOLD = 0xFFFFAA00;
    static final int AQUA = 0xFF55FFFF;
    static final int GREEN = 0xFF55FF55;
    static final int DARK_GREEN = 0xFF00AA00;
    static final int RED = 0xFFFF5555;
    static final int DARK_RED = 0xFFAA0000;

    static final Paint PANEL = Paint.solid(0xD7000000);
    static final Border PANEL_BORDER = Border.of(0xB9FFFFFF, 1f);

    private static final DecimalFormat CURRENCY = new DecimalFormat("#,###.##");

    private Look() {
    }

    static TextStyle text(final int color) {
        return TextStyle.DEFAULT.withColor(color);
    }

    static TextSpan span(final String text, final int color) {
        return new TextSpan(text, text(color));
    }

    static Label label(final ComponentFactory ui, final String text, final int color) {
        return ui.label(text).setStyle(text(color));
    }

    /**
     * A label made of colored runs, cut short with dots when it does not fit.
     */
    static Label rich(final ComponentFactory ui, final TextSpan... spans) {
        return ui.label("").setSpans(List.of(spans)).setOverflow(TextOverflow.ELLIPSIS);
    }

    /**
     * Almura's bordered black panel.
     */
    static <T extends Component<T>> T panel(final T component) {
        component.setBackground(PANEL);
        return component.setBorder(PANEL_BORDER);
    }

    static Button button(final ComponentFactory ui, final String text, final float width) {
        final Button button = ui.button(text);
        button.setWidth(width);
        return button;
    }

    static GridPanel grid(final ComponentFactory ui, final GridLength... columns) {
        final GridPanel grid = ui.gridPanel().columns(columns);
        grid.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.STRETCH);
        return grid;
    }

    static StackPanel row(final ComponentFactory ui, final float spacing, final Component<?>... children) {
        final StackPanel row = ui.stackPanel(Orientation.HORIZONTAL).setSpacing(spacing);
        for (final Component<?> child : children) {
            row.add(child.setAnchor(Anchor.MIDDLE_LEFT));
        }
        return row;
    }

    static Insets pad(final float horizontal, final float vertical) {
        return Insets.of(horizontal, vertical);
    }

    /**
     * Almura's {@code withSuffix}: plain below a million, then m, b and t.
     */
    static String withSuffix(final BigDecimal value) {
        final BigDecimal million = BigDecimal.valueOf(1_000_000L);
        if (value.compareTo(million) < 0) {
            return CURRENCY.format(value);
        }
        final String[] suffixes = {"m", "b", "t"};
        BigDecimal scaled = value.divide(million, 2, RoundingMode.DOWN);
        int index = 0;
        while (scaled.compareTo(BigDecimal.valueOf(1000)) >= 0 && index < suffixes.length - 1) {
            scaled = scaled.divide(BigDecimal.valueOf(1000), 2, RoundingMode.DOWN);
            index++;
        }
        return CURRENCY.format(scaled) + suffixes[index];
    }

    static String withSuffix(final long value) {
        return withSuffix(BigDecimal.valueOf(value));
    }

    static String currency(final BigDecimal value) {
        return CURRENCY.format(value);
    }
}
