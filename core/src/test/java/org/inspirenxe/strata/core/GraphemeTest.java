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
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.core.text.Graphemes;
import org.inspirenxe.strata.core.text.RichText;
import org.inspirenxe.strata.core.text.TextWrap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Grapheme clusters in wrapping, ellipsis, length limits and ranges from code (design 4.49). With the test platform
 * every code point is 6 units wide, so an accented letter written as a letter and a combining accent is 12 wide.
 */
class GraphemeTest {
    // e with a combining acute accent: two code points, one character
    private static final String E_ACUTE = "é";
    // The flag of Germany: two regional indicators, four chars
    private static final String FLAG = "🇩🇪";

    private final TextMetrics metrics = new TestPlatform().textMetrics();
    private final TestHost test = new TestHost(300, 200);

    @Test
    void aWordWiderThanARowBreaksBetweenClusters() {
        // 18 units hold three code points, which would split the second accented letter from its accent
        assertEquals(List.of(E_ACUTE + "a", E_ACUTE + "b"),
                TextWrap.wrap(E_ACUTE + "a" + E_ACUTE + "b", TextStyle.DEFAULT, 18, this.metrics));
        assertEquals(List.of(FLAG, FLAG), TextWrap.wrap(FLAG + FLAG, TextStyle.DEFAULT, 18, this.metrics));
    }

    @Test
    void styledTextWrapsBetweenClustersToo() {
        final List<TextSpan> spans = List.of(TextSpan.of("a" + E_ACUTE), TextSpan.of("b").withStyle(TextStyle.DEFAULT
                .withBold(true)));
        assertEquals(List.of(List.of(TextSpan.of("a")), List.of(TextSpan.of(E_ACUTE)),
                        List.of(TextSpan.of("b").withStyle(TextStyle.DEFAULT.withBold(true)))),
                RichText.wrap(spans, 12, this.metrics));
    }

    @Test
    void anEllipsisNeverCutsACluster() {
        // 30 units: the dots take 18, which leaves 12 for two code points, so only "a" and not half the e
        assertEquals(1, TextWrap.fittingPrefix("a" + E_ACUTE + "bcd", TextStyle.DEFAULT, 12, this.metrics));
        assertEquals(List.of(TextSpan.of("a" + RichText.ELLIPSIS)),
                RichText.ellipsize(List.of(TextSpan.of("a" + E_ACUTE + "bcd")), 30, this.metrics));
        assertEquals(List.of(TextSpan.of(E_ACUTE + RichText.ELLIPSIS)),
                RichText.ellipsize(List.of(TextSpan.of(E_ACUTE + "bcdef")), 30, this.metrics));
        assertEquals(0, TextWrap.fittingPrefix(FLAG, TextStyle.DEFAULT, 11, this.metrics));
        assertEquals(4, TextWrap.fittingPrefix(FLAG + "a", TextStyle.DEFAULT, 12, this.metrics));
    }

    @Test
    void aCutLeavesOutTheClusterThatWouldCrossTheLimit() {
        assertEquals(1, Graphemes.cut("a" + E_ACUTE, 2));
        assertEquals(3, Graphemes.cut("a" + E_ACUTE, 3));
        assertEquals(0, Graphemes.cut(FLAG, 1));
        assertEquals(4, Graphemes.cut(FLAG + FLAG, 3));
        assertEquals(0, Graphemes.cut("abc", 0));
    }

    private TextBox showBox() {
        final TextBox box = this.test.factory().textBox().setWidth(200);
        this.test.show(this.test.factory().absolutePanel().add(box));
        return box;
    }

    @Test
    void aLengthLimitKeepsClustersWhole() {
        final TextBox box = this.showBox();
        box.setText("ab" + E_ACUTE);
        box.setMaxLength(3);
        assertEquals("ab", box.text());
        box.setMaxLength(4).setText("x" + FLAG + FLAG);
        assertEquals("x" + FLAG, box.text());
    }

    @Test
    void typingPastTheLimitLeavesOutAWholeCluster() {
        final TextBox box = this.showBox();
        box.setMaxLength(3).setText("ab");
        this.test.click(10, 10);
        box.select(2, 2);
        this.test.platform().clipboard().set(FLAG);
        this.test.host().key(Key.V, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
        assertEquals("ab", box.text());
        box.setMaxLength(4);
        this.test.host().key(Key.V, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
        assertEquals("ab" + FLAG, box.text());
    }

    @Test
    void rangesFromCodeSnapToClusters() {
        final TextBox box = this.showBox();
        box.setText("a" + E_ACUTE + "b");
        box.select(2, 3);
        assertEquals(1, box.selectionStart());
        assertEquals(3, box.selectionEnd());
        box.replace(2, 3, "x");
        assertEquals("axb", box.text());
        box.setText(FLAG + "c");
        box.delete(2, 4);
        assertEquals("c", box.text());
    }

    @Test
    void aTextAreaCountsColumnsFromClusterStarts() {
        final TextArea area = this.test.factory().textArea().setWidth(200).setHeight(100);
        this.test.show(this.test.factory().absolutePanel().add(area));
        area.setText(E_ACUTE + "x");
        area.select(1, 1);
        assertEquals(0, area.caret());
        assertEquals(0, area.columnOf(1));
        assertEquals(2, area.columnOf(2));
    }

    @Test
    void charactersTypedOneByOneStillFormOneCluster() {
        final TextBox box = this.showBox();
        this.test.click(10, 10);
        this.test.host().charTyped('e', Modifiers.NONE);
        this.test.host().charTyped(0x301, Modifiers.NONE);
        assertEquals(E_ACUTE, box.text());
        assertEquals(2, box.caret());
    }
}
