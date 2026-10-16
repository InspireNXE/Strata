package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.UIEvents;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.MeasureNext;
import org.inspirenxe.strata.api.extension.Next;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ButtonBehavior;
import org.inspirenxe.strata.api.widget.Label;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionTest {
    private final TestHost test = new TestHost(300, 300);
    private final Ui ui = this.test.ui();

    // ------------------------------------------------------------------
    // Decorating built-ins with behaviors
    // ------------------------------------------------------------------

    @Test
    void behaviorExtendsBuiltInPaintThroughNext() {
        final List<String> calls = new ArrayList<>();
        final Button button = this.ui.button("Go", new ButtonBehavior() {
            @Override
            public void paint(final Button component, final Painter painter, final Rect bounds, final Next next) {
                calls.add("before");
                next.proceed();
                painter.fill(new Rect(0, 0, 1, 1), 0xFFFF0000);
                calls.add("after");
            }
        });
        this.test.show(this.ui.absolutePanel().add(button));

        final NullCanvas canvas = new NullCanvas();
        this.test.host().render(canvas);

        assertEquals(List.of("before", "after"), calls);
        assertTrue(canvas.fills().contains(new Rect(0, 0, 1, 1)), "the behavior's own drawing is kept");
        assertTrue(canvas.fills().contains(button.bounds()), "next drew the normal button face");
    }

    @Test
    void behaviorSkippingNextReplacesBuiltInBehavior() {
        final int[] presses = {0};
        final List<Button> seen = new ArrayList<>();
        final Button button = this.ui.button("Go", new ButtonBehavior() {
            @Override
            public void onClick(final Button component, final PointerButtonEvent event, final Next next) {
                seen.add(component);
            }
        }).onAction(() -> presses[0]++);
        this.test.show(this.ui.absolutePanel().add(button));

        this.test.click(5, 5);

        assertEquals(0, presses[0], "without next the button's own click handling never runs");
        assertEquals(List.of(button), seen);
    }

    @Test
    void behaviorCanChangeMeasuredSize() {
        final Button button = this.ui.button("Go", new ButtonBehavior() {
            @Override
            public Size measure(final Button component, final MeasureContext context, final float availableWidth,
                                final float availableHeight, final MeasureNext next) {
                final Size normal = next.proceed();
                return new Size(normal.width() * 2, normal.height());
            }
        });
        final Button plain = this.ui.button("Go");
        this.test.show(this.ui.absolutePanel().add(button).add(plain, 0, 100));

        assertEquals(plain.desiredWidth() * 2, button.desiredWidth());
    }

    @Test
    void canceledEventSkipsBehaviorToo() {
        final int[] calls = {0};
        final Button button = this.ui.button("Go", new ButtonBehavior() {
            @Override
            public void onClick(final Button component, final PointerButtonEvent event, final Next next) {
                calls[0]++;
                next.proceed();
            }
        });
        button.on(UIEvents.CLICK, event -> event.cancel());
        this.test.show(this.ui.absolutePanel().add(button));

        this.test.click(5, 5);

        assertEquals(0, calls[0]);
    }

    // ------------------------------------------------------------------
    // New component types
    // ------------------------------------------------------------------

    /**
     * A mod's own API type, as another mod would see it.
     */
    interface Counter extends Component<Counter> {
        int count();

        Counter reset();
    }

    static final class CounterImpl extends CustomComponent<Counter> implements Counter {
        private int count;

        CounterImpl(final Ui ui) {
            super(ui);
        }

        @Override
        public int count() {
            return this.count;
        }

        @Override
        public Counter reset() {
            this.count = 0;
            return this;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(context.text().width(TextSpan.of(String.valueOf(this.count))), 9f);
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            this.count++;
            this.invalidateLayout();
            event.consume();
        }
    }

    @Test
    void customComponentWorksThroughItsOwnInterface() {
        final Counter counter = new CounterImpl(this.ui).setMargin(3);
        this.test.show(this.ui.absolutePanel().add(counter));

        this.test.click(5, 5);
        this.test.platform().advanceMillis(1000);
        this.test.click(5, 5);

        assertEquals(2, counter.count());
        assertEquals(new Rect(3, 3, TestPlatform.CHAR_WIDTH, 9f), counter.bounds());
        assertTrue(counter.parent() instanceof AbsolutePanel);
    }

    /**
     * A custom container: lays children out in a row from right to left.
     */
    static final class ReverseRow extends CustomContainer<ReverseRow> {
        ReverseRow(final Ui ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            float width = 0f;
            float height = 0f;
            for (final Component<?> child : this.children()) {
                final Size outer = context.measure(child, availableWidth, availableHeight);
                width += outer.width();
                height = Math.max(height, outer.height());
            }
            return new Size(width, height);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            float right = bounds.right();
            for (final Component<?> child : this.children()) {
                final Size outer = context.outerSize(child);
                right -= outer.width();
                context.arrange(child, new Rect(right, bounds.y(), outer.width(), outer.height()));
            }
        }
    }

    @Test
    void customContainerArrangesChildren() {
        final Box first = new Box(this.ui, 10, 10);
        final Box second = new Box(this.ui, 20, 10);
        final ReverseRow row = new ReverseRow(this.ui).add(first).add(second);
        this.test.show(row);

        assertEquals(new Rect(20, 0, 10, 10), first.bounds());
        assertEquals(new Rect(0, 0, 20, 10), second.bounds());
        assertEquals(List.of(first, second), row.children());
    }

    // ------------------------------------------------------------------
    // What is rejected
    // ------------------------------------------------------------------

    @Test
    void componentWithoutCorePeerIsRejected() {
        final Label fake = (Label) Proxy.newProxyInstance(Label.class.getClassLoader(), new Class<?>[] {Label.class},
                (proxy, method, args) -> method.getName().equals("toString") ? "FakeLabel" : null);
        final AbsolutePanel panel = this.ui.absolutePanel();

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> panel.add(fake));
        assertTrue(error.getMessage().contains("CustomComponent"), error.getMessage());
        assertTrue(panel.children().isEmpty());
    }

    @Test
    void componentFromAnotherUiIsRejected() {
        final Ui other = new TestHost(100, 100).ui();
        final AbsolutePanel panel = this.ui.absolutePanel();

        assertThrows(IllegalArgumentException.class, () -> panel.add(other.label("elsewhere")));
    }

    // ------------------------------------------------------------------
    // Screens
    // ------------------------------------------------------------------

    @Test
    void invalidatedScreenRebuildsBeforeNextFrame() {
        final int[] builds = {0};
        final ScreenHandle[] handle = new ScreenHandle[1];
        this.test.host().open(new Screen() {
            @Override
            public Component<?> build(final Ui ui) {
                builds[0]++;
                return ui.label("build " + builds[0]);
            }

            @Override
            public void opened(final ScreenHandle opened) {
                handle[0] = opened;
            }
        });
        assertEquals(1, builds[0]);

        handle[0].invalidate();
        this.test.host().render(new NullCanvas());

        assertEquals(2, builds[0]);
    }

    @Test
    void handleOfReplacedScreenDoesNothing() {
        final ScreenHandle[] handle = new ScreenHandle[1];
        final int[] closes = {0};
        this.test.host().open(new Screen() {
            @Override
            public Component<?> build(final Ui ui) {
                return ui.panel();
            }

            @Override
            public void opened(final ScreenHandle opened) {
                handle[0] = opened;
            }

            @Override
            public void closed() {
                closes[0]++;
            }
        });
        this.test.host().open(ui -> ui.panel());
        assertEquals(1, closes[0]);

        handle[0].close();
        handle[0].invalidate();

        assertTrue(this.test.host().screen().isPresent(), "the stale handle must not close the new screen");
    }
}
