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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Padding keys in themes (design section 4.24). The test platform's text is 6 units per character and 9 per line.
 */
class ThemePaddingTest {

    private static Theme flatWith(final String key, final Insets insets) {
        return Themes.flat().toBuilder().insets(key, insets).build();
    }

    private static TestHost host(final Theme theme) {
        return new TestHost(new TestPlatform(), theme, 400, 300);
    }

    private static Rect place(final TestHost test, final Component<?> component) {
        test.show(test.factory().absolutePanel().add(component, 0, 0));
        return component.bounds();
    }

    @Test
    void aButtonFollowsTheThemeUntilItsPaddingIsSetAndAgainAfterNull() {
        final TestHost test = host(flatWith(Button.STYLE_PADDING, Insets.of(10, 6)));
        final Button button = test.factory().button("abcdef");
        // 36 of text plus 10 a side, 9 of line plus 6 above and below
        assertEquals(new Rect(0, 0, 56, 21), place(test, button));

        button.setTextPadding(Insets.of(2));
        test.host().layout();
        // 40 wide, and the 20-unit minimum height
        assertEquals(new Rect(0, 0, 40, 20), button.bounds());

        button.setTextPadding(null);
        test.host().layout();
        assertEquals(new Rect(0, 0, 56, 21), button.bounds());

        // Without the key, the built-in 8 and 5
        test.host().setTheme(Themes.flat());
        test.host().layout();
        assertEquals(new Rect(0, 0, 52, 20), button.bounds());
    }

    @Test
    void aTextAreaFallsBackToTheTextBoxPadding() {
        final TestHost test = host(flatWith(TextBox.STYLE_PADDING, Insets.of(7)));
        final TextBox box = test.factory().textBox();
        final TextArea area = test.factory().textArea();
        test.show(test.factory().absolutePanel().add(box, 0, 0).add(area, 0, 100));
        assertEquals(9 + 14, box.bounds().height());
        assertEquals(4 * 9 + 14, area.bounds().height());

        test.host().setTheme(Themes.flat().toBuilder().insets(TextBox.STYLE_PADDING, Insets.of(7))
                .insets(TextArea.STYLE_PADDING, Insets.of(1)).build());
        test.host().layout();
        assertEquals(4 * 9 + 2, area.bounds().height());
    }

    @Test
    void rowsAndHeadersAreALineTallPlusTheirPadding() {
        final Theme theme = Themes.flat().toBuilder()
                .insets(ComboBox.STYLE_PADDING, Insets.of(4, 6))
                .insets(Expander.STYLE_HEADER_PADDING, new Insets(4, 2, 4, 3))
                .build();
        final TestHost test = host(theme);
        final ComponentFactory ui = test.factory();
        final ComboBox<String> combo = ui.<String>comboBox(String::valueOf).setItems(List.of("ab"));
        final Expander expander = ui.expander("ab");
        test.show(ui.absolutePanel().add(combo, 0, 0).add(expander, 0, 100));
        assertEquals(9 + 12, combo.bounds().height());
        assertEquals(9 + 5, expander.bounds().height());
    }

    @Test
    void aCellLabelTakesTheCellPaddingUntilItsMarginIsSet() {
        final TestHost test = host(flatWith(ListView.STYLE_CELL_PADDING, new Insets(2, 1, 3, 1)));
        final Label cell = test.factory().cellLabel("ab");
        final Label fixed = test.factory().cellLabel("cd").setMargin(Insets.of(5));
        test.show(test.factory().absolutePanel().add(cell, 0, 0).add(fixed, 0, 50));
        assertEquals(new Insets(2, 1, 3, 1), cell.margin());
        assertEquals(Insets.of(5), fixed.margin());

        test.host().setTheme(Themes.flat());
        assertEquals(new Insets(ComponentFactory.CELL_PADDING, 0, ComponentFactory.CELL_PADDING, 0), cell.margin());
        assertEquals(Insets.of(5), fixed.margin());
    }

    @Test
    void aWindowsPaddingComesFromTheThemeUntilSet() {
        final TestHost test = host(flatWith(Window.STYLE_PADDING, Insets.of(9)));
        final Window window = test.factory().window("Title");
        place(test, window);
        assertEquals(Insets.of(9), window.padding());
        window.setPadding(Insets.of(1));
        assertEquals(Insets.of(1), window.padding());
        window.setPadding(null);
        assertEquals(Insets.of(9), window.padding());
    }

    @Test
    void aTooltipUsesTheThemesPadding() {
        final TestHost test = host(flatWith(Tooltip.STYLE_PADDING, Insets.of(1)));
        final Label label = test.factory().label("x").setTooltip("abc");
        place(test, label);
        test.host().pointerMoved(2, 2, Modifiers.NONE);
        test.host().layout();
        // 18 of text and 1 a side, 9 of line and 1 above and below
        final Rect bounds = test.host().root().tooltip().orElseThrow().bounds();
        assertEquals(20, bounds.width());
        assertEquals(11, bounds.height());
    }
}
