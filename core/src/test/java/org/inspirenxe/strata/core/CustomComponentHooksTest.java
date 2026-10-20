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
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.event.input.ScrollEvent;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The public extension point as a developer uses it: components written on {@link CustomComponent},
 * {@link CustomComposite} and {@link CustomContainer}, and every hook Strata calls on them. Strata's own controls are
 * built on core's own bases, so these tests are what keeps the public bases exercised.
 */
class CustomComponentHooksTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    /**
     * A focusable leaf that logs every hook it gets and draws itself with a paint read from the theme.
     */
    static final class Probe extends CustomComponent<Probe> implements Focusable<Probe>, HasCursor<Probe>,
            HasTooltip<Probe> {
        static final String STYLE_FACE = "probe.face";
        final List<String> log = new ArrayList<>();
        private final boolean needsFace;
        private boolean textInput;
        private TextInputKind kind;
        private Paint face = Paint.NONE;

        Probe(final ComponentFactory ui) {
            this(ui, false);
        }

        Probe(final ComponentFactory ui, final boolean needsFace) {
            super(ui);
            this.needsFace = needsFace;
        }

        Probe takeText(final TextInputKind kind) {
            this.textInput = true;
            this.kind = kind;
            return this;
        }

        boolean hovered() {
            return this.isHovered();
        }

        boolean pressed() {
            return this.isPressed();
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(20f, 20f);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.face.paint(painter, bounds);
        }

        @Override
        protected Set<String> requiredStyleKeys() {
            return this.needsFace ? Set.of(STYLE_FACE) : Set.of();
        }

        @Override
        protected void onApplyTheme(final Theme theme) {
            this.log.add("theme " + theme.name());
            this.face = theme.paintOr(STYLE_FACE, Paint.NONE);
        }

        @Override
        protected boolean acceptsTextInput() {
            return this.textInput;
        }

        @Override
        protected TextInputKind textInputKind() {
            return this.kind == null ? super.textInputKind() : this.kind;
        }

        @Override
        protected void onPointerEnter(final PointerEvent event) {
            this.log.add("enter");
        }

        @Override
        protected void onPointerExit(final PointerEvent event) {
            this.log.add("exit");
        }

        @Override
        protected void onPointerMove(final PointerEvent event) {
            this.log.add("move");
        }

        @Override
        protected void onPointerPress(final PointerButtonEvent event) {
            this.log.add("press");
        }

        @Override
        protected void onPointerRelease(final PointerButtonEvent event) {
            this.log.add("release");
        }

        @Override
        protected void onClick(final PointerButtonEvent event) {
            this.log.add("click");
            event.consume();
        }

        @Override
        protected void onDragStart(final DragEvent event) {
            this.log.add("drag start");
        }

        @Override
        protected void onDrag(final DragEvent event) {
            this.log.add("drag");
        }

        @Override
        protected void onDragEnd(final DragEvent event) {
            this.log.add("drag end");
        }

        @Override
        protected void onScroll(final ScrollEvent event) {
            this.log.add("scroll");
            event.consume();
        }

        @Override
        protected void onKey(final KeyEvent event) {
            this.log.add("key " + event.key());
        }

        @Override
        protected void onChar(final CharEvent event) {
            this.log.add("char " + Character.toString(event.codePoint()));
        }

        @Override
        protected void onFocusGained(final FocusEvent event) {
            this.log.add("focus gained");
        }

        @Override
        protected void onFocusLost(final FocusEvent event) {
            this.log.add("focus lost");
        }

        @Override
        protected void onOtherEvent(final EventType<?> type, final Event event) {
            this.log.add("other " + type.name());
        }
    }

    private Probe show(final Probe probe) {
        this.test.show(this.ui.absolutePanel().add(probe));
        probe.log.clear();
        return probe;
    }

    private void press(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
    }

    private void release(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    @Nested
    class InputHooks {
        @Test
        void pointerHooksRunInOrder() {
            final Probe probe = CustomComponentHooksTest.this.show(new Probe(CustomComponentHooksTest.this.ui));
            final var host = CustomComponentHooksTest.this.test.host();

            host.pointerMoved(5, 5, Modifiers.NONE);
            assertEquals(List.of("enter", "move"), probe.log);
            assertTrue(probe.hovered());

            probe.log.clear();
            CustomComponentHooksTest.this.press(5, 5);
            assertTrue(probe.pressed());
            CustomComponentHooksTest.this.release(5, 5);
            assertFalse(probe.pressed());
            assertEquals(List.of("focus gained", "press", "release", "click"), probe.log);

            probe.log.clear();
            host.pointerMoved(250, 250, Modifiers.NONE);
            assertEquals(List.of("exit"), probe.log);
            assertFalse(probe.hovered());
        }

        @Test
        void dragHooksRunPastTheThreshold() {
            final Probe probe = CustomComponentHooksTest.this.show(new Probe(CustomComponentHooksTest.this.ui));
            final var host = CustomComponentHooksTest.this.test.host();
            CustomComponentHooksTest.this.press(5, 5);
            probe.log.clear();

            host.pointerMoved(15, 5, Modifiers.NONE);
            CustomComponentHooksTest.this.release(15, 5);

            assertEquals(List.of("drag start", "drag", "move", "release", "drag end"), probe.log,
                    "a drag ends without a click");
        }

        @Test
        void scrollKeyAndCharHooksRun() {
            final Probe probe = CustomComponentHooksTest.this.show(new Probe(CustomComponentHooksTest.this.ui));
            final var host = CustomComponentHooksTest.this.test.host();
            probe.requestFocus();
            probe.log.clear();

            assertTrue(host.scroll(5, 5, 0f, 1f, ScrollKind.WHEEL, Modifiers.NONE), "the hook consumed the scroll");
            host.key(Key.A, KeyAction.PRESS, Modifiers.NONE);
            host.charTyped('a', Modifiers.NONE);

            assertEquals(List.of("enter", "scroll", "key A", "char a"), probe.log);
        }

        @Test
        void otherEventTypesReachOnOtherEvent() {
            final Probe probe = new Probe(CustomComponentHooksTest.this.ui);
            CustomComponentHooksTest.this.test.show(probe);
            probe.log.clear();

            CustomComponentHooksTest.this.test.host().key(Key.LEFT_SHIFT, KeyAction.PRESS, Modifiers.NONE);

            assertTrue(probe.log.contains("other " + InputEvents.MODIFIERS_CHANGED.name()), probe.log.toString());
        }

        @Test
        void cancelingAnEventSkipsTheHook() {
            final Probe probe = CustomComponentHooksTest.this.show(new Probe(CustomComponentHooksTest.this.ui));
            final List<String> heard = new ArrayList<>();
            probe.onEvent(InputEvents.CLICK, event -> {
                heard.add("handler");
                event.cancel();
            });

            CustomComponentHooksTest.this.test.click(5, 5);

            assertEquals(List.of("handler"), heard);
            assertFalse(probe.log.contains("click"), probe.log.toString());
        }

        @Test
        void aDisabledComponentGetsNoPointerInput() {
            final Probe probe = CustomComponentHooksTest.this.show(new Probe(CustomComponentHooksTest.this.ui));
            probe.setEnabled(false);

            CustomComponentHooksTest.this.test.click(5, 5);

            assertTrue(probe.log.isEmpty(), probe.log.toString());
        }
    }

    @Nested
    class Focus {
        @Test
        void declaringFocusableMakesTheComponentFocusableAndTabbable() {
            final Probe probe = new Probe(CustomComponentHooksTest.this.ui);
            final Box box = new Box(CustomComponentHooksTest.this.ui, 10, 10);
            box.setFocusable(true);

            assertTrue(probe.isFocusable());
            assertTrue(probe.isTabbable());
            assertFalse(box.isFocusable(), "a component that does not declare Focusable never takes focus");
        }

        @Test
        void focusHooksRunAsFocusMoves() {
            final Probe first = new Probe(CustomComponentHooksTest.this.ui);
            final Probe second = new Probe(CustomComponentHooksTest.this.ui);
            CustomComponentHooksTest.this.test.show(CustomComponentHooksTest.this.ui.stackPanel().add(first).add(second));
            first.log.clear();
            second.log.clear();

            assertTrue(first.requestFocus());
            assertTrue(second.requestFocus());

            assertEquals(List.of("focus gained", "focus lost"), first.log);
            assertEquals(List.of("focus gained"), second.log);
            assertSame(second, CustomComponentHooksTest.this.test.focused());
        }

        @Test
        void textInputStartsWithTheKindTheComponentAsksFor() {
            final Probe secret = new Probe(CustomComponentHooksTest.this.ui).takeText(TextInputKind.SECRET);
            final Probe plain = new Probe(CustomComponentHooksTest.this.ui).takeText(null);
            final Probe none = new Probe(CustomComponentHooksTest.this.ui);
            CustomComponentHooksTest.this.test.show(CustomComponentHooksTest.this.ui.stackPanel()
                    .add(secret).add(plain).add(none));
            final TestPlatform platform = CustomComponentHooksTest.this.test.platform();

            secret.requestFocus();
            assertEquals(TextInputKind.SECRET, platform.textInputKind());
            plain.requestFocus();
            assertEquals(TextInputKind.TEXT, platform.textInputKind(), "the default kind is plain text");
            none.requestFocus();
            assertNull(platform.textInputKind(), "a component that takes no text stops text input");
        }
    }

    @Nested
    class ThemeKeys {
        @Test
        void aThemeWithoutARequiredKeyIsRefused() {
            final Probe probe = new Probe(CustomComponentHooksTest.this.ui, true);

            final ThemeException error = assertThrows(ThemeException.class,
                    () -> CustomComponentHooksTest.this.test.show(probe));

            assertEquals(Set.of(Probe.STYLE_FACE), error.missingKeys());
        }

        @Test
        void theThemeIsAppliedOnAttachAndOnEveryChange() {
            final Theme first = Themes.flat().toBuilder().paint(Probe.STYLE_FACE, Paint.solid(0xFF112233)).build();
            final TestHost host = new TestHost(new TestPlatform(), first, 100, 100);
            final Probe probe = new Probe(host.factory(), true);

            host.show(probe);
            final NullCanvas canvas = new NullCanvas();
            host.host().render(canvas);
            assertEquals(List.of("theme " + first.name()), probe.log);
            assertTrue(canvas.fills().contains(probe.bounds()), "the face read from the theme is drawn");

            final Theme second = first.toBuilder().paint(Probe.STYLE_FACE, Paint.NONE).build();
            host.host().setTheme(second);
            final NullCanvas after = new NullCanvas();
            host.host().render(after);
            assertEquals(List.of("theme " + first.name(), "theme " + second.name()), probe.log);
            assertFalse(after.fills().contains(probe.bounds()), "the new theme's face is drawn instead");
        }
    }

    @Nested
    class CursorTooltipAndOffset {
        @Test
        void cursorAndTooltipCountOnlyWhenDeclared() {
            final Probe probe = new Probe(CustomComponentHooksTest.this.ui)
                    .setCursor(CursorKind.HAND)
                    .setTooltip(Tooltip.text("Ana"));
            final Plain plain = new Plain(CustomComponentHooksTest.this.ui);
            plain.setCursor(CursorKind.HAND);

            assertEquals(CursorKind.HAND, probe.cursor());
            assertTrue(probe.tooltip() != null);
            assertNull(plain.cursor(), "a component that does not declare HasCursor has no cursor");
        }

        @Test
        void anOffsetMovesTheComponentFromItsSlot() {
            final Box box = new Box(CustomComponentHooksTest.this.ui, 10, 10);
            CustomComponentHooksTest.this.test.show(CustomComponentHooksTest.this.ui.absolutePanel().add(box));

            box.setOffset(4, 6);
            CustomComponentHooksTest.this.test.host().layout();

            assertEquals(new Rect(4, 6, 10, 10), box.bounds());
        }
    }

    /**
     * A leaf that declares none of the optional interfaces.
     */
    static final class Plain extends CustomComponent<Plain> {
        Plain(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(10f, 10f);
        }
    }

    /**
     * A composite with one private part that is wider than the composite's clip, and an optional content child. It
     * logs the composite hooks.
     */
    static final class Frame extends CustomComposite<Frame> {
        static final Rect UNDER = new Rect(0, 0, 1, 1);
        static final Rect OVER = new Rect(0, 0, 2, 2);
        final List<String> log = new ArrayList<>();
        final Box part;
        int measures;
        int arranges;

        Frame(final ComponentFactory ui) {
            super(ui);
            this.part = new Box(ui, 40, 40);
            this.addChild(this.part);
        }

        void content(final Component<?> content) {
            this.addContent(content);
        }

        void drop(final Component<?> child) {
            this.removeChild(child);
        }

        void clip(final boolean clip) {
            this.setChildClipping(clip);
        }

        void rearrange() {
            this.invalidateArrangement();
        }

        List<Component<?>> kids() {
            return this.childList();
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            this.measures++;
            for (final Component<?> child : this.childList()) {
                context.measure(child, availableWidth, availableHeight);
            }
            return new Size(40f, 40f);
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            this.arranges++;
            float y = bounds.y();
            for (final Component<?> child : this.childList()) {
                final Size outer = context.outerSize(child);
                context.arrange(child, new Rect(bounds.x(), y, outer.width(), outer.height()));
                y += outer.height();
            }
        }

        @Override
        protected Rect childClip() {
            final Rect bounds = this.bounds();
            return new Rect(bounds.x(), bounds.y(), bounds.width() / 2f, bounds.height());
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            painter.fill(UNDER, 0xFF000000);
        }

        @Override
        protected void paintOverChildren(final Painter painter, final Rect bounds) {
            painter.fill(OVER, 0xFFFFFFFF);
        }

        @Override
        protected void onChildRemoved(final Component<?> child) {
            this.log.add("removed " + child);
        }

        @Override
        protected void onDescendantFocused(final Component<?> descendant) {
            this.log.add("descendant " + descendant);
        }
    }

    @Nested
    class Composites {
        @Test
        void childrenArePrivateAndPaintBetweenTheCompositesOwnLayers() {
            final Frame frame = new Frame(CustomComponentHooksTest.this.ui);
            frame.part.setBorder(Border.of(0xFF00FF00, 1f));
            CustomComponentHooksTest.this.test.show(CustomComponentHooksTest.this.ui.absolutePanel().add(frame));

            final NullCanvas canvas = new NullCanvas();
            CustomComponentHooksTest.this.test.host().render(canvas);

            final List<Rect> fills = canvas.fills();
            final int under = fills.indexOf(Frame.UNDER);
            final int over = fills.indexOf(Frame.OVER);
            assertTrue(under >= 0 && over > under, fills.toString());
            assertTrue(fills.subList(under, over).stream().anyMatch(rect -> rect.y() == 0f && rect.height() == 1f
                    && rect.width() == 40f), "the part's border is drawn between the two: " + fills);
            assertEquals(List.of(frame.part), frame.kids());
        }

        @Test
        void theChildClipLimitsHitTestingUntilClippingIsOff() {
            final Frame frame = new Frame(CustomComponentHooksTest.this.ui);
            CustomComponentHooksTest.this.test.show(CustomComponentHooksTest.this.ui.absolutePanel().add(frame));
            final var host = CustomComponentHooksTest.this.test.host();

            host.pointerMoved(30, 5, Modifiers.NONE);
            assertSame(frame, host.hovered().orElseThrow(), "the part is clipped away at x 30");
            host.pointerMoved(5, 5, Modifiers.NONE);
            assertSame(frame.part, host.hovered().orElseThrow());

            frame.clip(false);
            host.pointerMoved(30, 5, Modifiers.NONE);
            assertSame(frame.part, host.hovered().orElseThrow());
        }

        @Test
        void removingAChildCallsOnChildRemoved() {
            final Frame frame = new Frame(CustomComponentHooksTest.this.ui);

            frame.drop(frame.part);

            assertEquals(List.of("removed Box"), frame.log);
            assertTrue(frame.kids().isEmpty());
            assertNull(frame.part.parent());
        }

        @Test
        void focusInsideCallsOnDescendantFocused() {
            final Frame frame = new Frame(CustomComponentHooksTest.this.ui);
            final Probe probe = new Probe(CustomComponentHooksTest.this.ui);
            frame.content(probe);
            CustomComponentHooksTest.this.test.show(CustomComponentHooksTest.this.ui.absolutePanel().add(frame));

            probe.requestFocus();

            assertEquals(List.of("descendant Probe"), frame.log);
        }

        @Test
        void invalidateArrangementArrangesWithoutMeasuring() {
            final Frame frame = new Frame(CustomComponentHooksTest.this.ui);
            CustomComponentHooksTest.this.test.show(CustomComponentHooksTest.this.ui.absolutePanel().add(frame));
            final int measures = frame.measures;
            final int arranges = frame.arranges;

            frame.rearrange();
            CustomComponentHooksTest.this.test.host().layout();

            assertEquals(measures, frame.measures);
            assertEquals(arranges + 1, frame.arranges);
        }
    }

    /**
     * A container that stacks children inside its padding.
     */
    static final class Column extends CustomContainer<Column> {
        Rect box;

        Column(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final Insets padding = this.padding();
            float width = 0f;
            float height = 0f;
            for (final Component<?> child : this.children()) {
                final Size outer = context.measure(child, availableWidth - padding.horizontal(),
                        availableHeight - padding.vertical());
                width = Math.max(width, outer.width());
                height += outer.height();
            }
            return new Size(width + padding.horizontal(), height + padding.vertical());
        }

        @Override
        protected void arrange(final ArrangeContext context, final Rect bounds) {
            this.box = this.contentBox();
            float y = this.box.y();
            for (final Component<?> child : this.children()) {
                final Size outer = context.outerSize(child);
                context.arrange(child, new Rect(this.box.x(), y, outer.width(), outer.height()));
                y += outer.height();
            }
        }
    }

    @Nested
    class Containers {
        @Test
        void childrenGoInsideThePadding() {
            final Box first = new Box(CustomComponentHooksTest.this.ui, 10, 5);
            final Box second = new Box(CustomComponentHooksTest.this.ui, 20, 5);
            final Column column = new Column(CustomComponentHooksTest.this.ui).setPadding(Insets.of(2))
                    .add(first).add(second);
            CustomComponentHooksTest.this.test.show(column);

            assertEquals(new Rect(2, 2, 20, 10), column.box);
            assertEquals(new Rect(2, 2, 10, 5), first.bounds());
            assertEquals(new Rect(2, 7, 20, 5), second.bounds());
            assertEquals(24f, column.desiredWidth());
        }

        @Test
        void builtInsAndCustomComponentsMixInBothDirections() {
            final Column column = new Column(CustomComponentHooksTest.this.ui);
            final var label = CustomComponentHooksTest.this.ui.label("Ben");
            final AbsolutePanel panel = CustomComponentHooksTest.this.ui.absolutePanel();
            column.add(label);
            panel.add(column);
            CustomComponentHooksTest.this.test.show(panel);

            assertSame(column, label.parent());
            assertSame(panel, column.parent());
            assertTrue(label.isAttached());
        }
    }

    @Nested
    class TellingThemApart {
        @Test
        void nothingTheFactoryMakesIsACustomComponent() {
            final ComponentFactory ui = CustomComponentHooksTest.this.ui;
            final List<Function<ComponentFactory, Component<?>>> makers = List.of(
                    ComponentFactory::anchorPanel, ComponentFactory::stackPanel, ComponentFactory::gridPanel,
                    ComponentFactory::absolutePanel, ComponentFactory::scrollPanel, ComponentFactory::spacer,
                    ComponentFactory::label, ComponentFactory::image, ComponentFactory::button,
                    ComponentFactory::checkBox, ComponentFactory::slider, ComponentFactory::textBox,
                    ComponentFactory::passwordTextBox, f -> f.listView(), f -> f.comboBox(), ComponentFactory::window,
                    ComponentFactory::messageBox, ComponentFactory::tabBar, ComponentFactory::progressBar,
                    ComponentFactory::separator, f -> f.separator(Orientation.VERTICAL), ComponentFactory::toggleSwitch,
                    ComponentFactory::spinner, ComponentFactory::textArea, f -> f.tableView(), f -> f.treeView(),
                    ComponentFactory::menu, ComponentFactory::menuBar, ComponentFactory::expander,
                    ComponentFactory::splitPane, f -> f.gridView(), f -> f.cycleButton(), ComponentFactory::scrollBar,
                    ComponentFactory::busyIndicator, ComponentFactory::colorPicker,
                    f -> f.<String>radioGroup().add("Cleo", "cleo"), f -> f.cell("Dev"));
            for (final Function<ComponentFactory, Component<?>> maker : makers) {
                final Component<?> component = maker.apply(ui);
                assertFalse(component instanceof CustomComponent<?>, component + " is Strata's own");
            }
            assertTrue(new Probe(ui) instanceof CustomComponent<?>);
        }
    }
}
