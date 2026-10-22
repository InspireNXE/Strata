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
import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.NavigationView;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@code parent()}, {@code parent(Class)} and {@code ancestor(Class)} return for components in containers and in
 * composites, the built-in ones and a developer's, and that a composite's content cannot be added anywhere else.
 */
class ParentTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    @Nested
    class TheParent {
        @Test
        void ofAPanelsChildIsThePanel() {
            final Label label = ParentTest.this.ui.label("Ana");
            final StackPanel panel = ParentTest.this.ui.stackPanel().add(label);
            ParentTest.this.test.show(panel);

            assertSame(panel, label.parent().orElseThrow());
            assertEquals(Optional.empty(), panel.parent(), "the screen's root has no parent");
        }

        @Test
        void ofAnExpandersContentIsTheExpander() {
            final Label body = ParentTest.this.ui.label("Ben");
            final Expander expander = ParentTest.this.ui.expander("More").setContent(body).setExpanded(true);
            ParentTest.this.test.show(ParentTest.this.ui.stackPanel().add(expander));

            assertSame(expander, body.parent().orElseThrow());
            expander.setExpanded(false);
            assertEquals(Optional.empty(), body.parent(), "collapsing takes the content out of the tree");
        }

        @Test
        void ofAnInfoBarsActionIsTheBar() {
            final Button action = ParentTest.this.ui.button("Retry");
            final InfoBar bar = ParentTest.this.ui.infoBar().setAction(action);
            ParentTest.this.test.show(ParentTest.this.ui.stackPanel().add(bar));

            assertSame(bar, action.parent().orElseThrow());
        }

        @Test
        void ofANavigationViewsPageIsTheView() {
            final Label page = ParentTest.this.ui.label("Cleo");
            final NavigationView view = ParentTest.this.ui.navigationView().addItem("General", factory -> page);
            ParentTest.this.test.show(view);
            view.select(0);

            assertSame(view, page.parent().orElseThrow());
        }

        @Test
        void ofAnOverlaysContentIsNull() {
            ParentTest.this.test.show(ParentTest.this.ui.stackPanel());
            final Label popup = ParentTest.this.ui.label("Dev");
            ParentTest.this.ui.overlays().popup(popup, new Rect(0f, 0f, 10f, 10f));

            assertEquals(Optional.empty(), popup.parent());
        }
    }

    @Nested
    class ByType {
        @Test
        void parentMatchesOnlyTheDirectParent() {
            final Button button = ParentTest.this.ui.button("Save");
            final StackPanel column = ParentTest.this.ui.stackPanel().add(button);
            final Expander expander = ParentTest.this.ui.expander("Form").setContent(column).setExpanded(true);
            ParentTest.this.test.show(ParentTest.this.ui.stackPanel().add(expander));

            assertEquals(Optional.of(column), button.parent(Container.class));
            assertEquals(Optional.of(column), button.parent(StackPanel.class));
            assertEquals(Optional.empty(), button.parent(Expander.class), "the expander is the grandparent");
            assertEquals(Optional.of(expander), column.parent(Expander.class));
            assertEquals(Optional.empty(), column.parent(Container.class), "an expander is not a container");
        }

        @Test
        void parentIsEmptyWithoutAParent() {
            final Label label = ParentTest.this.ui.label("Ana");

            assertEquals(Optional.empty(), label.parent(Component.class));
        }

        @Test
        void ancestorLooksUpThroughComposites() {
            final Label label = ParentTest.this.ui.label("Ben");
            final StackPanel page = ParentTest.this.ui.stackPanel().add(label);
            final NavigationView view = ParentTest.this.ui.navigationView().addItem("General", factory -> page);
            final StackPanel screen = ParentTest.this.ui.stackPanel().add(view);
            ParentTest.this.test.show(screen);
            view.select(0);

            assertEquals(Optional.of(page), label.ancestor(Container.class), "the nearest container");
            assertEquals(Optional.of(view), label.ancestor(NavigationView.class));
            assertEquals(Optional.of(view), label.ancestor(SingleSelection.class), "by a raw attribute");
            assertEquals(Optional.of(screen), view.ancestor(Container.class));
            assertEquals(Optional.empty(), label.ancestor(Expander.class));
        }

        @Test
        void ancestorStartsAtTheParent() {
            final Label body = ParentTest.this.ui.label("Cleo");
            final Expander inner = ParentTest.this.ui.expander("Inner").setContent(body).setExpanded(true);
            final Expander outer = ParentTest.this.ui.expander("Outer").setContent(inner).setExpanded(true);
            ParentTest.this.test.show(outer);

            assertEquals(Optional.of(inner), body.ancestor(Expander.class), "the nearest one wins");
            assertEquals(Optional.of(outer), inner.ancestor(Expander.class), "a component is not its own ancestor");
            assertEquals(Optional.empty(), outer.ancestor(Expander.class));
        }

        @Test
        void ancestorFindsAnInfoBarByItsAttribute() {
            final Button button = ParentTest.this.ui.button("Retry");
            final InfoBar bar = ParentTest.this.ui.infoBar()
                    .setAction(ParentTest.this.ui.stackPanel().add(button));
            ParentTest.this.test.show(ParentTest.this.ui.stackPanel().add(bar));

            assertEquals(Optional.of(bar), button.ancestor(Openable.class));
        }
    }

    @Nested
    class ContentCannotMove {
        @Test
        void anExpandersContentCannotGoInAPanel() {
            final Label body = ParentTest.this.ui.label("Ana");
            final Expander expander = ParentTest.this.ui.expander("More").setContent(body).setExpanded(true);

            final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> ParentTest.this.ui.stackPanel().add(body));
            assertTrue(thrown.getMessage().contains(String.valueOf(expander)), thrown.getMessage());
        }

        @Test
        void anInfoBarsActionCannotGoInAPanel() {
            final Button action = ParentTest.this.ui.button("Retry");
            final InfoBar bar = ParentTest.this.ui.infoBar().setAction(action);

            final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> ParentTest.this.ui.stackPanel().add(action));
            assertTrue(thrown.getMessage().contains(String.valueOf(bar)), thrown.getMessage());
        }

        @Test
        void aNavigationViewsPageCannotGoInAPanel() {
            final Label page = ParentTest.this.ui.label("Ben");
            final NavigationView view = ParentTest.this.ui.navigationView().addItem("General", factory -> page);
            ParentTest.this.test.show(view);
            view.select(0);

            final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> ParentTest.this.ui.stackPanel().add(page));
            assertTrue(thrown.getMessage().contains(String.valueOf(view)), thrown.getMessage());
        }

        @Test
        void contentInAnotherCompositeCannotGoInAnExpanderOrASplitPane() {
            final Button action = ParentTest.this.ui.button("Retry");
            ParentTest.this.ui.infoBar().setAction(action);

            assertThrows(IllegalArgumentException.class, () -> ParentTest.this.ui.expander("More").setContent(action));
            assertThrows(IllegalArgumentException.class, () -> ParentTest.this.ui.splitPane().setFirst(action));
        }
    }

    /**
     * A card with a heading of its own and a body passed in by whoever makes it, written as a developer would write it.
     */
    static final class Card extends CustomComposite<Card> {
        final Label heading;

        Card(final ComponentFactory ui, final Component<?> body) {
            super(ui);
            this.heading = ui.label("Profile");
            this.addChild(this.heading);
            this.addContent(body);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            float width = 0f;
            float height = 0f;
            for (final Component<?> child : this.childList()) {
                final Size outer = context.measure(child, availableWidth, availableHeight);
                width = Math.max(width, outer.width());
                height += outer.height();
            }
            return new Size(width, height);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            float y = bounds.y();
            for (final Component<?> child : this.childList()) {
                final Size outer = context.outerSize(child);
                context.arrange(child, new Rect(bounds.x(), y, outer.width(), outer.height()));
                y += outer.height();
            }
        }
    }

    /**
     * A link that closes whatever closable control it sits in, found by asking for the attribute.
     */
    static final class CloseLink extends CustomComponent<CloseLink> {
        CloseLink(final ComponentFactory ui) {
            super(ui);
        }

        boolean close() {
            return this.ancestor(Closable.class).map(closable -> closable.close() != null).orElse(false);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(30f, 9f);
        }
    }

    @Nested
    class InADevelopersComposite {
        @Test
        void partsAndContentHaveTheCardAsParent() {
            final Label body = ParentTest.this.ui.label("Cleo");
            final Card card = new Card(ParentTest.this.ui, body);
            ParentTest.this.test.show(ParentTest.this.ui.stackPanel().add(card));

            assertSame(card, card.heading.parent().orElseThrow());
            assertSame(card, body.parent().orElseThrow());
            assertEquals(Optional.of(card), body.parent(Card.class));
            assertEquals(Optional.empty(), body.parent(Container.class), "a composite is not a container");
        }

        @Test
        void ancestorFindsTheCardFromDeepInside() {
            final Button button = ParentTest.this.ui.button("Edit");
            final StackPanel body = ParentTest.this.ui.stackPanel().add(button);
            final Card card = new Card(ParentTest.this.ui, body);
            final StackPanel screen = ParentTest.this.ui.stackPanel().add(card);
            ParentTest.this.test.show(screen);

            assertEquals(Optional.of(card), button.ancestor(Card.class));
            assertEquals(Optional.of(screen), card.ancestor(Container.class));
        }

        @Test
        void theCardsBodyCannotGoInAPanel() {
            final Label body = ParentTest.this.ui.label("Dev");
            final Card card = new Card(ParentTest.this.ui, body);

            final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> ParentTest.this.ui.stackPanel().add(body));
            assertTrue(thrown.getMessage().contains(String.valueOf(card)), thrown.getMessage());
        }

        @Test
        void aComponentCanAskForAnAttributeAboveIt() {
            final CloseLink link = new CloseLink(ParentTest.this.ui);
            final InfoBar bar = ParentTest.this.ui.infoBar().setAction(link);
            ParentTest.this.test.show(ParentTest.this.ui.stackPanel().add(bar));

            assertTrue(link.close());
            assertFalse(bar.isOpen());
            assertFalse(link.close(), "a closed bar drops its action, so nothing closable is above it");
        }
    }
}
