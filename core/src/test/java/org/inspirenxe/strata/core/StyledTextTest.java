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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.StyleRun;
import org.inspirenxe.strata.api.text.TextHighlighter;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.text.StyledText;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Styled text in text fields: highlighter runs, how fields measure and draw them, and the monospace flag.
 */
class StyledTextTest {
    private static final int KEYWORD = 0xFFC678DD;
    /**
     * Styles every "if" as a monospaced keyword, keeping the rest of the field's style.
     */
    private static final TextHighlighter KEYWORDS = (text, base) -> {
        final List<StyleRun> runs = new ArrayList<>();
        int at = text.indexOf("if");
        while (at >= 0) {
            runs.add(new StyleRun(at, at + 2, base.withColor(KEYWORD).withMonospace(true)));
            at = text.indexOf("if", at + 2);
        }
        return runs;
    };

    @Test
    void segmentsCoverTheTextAndDropBadRuns() {
        final StyledText styled = new StyledText();
        final TextStyle base = TextStyle.DEFAULT;
        final TextStyle mark = base.withBold(true);
        styled.update("abcdefgh", base, (text, b) -> List.of(
                new StyleRun(1, 3, mark),
                new StyleRun(2, 4, mark),   // overlaps the first: dropped
                new StyleRun(5, 5, mark),   // empty: dropped
                new StyleRun(6, 20, mark),  // past the end: dropped
                new StyleRun(6, 7, mark)), true);

        final TextSpan[] spans = styled.spans(0, 8);
        assertEquals(List.of("a", "bc", "def", "g", "h"),
                List.of(spans).stream().map(TextSpan::text).toList());
        assertEquals(List.of(base, mark, base, mark, base), List.of(spans).stream().map(TextSpan::style).toList());
    }

    @Test
    void ignoringRunsDrawsEverythingInTheBaseStyle() {
        final StyledText styled = new StyledText();
        styled.update("if x", TextStyle.DEFAULT, KEYWORDS, false);
        assertEquals(List.of(new TextSpan("if x", TextStyle.DEFAULT)), List.of(styled.spans(0, 4)));
    }

    @Test
    void runsMustBeInsideTheirOwnBounds() {
        assertThrows(IllegalArgumentException.class, () -> new StyleRun(3, 2, TextStyle.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> new StyleRun(-1, 2, TextStyle.DEFAULT));
    }

    @Test
    void aTextBoxDrawsEachRunInItsStyleKeepingTheBase() {
        final TestHost test = new TestHost(300, 100);
        final TextBox box = test.factory().textBox().setText("if x").setWidth(200).setHighlighter(KEYWORDS);
        test.show(test.factory().absolutePanel().add(box, 0, 0));

        final TextCanvas canvas = new TextCanvas();
        test.host().render(canvas);

        assertEquals(List.of("if", " x"), canvas.texts);
        final TextStyle keyword = canvas.styles.getFirst();
        assertEquals(KEYWORD, keyword.color());
        assertTrue(keyword.monospace());
        // The rest of the keyword's style is the field's own
        assertEquals(canvas.styles.get(1), keyword.withColor(canvas.styles.get(1).color()).withMonospace(false));
        // The second span starts after the monospaced keyword, measured in its own style
        assertEquals(2 * TestPlatform.MONO_CHAR_WIDTH, canvas.xs.get(1) - canvas.xs.getFirst());
    }

    @Test
    void theCaretLandsByStyledWidths() {
        final TestHost test = new TestHost(300, 100);
        final TextBox box = test.factory().textBox().setText("if x").setWidth(200).setHighlighter(KEYWORDS);
        test.show(test.factory().absolutePanel().add(box, 0, 0));
        final float textLeft = box.bounds().x() + 4f;

        // Just past the monospaced "if" (14 wide), not past the proportional width it would have had (12)
        final float x = textLeft + 2 * TestPlatform.MONO_CHAR_WIDTH + 1f;
        final float y = box.bounds().y() + box.bounds().height() / 2f;
        test.host().pointerMoved(x, y, Modifiers.NONE);
        test.host().pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
        test.host().pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);

        assertEquals(2, box.caret());
    }

    @Test
    void disabledAndPasswordFieldsIgnoreRuns() {
        final TestHost test = new TestHost(300, 100);
        final TextBox disabled = test.factory().textBox().setText("if").setHighlighter(KEYWORDS).setEnabled(false);
        final TextBox password = test.factory().textBox().setText("if").setHighlighter(KEYWORDS).setPassword(true);
        test.show(test.factory().stackPanel().add(disabled).add(password));

        final TextCanvas canvas = new TextCanvas();
        test.host().render(canvas);

        assertEquals(List.of("if", "**"), canvas.texts);
        assertTrue(canvas.styles.stream().noneMatch(style -> style.color() == KEYWORD));
    }

    @Test
    void aTextAreaDrawsRunsOnEveryRow() {
        final TestHost test = new TestHost(300, 200);
        final TextArea area = test.factory().textArea().setText("if a\nb if").setHighlighter(KEYWORDS);
        area.setSize(200, 100);
        test.show(test.factory().absolutePanel().add(area, 0, 0));

        final TextCanvas canvas = new TextCanvas();
        test.host().render(canvas);

        assertEquals(List.of("if", " a", "b ", "if"), canvas.texts);
        assertEquals(KEYWORD, canvas.styles.get(3).color());
    }

    @Test
    void changingTheHighlighterRestylesTheText() {
        final TestHost test = new TestHost(300, 100);
        final TextBox box = test.factory().textBox().setText("if x").setWidth(200);
        test.show(test.factory().absolutePanel().add(box, 0, 0));
        final TextCanvas plain = new TextCanvas();
        test.host().render(plain);
        assertEquals(List.of("if x"), plain.texts);

        box.setHighlighter(KEYWORDS);
        final TextCanvas styled = new TextCanvas();
        test.host().render(styled);
        assertEquals(List.of("if", " x"), styled.texts);
        assertEquals(KEYWORDS, box.highlighter());
    }

    /**
     * Records the text drawn, its style and where.
     */
    private static final class TextCanvas implements Canvas {
        final List<String> texts = new ArrayList<>();
        final List<TextStyle> styles = new ArrayList<>();
        final List<Float> xs = new ArrayList<>();

        @Override
        public void fill(final Rect rect, final int argb) {
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.texts.add(span.text());
            this.styles.add(span.style());
            this.xs.add(x);
        }

        @Override
        public void pushClip(final Rect rect) {
        }

        @Override
        public void popClip() {
        }

        @Override
        public void pushTranslation(final float dx, final float dy) {
        }

        @Override
        public void pushScale(final float scale) {
        }

        @Override
        public void popTransform() {
        }

        @Override
        public void visual(final Visual visual, final Rect rect, final int tint) {
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }
}
