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

import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.InlineSprite;
import org.inspirenxe.strata.api.text.InlineVisual;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.text.InlineMetrics;
import org.inspirenxe.strata.core.text.RichText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.inspirenxe.strata.core.TestPlatform.CHAR_WIDTH;
import static org.inspirenxe.strata.core.TestPlatform.LINE_HEIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Visuals and sprites inside text: how text keeps them, how labels measure and draw them, and that the engine's
 * canvas never sees one.
 */
class InlineTextTest {
    private static final SpriteId COIN = SpriteId.of("test", "coin");
    private static final Picture TALL = new Picture("tall", 20f, 30f);
    private static final String OBJECT = TextSpan.OBJECT_REPLACEMENT;

    private record Picture(String name, float width, float height) implements Visual {
        @Override
        public Size size() {
            return new Size(this.width, this.height);
        }
    }

    private static TextSpan inline(final Visual visual, final TextStyle style) {
        return TextSpan.of(new InlineVisual(visual), style);
    }

    @Test
    void aVisualResolvesToOneSpanInTheBaseStyle() {
        final TextStyle base = TextStyle.DEFAULT.withColor(0xFF112233);
        final Text text = Text.visual(TALL);
        assertEquals(List.of(inline(TALL, base)), text.resolve(base));
        assertEquals(OBJECT, text.plain());
        assertFalse(text.isEmpty());
    }

    @Test
    void textOperationsKeepElements() {
        final Text text = Text.concat(Text.of("  "), Text.visual(TALL), Text.of(" x  "));
        final List<TextSpan> trimmed = text.trim().resolve(TextStyle.DEFAULT);
        assertEquals(List.of(inline(TALL, TextStyle.DEFAULT), new TextSpan(" x", TextStyle.DEFAULT)), trimmed);

        final TextStyle bold = TextStyle.DEFAULT.withBold(true);
        assertEquals(inline(TALL, bold), text.bold().resolve(TextStyle.DEFAULT).get(1));
        assertEquals(inline(TALL, bold),
                text.styleMatches(Pattern.compile(OBJECT), style -> style.withBold(true)).resolve(TextStyle.DEFAULT).get(1));
        assertEquals(inline(TALL, TextStyle.DEFAULT), text.replace("x", "y").normalizeWhitespace()
                .resolve(TextStyle.DEFAULT).get(1));
    }

    @Test
    void adjacentElementsStaySeparate() {
        final List<TextSpan> spans = Text.concat(Text.visual(TALL), Text.visual(TALL)).resolve(TextStyle.DEFAULT);
        assertEquals(List.of(inline(TALL, TextStyle.DEFAULT), inline(TALL, TextStyle.DEFAULT)), spans);
    }

    @Test
    void spansCheckTheirElement() {
        assertThrows(IllegalArgumentException.class, () -> new TextSpan("x", TextStyle.DEFAULT, new InlineVisual(TALL)));
        final TextSpan span = inline(TALL, TextStyle.DEFAULT);
        assertNull(span.withText("x").element());
        assertEquals(span, span.withText(OBJECT));
        assertThrows(IllegalArgumentException.class, () -> new InlineSprite(COIN, 0f, 4f));
        assertThrows(IllegalArgumentException.class, () -> Text.sprite(COIN, 0f, 0f));
    }

    @Test
    void aLabelIsTallEnoughForAVisualAndCentersItsText() {
        final TestHost test = new TestHost(200, 100);
        final Label label = test.factory().label(Text.concat(Text.visual(TALL), Text.of("ab")));
        test.show(test.factory().absolutePanel().add(label, 0, 0));
        assertEquals(20f + 2 * CHAR_WIDTH, label.desiredWidth());
        assertEquals(30f, label.desiredHeight());

        final DrawLog canvas = new DrawLog();
        test.host().render(canvas);
        final Rect bounds = label.bounds();
        assertEquals(List.of("visual " + TALL + " " + DrawLog.rect(new Rect(bounds.x(), bounds.y(), 20, 30)) + " #FFFFFFFF"),
                canvas.lines("visual"));
        assertEquals(0f, bounds.x());
        assertEquals(0f, bounds.y());
        assertEquals(1, canvas.lines("text ab 20,10.5 ").size(), canvas.lines().toString());
    }

    @Test
    void aSpriteWithNoSizeFitsTheLine() {
        final TestHost test = new TestHost(200, 100);
        final Label label = test.factory().label(Text.concat(Text.of("a"), Text.sprite(COIN)));
        test.show(test.factory().absolutePanel().add(label, 0, 0));
        assertEquals(CHAR_WIDTH + LINE_HEIGHT, label.desiredWidth());
        assertEquals(LINE_HEIGHT, label.desiredHeight());

        final DrawLog canvas = new DrawLog();
        test.host().render(canvas);
        final Rect bounds = label.bounds();
        assertEquals(List.of("sprite " + COIN + " "
                        + DrawLog.rect(new Rect(bounds.x() + CHAR_WIDTH, bounds.y(), LINE_HEIGHT, LINE_HEIGHT)) + " #FFFFFFFF"),
                canvas.lines("sprite"));
    }

    @Test
    void aSmallSpriteIsCenteredOnTheLine() {
        final TestHost test = new TestHost(200, 100);
        final Label label = test.factory().label(Text.sprite(COIN, 4f, 5f));
        test.show(test.factory().absolutePanel().add(label, 0, 0));
        assertEquals(LINE_HEIGHT, label.desiredHeight());

        final DrawLog canvas = new DrawLog();
        test.host().render(canvas);
        final Rect bounds = label.bounds();
        assertEquals(List.of("sprite " + COIN + " " + DrawLog.rect(new Rect(bounds.x(), bounds.y() + 2f, 4, 5)) + " #FFFFFFFF"),
                canvas.lines("sprite"));
    }

    @Test
    void theEngineCanvasNeverSeesAnElement() {
        final TestHost test = new TestHost(200, 100);
        test.show(test.factory().stackPanel()
                .add(test.factory().label(Text.concat(Text.of("one "), Text.visual(TALL), Text.of(" two"))))
                .add(test.factory().button(Text.concat(Text.sprite(COIN), Text.of(" Buy")))));
        final DrawLog canvas = new DrawLog();
        test.host().render(canvas);
        for (final String line : canvas.lines("text")) {
            assertFalse(line.contains(OBJECT), line);
        }
        assertEquals(1, canvas.lines("visual").size());
        assertEquals(1, canvas.lines("sprite " + COIN).size());
    }

    @Test
    void wrappingKeepsAnElementWhole() {
        final InlineMetrics metrics = InlineMetrics.of(new TestPlatform().textMetrics());
        final List<TextSpan> spans = Text.concat(Text.of("a "), Text.visual(TALL), Text.of(" b")).resolve(TextStyle.DEFAULT);
        final List<List<TextSpan>> lines = RichText.wrap(spans, 25f, metrics);
        assertEquals(List.of(List.of(TextSpan.of("a")), List.of(inline(TALL, TextStyle.DEFAULT)), List.of(TextSpan.of("b"))),
                lines);
    }

    @Test
    void anElementThatDoesNotFitGivesWayToTheEllipsis() {
        final InlineMetrics metrics = InlineMetrics.of(new TestPlatform().textMetrics());
        final List<TextSpan> line = List.of(TextSpan.of("ab"), inline(TALL, TextStyle.DEFAULT));
        assertEquals(List.of(TextSpan.of("ab"), TextSpan.of(RichText.ELLIPSIS)), RichText.ellipsize(line, 30f, metrics));
    }

    @Test
    void ellipsizedLabelsDropElementsCleanly() {
        final TestHost test = new TestHost(30, 100);
        final Label label = test.factory().label(Text.concat(Text.of("ab"), Text.visual(TALL)))
                .setOverflow(TextOverflow.ELLIPSIS);
        test.show(label);
        final DrawLog canvas = new DrawLog();
        test.host().render(canvas);
        assertTrue(canvas.lines("visual").isEmpty(), canvas.lines().toString());
        assertEquals(2, canvas.lines("text").size(), canvas.lines().toString());
    }
}
