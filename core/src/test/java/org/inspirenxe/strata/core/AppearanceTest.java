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

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.appearance.Appearance;
import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.visual.Visual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The appearance registry, and the controls that show values through it.
 */
class AppearanceTest {
    private final Appearances appearances = Strata.appearances();
    private final List<Subscription> registered = new ArrayList<>();

    private interface Named {
    }

    private interface Priced {
    }

    private static class Fruit implements Named, Priced {
        private final String name;

        Fruit(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Fruit " + this.name;
        }
    }

    private static final class Pear extends Fruit {
        Pear() {
            super("pear");
        }
    }

    private record Listing(Fruit fruit, int price) {
    }

    private record Icon(String name) implements Visual {
        @Override
        public Size size() {
            return new Size(12f, 12f);
        }
    }

    @AfterEach
    void unregister() {
        this.registered.forEach(Subscription::unsubscribe);
    }

    private <T> void register(final Class<T> type, final Appearance<? super T> appearance) {
        this.registered.add(this.appearances.register(type, appearance));
    }

    private static Appearance<Fruit> fruits() {
        return Appearance.<Fruit>builder()
                .icon(fruit -> new Icon(fruit.name))
                .name(fruit -> Text.of(fruit.name.toUpperCase()))
                .tooltip(fruit -> Tooltip.text("Fresh " + fruit.name))
                .build();
    }

    @Test
    void aValueWithNoAppearanceShowsItsString() {
        final Object value = new Object() {
            @Override
            public String toString() {
                return "plain";
            }
        };
        assertEquals("plain", this.appearances.nameOf(value).plain());
        assertEquals(Optional.empty(), this.appearances.iconOf(value));
        assertEquals(Optional.empty(), this.appearances.tooltipOf(value));
    }

    @Test
    void textIsNamedByItselfAndAVisualIsItsOwnIcon() {
        final Text text = Text.of("Ana").bold();
        assertSame(text, this.appearances.nameOf(text));
        final Icon icon = new Icon("x");
        assertEquals(Optional.of(icon), this.appearances.iconOf(icon));
    }

    @Test
    void subclassesTakeTheNearestRegistration() {
        this.register(Fruit.class, fruits());
        assertEquals("PEAR", this.appearances.nameOf(new Pear()).plain());
        this.register(Pear.class, Appearance.<Pear>builder().name(pear -> Text.of("a pear")).build());
        assertEquals("a pear", this.appearances.nameOf(new Pear()).plain());
    }

    @Test
    void ofTwoInterfacesTheFirstRegisteredWins() {
        this.register(Priced.class, Appearance.builder().name(value -> Text.of("priced")).build());
        this.register(Named.class, Appearance.builder().name(value -> Text.of("named")).build());
        assertEquals("priced", this.appearances.nameOf(new Fruit("fig")).plain());
    }

    @Test
    void aLaterRegistrationHidesAnEarlierOneUntilItIsRemoved() {
        this.register(Fruit.class, fruits());
        final Subscription override = this.appearances.register(Fruit.class,
                Appearance.<Fruit>builder().name(fruit -> Text.of("hidden")).build());
        assertEquals("hidden", this.appearances.nameOf(new Fruit("fig")).plain());
        override.unsubscribe();
        assertEquals("FIG", this.appearances.nameOf(new Fruit("fig")).plain());
    }

    @Test
    void asLooksLikeTheValueItLeadsTo() {
        this.register(Fruit.class, fruits());
        this.register(Listing.class, Appearance.as(Listing::fruit));
        final Listing listing = new Listing(new Fruit("plum"), 3);
        assertEquals("PLUM", this.appearances.nameOf(listing).plain());
        assertEquals(Optional.of(new Icon("plum")), this.appearances.iconOf(listing));
    }

    @Test
    void defaultListRowsShowTheIconBeforeTheName() {
        this.register(Fruit.class, fruits());
        final TestHost test = new TestHost(200, 100);
        final Component<?> row = test.factory().cell(new Fruit("kiwi"));
        final StackPanel panel = assertInstanceOf(StackPanel.class, row);
        test.show(panel);
        final List<Component<?>> children = panel.children();
        assertEquals(new Icon("kiwi"), assertInstanceOf(Image.class, children.get(0)).visual());
        assertEquals("KIWI", assertInstanceOf(Label.class, children.get(1)).text().plain());
        assertEquals(Tooltip.text("Fresh kiwi"), ((HasTooltip<?>) row).tooltip());
    }

    @Test
    void rowsAskForTheTooltipEachTimeThePointerArrives() {
        final AtomicInteger asked = new AtomicInteger();
        this.register(Fruit.class, Appearance.<Fruit>builder()
                .tooltip(fruit -> Tooltip.text("asked " + asked.incrementAndGet()))
                .build());
        final TestHost test = new TestHost(200, 100);
        final Label row = assertInstanceOf(Label.class, test.factory().cell(new Fruit("fig")));
        test.show(test.factory().absolutePanel().add(row, 0, 0));
        test.host().pointerMoved(row.bounds().x() + 2, row.bounds().y() + 2, Modifiers.NONE);
        assertEquals(Tooltip.text("asked 2"), row.tooltip());
    }

    @Test
    void labelFunctionsTakeTextOrAnyValue() {
        this.register(Fruit.class, fruits());
        final TestHost test = new TestHost(300, 100);
        final ComboBox<Fruit> box = test.factory().<Fruit>comboBox().setItems(List.of(new Fruit("fig")));
        final CycleButton<Integer> sizes = test.factory().<Integer>cycleButton("Size", size -> Text.of(size + " cm"))
                .setItems(List.of(10));
        test.show(test.factory().stackPanel().add(box).add(sizes));
        final NullCanvas canvas = new NullCanvas();
        test.host().render(canvas);
        final List<String> drawn = canvas.texts().stream().map(TextSpan::text).toList();
        assertTrue(drawn.contains("FIG"), drawn.toString());
        assertTrue(drawn.contains("Size: 10 cm"), drawn.toString());
    }

    @Test
    void anImageOfAValueShowsItsIconAndTooltip() {
        this.register(Fruit.class, fruits());
        final TestHost test = new TestHost(200, 100);
        final Image image = test.factory().image((Object) new Fruit("lime"));
        assertEquals(new Icon("lime"), image.visual());
        assertEquals(12f, image.imageWidth());
        assertEquals(Tooltip.text("Fresh lime"), image.tooltip());
        image.setTooltip("Mine");
        image.setValue(new Fruit("pear"));
        assertEquals(Tooltip.text("Fresh pear"), image.tooltip());
        image.setVisual(new Icon("plain"));
        assertNull(image.value());
    }
}
