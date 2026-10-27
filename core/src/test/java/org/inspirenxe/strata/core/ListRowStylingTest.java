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
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.spi.ScreenHost;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A list's rows styled from the theme a screen opens with, and cells that draw the row's look themselves.
 */
class ListRowStylingTest {
    private static final int ROW = 0xFF101010;
    private static final int HOVERED = 0xFF202020;
    private static final int SELECTED = 0xFF303030;
    private static final int SELECTED_UNFOCUSED = 0xFF404040;
    private static final int ACCENT = 0xFFE0C060;

    private final ScreenHost host = StrataCore.createScreenHost(new TestPlatform(), ThemeRef.platform()
            .override(ListView.STYLE_ROW, Paint.solid(ROW))
            .override(ListView.STYLE_ROW_HOVERED, Paint.solid(HOVERED))
            .override(ListView.STYLE_ROW_SELECTED, Paint.solid(SELECTED))
            .override(ListView.STYLE_ROW_SELECTED_UNFOCUSED, Paint.solid(SELECTED_UNFOCUSED))
            .override("menu.row.selected", Paint.solid(ACCENT)), null);

    private final List<MenuRow> made = new ArrayList<>();
    private ListView<String> list;

    private void show(final Function<ComponentFactory, ListView<String>> build) {
        this.host.resize(100, 100);
        this.host.open(ui -> {
            this.list = build.apply(ui).setItems(List.of("Ana", "Ben", "Cleo")).setRowHeight(10);
            return ui.absolutePanel().add(this.list.setSize(100, 30), 0, 0);
        });
        this.host.layout();
    }

    private List<String> fills() {
        final DrawLog canvas = new DrawLog();
        this.host.render(canvas);
        return canvas.lines("fill ");
    }

    @Test
    void everyRowStateTakesTheOverrideTheScreenOpenedWith() {
        this.show(ui -> ui.listView());
        this.list.select(1);
        this.host.pointerMoved(5, 25, Modifiers.NONE);
        final List<String> unfocused = this.fills();
        assertTrue(unfocused.contains("fill 0,0 100x10 #FF101010"), unfocused::toString);
        assertTrue(unfocused.contains("fill 0,10 100x10 #FF404040"), unfocused::toString);
        assertTrue(unfocused.contains("fill 0,20 100x10 #FF202020"), unfocused::toString);
        this.list.requestFocus();
        assertTrue(this.fills().contains("fill 0,10 100x10 #FF303030"));
    }

    @Test
    void theRequiredSelectedPaintCanBeTurnedOff() {
        final ScreenHost bare = StrataCore.createScreenHost(new TestPlatform(), ThemeRef.platform()
                .override(ListView.STYLE_ROW_SELECTED, Paint.NONE), null);
        bare.resize(100, 100);
        bare.open(ui -> ui.listView().setItems(List.of("Ana")).select(0));
        bare.layout();
        final DrawLog canvas = new DrawLog();
        bare.render(canvas);
        assertTrue(canvas.lines().stream().noneMatch(line -> line.contains("#FF303030")));
    }

    @Test
    void cellsHearTheirRowsStateAndCanDrawTheLookWithoutTheList() {
        this.show(ui -> ui.listView((String name) -> {
            final MenuRow row = new MenuRow(ui);
            this.made.add(row);
            return row;
        }).setRowBackgrounds(false));
        assertFalse(this.list.hasRowBackgrounds());
        this.list.select(1);
        this.host.pointerMoved(5, 25, Modifiers.NONE);
        final List<String> fills = this.fills();
        assertTrue(fills.contains("fill 0,10 2x10 #FFE0C060"), fills::toString);
        assertTrue(fills.stream().noneMatch(line -> line.contains(" 100x10 ")), "no row paints: " + fills);
        assertEquals(3, this.made.size());
        assertTrue(this.made.get(1).selected);
        assertFalse(this.made.get(0).selected);
        assertTrue(this.made.get(2).hovered);

        this.list.select(0);
        this.fills();
        assertFalse(this.made.get(1).selected, "the state turns off again");
        assertTrue(this.made.get(0).selected);
    }

    /**
     * A row a developer writes, with an accent at its left while selected.
     */
    static final class MenuRow extends CustomComponent<MenuRow> {
        boolean selected;
        boolean hovered;

        MenuRow(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(availableWidth, 10);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.selected = this.hasStyleState(ListView.STATE_SELECTED);
            this.hovered = this.hasStyleState(ListView.STATE_ROW_HOVERED);
            final Paint accent = this.stylePaint("menu.row", Paint.NONE);
            accent.paint(painter, new Rect(bounds.x(), bounds.y(), 2, bounds.height()));
        }
    }
}
