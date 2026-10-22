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
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Components hear when they join and leave a shown tree, through {@link Component#ATTACHED} and
 * {@link Component#DETACHED}, the {@code onAttached} and {@code onDetached} hooks, and {@code whileAttached}.
 */
class LifecycleTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    /**
     * A chart as a developer writes it: it starts listening to a feed when it shows and stops when it goes.
     */
    static final class Chart extends CustomComponent<Chart> {
        final List<String> log;
        final String name;

        Chart(final ComponentFactory factory, final String name, final List<String> log) {
            super(factory);
            this.name = name;
            this.log = log;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(40, 20);
        }

        @Override
        protected void onAttached() {
            this.log.add(this.name + " started, attached " + this.isAttached());
        }

        @Override
        protected void onDetached() {
            this.log.add(this.name + " stopped, attached " + this.isAttached());
        }
    }

    private <C extends Component<C>> C logged(final C component, final String name) {
        return component.onEvent(Component.ATTACHED, event -> this.heard.add("attached " + name))
                .onEvent(Component.DETACHED, event -> this.heard.add("detached " + name));
    }

    @Test
    void aSubtreeHearsAttachParentsFirstAndDetachChildrenFirst() {
        final Label inner = this.logged(this.ui.label("inner"), "inner");
        final StackPanel middle = this.logged(this.ui.stackPanel().add(inner), "middle");
        final StackPanel top = this.logged(this.ui.stackPanel(), "top");
        this.test.show(top);
        assertEquals(List.of("attached top"), this.heard);
        this.heard.clear();

        top.add(middle);
        assertEquals(List.of("attached middle", "attached inner"), this.heard);
        this.heard.clear();

        top.remove(middle);
        assertEquals(List.of("detached inner", "detached middle"), this.heard);
        assertFalse(inner.isAttached());
    }

    @Test
    void theHooksRunEveryTimeAndSeeTheComponentAttached() {
        final Chart chart = new Chart(this.ui, "chart", this.heard);
        final StackPanel top = this.ui.stackPanel();
        this.test.show(top);

        top.add(chart);
        top.remove(chart);
        top.add(chart);

        assertEquals(List.of("chart started, attached true", "chart stopped, attached true",
                "chart started, attached true"), this.heard);
    }

    @Test
    void theHookRunsBeforeTheHandlersOnAttachAndAfterThemOnDetach() {
        final Chart chart = new Chart(this.ui, "chart", this.heard);
        this.logged(chart, "chart");
        final StackPanel top = this.ui.stackPanel();
        this.test.show(top);

        top.add(chart);
        top.remove(chart);

        assertEquals(List.of("chart started, attached true", "attached chart", "detached chart",
                "chart stopped, attached true"), this.heard);
    }

    @Test
    void bindingsAreLiveOnAttachAndStillLiveOnDetach() {
        final State<Text> text = State.of(Text.of("Ana"));
        final Label label = this.ui.label();
        label.bind(HasText.TEXT, text);
        label.onEvent(Component.ATTACHED, event -> this.heard.add("attached " + label.text().plain()));
        label.onEvent(Component.DETACHED, event -> {
            text.set(Text.of("Ben"));
            this.test.host().flush();
            this.heard.add("detached " + label.text().plain());
        });
        final StackPanel top = this.ui.stackPanel();
        this.test.show(top);

        top.add(label);
        top.remove(label);

        assertEquals(List.of("attached Ana", "detached Ben"), this.heard);
    }

    @Test
    void openingClosingAndRebuildingAScreenAttachAndDetachItsTree() {
        final ScreenHandle[] handle = new ScreenHandle[1];
        final int[] builds = {0};
        this.test.host().open(new Screen() {
            @Override
            public Component<?> build(final ComponentFactory factory) {
                builds[0]++;
                return LifecycleTest.this.logged(factory.label("page"), "page " + builds[0]);
            }

            @Override
            public void opened(final ScreenHandle opened) {
                handle[0] = opened;
            }

            @Override
            public void closed() {
                LifecycleTest.this.heard.add("closed");
            }
        });
        assertEquals(List.of("attached page 1"), this.heard);

        handle[0].invalidate();
        this.test.host().flush();
        assertEquals(List.of("attached page 1", "detached page 1", "attached page 2"), this.heard);

        this.test.host().close();
        assertEquals(List.of("attached page 1", "detached page 1", "attached page 2", "closed", "detached page 2"),
                this.heard);
    }

    @Test
    void anExpanderDetachesItsContentWhenItCollapsesAndAttachesItAgainWhenItExpands() {
        final Chart chart = new Chart(this.ui, "chart", this.heard);
        final Expander expander = this.ui.expander("Prices").setContent(chart).setExpanded(true);
        this.test.show(this.ui.stackPanel().add(expander));

        expander.setExpanded(false);
        expander.setExpanded(true);

        assertEquals(List.of("chart started, attached true", "chart stopped, attached true",
                "chart started, attached true"), this.heard);
    }

    @Test
    void anOverlayDetachesItsContentWhenItCloses() {
        this.test.show(this.ui.stackPanel());
        final Chart chart = new Chart(this.ui, "chart", this.heard);

        final Overlay popup = this.test.host().root().overlays().popup(chart, new Rect(0, 0, 10, 10));
        popup.close();

        assertEquals(List.of("chart started, attached true", "chart stopped, attached true"), this.heard);
    }

    @Test
    void whileAttachedStartsOnEveryAttachAndStopsOnEveryDetach() {
        final int[] running = {0};
        final int[] starts = {0};
        final Label chart = this.ui.label("chart").whileAttached(() -> {
            starts[0]++;
            running[0]++;
            return () -> running[0]--;
        });
        final StackPanel top = this.ui.stackPanel();
        this.test.show(top);
        assertEquals(0, starts[0], "nothing runs while detached");

        top.add(chart);
        assertEquals(1, running[0]);
        top.remove(chart);
        assertEquals(0, running[0]);
        top.add(chart);
        assertEquals(1, running[0]);
        assertEquals(2, starts[0]);
    }

    @Test
    void whileAttachedOnAnAttachedComponentStartsAtOnce() {
        final Label chart = this.ui.label("chart");
        final StackPanel top = this.ui.stackPanel().add(chart);
        this.test.show(top);
        final int[] running = {0};

        chart.whileAttached(() -> {
            running[0]++;
            return () -> running[0]--;
        });
        assertEquals(1, running[0]);

        this.test.host().close();
        assertEquals(0, running[0]);
    }

    @Test
    void whileAttachedRefusesASupplierThatReturnsNull() {
        final Label chart = this.ui.label("chart");
        this.test.show(this.ui.stackPanel().add(chart));

        assertThrows(NullPointerException.class, () -> chart.whileAttached(() -> null));
    }

    @Test
    void aHandlerThatThrowsDoesNotStopTheRestOfTheSubtree() {
        final Label inner = this.logged(this.ui.label("inner"), "inner");
        final StackPanel middle = this.ui.stackPanel().add(inner);
        middle.onEvent(Component.DETACHED, event -> {
            throw new IllegalStateException("middle");
        });
        middle.onEvent(Component.ATTACHED, event -> {
            throw new IllegalStateException("middle");
        });
        final StackPanel top = this.ui.stackPanel();
        this.test.show(top);

        assertThrows(IllegalStateException.class, () -> top.add(middle));
        assertTrue(inner.isAttached());
        assertSame(top, middle.parent().orElseThrow());

        assertThrows(IllegalStateException.class, () -> top.remove(middle));
        assertFalse(inner.isAttached());
        assertTrue(middle.parent().isEmpty());
        assertEquals(List.of("attached inner", "detached inner"), this.heard);
    }

    @Test
    void aComponentAttachedByABindingHearsAttachOnce() {
        final StackPanel holder = this.ui.stackPanel();
        final Label late = this.logged(this.ui.label("late"), "late");
        holder.bind(Component.VISIBLE, () -> {
            if (holder.children().isEmpty() && holder.isAttached()) {
                holder.add(late);
            }
            return true;
        });
        this.test.show(this.ui.stackPanel().add(holder));

        assertEquals(List.of("attached late"), this.heard);
    }

    @Test
    void builtInControlsGetTheSameHooks() {
        final BuiltInProbe probe = new BuiltInProbe((ComponentFactoryImpl) this.ui, this.heard);
        final StackPanel top = this.ui.stackPanel();
        this.test.show(top);

        top.add(probe);
        top.remove(probe);

        assertEquals(List.of("built-in attached", "built-in detached"), this.heard);
    }

    /**
     * A control written on core's own base, as Strata's controls are.
     */
    static final class BuiltInProbe extends BuiltInComponent<BuiltInProbe> {
        private final List<String> log;

        BuiltInProbe(final ComponentFactoryImpl factory, final List<String> log) {
            super(factory);
            this.log = log;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return Size.ZERO;
        }

        @Override
        protected void onAttached() {
            this.log.add("built-in attached");
        }

        @Override
        protected void onDetached() {
            this.log.add("built-in detached");
        }
    }
}
