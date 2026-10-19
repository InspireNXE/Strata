package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventTypes;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void eventsTakeNoSnapshotsWithoutAHandlerThatAsks() {
        final TextBox box = this.ui.textBox().setWidth(100);
        this.test.show(this.ui.absolutePanel().add(box));
        final List<Optional<ComponentSnapshot>> seen = new ArrayList<>();
        final List<Event> events = new ArrayList<>();
        box.on(EventTypes.CHAR, event -> {
            seen.add(event.snapshotOf(box));
            events.add(event);
        });
        box.requestFocus();

        this.test.host().charTyped('a', Modifiers.NONE);

        assertEquals(List.of(Optional.empty()), seen);
        assertThrows(IllegalStateException.class, events.getFirst()::snapshot);
    }

    @Test
    void snapshotHoldsStateFromBeforeTheDefaultBehavior() {
        final TextBox box = this.ui.textBox().setWidth(100).setText("ab");
        this.test.show(this.ui.absolutePanel().add(box));
        box.requestFocus();
        final List<TextBox.State> before = new ArrayList<>();
        final List<String> live = new ArrayList<>();
        box.onWithSnapshot(EventTypes.CHAR, Phase.AFTER, event -> {
            before.add(event.snapshot().state(TextBox.State.class).orElseThrow());
            live.add(box.text());
        });

        this.test.host().charTyped('c', Modifiers.NONE);

        assertEquals(List.of(new TextBox.State("ab", 2, 2, box.maxLength(), false)), before);
        assertEquals(List.of("abc"), live);
    }

    @Test
    void ancestorSeesTargetAndItselfAsTheyWereWhenTheEventFired() {
        final Button button = this.ui.button("Go").setId("button");
        final StackPanel middle = this.ui.stackPanel().setId("middle").add(button);
        final AbsolutePanel outer = this.ui.absolutePanel().setId("outer").add(middle);
        this.test.show(outer);

        // A handler on the target runs first and changes both components
        button.on(EventTypes.POINTER_PRESS, event -> {
            button.setId("button-changed");
            outer.setId("outer-changed");
        });
        final List<String> seen = new ArrayList<>();
        outer.onWithSnapshot(EventTypes.POINTER_PRESS, event -> {
            seen.add(event.snapshot().id());
            seen.add(event.currentSnapshot().id());
            seen.add(String.valueOf(event.snapshot().pressed()));
            seen.add(String.valueOf(event.snapshotOf(middle).isPresent()));
            seen.add(button.id() + "/" + outer.id());
        });

        this.test.click(5, 5);

        assertEquals(List.of("button", "outer", "true", "false", "button-changed/outer-changed"), seen);
    }

    @Test
    void containerSnapshotHoldsOnlyItsOwnState() {
        final Button button = this.ui.button("Go");
        final StackPanel panel = this.ui.stackPanel().setOrientation(Orientation.HORIZONTAL).setSpacing(3).add(button);
        this.test.show(this.ui.absolutePanel().add(panel));
        final List<Object> states = new ArrayList<>();
        panel.onWithSnapshot(EventTypes.POINTER_PRESS, event -> states.add(event.currentSnapshot().state()));

        this.test.click(5, 5);

        assertEquals(List.of(new StackPanel.State(Orientation.HORIZONTAL, 3)), states);
    }

    @Test
    void directEventsDoNotSnapshotForAncestors() {
        final TextBox box = this.ui.textBox().setWidth(100);
        final AbsolutePanel panel = this.ui.absolutePanel().add(box);
        this.test.show(panel);
        final List<Boolean> seen = new ArrayList<>();
        panel.onWithSnapshot(EventTypes.FOCUS_GAINED, event -> seen.add(true));
        box.on(EventTypes.FOCUS_GAINED, event -> seen.add(event.snapshotOf(box).isPresent()));

        box.requestFocus();

        assertEquals(List.of(false), seen, "focus does not bubble, so the panel's handler is off the path");
    }

    @Test
    void customStateIsReadOnlyWhenSnapshotsAreTaken() {
        final Counter counter = new Counter(this.ui);
        this.test.show(this.ui.absolutePanel().add(counter));
        this.test.click(5, 5);
        assertEquals(0, counter.snapshots, "no handler asked for snapshots");

        final List<Integer> before = new ArrayList<>();
        final Subscription subscription = counter.onWithSnapshot(EventTypes.CLICK, Phase.AFTER,
                event -> before.add(event.snapshot().state(Integer.class).orElseThrow()));
        this.test.click(5, 5);
        this.test.click(5, 5);
        subscription.close();
        this.test.click(5, 5);

        assertEquals(List.of(1, 2), before, "each snapshot holds the count from before that click");
        assertEquals(2, counter.snapshots);
        assertEquals(4, counter.clicks);
    }

    @Test
    void snapshotStaysValidAfterDelivery() {
        final Button button = this.ui.button("Go");
        this.test.show(this.ui.absolutePanel().add(button));
        final List<ComponentSnapshot> kept = new ArrayList<>();
        button.onWithSnapshot(EventTypes.CLICK, event -> kept.add(event.snapshot()));

        this.test.click(5, 5);
        button.setText("Changed").setEnabled(false);

        final ComponentSnapshot snapshot = kept.getFirst();
        assertTrue(snapshot.enabled());
        assertEquals(Optional.of(new Button.State("Go")), snapshot.state(Button.State.class));
        assertEquals(Optional.empty(), snapshot.state(TextBox.State.class), "a state of another type is empty");
        assertFalse(button.isEnabled());
    }

    private static final class Counter extends CustomComponent<Counter> {
        private int clicks;
        private int snapshots;

        Counter(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(20, 20);
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            this.clicks++;
        }

        @Override
        protected Object snapshotState() {
            this.snapshots++;
            return this.clicks;
        }
    }
}
