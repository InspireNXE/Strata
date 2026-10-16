package org.inspirenxe.strata.examples.adapter;

// --8<-- [start:metrics]
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

/**
 * Text metrics for an engine with a fixed-width bitmap font: every code point is 6 units wide, one more when bold,
 * and lines are 9 units tall.
 */
public final class MonospaceTextMetrics implements TextMetrics {

    @Override
    public float width(final TextSpan span) {
        final int advance = span.style().bold() ? 7 : 6;
        return span.text().codePointCount(0, span.text().length()) * (float) advance;
    }

    @Override
    public float lineHeight(final TextStyle style) {
        return 9f;
    }
}
// --8<-- [end:metrics]
