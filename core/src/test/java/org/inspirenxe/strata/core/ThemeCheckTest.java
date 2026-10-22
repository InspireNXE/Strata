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
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Themes are checked against a tree before anything changes, so a theme that lacks a key leaves the tree, the
 * host and the stack as they were.
 */
class ThemeCheckTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final TestPlatform platform = new TestPlatform();
    private final ScreenStack stack = StrataCore.createScreenStack(this.platform, Themes.flat());

    {
        this.stack.resize(300, 300);
    }

    @Test
    void aChildTheThemeCannotDrawIsNotAddedAtAll() {
        final AbsolutePanel panel = this.ui.absolutePanel();
        this.test.show(panel);
        final Needy needy = new Needy(this.ui, "test.missing");

        assertThrows(ThemeException.class, () -> panel.add(needy));

        assertEquals(List.of(), panel.children());
        assertNull(needy.parent());
        assertFalse(needy.isAttached());
        // Not left half added, so another container can take it
        this.ui.absolutePanel().add(needy);
    }

    @Test
    void aTooltipTheThemeCannotDrawLeavesNoTooltipBehind() {
        final Box owner = new Box(this.ui, 50, 50)
                .setTooltip(Tooltip.of(ui -> new Needy(ui, "test.missing")));
        this.test.show(this.ui.absolutePanel().add(owner, 0, 0));

        assertThrows(ThemeException.class,
                () -> this.test.host().pointerMoved(10, 10, Modifiers.NONE));

        assertTrue(this.test.host().root().tooltip().isEmpty());
    }

    @Test
    void aThemeOneScreenCannotUseChangesNoScreen() {
        final Theme both = TestThemes.flatWith("test.first", "test.second");
        this.stack.setTheme(both);
        final Needy[] first = new Needy[1];
        this.stack.push(ui -> {
            first[0] = new Needy(ui, "test.first");
            return ui.absolutePanel().add(first[0], 0, 0);
        });
        this.stack.push(ui -> ui.absolutePanel().add(new Needy(ui, "test.second"), 0, 0));

        assertThrows(ThemeException.class, () -> this.stack.setTheme(TestThemes.flatWith("test.first")));

        assertSame(both, first[0].current());
    }

    @Test
    void componentsReadTheNewThemeFromTheFactoryWhileItIsApplied() {
        final Theme next = TestThemes.flatWith("test.key");
        final TestHost host = new TestHost(new TestPlatform(), TestThemes.flatWith("test.key"), 300, 300);
        final Needy shown = new Needy(host.factory(), "test.key");
        host.show(host.factory().absolutePanel().add(shown, 0, 0));
        shown.seen.clear();

        host.host().setTheme(next);

        assertEquals(List.of(next), shown.seen);
    }
}
