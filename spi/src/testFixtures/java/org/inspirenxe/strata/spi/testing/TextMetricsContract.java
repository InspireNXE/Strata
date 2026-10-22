/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.spi.testing;

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks a {@link TextMetrics} against the requirements in its documentation. Extend it in an adapter's tests and
 * return the adapter's metrics from {@link #metrics()}.
 *
 * <pre>{@code
 * class SwingTextMetricsTest extends TextMetricsContract {
 *     protected TextMetrics metrics() {
 *         return SwingTextMetrics.defaults();
 *     }
 * }
 * }</pre>
 */
public abstract class TextMetricsContract {
    /**
     * Text the checks measure: plain words, punctuation, digits, accented letters and an emoji outside the Basic
     * Multilingual Plane.
     */
    protected static final List<String> SAMPLES = List.of(
            "a", "Hello, world", "The quick brown fox jumps over the lazy dog", "0123456789", "!@#$%^&*()",
            "Grüße, señor", "smile 😀 now", "    ", "W"
    );

    /**
     * Styles the checks measure in: plain, bold, italic, both, decorated and shadowed.
     */
    protected static final List<TextStyle> STYLES = List.of(
            TextStyle.DEFAULT,
            TextStyle.DEFAULT.withBold(true),
            TextStyle.DEFAULT.withItalic(true),
            TextStyle.DEFAULT.withBold(true).withItalic(true),
            TextStyle.DEFAULT.withUnderline(true).withStrikethrough(true),
            TextStyle.DEFAULT.withShadow(true)
    );

    /**
     * Returns the metrics under test. Called once per check.
     *
     * @return the adapter's text metrics
     */
    protected abstract TextMetrics metrics();

    @Test
    void emptyTextIsZeroWide() {
        final TextMetrics metrics = this.metrics();
        for (final TextStyle style : STYLES) {
            assertEquals(0f, metrics.width(new TextSpan("", style)), 0.001f, "empty text in " + style);
        }
    }

    @Test
    void widthIsNeverNegative() {
        final TextMetrics metrics = this.metrics();
        for (final TextStyle style : STYLES) {
            for (final String sample : SAMPLES) {
                assertTrue(metrics.width(new TextSpan(sample, style)) >= 0f, sample + " in " + style);
            }
        }
    }

    @Test
    void visibleTextHasWidth() {
        final TextMetrics metrics = this.metrics();
        assertTrue(metrics.width(TextSpan.of("W")) > 0f, "a visible character takes space");
    }

    @Test
    void widthNeverShrinksAsTextGrows() {
        final TextMetrics metrics = this.metrics();
        for (final TextStyle style : STYLES) {
            for (final String sample : SAMPLES) {
                float previous = 0f;
                int index = 0;
                while (index < sample.length()) {
                    index = sample.offsetByCodePoints(index, 1);
                    final float width = metrics.width(new TextSpan(sample.substring(0, index), style));
                    assertTrue(width + 0.001f >= previous,
                            "prefix '" + sample.substring(0, index) + "' in " + style + " is narrower than the one before");
                    previous = width;
                }
            }
        }
    }

    @Test
    void sameSpanMeasuresTheSame() {
        final TextMetrics metrics = this.metrics();
        for (final String sample : SAMPLES) {
            final TextSpan span = TextSpan.of(sample);
            assertEquals(metrics.width(span), metrics.width(span), 0f, sample);
        }
    }

    @Test
    void lineHeightIsPositive() {
        final TextMetrics metrics = this.metrics();
        for (final TextStyle style : STYLES) {
            assertTrue(metrics.lineHeight(style) > 0f, "line height in " + style);
        }
    }

    @Test
    void ascentIsPositiveAndWithinTheLine() {
        final TextMetrics metrics = this.metrics();
        for (final TextStyle style : STYLES) {
            final float ascent = metrics.ascent(style);
            assertTrue(ascent > 0f, "ascent in " + style);
            assertTrue(ascent <= metrics.lineHeight(style), "ascent within the line height in " + style);
        }
    }
}
