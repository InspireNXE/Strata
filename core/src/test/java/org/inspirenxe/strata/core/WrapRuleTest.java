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

import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.core.text.RichText;
import org.inspirenxe.strata.core.text.TextWrap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The one wrap rule that labels and text areas share. With the test platform every code point is 6 units wide, so a
 * width of 24 holds four.
 */
class WrapRuleTest {
    private static final String SMILE = "😀";

    private final TextMetrics metrics = new TestPlatform().textMetrics();

    private List<String> wrap(final String text, final float width) {
        return TextWrap.wrap(text, TextStyle.DEFAULT, width, this.metrics);
    }

    @Nested
    class Labels {

        @Test
        void whitespaceAtTheStartOfALineIsKept() {
            assertEquals(List.of("  ab", "cd"), WrapRuleTest.this.wrap("  ab cd", 24));
        }

        @Test
        void whitespaceWhereARowBreaksIsNeverDrawn() {
            assertEquals(List.of("aaa", "bbb"), WrapRuleTest.this.wrap("aaa  bbb", 24));
        }

        @Test
        void aTabBreaksARowAsASpaceDoes() {
            assertEquals(List.of("ab", "cd"), WrapRuleTest.this.wrap("ab\tcd", 18));
        }

        @Test
        void aNoBreakSpaceKeepsItsWordsTogether() {
            assertEquals(List.of("ab c", "d"), WrapRuleTest.this.wrap("ab cd", 24));
        }

        @Test
        void styledSpansBreakWhereOneSpanDoes() {
            final TextStyle red = TextStyle.DEFAULT.withColor(0xFFFF0000);
            final List<List<TextSpan>> lines = RichText.wrap(
                    List.of(new TextSpan("  ab ", TextStyle.DEFAULT), new TextSpan("cd\tef", red)), 24,
                    WrapRuleTest.this.metrics);
            final List<String> texts = lines.stream()
                    .map(line -> line.stream().map(TextSpan::text).reduce("", String::concat))
                    .toList();

            assertEquals(WrapRuleTest.this.wrap("  ab cd\tef", 24), texts);
        }

        @Test
        void theLongestFittingPrefixKeepsSurrogatePairsWhole() {
            assertEquals(4, TextWrap.fittingPrefix(SMILE + SMILE + SMILE, TextStyle.DEFAULT, 13, WrapRuleTest.this.metrics));
            assertEquals(0, TextWrap.fittingPrefix(SMILE, TextStyle.DEFAULT, 5, WrapRuleTest.this.metrics));
        }
    }

    @Nested
    class TextAreas {

        @Test
        void aWordOfEmojiFillsTheRowBeforeItBreaks() {
            final TestHost test = new TestHost(300, 200);
            // 44 wide less 4 of padding on each side leaves 36, room for six emoji
            final TextArea area = test.factory().textArea().setAnchor(Anchor.TOP_LEFT).setWidth(44);
            test.show(area);
            area.setText(SMILE.repeat(10));

            final NullCanvas canvas = new NullCanvas();
            test.host().render(canvas);

            assertEquals(List.of(SMILE.repeat(6), SMILE.repeat(4)),
                    canvas.texts().stream().map(TextSpan::text).toList());
        }
    }
}
