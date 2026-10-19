package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.TextSpan;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    // ------------------------------------------------------------------
    // Changing built-ins through their events
    // ------------------------------------------------------------------

    @Test
    void paintedHandlersDrawOverTheBuiltInFace() {
        final Button button = this.ui.button("Go");
        final Rect mark = new Rect(0, 0, 1, 1);
        button.onEvent(Component.PAINTED, event -> event.painter().fill(mark, 0xFFFF0000));
        this.test.show(this.ui.absolutePanel().add(button));

        final NullCanvas canvas = new NullCanvas();
        this.test.host().render(canvas);

        assertTrue(canvas.fills().contains(button.bounds()), "the normal button face is drawn");
        assertTrue(canvas.fills().indexOf(mark) > canvas.fills().indexOf(button.bounds()), "and the mark over it");
    }

    @Test
    void cancelingPaintReplacesTheBuiltInFace() {
        final Button button = this.ui.button("Go");
        final Rect mark = new Rect(0, 0, 1, 1);
        button.onEvent(Component.PAINT, event -> {
            event.painter().fill(mark, 0xFFFF0000);
            event.cancel();
        });
        this.test.show(this.ui.absolutePanel().add(button));

        final NullCanvas canvas = new NullCanvas();
        this.test.host().render(canvas);

        assertEquals(List.of(mark), canvas.fills());
        assertTrue(canvas.texts().isEmpty(), "the caption belongs to the face and is skipped too");
    }

    @Test
    void measuredHandlersChangeABuiltInsSize() {
        final Button button = this.ui.button("Go")
                .onEvent(Component.MEASURED, event -> event.setWidth(Math.max(120f, event.width())));
        this.test.show(this.ui.absolutePanel().add(button));
        this.test.host().render(new NullCanvas());

        assertEquals(120f, button.desiredWidth());
    }

    @Test
    void cancelingThePressRequestSkipsTheReport() {
        final List<String> seen = new ArrayList<>();
        final Button button = this.ui.button("Go")
                .onEvent(Button.PRESS, event -> {
                    seen.add("asked");
                    event.cancel();
                })
                .onEvent(Button.PRESSED, event -> seen.add("pressed"));
        this.test.show(this.ui.absolutePanel().add(button));

        this.test.click(5, 5);

        assertEquals(List.of("asked"), seen);
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

        CounterImpl(final ComponentFactory ui) {
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
        ReverseRow(final ComponentFactory ui) {
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
        final ComponentFactory other = new TestHost(100, 100).factory();
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
            public Component<?> build(final ComponentFactory ui) {
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
            public Component<?> build(final ComponentFactory ui) {
                return ui.anchorPanel();
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
        this.test.host().open(ui -> ui.anchorPanel());
        assertEquals(1, closes[0]);

        handle[0].close();
        handle[0].invalidate();

        assertTrue(this.test.host().screen().isPresent(), "the stale handle must not close the new screen");
    }
}
