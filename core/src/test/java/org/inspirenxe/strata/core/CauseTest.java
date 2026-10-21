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
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.event.cause.Cause;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKey;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.event.cause.Device;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.event.CauseStackImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Causes as a developer meets them: who an event came from, what a host puts for input, frames opened around a
 * change from another person, bindings, a control acting on another, and the frame rules of the stack itself.
 */
class CauseTest {
    private static final Object ANA = "Ana";

    @AfterEach
    void stackIsEmptyAgain() {
        assertEquals(0, CauseStackImpl.forThread().depth(), "every frame a test opened was closed");
    }

    private static void clickCenterOf(final TestHost test, final Component<?> component) {
        final Rect bounds = component.bounds();
        test.click(bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f);
    }

    @Nested
    class Input {
        private final TestPlatform platform = new TestPlatform();
        private final TestHost test;
        private final ComponentFactory ui;

        Input() {
            this.platform.setUser(ANA);
            this.test = new TestHost(this.platform, Themes.flat(), 300, 300);
            this.ui = this.test.factory();
        }

        @Test
        void aClickIsTheUsersWithTheMouseButtonAndClicks() {
            final CheckBox box = this.ui.checkBox(Text.of("Agree"));
            this.test.show(this.ui.stackPanel().add(box));
            final List<Cause> causes = new ArrayList<>();
            final List<Origin> origins = new ArrayList<>();
            box.onEvent(Toggle.TOGGLE, event -> causes.add(event.cause()))
                    .onEvent(Toggle.TOGGLED, event -> origins.add(event.origin()));

            clickCenterOf(this.test, box);

            assertEquals(1, causes.size(), "the request fired for the user's click");
            final Cause cause = causes.getFirst();
            assertEquals(new Origin.User(ANA), cause.origin(), "the platform's user is the identity");
            assertEquals(Optional.of(Device.MOUSE), cause.context(CauseKeys.DEVICE));
            assertEquals(Optional.of(MouseButton.LEFT), cause.context(CauseKeys.BUTTON));
            assertEquals(Optional.of(1), cause.context(CauseKeys.CLICKS));
            assertEquals(Optional.of(Modifiers.NONE), cause.context(CauseKeys.MODIFIERS));
            assertTrue(cause.context(CauseKeys.SCREEN).isPresent(), "the host's screen is in the context");
            assertEquals(List.of(new Origin.User(ANA)), origins, "the report carries the same origin");
        }

        @Test
        void aKeyIsTheUsersWithTheKeyboard() {
            final CheckBox box = this.ui.checkBox(Text.of("Agree"));
            this.test.show(this.ui.stackPanel().add(box));
            box.requestFocus();
            final List<Cause> causes = new ArrayList<>();
            box.onEvent(Toggle.TOGGLED, event -> causes.add(event.cause()));

            this.test.host().key(Key.SPACE, KeyAction.PRESS, Modifiers.NONE);
            this.test.host().key(Key.SPACE, KeyAction.RELEASE, Modifiers.NONE);

            assertEquals(1, causes.size(), "space toggled the box");
            assertInstanceOf(Origin.User.class, causes.getFirst().origin());
            assertEquals(Optional.of(Device.KEYBOARD), causes.getFirst().context(CauseKeys.DEVICE));
            assertTrue(causes.getFirst().context(CauseKeys.BUTTON).isEmpty(), "keys have no mouse button");
        }

        @Test
        void aRadioGroupIsOnTheChainOfTheButtonItDeselects() {
            final RadioGroup<String> group = this.ui.radioGroup();
            final RadioButton first = group.add(Text.of("First"), "first");
            final RadioButton second = group.add(Text.of("Second"), "second");
            this.test.show(this.ui.stackPanel().add(first).add(second));
            group.selectItem("first");
            final List<Cause> causes = new ArrayList<>();
            first.onEvent(Toggle.TOGGLED, event -> causes.add(event.cause()));

            clickCenterOf(this.test, second);

            assertEquals(1, causes.size(), "the first button reported its deselection");
            assertEquals(new Origin.User(ANA), causes.getFirst().origin(), "the user's click is still the origin");
            assertSame(group, causes.getFirst().first(RadioGroup.class).orElseThrow(), "the group deselected it");
            assertSame(group, causes.getFirst().root().orElseThrow(), "the group is the direct cause");
        }

        @Test
        void nothingIsLeftOpenWhenAHandlerThrows() {
            final CheckBox box = this.ui.checkBox(Text.of("Agree"));
            this.test.show(this.ui.stackPanel().add(box));
            box.onEvent(Toggle.TOGGLE, event -> {
                throw new IllegalStateException("boom");
            });

            assertThrows(IllegalStateException.class, () -> clickCenterOf(this.test, box));
            // stackIsEmptyAgain checks the frames
        }
    }

    @Nested
    class FromCode {
        private final TestHost test = new TestHost(300, 300);
        private final ComponentFactory ui = this.test.factory();

        @Test
        void aSetterReportsCodeAndAsksNobody() {
            final CheckBox box = this.ui.checkBox(Text.of("Agree"));
            this.test.show(this.ui.stackPanel().add(box));
            final List<Origin> asked = new ArrayList<>();
            final List<Origin> told = new ArrayList<>();
            box.onEvent(Toggle.TOGGLE, event -> asked.add(event.origin()))
                    .onEvent(Toggle.TOGGLED, event -> told.add(event.origin()));

            box.setSelected(true);

            assertEquals(List.of(), asked, "requests fire only for the user");
            assertEquals(List.of(Origin.CODE), told);
        }

        @Test
        void aRemoteChangeReportsThePersonAndCannotBeCanceled() {
            final CheckBox box = this.ui.checkBox(Text.of("Agree"));
            this.test.show(this.ui.stackPanel().add(box));
            box.onEvent(Toggle.TOGGLE, event -> event.cancel());
            final List<Origin> told = new ArrayList<>();
            box.onEvent(Toggle.TOGGLED, event -> told.add(event.origin()));

            try (CauseFrame _ = this.ui.causes().pushFrame().put(CauseKeys.ORIGIN, new Origin.Remote("Ben"))) {
                box.setSelected(true);
            }

            assertTrue(box.isSelected(), "a remote change is applied, not asked about");
            assertEquals(List.of(new Origin.Remote("Ben")), told);
        }

        @Test
        void requestsFromCodeGoAheadWithoutFiring() {
            final EventSystemTest.CounterImpl counter = new EventSystemTest.CounterImpl(this.ui);
            this.test.show(this.ui.absolutePanel().add(counter));
            counter.onEvent(EventSystemTest.Counter.STEP, event -> event.cancel());

            try (CauseFrame _ = this.ui.causes().pushFrame().put(CauseKeys.ORIGIN, new Origin.Remote("Ben"))) {
                counter.remoteStep();
            }

            assertEquals(1, counter.count(), "a request with any origin but the user's applies at once");
        }

        @Test
        void aBindingWritesWithItsOwnOrigin() {
            final CheckBox box = this.ui.checkBox(Text.of("Agree"));
            final State<Boolean> agreed = State.of(false);
            box.bind(Toggle.SELECTED, agreed);
            this.test.show(this.ui.stackPanel().add(box));
            final List<Origin> asked = new ArrayList<>();
            final List<Origin> told = new ArrayList<>();
            box.onEvent(Toggle.TOGGLE, event -> asked.add(event.origin()))
                    .onEvent(Toggle.TOGGLED, event -> told.add(event.origin()));

            agreed.set(true);
            this.test.host().flush();

            assertTrue(box.isSelected());
            assertEquals(List.of(), asked, "a binding's write is not the user's");
            assertEquals(List.of(new Origin.Binding(agreed)), told, "the report names the binding's source");
        }
    }

    @Nested
    class Stack {
        private final CauseStack stack = CauseStackImpl.forThread();
        private final CauseKey<String> note = CauseKey.of("test:note", String.class);

        @Test
        void anInnerFrameHidesAnOuterValueUntilItCloses() {
            try (CauseFrame _ = this.stack.pushFrame().put(this.note, "outer")) {
                try (CauseFrame _ = this.stack.pushFrame().put(this.note, "inner")) {
                    assertEquals(Optional.of("inner"), this.stack.context(this.note));
                    assertEquals(Optional.of("inner"), this.stack.current().context(this.note));
                }
                assertEquals(Optional.of("outer"), this.stack.context(this.note));
            }
            assertTrue(this.stack.context(this.note).isEmpty());
            assertSame(Origin.CODE, this.stack.origin(), "no frame, no origin");
        }

        @Test
        void theChainListsTheMostRecentFirst() {
            try (CauseFrame outer = this.stack.pushFrame().push("a")) {
                outer.push("b");
                try (CauseFrame _ = this.stack.pushFrame().push(1)) {
                    final Cause cause = this.stack.current();
                    assertEquals(List.of(1, "b", "a"), cause.all());
                    assertEquals(Optional.of(1), cause.root());
                    assertEquals(Optional.of("b"), cause.first(String.class));
                    assertEquals(List.of("b", "a"), cause.all(String.class));
                }
            }
            assertTrue(this.stack.current().root().isEmpty(), "an empty stack has no root");
        }

        @Test
        void aCauseDoesNotChangeOnceMadeAndIsKeptUntilTheStackDoes() {
            try (CauseFrame frame = this.stack.pushFrame().push("a")) {
                final Cause before = this.stack.current();
                assertSame(before, this.stack.current(), "asking again makes nothing new");
                frame.push("b");
                assertEquals(List.of("a"), before.all(), "a cause already made keeps its chain");
                assertEquals(List.of("b", "a"), this.stack.current().all());
            }
        }

        @Test
        void closingAFrameBelowTheTopClosesTheOnesAboveIt() {
            final CauseFrame outer = this.stack.pushFrame().put(this.note, "outer");
            final CauseFrame inner = this.stack.pushFrame().put(this.note, "inner");
            outer.close();
            assertTrue(this.stack.context(this.note).isEmpty());
            assertThrows(IllegalStateException.class, () -> inner.push("late"), "the inner frame closed too");
            inner.close();
            outer.close();
        }

        @Test
        void aValueMustMatchItsKeysType() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            final CauseKey<Object> raw = (CauseKey) this.note;
            try (CauseFrame frame = this.stack.pushFrame()) {
                assertThrows(ClassCastException.class, () -> frame.put(raw, 42));
            }
        }
    }

    @Nested
    class PlatformStack {

        @Test
        void hostsPushInputFramesOnThePlatformsStack() {
            final RecordingStack recording = new RecordingStack();
            final TestPlatform platform = new TestPlatform();
            platform.setCauses(recording);
            final TestHost test = new TestHost(platform, Themes.flat(), 300, 300);
            final ComponentFactory ui = test.factory();
            assertSame(recording, ui.causes(), "developers get the platform's stack");
            final CheckBox box = ui.checkBox(Text.of("Agree"));
            test.show(ui.stackPanel().add(box));
            final List<Origin> told = new ArrayList<>();
            box.onEvent(Toggle.TOGGLED, event -> told.add(event.origin()));

            clickCenterOf(test, box);

            assertTrue(recording.pushed > 0, "the host opened its frames on the platform's stack");
            assertInstanceOf(Origin.User.class, told.getFirst(), "events read the platform's stack");
        }
    }

    /**
     * A platform's stack, as an adapter would supply one: it delegates to a stack of its own and counts frames.
     */
    private static final class RecordingStack implements CauseStack {
        private final CauseStackImpl inner = CauseStackImpl.forThread();
        private int pushed;

        @Override
        public CauseFrame pushFrame() {
            this.pushed++;
            return this.inner.pushFrame();
        }

        @Override
        public Cause current() {
            return this.inner.current();
        }

        @Override
        public <V> Optional<V> context(final CauseKey<V> key) {
            return this.inner.context(key);
        }
    }
}
