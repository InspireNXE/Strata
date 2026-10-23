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
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focus within a subtree, the enter and exit events, and controls that keep focus on an inner part (design 4.41).
 */
class FocusWithinTest {
    private final TestHost test = new TestHost(400, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private static String name(final FocusEvent event) {
        return event.other().map(other -> ((Button) other).text().plain()).orElse("none");
    }

    /**
     * A form section written as a developer would: a column that checks its fields once focus leaves it.
     */
    static final class Section extends CustomContainer<Section> {
        int checks;

        Section(final ComponentFactory ui) {
            super(ui);
            this.onEvent(InputEvents.FOCUS_EXIT, event -> {
                if (!this.isFocusWithin()) {
                    this.checks++;
                }
            });
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            float width = 0f;
            float height = 0f;
            for (final Component<?> child : this.children()) {
                final Size outer = context.measure(child, availableWidth, availableHeight);
                width = Math.max(width, outer.width());
                height += outer.height();
            }
            return new Size(width, height);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            float y = bounds.y();
            for (final Component<?> child : this.children()) {
                final float height = child.desiredHeight();
                context.arrange(child, new Rect(bounds.x(), y, bounds.width(), height));
                y += height;
            }
        }
    }

    @Nested
    class Within {
        @Test
        void aPanelHasFocusWithinWhileADescendantHasIt() {
            final FocusWithinTest outer = FocusWithinTest.this;
            final Button inside = outer.ui.button("Inside");
            final StackPanel inner = outer.ui.stackPanel().add(inside);
            final Button outside = outer.ui.button("Outside");
            final StackPanel column = outer.ui.stackPanel().add(inner).add(outside);
            outer.test.show(column.setAnchor(Anchor.TOP_LEFT));
            assertFalse(column.isFocusWithin());
            inside.requestFocus();
            assertTrue(inside.isFocusWithin());
            assertTrue(inner.isFocusWithin());
            assertTrue(column.isFocusWithin());
            outside.requestFocus();
            assertFalse(inner.isFocusWithin());
            assertTrue(column.isFocusWithin());
            assertFalse(outer.ui.button("Detached").isFocusWithin());
        }

        @Test
        void exitsFireInnermostFirstAndEntersOutermostFirst() {
            final FocusWithinTest outer = FocusWithinTest.this;
            final Button a = outer.ui.button("A");
            final Button b = outer.ui.button("B");
            final StackPanel innerA = outer.ui.stackPanel().add(a);
            final StackPanel groupA = outer.ui.stackPanel().add(innerA);
            final StackPanel innerB = outer.ui.stackPanel().add(b);
            final StackPanel groupB = outer.ui.stackPanel().add(innerB);
            final StackPanel column = outer.ui.stackPanel().add(groupA).add(groupB);
            a.onEvent(Focusable.FOCUS_LOST, event -> outer.heard.add("lost A"));
            b.onEvent(Focusable.FOCUS_GAINED, event -> outer.heard.add("gained B"));
            innerA.onEvent(InputEvents.FOCUS_EXIT, event -> outer.heard.add("exit innerA to " + name(event)));
            groupA.onEvent(InputEvents.FOCUS_EXIT, event -> outer.heard.add("exit groupA"));
            groupB.onEvent(InputEvents.FOCUS_ENTER, event -> outer.heard.add("enter groupB from " + name(event)));
            innerB.onEvent(InputEvents.FOCUS_ENTER, event -> outer.heard.add("enter innerB"));
            column.onEvent(InputEvents.FOCUS_ENTER, event -> outer.heard.add("enter column"));
            column.onEvent(InputEvents.FOCUS_EXIT, event -> outer.heard.add("exit column"));
            outer.test.show(column.setAnchor(Anchor.TOP_LEFT));
            a.requestFocus();
            outer.heard.clear();
            b.requestFocus();
            assertEquals(List.of("lost A", "exit innerA to B", "exit groupA", "enter groupB from A", "enter innerB",
                    "gained B"), outer.heard);
        }

        @Test
        void clearingFocusExitsEveryAncestor() {
            final FocusWithinTest outer = FocusWithinTest.this;
            final Button a = outer.ui.button("A");
            final StackPanel column = outer.ui.stackPanel().add(a);
            column.onEvent(InputEvents.FOCUS_EXIT, event -> outer.heard.add("exit to " + name(event)));
            outer.test.show(column.setAnchor(Anchor.TOP_LEFT));
            a.requestFocus();
            outer.test.host().root().clearFocus();
            assertEquals(List.of("exit to none"), outer.heard);
        }

        @Test
        void aDevelopersSectionHearsFocusLeaveIt() {
            final FocusWithinTest outer = FocusWithinTest.this;
            final TextBox first = outer.ui.textBox();
            final TextBox second = outer.ui.textBox();
            final Section section = new Section(outer.ui).add(first).add(second);
            final Button after = outer.ui.button("After");
            outer.test.show(outer.ui.stackPanel().add(section).add(after).setAnchor(Anchor.TOP_LEFT));
            first.requestFocus();
            outer.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
            assertSame(second, outer.test.focused());
            assertEquals(0, section.checks);
            assertTrue(section.isFocusWithin());
            outer.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
            assertSame(after, outer.test.focused());
            assertEquals(1, section.checks);
        }
    }

    @Nested
    class Delegates {
        @Test
        void aSpinnerTakesFocusThroughItsField() {
            final FocusWithinTest outer = FocusWithinTest.this;
            final Spinner spinner = outer.ui.spinner();
            spinner.onEvent(Focusable.FOCUS_GAINED, event -> outer.heard.add("gained"))
                    .onEvent(Focusable.FOCUS_LOST, event -> outer.heard.add("lost"));
            final Button after = outer.ui.button("After");
            outer.test.show(outer.ui.stackPanel().add(spinner).add(after).setAnchor(Anchor.TOP_LEFT));
            assertTrue(spinner.requestFocus());
            assertTrue(spinner.isFocused());
            assertNotSame(spinner, outer.test.focused());
            assertTrue(outer.test.focused() instanceof TextBox);
            after.requestFocus();
            assertFalse(spinner.isFocused());
            assertEquals(List.of("gained", "lost"), outer.heard);
        }

        @Test
        void theTabOrderHoldsASpinnerOnce() {
            final FocusWithinTest outer = FocusWithinTest.this;
            final Button before = outer.ui.button("Before");
            final Spinner spinner = outer.ui.spinner();
            final Button after = outer.ui.button("After");
            outer.test.show(outer.ui.stackPanel().add(before).add(spinner).add(after).setAnchor(Anchor.TOP_LEFT));
            assertEquals(3, outer.test.tabOrder().size());
            spinner.setTabbable(false);
            assertEquals(List.of(before, after), outer.test.tabOrder());
            spinner.setTabbable(true).setTabIndex(-1);
            assertTrue(outer.test.tabOrder().get(0) instanceof TextBox);
            spinner.setFocusable(false);
            assertFalse(spinner.requestFocus());
        }

        @Test
        void aPressOnTheStepButtonsFocusesTheField() {
            final FocusWithinTest outer = FocusWithinTest.this;
            final Spinner spinner = outer.ui.spinner();
            outer.test.show(spinner.setAnchor(Anchor.TOP_LEFT));
            final Rect bounds = spinner.bounds();
            outer.test.click(bounds.right() - 2, bounds.y() + 2);
            assertTrue(spinner.isFocused());
        }
    }
}
