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
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Details of how built-in controls look: list backgrounds and cells, disabled tab bars and text box placeholders.
 */
class ControlPolishTest {

    @Test
    void anEmptyListStillDrawsItsBackground() {
        final TestHost test = new TestHost(200, 100);
        final ListView<String> list = test.factory().<String>listView().setPlaceholder("Nothing here");
        list.setSize(100, 60);
        test.show(test.factory().absolutePanel().add(list, 0, 0));

        final RecordingCanvas canvas = new RecordingCanvas();
        test.host().render(canvas);

        assertTrue(canvas.fills.contains(new Fill(new Rect(0, 0, 100, 60), 0xFF18191C)), "background drawn");
        assertEquals(List.of("Nothing here"), canvas.texts);
    }

    @Test
    void defaultListCellsArePaddedLikeTableCells() {
        final TestHost test = new TestHost(200, 100);
        final ComponentFactory ui = test.factory();
        final Label cell = ui.cellLabel("Ana");

        assertEquals(Insets.of(ComponentFactory.CELL_PADDING, 0f), cell.margin());
        assertEquals(TextOverflow.ELLIPSIS, cell.overflow());
        assertEquals(Anchor.MIDDLE_LEFT.horizontal(), cell.horizontalAlignment());

        final ListView<String> list = ui.<String>listView().setItems(List.of("Ana"));
        list.setSize(100, 60);
        test.show(ui.absolutePanel().add(list, 0, 0));
        final RecordingCanvas canvas = new RecordingCanvas();
        test.host().render(canvas);
        assertEquals(List.of(ComponentFactory.CELL_PADDING), canvas.textX);
    }

    @Test
    void aDisabledBarDrawsItsSelectedTabDisabled() {
        final TestHost test = new TestHost(200, 100);
        final TabBar tabs = test.factory().tabBar().addTab("Buy").addTab("Sell");
        tabs.setWidth(200);
        test.show(test.factory().absolutePanel().add(tabs, 0, 0));

        final RecordingCanvas enabled = new RecordingCanvas();
        test.host().render(enabled);
        assertTrue(enabled.colors().contains(0xFF3A4D6E), "selected look while enabled");

        tabs.setEnabled(false);
        final RecordingCanvas disabled = new RecordingCanvas();
        test.host().render(disabled);
        assertFalse(disabled.colors().contains(0xFF3A4D6E), "no selected look while disabled");
        assertTrue(disabled.colors().contains(0xFF2A3140), "selected disabled look");
        assertTrue(disabled.colors().contains(0xFF1B1C1F), "disabled look for the other tab");
    }

    @Test
    void aPlaceholderShowsOnlyWhileTheBoxIsEmpty() {
        final TestHost test = new TestHost(200, 100);
        final TextBox box = test.factory().textBox().setPlaceholder("Search items").setWidth(120);
        test.show(test.factory().absolutePanel().add(box, 0, 0));
        assertEquals("Search items", box.placeholder().plain());
        assertEquals("", box.text());

        final RecordingCanvas empty = new RecordingCanvas();
        test.host().render(empty);
        assertEquals(List.of("Search items"), empty.texts);

        box.setText("apple");
        final RecordingCanvas typed = new RecordingCanvas();
        test.host().render(typed);
        assertEquals(List.of("apple"), typed.texts);

        box.setPlaceholder(Text.empty());
        assertEquals("", box.placeholder().plain());
    }

    private record Fill(Rect rect, int argb) {
    }

    /**
     * Records solid fills and text.
     */
    private static final class RecordingCanvas implements Canvas {
        final List<Fill> fills = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        final List<Float> textX = new ArrayList<>();

        List<Integer> colors() {
            return this.fills.stream().map(Fill::argb).toList();
        }

        @Override
        public void fill(final Rect rect, final int argb) {
            this.fills.add(new Fill(rect, argb));
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
            this.textX.add(x);
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
