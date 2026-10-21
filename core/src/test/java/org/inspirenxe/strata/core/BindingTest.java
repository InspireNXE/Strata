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
import org.inspirenxe.strata.api.control.attribute.item.ItemSource;
import org.inspirenxe.strata.api.control.attribute.item.Selection;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.state.BindingQueue;
import org.inspirenxe.strata.core.state.StateImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * States, sources and {@code Component.bind}: tracking, two-way bindings through each control's request and report,
 * the attach and detach lifetime, the frame's flush, the cycle limit and a developer's own key.
 */
class BindingTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private void flush() {
        this.test.host().flush();
    }

    private void clickCenter(final Component<?> component) {
        final Rect bounds = component.bounds();
        this.test.click(bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f);
    }

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    private void type(final String text) {
        text.codePoints().forEach(codePoint -> this.test.host().charTyped(codePoint, Modifiers.NONE));
    }

    private static int dependents(final State<?> state) {
        return ((StateImpl<?>) state).dependentCount();
    }

    @Nested
    class States {
        @Test
        void holdAValueAndIgnoreEqualSets() {
            final State<String> name = State.of("Ana");
            assertEquals("Ana", name.get());
            name.set("Ben");
            assertEquals("Ben", name.get());
            final State<String> empty = State.of(null);
            assertEquals(null, empty.get());
        }

        @Test
        void mapAndCombineComputeFromTheirSources() {
            final State<Integer> price = State.of(3);
            final State<Integer> count = State.of(4);
            final Source<Integer> total = State.combine(price, count, (p, c) -> p * c);
            final Source<String> label = total.map(value -> "Total: " + value);
            assertEquals("Total: 12", label.get());
            count.set(5);
            assertEquals("Total: 15", label.get());
        }

        @Test
        void aComputedSourceFollowsWhatItReadLastTime() {
            final State<Boolean> useFirst = State.of(true);
            final State<String> first = State.of("Ana");
            final State<String> second = State.of("Ben");
            final Label label = BindingTest.this.ui.label();
            label.bind(HasText.TEXT, () -> Text.of(useFirst.get() ? first.get() : second.get()));
            BindingTest.this.test.show(label);

            assertEquals("Ana", label.text().plain());
            assertEquals(1, dependents(first));
            assertEquals(0, dependents(second), "the source never read the second state");

            useFirst.set(false);
            BindingTest.this.flush();
            assertEquals("Ben", label.text().plain());
            assertEquals(0, dependents(first), "the source no longer reads the first state");
            assertEquals(1, dependents(second));

            first.set("Cleo");
            BindingTest.this.flush();
            assertEquals("Ben", label.text().plain());
            second.set("Dev");
            BindingTest.this.flush();
            assertEquals("Dev", label.text().plain());
        }

        @Test
        void aMappedSourceDrivesATitle() {
            final State<Integer> count = State.of(2);
            final Expander expander = BindingTest.this.ui.expander();
            expander.bind(Titled.TITLE, count.map(value -> Text.of(value + " offers")));
            BindingTest.this.test.show(expander);
            assertEquals("2 offers", expander.title().plain());
            count.set(3);
            BindingTest.this.flush();
            assertEquals("3 offers", expander.title().plain());
        }

        @Test
        void changesWaitForTheFlushAndCostOneUpdate() {
            final State<String> name = State.of("Ana");
            final AtomicInteger evaluations = new AtomicInteger();
            final Label label = BindingTest.this.ui.label();
            label.bind(HasText.TEXT, () -> {
                evaluations.incrementAndGet();
                return Text.of(name.get());
            });
            BindingTest.this.test.show(label);
            assertEquals(1, evaluations.get());

            name.set("Ben");
            name.set("Cleo");
            name.set("Dev");
            assertEquals("Ana", label.text().plain(), "nothing changes before the flush");
            BindingTest.this.flush();
            assertEquals("Dev", label.text().plain());
            assertEquals(2, evaluations.get(), "three sets cost one evaluation");
        }
    }

    @Nested
    class TwoWay {
        @Test
        void aToggleFollowsItsStateAndWritesTheUsersChanges() {
            final State<Boolean> music = State.of(true);
            final CheckBox box = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, music);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));
            assertTrue(box.isSelected(), "the binding sets the box on attach");

            music.set(false);
            BindingTest.this.flush();
            assertFalse(box.isSelected());

            BindingTest.this.clickCenter(box);
            assertTrue(box.isSelected());
            assertTrue(music.get(), "the user's change reaches the state");
        }

        @Test
        void aStateChangeFiresNoRequest() {
            final State<Boolean> music = State.of(false);
            final CheckBox box = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, music);
            final List<String> log = new ArrayList<>();
            box.onEvent(Toggle.TOGGLE, event -> log.add("toggle"))
                    .onEvent(Toggle.TOGGLED, event -> log.add("toggled " + event.value()));
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));

            music.set(true);
            BindingTest.this.flush();
            assertEquals(List.of("toggled true"), log);
        }

        @Test
        void aRefusedToggleLeavesTheStateAlone() {
            final State<Boolean> music = State.of(false);
            final CheckBox box = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, music);
            box.onEvent(Toggle.TOGGLE, event -> event.cancel());
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));

            BindingTest.this.clickCenter(box);
            assertFalse(box.isSelected());
            assertFalse(music.get());
        }

        @Test
        void aSliderWritesTheValueItsRequestAccepted() {
            final State<Double> volume = State.of(4d);
            final Slider slider = BindingTest.this.ui.slider().setRange(0d, 10d).setStep(1d);
            slider.bind(Ranged.VALUE, volume);
            slider.onEvent(Ranged.CHANGE, event -> event.setValue(event.value() + 1d));
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(slider));
            assertEquals(4d, slider.value());

            slider.requestFocus();
            BindingTest.this.key(Key.RIGHT);
            assertEquals(6d, slider.value(), "the handler rewrote 5 to 6");
            assertEquals(6d, volume.get(), "only the accepted value reaches the state");

            volume.set(2d);
            BindingTest.this.flush();
            assertEquals(2d, slider.value());
        }

        @Test
        void aTextBoxWritesEditsTheRequestAccepted() {
            final State<String> name = State.of("");
            final TextBox box = BindingTest.this.ui.textBox().setWidth(200f).bind(TextInput.TEXT, name);
            box.onEvent(TextInput.EDIT, event -> {
                if (event.value().contains("!")) {
                    event.cancel();
                } else {
                    event.setValue(event.value().toUpperCase());
                }
            });
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));
            box.requestFocus();

            BindingTest.this.type("ana");
            assertEquals("ANA", box.text());
            assertEquals("ANA", name.get());
            BindingTest.this.type("!");
            assertEquals("ANA", name.get(), "a refused edit writes nothing");

            name.set("Ben");
            BindingTest.this.flush();
            assertEquals("Ben", box.text());
        }

        @Test
        void codeChangesAreWrittenToo() {
            final State<Boolean> music = State.of(false);
            final CheckBox box = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, music);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));
            box.setSelected(true);
            assertTrue(music.get(), "the report fires for code too, so the state follows");
        }
    }

    @Nested
    class OneWay {
        @Test
        void aComputedSourceEnablesAButton() {
            final State<String> name = State.of("");
            final TextBox box = BindingTest.this.ui.textBox().setWidth(200f).bind(TextInput.TEXT, name);
            final Button save = BindingTest.this.ui.button("Save")
                    .bind(Component.ENABLED, () -> !name.get().isBlank());
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box).add(save));
            assertFalse(save.isEnabled());

            box.requestFocus();
            BindingTest.this.type("Cleo");
            BindingTest.this.flush();
            assertTrue(save.isEnabled());
        }

        @Test
        void aOneWayKeyNeverWritesBack() {
            final State<Boolean> shown = State.of(true);
            final Label label = BindingTest.this.ui.label("Ana").bind(Component.VISIBLE, shown);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(label));
            label.setVisible(false);
            assertTrue(shown.get());
        }
    }

    @Nested
    class Loops {
        @Test
        void equalValuesAreNotWritten() {
            final State<Double> volume = State.of(3d);
            final Slider slider = BindingTest.this.ui.slider().setRange(0d, 10d).setValue(3d);
            final List<Double> reports = new ArrayList<>();
            slider.onEvent(Ranged.CHANGED, event -> reports.add(event.value()));
            slider.bind(Ranged.VALUE, volume);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(slider));
            assertEquals(List.of(), reports, "the slider already held the state's value");

            volume.set(5d);
            BindingTest.this.flush();
            BindingTest.this.flush();
            assertEquals(List.of(5d), reports, "one change, one report, no echo");
        }

        @Test
        void aValueTheControlAdjustsIsNotWrittenBack() {
            final State<Double> volume = State.of(15d);
            final Slider slider = BindingTest.this.ui.slider().setRange(0d, 10d).bind(Ranged.VALUE, volume);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(slider));
            assertEquals(10d, slider.value());
            assertEquals(15d, volume.get(), "the report the binding caused is not written back");
        }

        @Test
        void twoControlsOnOneStateStayInStep() {
            final State<Boolean> music = State.of(false);
            final CheckBox first = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, music);
            final CheckBox second = BindingTest.this.ui.checkBox("Also music").bind(Toggle.SELECTED, music);
            final AtomicInteger toggles = new AtomicInteger();
            first.onEvent(Toggle.TOGGLED, event -> toggles.incrementAndGet());
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(first).add(second));

            BindingTest.this.clickCenter(second);
            assertTrue(music.get());
            BindingTest.this.flush();
            assertTrue(first.isSelected());
            assertEquals(1, toggles.get());
            BindingTest.this.flush();
            assertEquals(1, toggles.get(), "nothing bounces back");
        }

        @Test
        void aCycleIsCutOffAtTheRoundLimit() {
            final State<Boolean> first = State.of(false);
            final State<Boolean> second = State.of(false);
            final CheckBox a = BindingTest.this.ui.checkBox("A").bind(Toggle.SELECTED, first);
            final CheckBox b = BindingTest.this.ui.checkBox("B").bind(Toggle.SELECTED, second);
            final AtomicInteger toggles = new AtomicInteger();
            a.onEvent(Toggle.TOGGLED, event -> {
                toggles.incrementAndGet();
                second.set(event.value());
            });
            b.onEvent(Toggle.TOGGLED, event -> first.set(!event.value()));
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(a).add(b));

            first.set(true);
            assertFalse(BindingTest.this.test.host().root().flushBindings(), "the cycle is reported");
            assertEquals(BindingQueue.ROUND_LIMIT / 2, toggles.get(), "A toggled every other round until the limit");
            assertTrue(BindingTest.this.test.host().root().flushBindings(), "the dropped updates do not come back");
        }
    }

    @Nested
    class Lifetime {
        @Test
        void aBindingFollowsItsStateOnlyWhileAttached() {
            final State<Boolean> music = State.of(true);
            final CheckBox box = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, music);
            assertFalse(box.isSelected(), "nothing happens before attach");
            assertEquals(0, dependents(music));

            final StackPanel panel = BindingTest.this.ui.stackPanel().add(box);
            BindingTest.this.test.show(panel);
            assertTrue(box.isSelected());
            assertEquals(1, dependents(music));

            panel.remove(box);
            assertEquals(0, dependents(music), "a detached binding lets go of its state");
            music.set(false);
            BindingTest.this.flush();
            assertTrue(box.isSelected(), "a detached box does not follow");
            box.setSelected(true);
            box.setSelected(false);
            assertFalse(music.get());

            music.set(true);
            panel.add(box);
            assertTrue(box.isSelected(), "attaching again reads the state");
            assertEquals(1, dependents(music));
        }

        @Test
        void closingTheScreenLeaksNothing() {
            final State<String> name = State.of("Ana");
            final TextBox box = BindingTest.this.ui.textBox().bind(TextInput.TEXT, name);
            final Button save = BindingTest.this.ui.button("Save").bind(Component.ENABLED, () -> !name.get().isEmpty());
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box).add(save));
            assertEquals(2, dependents(name));

            BindingTest.this.test.host().close();
            assertEquals(0, dependents(name));
        }

        @Test
        void bindingAnAttachedComponentSetsItAtOnce() {
            final Label label = BindingTest.this.ui.label("Ana");
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(label));
            label.bind(HasText.TEXT, State.of(Text.of("Ben")));
            assertEquals("Ben", label.text().plain());
        }

        @Test
        void bindingAKeyAgainReplacesTheEarlierBinding() {
            final State<Boolean> first = State.of(true);
            final State<Boolean> second = State.of(false);
            final CheckBox box = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, first);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));
            box.bind(Toggle.SELECTED, second);
            assertFalse(box.isSelected());
            assertEquals(0, dependents(first));

            first.set(false);
            first.set(true);
            BindingTest.this.flush();
            assertFalse(box.isSelected(), "the first state no longer drives the box");
        }

        @Test
        void aSetFromAnotherThreadWaitsForTheFlush() throws InterruptedException {
            final State<Boolean> music = State.of(false);
            final CheckBox box = BindingTest.this.ui.checkBox("Music").bind(Toggle.SELECTED, music);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));

            final Thread network = new Thread(() -> music.set(true));
            network.start();
            network.join();
            assertFalse(box.isSelected(), "the UI thread has not flushed yet");
            BindingTest.this.flush();
            assertTrue(box.isSelected());
        }

        @Test
        void layoutAndRenderFlushFirst() {
            final State<Boolean> shown = State.of(true);
            final Label label = BindingTest.this.ui.label("Ana").bind(Component.VISIBLE, shown);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(label));
            shown.set(false);
            BindingTest.this.test.host().layout();
            assertFalse(label.isVisible());
            shown.set(true);
            BindingTest.this.test.host().render(new NullCanvas());
            assertTrue(label.isVisible());
        }
    }

    @Nested
    class Lists {
        @Test
        void boundItemsAreReplacedWholeAndKeepTheSelection() {
            final State<List<String>> names = State.of(List.of("Ana", "Ben", "Cleo"));
            final State<String> chosen = State.of("Ben");
            final ListView<String> list = BindingTest.this.ui.<String>listView()
                    .bind(ItemSource.itemsKey(), names)
                    .bind(Selection.selectedItem(), chosen);
            BindingTest.this.test.show(list);
            assertEquals(List.of("Ana", "Ben", "Cleo"), list.items());
            assertEquals(1, list.selectedIndex());

            names.set(List.of("Cleo", "Ben"));
            BindingTest.this.flush();
            assertEquals(List.of("Cleo", "Ben"), list.items());
            assertEquals(1, list.selectedIndex(), "the selection stays on Ben");
            assertEquals("Ben", chosen.get());

            list.select(0);
            assertEquals("Cleo", chosen.get());
            names.set(List.of("Ana"));
            BindingTest.this.flush();
            assertEquals(-1, list.selectedIndex());
            assertEquals(null, chosen.get(), "no selection writes null");
        }

        @Test
        void theStateWinsOverTheItemAComboBoxSelectsFirst() {
            final State<List<String>> sorts = State.of(List.of("Name", "Price", "Seller"));
            final State<String> sort = State.of("Price");
            final ComboBox<String> box = BindingTest.this.ui.<String>comboBox()
                    .bind(ItemSource.itemsKey(), sorts)
                    .bind(Selection.selectedItem(), sort);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(box));
            assertEquals("Price", box.selected().orElseThrow());
            assertEquals("Price", sort.get());
        }

        @Test
        void aBoundIndexFollowsBothWays() {
            final State<Integer> index = State.of(2);
            final ListView<String> list = BindingTest.this.ui.<String>listView().setItems(List.of("Ana", "Ben", "Cleo"))
                    .bind(Selection.SELECTED_INDEX, index);
            BindingTest.this.test.show(list);
            assertEquals(2, list.selectedIndex());
            list.select(0);
            assertEquals(0, index.get());
        }
    }

    @Nested
    class DevelopersOwnKey {
        @Test
        void aCustomControlBindsThroughTheKeyItDeclares() {
            final State<Integer> stars = State.of(2);
            final Rating rating = new Rating(BindingTest.this.ui).bind(Rating.STARS, stars);
            BindingTest.this.test.show(BindingTest.this.ui.stackPanel().add(rating));
            assertEquals(2, rating.stars());

            BindingTest.this.clickCenter(rating);
            assertEquals(3, rating.stars());
            assertEquals(3, stars.get());

            rating.onEvent(Rating.RATE, event -> event.cancel());
            BindingTest.this.clickCenter(rating);
            assertEquals(3, stars.get(), "a refused request writes nothing");

            stars.set(5);
            BindingTest.this.flush();
            assertEquals(5, rating.stars());
        }
    }

    /**
     * A control written the way a developer would: a request for the user's change, a report for every change, and
     * a key beside them.
     */
    static final class Rating extends CustomComponent<Rating> {
        static final EventType<ValueRequestEvent<Rating, Integer>> RATE = EventType.valueRequest("rating.rate");
        static final EventType<ValueEvent<Rating, Integer>> RATED = EventType.valueReport("rating.rated");
        static final PropertyKey<Rating, Integer> STARS = PropertyKey.of("rating.stars", Rating::stars,
                Rating::setStars, RATED);

        private int stars;

        Rating(final ComponentFactory ui) {
            super(ui);
        }

        int stars() {
            return this.stars;
        }

        Rating setStars(final int stars) {
            final int previous = this.stars;
            if (stars != previous) {
                this.stars = stars;
                this.report(RATED, stars, previous);
            }
            return this;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(50f, 10f);
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            this.request(RATE, Math.min(this.stars + 1, 5), this::setStars);
            event.consume();
        }
    }
}
