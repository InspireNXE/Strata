package org.inspirenxe.strata.core.text;

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy word wrapping using an engine's text metrics.
 */
public final class TextWrap {

    private TextWrap() {
    }

    /**
     * Splits {@code text} into lines no wider than {@code maxWidth}. Explicit newlines always break. A word wider
     * than a whole line is broken between characters. An infinite width only splits at newlines.
     */
    public static List<String> wrap(final String text, final TextStyle style, final float maxWidth, final TextMetrics metrics) {
        final List<String> lines = new ArrayList<>();
        for (final String paragraph : text.split("\n", -1)) {
            if (Float.isInfinite(maxWidth)) {
                lines.add(paragraph);
            } else {
                wrapParagraph(paragraph, style, maxWidth, metrics, lines);
            }
        }
        return lines;
    }

    private static void wrapParagraph(final String paragraph, final TextStyle style, final float maxWidth,
                                      final TextMetrics metrics, final List<String> out) {
        if (paragraph.isEmpty()) {
            out.add("");
            return;
        }

        final StringBuilder line = new StringBuilder();
        for (final String word : paragraph.split(" ", -1)) {
            final String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(candidate, style, metrics) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                out.add(line.toString());
                line.setLength(0);
            }
            // The word alone may still be too wide: break it between characters
            String rest = word;
            while (!rest.isEmpty() && width(rest, style, metrics) > maxWidth) {
                final int fit = Math.max(1, fittingPrefix(rest, style, maxWidth, metrics));
                out.add(rest.substring(0, fit));
                rest = rest.substring(fit);
            }
            line.append(rest);
        }
        out.add(line.toString());
    }

    /**
     * The number of chars from the start of {@code text} that fit in {@code maxWidth}, never splitting a
     * surrogate pair.
     */
    public static int fittingPrefix(final String text, final TextStyle style, final float maxWidth, final TextMetrics metrics) {
        int fit = 0;
        int index = 0;
        while (index < text.length()) {
            final int next = text.offsetByCodePoints(index, 1);
            if (width(text.substring(0, next), style, metrics) > maxWidth) {
                break;
            }
            fit = next;
            index = next;
        }
        return fit;
    }

    private static float width(final String text, final TextStyle style, final TextMetrics metrics) {
        return metrics.width(new TextSpan(text, style));
    }
}
