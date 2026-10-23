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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.core.text.BidiRuns;
import org.inspirenxe.strata.core.text.TextLine;
import org.inspirenxe.strata.spi.Capabilities;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bidirectional algorithm over drawn lines (design 4.49). With the test platform every code point is 6 units
 * wide.
 */
class BidiTest {
    // Alef, bet, gimel
    private static final String HEBREW = "אבג";
    private static final String REVERSED = "גבא";

    private final TextMetrics metrics = new TestPlatform().textMetrics();

    private static List<String> texts(final BidiRuns runs) {
        return runs.spans().stream().map(TextSpan::text).toList();
    }

    @Test
    void leftToRightTextKeepsItsSpans() {
        final List<TextSpan> spans = List.of(TextSpan.of("Ana and Ben"));
        assertSame(spans, BidiRuns.of(spans, null).spans());
        assertSame(spans, BidiRuns.of(spans, false).spans());
    }

    @Test
    void aHebrewWordInsideEnglishIsOneRightToLeftRun() {
        final BidiRuns runs = BidiRuns.of(List.of(TextSpan.of("Ana " + HEBREW + " Ben")), false);
        assertEquals(List.of("Ana ", HEBREW, " Ben"), texts(runs));
        assertFalse(runs.isRightToLeft(0));
        assertTrue(runs.isRightToLeft(1));
        assertFalse(runs.isRightToLeft(2));
    }

    @Test
    void anEnglishNameInsideHebrewIsDrawnFirstFromTheLeft() {
        final BidiRuns runs = BidiRuns.of(List.of(TextSpan.of(HEBREW + " Cleo")), true);
        assertEquals(List.of("Cleo", HEBREW + " "), texts(runs));
        assertFalse(runs.isRightToLeft(0));
        assertTrue(runs.isRightToLeft(1));
    }

    @Test
    void theFirstLetterDecidesWithNoDirection() {
        final BidiRuns runs = BidiRuns.of(List.of(TextSpan.of(HEBREW + " Cleo")), null);
        assertEquals(List.of("Cleo", HEBREW + " "), texts(runs));
    }

    @Test
    void spansSplitWhereTheLevelChanges() {
        final TextStyle bold = TextStyle.DEFAULT.withBold(true);
        final BidiRuns runs = BidiRuns.of(List.of(TextSpan.of("Dev " + HEBREW.substring(0, 1)),
                TextSpan.of(HEBREW.substring(1)).withStyle(bold)), false);
        // The two Hebrew pieces read right to left, so the bold one is drawn first
        assertEquals(List.of("Dev ", HEBREW.substring(1), HEBREW.substring(0, 1)), texts(runs));
        assertEquals(bold, runs.spans().get(1).style());
    }

    @Test
    void anEngineThatDoesNotShapeGetsTheRunReversed() {
        final DrawLog log = new DrawLog();
        final CanvasPainter painter = new CanvasPainter().begin(log, new Capabilities(false, false), 1f);
        TextLine.of(Text.of("Ana " + HEBREW), TextStyle.DEFAULT, this.metrics).draw(painter, 10, 0);
        assertEquals(List.of("text Ana  10,0 #FFFFFFFF", "text " + REVERSED + " 34,0 #FFFFFFFF"),
                log.lines("text"));
    }

    @Test
    void anEngineThatShapesGetsTheRunInLogicalOrder() {
        final List<String> drawn = new ArrayList<>();
        final DrawLog log = new DrawLog() {
            @Override
            public void text(final TextSpan span, final float x, final float y, final boolean rightToLeft) {
                drawn.add(span.text() + (rightToLeft ? " rtl" : ""));
            }
        };
        final CanvasPainter painter = new CanvasPainter().begin(log, new Capabilities(false, false, false, false,
                true), 1f);
        TextLine.of(Text.of("Ana " + HEBREW), TextStyle.DEFAULT, this.metrics).draw(painter, 0, 0);
        assertEquals(List.of(HEBREW + " rtl"), drawn);
    }

    @Test
    void reversingKeepsMarksWithTheirLettersAndMirrorsBrackets() {
        final String text = "(אְב)";
        final DrawLog log = new DrawLog();
        final CanvasPainter painter = new CanvasPainter().begin(log, new Capabilities(false, false), 1f);
        TextLine.of(List.of(TextSpan.of(text)), TextStyle.DEFAULT, this.metrics, true).draw(painter, 0, 0);
        assertEquals("text (באְ) 0,0 #FFFFFFFF", log.lines("text").get(0));
    }

    @Test
    void aLineMeasuresTheSameInAnyOrder() {
        final TextLine line = TextLine.of(Text.of("Ana " + HEBREW + " Ben"), TextStyle.DEFAULT, this.metrics);
        assertEquals(66f, line.width());
        assertEquals("Ana " + HEBREW + " Ben", line.spans().get(0).text());
    }

    @Test
    void aLabelOrdersItsLinesInItsDirection() {
        final TestHost test = new TestHost(200, 100);
        final var label = test.factory().label(HEBREW + " Cleo");
        final var panel = test.factory().stackPanel().add(label);
        panel.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        test.show(panel);
        final DrawLog log = new DrawLog();
        test.host().render(log);
        // Right-aligned: 200 less 8 code points of 6 units
        assertEquals(List.of("text Cleo 152,0 #FFFFFFFF", "text  " + REVERSED + " 176,0 #FFFFFFFF"),
                log.lines("text").stream().map(line -> line.replaceAll("#[0-9A-F]{8}", "#FFFFFFFF")).toList());
    }
}
