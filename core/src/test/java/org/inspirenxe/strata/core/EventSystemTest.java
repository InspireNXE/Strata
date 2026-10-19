package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The event system as a developer's own control sees it: priorities, requests, reports, and the paint and measure
 * events.
 */
class EventSystemTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    /**
     * A control with a request that carries a value, one that does not, and their reports.
     */
    interface Counter extends Component<Counter> {
        EventType<ValueRequestEvent<Counter, Integer>> STEP = EventType.valueRequest("counter.step");
        EventType<ValueEvent<Counter, Integer>> STEPPED = EventType.valueReport("counter.stepped");
        EventType<RequestEvent<Counter>> CLEAR = EventType.request("counter.clear");
        EventType<ControlEvent<Counter>> CLEARED = EventType.report("counter.cleared");

        int count();

        Counter setCount(int count);
    }

    static final class CounterImpl extends CustomComponent<Counter> implements Counter {
        private int count;

        CounterImpl(final ComponentFactory ui) {
            super(ui);
        }

        void relayout() {
            this.invalidateLayout();
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public Counter setCount(final int count) {
            final int previous = this.count;
            if (count != previous) {
                this.count = count;
                this.report(STEPPED, count, previous);
            }
            return this;
        }

        void userStep() {
            this.request(STEP, this.count + 1, this::setCount);
        }

        void userClear() {
            if (this.request(CLEAR)) {
                this.count = 0;
                this.report(CLEARED);
            }
        }

        void misuse() {
            this.request(STEP);
        }

        boolean stepHandled() {
            return this.isHandled(STEP);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(20f, 10f);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            painter.fill(bounds, 0xFF00FF00);
        }
    }

    private CounterImpl counter() {
        final CounterImpl counter = new CounterImpl(this.ui);
        this.test.show(this.ui.absolutePanel().add(counter));
        return counter;
    }

    @Nested
    class Priority {
        @Test
        void handlersRunInAscendingPriorityAndTiesInRegistrationOrder() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final List<String> order = new ArrayList<>();
            counter.onEvent(Counter.STEP, 10, e -> order.add("late"))
                    .onEvent(Counter.STEP, e -> order.add("first zero"))
                    .onEvent(Counter.STEP, -5, e -> order.add("early"))
                    .onEvent(Counter.STEP, e -> order.add("second zero"));

            counter.userStep();

            assertEquals(List.of("early", "first zero", "second zero", "late"), order);
        }

        @Test
        void onEventReturnsTheComponentForChaining() {
            final CounterImpl counter = EventSystemTest.this.counter();
            assertSame(counter, counter.onEvent(Counter.STEP, e -> { }));
        }

        @Test
        void subscriptionRemovesTheHandler() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final int[] calls = {0};
            final Subscription subscription = counter.subscribe(Counter.STEP, 0, e -> calls[0]++);
            counter.userStep();
            subscription.close();
            counter.userStep();

            assertEquals(1, calls[0]);
            assertEquals(2, counter.count());
        }
    }

    @Nested
    class Requests {
        @Test
        void withoutHandlersTheChangeAppliesAtOnce() {
            final CounterImpl counter = EventSystemTest.this.counter();
            assertFalse(counter.stepHandled());
            counter.userStep();
            assertEquals(1, counter.count());
        }

        @Test
        void cancelingRefusesTheChange() {
            final CounterImpl counter = EventSystemTest.this.counter();
            counter.onEvent(Counter.STEP, e -> e.cancel());
            counter.userStep();
            assertEquals(0, counter.count());
        }

        @Test
        void aHandlerCanRewriteTheValueAndLaterHandlersSeeIt() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final List<Integer> seen = new ArrayList<>();
            counter.onEvent(Counter.STEP, e -> e.setValue(e.value() + 9))
                    .onEvent(Counter.STEP, 1, e -> seen.add(e.value()));
            counter.userStep();
            assertEquals(List.of(10), seen);
            assertEquals(10, counter.count());
        }

        @Test
        void anAncestorCanRefuseBecauseRequestsBubble() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final List<Component<?>> targets = new ArrayList<>();
            counter.parent().onEvent(Counter.STEP, e -> {
                targets.add(e.target());
                e.cancel();
            });
            assertTrue(counter.stepHandled());

            counter.userStep();

            assertEquals(List.of(counter), targets);
            assertEquals(0, counter.count());
        }

        @Test
        void aRequestWithoutAValueReportsWhetherItWasRefused() {
            final CounterImpl counter = EventSystemTest.this.counter();
            counter.setCount(4);
            counter.onEvent(Counter.CLEAR, e -> e.cancel());
            counter.userClear();
            assertEquals(4, counter.count());
        }

        @Test
        void requestsCarryASnapshotOfTheControlBeforeTheChange() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final List<Boolean> present = new ArrayList<>();
            counter.onEvent(Counter.STEP, e -> present.add(e.snapshotOf(counter).isPresent()));
            counter.userStep();
            assertEquals(List.of(true), present);
        }

        @Test
        void firingAValueTypeWithoutAValueFails() {
            final CounterImpl counter = EventSystemTest.this.counter();
            assertThrows(IllegalArgumentException.class, counter::misuse);
        }
    }

    /**
     * A composite whose only part is a counter, private or held as content.
     */
    static final class Wrapper extends CustomComposite<Wrapper> {
        private final CounterImpl inner;

        Wrapper(final ComponentFactory ui) {
            this(ui, false);
        }

        Wrapper(final ComponentFactory ui, final boolean content) {
            super(ui);
            this.inner = new CounterImpl(ui);
            if (content) {
                this.addContent(this.inner);
            } else {
                this.addChild(this.inner);
            }
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return context.measure(this.inner, availableWidth, availableHeight);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            context.arrange(this.inner, bounds);
        }
    }

    /**
     * A composite holding another component as a private part.
     */
    static final class Outer extends CustomComposite<Outer> {
        private final Component<?> part;

        Outer(final ComponentFactory ui, final Component<?> part) {
            super(ui);
            this.part = part;
            this.addChild(part);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return context.measure(this.part, availableWidth, availableHeight);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            context.arrange(this.part, bounds);
        }
    }

    @Nested
    class Composites {
        @Test
        void controlEventsOfPrivatePartsReachTheCompositeButNothingAboveIt() {
            final Wrapper wrapper = new Wrapper(EventSystemTest.this.ui);
            final AbsolutePanel panel = EventSystemTest.this.ui.absolutePanel().add(wrapper);
            EventSystemTest.this.test.show(panel);
            final List<String> seen = new ArrayList<>();
            wrapper.onEvent(Counter.STEPPED, e -> seen.add("composite"));
            panel.onEvent(Counter.STEPPED, e -> seen.add("outside"));

            wrapper.inner.userStep();

            assertEquals(List.of("composite"), seen);
            assertEquals(1, wrapper.inner.count());
        }

        @Test
        void controlEventsFromContentPassEveryCompositeAbove() {
            final Wrapper wrapper = new Wrapper(EventSystemTest.this.ui, true);
            final Outer outer = new Outer(EventSystemTest.this.ui, wrapper);
            final AbsolutePanel panel = EventSystemTest.this.ui.absolutePanel().add(outer);
            EventSystemTest.this.test.show(panel);
            final List<String> seen = new ArrayList<>();
            panel.onEvent(Counter.STEPPED, e -> seen.add("outside"));

            wrapper.inner.userStep();

            assertEquals(List.of("outside"), seen, "the outer composite holds the wrapper as a part, but the counter "
                    + "came from the wrapper's content");
        }

        @Test
        void controlEventsOfAPartInsideContentStillStopAtTheirOwnComposite() {
            final Wrapper inner = new Wrapper(EventSystemTest.this.ui, false);
            final Wrapper holder = new Wrapper(EventSystemTest.this.ui, true);
            final Outer outer = new Outer(EventSystemTest.this.ui, inner);
            final AbsolutePanel panel = EventSystemTest.this.ui.absolutePanel().add(outer).add(holder);
            EventSystemTest.this.test.show(panel);
            final List<String> seen = new ArrayList<>();
            panel.onEvent(Counter.STEPPED, e -> seen.add("outside"));

            inner.inner.userStep();

            assertEquals(List.of(), seen);
        }

        @Test
        void inputEventsStillBubblePastComposites() {
            final Wrapper wrapper = new Wrapper(EventSystemTest.this.ui);
            final AbsolutePanel panel = EventSystemTest.this.ui.absolutePanel().add(wrapper);
            EventSystemTest.this.test.show(panel);
            final List<String> seen = new ArrayList<>();
            panel.onEvent(InputEvents.CLICK, e -> seen.add("outside"));

            final Rect inner = wrapper.inner.bounds();
            EventSystemTest.this.test.click(inner.x() + 1f, inner.y() + 1f);

            assertEquals(List.of("outside"), seen);
        }
    }

    @Nested
    class Reports {
        @Test
        void reportsFireForChangesFromCodeWithTheValueBefore() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final List<String> seen = new ArrayList<>();
            counter.onEvent(Counter.STEPPED, e -> seen.add(e.previous() + "->" + e.value()));
            counter.setCount(3);
            counter.userStep();
            assertEquals(List.of("0->3", "3->4"), seen);
        }

        @Test
        void reportsReachAncestors() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final List<Counter> seen = new ArrayList<>();
            ((AbsolutePanel) counter.parent()).onEvent(Counter.CLEARED, e -> seen.add(e.control()));
            counter.userClear();
            assertEquals(List.of(counter), seen);
        }
    }

    @Nested
    class Drawing {
        @Test
        void paintedHandlersDrawOnTopOfTheComponent() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final Rect mark = new Rect(1, 1, 2, 2);
            counter.onEvent(Component.PAINTED, e -> e.painter().fill(mark, 0xFFFF0000));
            final NullCanvas canvas = new NullCanvas();
            EventSystemTest.this.test.host().render(canvas);

            final List<Rect> fills = canvas.fills();
            assertTrue(fills.indexOf(mark) > fills.indexOf(counter.bounds()), "drawn after the component");
        }

        @Test
        void cancelingPaintSkipsTheComponentsOwnDrawing() {
            final CounterImpl counter = EventSystemTest.this.counter();
            counter.onEvent(Component.PAINT, e -> e.cancel());
            final NullCanvas canvas = new NullCanvas();
            EventSystemTest.this.test.host().render(canvas);
            assertFalse(canvas.fills().contains(counter.bounds()));
        }

        @Test
        void paintReusesOneEventObjectAcrossFrames() {
            final CounterImpl counter = EventSystemTest.this.counter();
            final List<Object> events = new ArrayList<>();
            counter.onEvent(Component.PAINTED, events::add);
            EventSystemTest.this.test.host().render(new NullCanvas());
            EventSystemTest.this.test.host().render(new NullCanvas());
            assertEquals(2, events.size());
            assertSame(events.get(0), events.get(1));
        }

        @Test
        void measuredHandlersChangeTheSizeTheComponentAsksFor() {
            final CounterImpl counter = EventSystemTest.this.counter();
            counter.onEvent(Component.MEASURED, e -> e.setWidth(Math.max(80f, e.width())));
            counter.relayout();
            EventSystemTest.this.test.host().render(new NullCanvas());
            assertEquals(80f, counter.desiredWidth());
            assertEquals(10f, counter.desiredHeight());
        }
    }
}
