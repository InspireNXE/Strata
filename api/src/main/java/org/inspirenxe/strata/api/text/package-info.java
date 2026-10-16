/**
 * Styled text: spans of characters with a {@link org.inspirenxe.strata.api.text.TextStyle}, and the
 * {@link org.inspirenxe.strata.api.text.TextMetrics} that measures them.
 *
 * <p>Text values carry no engine objects. The engine adapter decides the font, renders the span and measures it,
 * so the same span is a little wider on one engine than another. Build layouts that measure text through
 * {@code TextMetrics} instead of assuming character widths.</p>
 */
package org.inspirenxe.strata.api.text;
