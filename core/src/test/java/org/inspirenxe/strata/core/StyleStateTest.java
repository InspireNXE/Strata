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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Design 4.52: style states on a developer's component, and the keys they pick.
 */
class StyleStateTest {
    private static final Paint PLAIN = Paint.solid(0xFF000001);
    private static final Paint WARNING = Paint.solid(0xFF000002);
    private static final Paint SELECTED = Paint.solid(0xFF000003);
    private static final Paint DISABLED = Paint.solid(0xFF000004);
    private static final Paint HOVERED = Paint.solid(0xFF000005);
    private static final Paint FALLBACK = Paint.solid(0xFF0000FF);
    private static final TextStyle WARNING_TEXT = TextStyle.DEFAULT.withColor(0xFFFF0000);

    private static final Theme THEME = Themes.flat().toBuilder()
            .paint("fancy.card", PLAIN)
            .paint("fancy.card.warning", WARNING)
            .paint("fancy.card.selected", SELECTED)
            .paint("fancy.card.disabled", DISABLED)
            .paint("fancy.card.hovered", HOVERED)
            .textStyle("fancy.card.text.warning", WARNING_TEXT)
            .build();

    private final TestHost test = new TestHost(new TestPlatform(), THEME, 100, 100);
    private final ComponentFactory ui = this.test.factory();

    /**
     * A card as a developer would write one, with its own warning and selected states.
     */
    static final class Card extends CustomComponent<Card> {
        Paint painted;

        Card(final ComponentFactory ui) {
            super(ui);
        }

        Card warn(final boolean warning) {
            this.setStyleState("warning", warning);
            return this;
        }

        Card select(final boolean selected) {
            this.setStyleState("selected", selected);
            return this;
        }

        Paint face() {
            return this.stylePaint("fancy.card", FALLBACK);
        }

        Paint face(final String key) {
            return this.stylePaint(key, FALLBACK);
        }

        TextStyle text() {
            return this.styleText("fancy.card.text", TextStyle.DEFAULT);
        }

        void setState(final String state) {
            this.setStyleState(state, true);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(50, 50);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.painted = this.face();
        }
    }

    private Card shown() {
        final Card card = new Card(this.ui);
        this.test.show(card);
        return card;
    }

    @Test
    void thePlainKeyWithNoStateOn() {
        assertEquals(PLAIN, this.shown().face());
    }

    @Test
    void anOwnStatePicksItsKey() {
        final Card card = this.shown().warn(true);
        assertEquals(WARNING, card.face());
        assertEquals(WARNING_TEXT, card.text());
        card.warn(false);
        assertEquals(PLAIN, card.face());
        assertEquals(TextStyle.DEFAULT, card.text());
    }

    @Test
    void ownStatesCountInTheOrderTheyWereFirstSet() {
        final Card card = this.shown().select(true).warn(true);
        assertEquals(SELECTED, card.face());
        card.select(false).select(true);
        assertEquals(SELECTED, card.face(), "setting it again keeps its place");
    }

    @Test
    void disabledWinsOverEverything() {
        final Card card = this.shown().warn(true);
        card.setEnabled(false);
        assertEquals(DISABLED, card.face());
    }

    @Test
    void disabledThroughAnAncestorCounts() {
        final Card card = new Card(this.ui);
        final AnchorPanel panel = this.ui.anchorPanel().add(card);
        this.test.show(panel);
        panel.setEnabled(false);
        assertEquals(DISABLED, card.face());
    }

    @Test
    void hoveredComesAfterOwnStates() {
        final Card card = this.shown();
        this.test.host().pointerMoved(10, 10, Modifiers.NONE);
        assertEquals(HOVERED, card.face());
        card.warn(true);
        assertEquals(WARNING, card.face());
    }

    @Test
    void paintingReadsTheStateLook() {
        final Card card = this.shown().warn(true);
        this.test.host().render(new DrawLog());
        assertEquals(WARNING, card.painted);
    }

    @Test
    void aKeyWithNothingInTheThemeGivesTheFallback() {
        final Card card = this.shown().warn(true);
        assertSame(FALLBACK, card.face("fancy.other"));
    }

    @Test
    void aDetachedComponentGivesTheFallback() {
        assertSame(FALLBACK, new Card(this.ui).warn(true).face());
    }

    @Test
    void aNewThemeIsReadAgain() {
        final Card card = this.shown().warn(true);
        assertEquals(WARNING, card.face());
        this.test.host().setTheme(THEME.toBuilder().paint("fancy.card.warning", SELECTED).build());
        assertEquals(SELECTED, card.face());
    }

    @Test
    void strataStatesCannotBeSetByHand() {
        final Card card = this.shown();
        for (final String state : new String[] {"disabled", "pressed", "hovered", "focused", ""}) {
            assertThrows(IllegalArgumentException.class, () -> card.setState(state), state);
        }
    }
}
