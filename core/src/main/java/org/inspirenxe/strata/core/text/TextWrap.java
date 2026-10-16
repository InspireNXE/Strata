package org.inspirenxe.strata.core.text;

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Greedy word wrapping with an engine's text metrics. Labels use it to measure and paint wrapped text.
 */
public final class TextWrap {

    private TextWrap() {
    }

    /**
     * Splits text into lines no wider than a width. Each line takes as many space-separated words as fit. Newlines
     * always break, and a word wider than a whole line breaks between code points. The space at each wrap point is
     * dropped; other spaces are kept.
     *
     * <p>A single code point wider than the width still gets a line of its own, so the result can be wider than
     * {@code maxWidth} when the width is smaller than one character.</p>
     *
     * @param text the text, with {@code \n} as the only line break
     * @param style the style to measure with
     * @param maxWidth the widest a line may be, in UI units; {@link Float#POSITIVE_INFINITY} splits only at newlines
     * @param metrics the engine's text metrics
     * @return the lines in order, at least one; an empty paragraph gives an empty line
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
            // The word alone may still be too wide: break it between code points, at least one per line
            String rest = word;
            while (!rest.isEmpty() && width(rest, style, metrics) > maxWidth) {
                final int prefix = fittingPrefix(rest, style, maxWidth, metrics);
                final int fit = prefix > 0 ? prefix : rest.offsetByCodePoints(0, 1);
                out.add(rest.substring(0, fit));
                rest = rest.substring(fit);
            }
            line.append(rest);
        }
        out.add(line.toString());
    }

    /**
     * Returns how much of the start of a string fits in a width, in whole code points.
     *
     * @param text the text
     * @param style the style to measure with
     * @param maxWidth the width to fit, in UI units
     * @param metrics the engine's text metrics
     * @return the length in {@code char}s of the longest fitting prefix, never splitting a surrogate pair; 0 if
     *     not even the first code point fits
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
