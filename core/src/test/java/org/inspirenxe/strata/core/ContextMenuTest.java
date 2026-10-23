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
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.Conventions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Context menus: the right click, the Menu key and Shift+F10, the request before one opens, and a developer's own
 * component (design 4.40).
 */
class ContextMenuTest {
    private TestHost test = new TestHost(400, 300);
    private ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private Menu menu(final String name) {
        return this.ui.menu().addItem(name, () -> this.heard.add(name));
    }

    private void rightClick(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.RIGHT, true, Modifiers.NONE);
        this.test.host().pointerButton(x, y, MouseButton.RIGHT, false, Modifiers.NONE);
    }

    private void rightClickCenter(final Rect bounds) {
        this.rightClick(bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f);
    }

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean used = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return used;
    }

    /**
     * A swatch written as a developer would, declaring the attribute to take part.
     */
    static final class Swatch extends CustomComponent<Swatch> implements HasContextMenu<Swatch> {
        Swatch(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(20f, 20f);
        }
    }

    @Test
    void aRightClickOpensTheMenuAtThePointer() {
        final Menu menu = this.menu("copy");
        final Button button = this.ui.button("Target").setContextMenu(menu);
        this.test.show(button.setAnchor(Anchor.TOP_LEFT));
        assertSame(menu, button.contextMenu().orElseThrow());
        this.rightClick(10, 5);
        this.test.host().layout();
        assertTrue(menu.isOpen());
        assertEquals(10f, menu.bounds().x());
        assertEquals(5f, menu.bounds().y());
    }

    @Test
    void anAncestorsMenuCoversWhatIsInsideIt() {
        final Menu menu = this.menu("panel");
        final Label label = this.ui.label("Inside");
        final StackPanel panel = this.ui.stackPanel().add(label);
        panel.setContextMenu(menu);
        this.test.show(panel.setAnchor(Anchor.TOP_LEFT));
        this.rightClickCenter(label.bounds());
        assertTrue(menu.isOpen());
    }

    @Test
    void theRequestCanCancelOrRewriteThePopup() {
        final Menu first = this.menu("first");
        final Menu second = this.menu("second");
        final Button button = this.ui.button("Target").setContextMenu(first);
        this.test.show(button.setAnchor(Anchor.TOP_LEFT));
        final var refuse = button.subscribe(HasContextMenu.SHOW_CONTEXT_MENU, 0, event -> event.cancel());
        this.rightClickCenter(button.bounds());
        assertFalse(first.isOpen());
        refuse.unsubscribe();
        button.onEvent(HasContextMenu.SHOW_CONTEXT_MENU, event -> {
            assertSame(first, event.value());
            event.setValue(second);
        });
        this.rightClickCenter(button.bounds());
        assertFalse(first.isOpen());
        assertTrue(second.isOpen());
    }

    @Test
    void theMenuKeyAndShiftF10OpenBelowTheFocusedComponent() {
        final Menu menu = this.menu("edit");
        // Without its own default menu, the box shows the panel's
        final TextBox box = this.ui.textBox().setContextMenu(null);
        final StackPanel panel = this.ui.stackPanel().add(this.ui.button("Above")).add(box);
        panel.setContextMenu(menu);
        this.test.show(panel.setAnchor(Anchor.TOP_LEFT));
        box.requestFocus();
        assertTrue(this.press(Key.MENU, Modifiers.NONE));
        this.test.host().layout();
        assertTrue(menu.isOpen());
        assertEquals(panel.bounds().bottom(), menu.bounds().y());
        menu.close();
        box.requestFocus();
        assertTrue(this.press(Key.F10, Modifiers.of(Modifier.SHIFT)));
        assertTrue(menu.isOpen());
    }

    @Test
    void aConsumedRightClickOpensNothing() {
        final Menu menu = this.menu("panel");
        final CycleButton<String> cycle = this.ui.<String>cycleButton("Size").setItems(List.of("S", "M", "L"));
        final StackPanel panel = this.ui.stackPanel().add(cycle);
        panel.setContextMenu(menu);
        this.test.show(panel.setAnchor(Anchor.TOP_LEFT));
        this.rightClickCenter(cycle.bounds());
        assertFalse(menu.isOpen());
    }

    @Test
    void aRightClickOnADisabledComponentOpensNothing() {
        final Menu own = this.menu("own");
        final Menu panelMenu = this.menu("panel");
        final Button button = this.ui.button("Off").setContextMenu(own);
        button.setEnabled(false);
        final StackPanel panel = this.ui.stackPanel().add(button);
        panel.setContextMenu(panelMenu);
        this.test.show(panel.setAnchor(Anchor.TOP_LEFT));
        this.rightClickCenter(button.bounds());
        assertFalse(own.isOpen());
        assertFalse(panelMenu.isOpen());
    }

    @Test
    void onAPlatformThatActsOnPressItOpensOnThePress() {
        final TestPlatform platform = new TestPlatform();
        platform.setConventions(new Conventions(true));
        this.test = new TestHost(platform, Themes.flat(), 400, 300);
        this.ui = this.test.factory();
        final Menu menu = this.menu("copy");
        final Button button = this.ui.button("Target").setContextMenu(menu);
        this.test.show(button.setAnchor(Anchor.TOP_LEFT));
        this.test.host().pointerButton(10, 5, MouseButton.RIGHT, true, Modifiers.NONE);
        assertTrue(menu.isOpen());
    }

    @Test
    void aDevelopersComponentThatDeclaresTheAttributeHasOne() {
        final Menu menu = this.menu("swatch");
        final Swatch swatch = new Swatch(this.ui).setContextMenu(menu);
        this.test.show(swatch.setAnchor(Anchor.TOP_LEFT));
        assertSame(menu, swatch.contextMenu().orElseThrow());
        this.rightClick(5, 5);
        assertTrue(menu.isOpen());
        swatch.setContextMenu(null);
        assertTrue(swatch.contextMenu().isEmpty());
    }

    @Test
    void aDevelopersComponentWithoutTheAttributeOpensNothing() {
        final Menu menu = this.menu("box");
        final Box box = new Box(this.ui, 20, 20).setContextMenu(menu);
        this.test.show(box.setAnchor(Anchor.TOP_LEFT));
        assertTrue(box.contextMenu().isEmpty());
        this.rightClick(5, 5);
        assertFalse(menu.isOpen());
    }
}
