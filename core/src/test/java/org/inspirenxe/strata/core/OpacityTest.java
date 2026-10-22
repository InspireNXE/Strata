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
import org.inspirenxe.strata.api.animation.Easing;
import org.inspirenxe.strata.api.animation.Interpolator;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opacity on components (design 4.51): the property, how it draws, and toasts that fade.
 */
class OpacityTest {
    private final TestPlatform platform = new TestPlatform();
    private TestHost test = new TestHost(this.platform, Themes.flat(), 300, 200);
    private ComponentFactory ui = this.test.factory();

    private DrawLog frame(final long millis) {
        this.platform.advanceMillis(millis);
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        return log;
    }

    /**
     * A box of one color, written as a developer would.
     */
    static final class Swatch extends CustomComponent<Swatch> {
        private final int color;

        Swatch(final ComponentFactory factory, final int color) {
            super(factory);
            this.color = color;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(20, 10);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            painter.fill(bounds, this.color);
        }
    }

    @Nested
    class Property {
        @Test
        void opaqueByDefaultAndCheckedWhenSet() {
            final Swatch swatch = new Swatch(OpacityTest.this.ui, 0xFFFF0000);
            assertEquals(1f, swatch.opacity());
            assertSame(swatch, swatch.setOpacity(0.25f));
            assertEquals(0.25f, swatch.opacity());
            assertThrows(IllegalArgumentException.class, () -> swatch.setOpacity(1.5f));
            assertThrows(IllegalArgumentException.class, () -> swatch.setOpacity(Float.NaN));
            assertEquals(0.25f, OpacityTest.this.ui.label().setOpacity(0.25f).opacity(), "built-ins have it too");
        }

        @Test
        void aChangeRedrawsAndLaysNothingOut() {
            final Swatch swatch = new Swatch(OpacityTest.this.ui, 0xFFFF0000);
            OpacityTest.this.test.show(swatch);
            OpacityTest.this.frame(0L);
            swatch.setOpacity(0.5f);
            assertTrue(OpacityTest.this.test.host().needsFrame());
            assertFalse(OpacityTest.this.test.host().root().content().isLayoutDirty());
        }
    }

    @Nested
    class Drawing {
        @Test
        void aPartOpaqueComponentDrawsInsideAnOpacityAndATransparentOneNotAtAll() {
            final Swatch faded = new Swatch(OpacityTest.this.ui, 0xFFFF0000).setOpacity(0.5f);
            final Swatch hidden = new Swatch(OpacityTest.this.ui, 0xFF00FF00).setOpacity(0f);
            final StackPanel panel = OpacityTest.this.ui.stackPanel();
            panel.add(faded);
            panel.add(hidden);
            OpacityTest.this.test.show(panel);
            assertEquals(List.of("opacity 0.5", "fill 0,0 20x10 #FFFF0000", "opacity end"),
                    OpacityTest.this.frame(0L).lines());
        }

        @Test
        void aTransparentComponentStillTakesThePointer() {
            final Swatch hidden = new Swatch(OpacityTest.this.ui, 0xFF00FF00).setOpacity(0f);
            OpacityTest.this.test.show(hidden);
            OpacityTest.this.test.host().pointerMoved(5, 5, Modifiers.NONE);
            assertSame(hidden, OpacityTest.this.test.host().hovered().orElseThrow());
        }

        @Test
        void aFadeAnimatesTheOpacity() {
            final Swatch swatch = new Swatch(OpacityTest.this.ui, 0xFFFF0000);
            OpacityTest.this.test.show(swatch);
            OpacityTest.this.ui.animator().animate(swatch, Component.OPACITY, 0f, 100L, Easing.LINEAR,
                    Interpolator.FLOAT);
            assertEquals(List.of("opacity 0.5", "fill 0,0 20x10 #FFFF0000", "opacity end"),
                    OpacityTest.this.frame(50L).lines());
            assertTrue(OpacityTest.this.frame(50L).lines().isEmpty());
        }
    }

    @Nested
    class Toasts {
        @Test
        void aToastFadesInAndOutWhenTheThemeSaysSo() {
            OpacityTest.this.test = new TestHost(OpacityTest.this.platform, Themes.flat().toBuilder()
                    .metric(Overlays.STYLE_TOAST_FADE, 200f).build(), 300, 200);
            OpacityTest.this.ui = OpacityTest.this.test.factory();
            OpacityTest.this.test.show(OpacityTest.this.ui.label(Text.of("Base")));
            final Swatch content = new Swatch(OpacityTest.this.ui, 0xFFFF0000);
            final Overlay toast = OpacityTest.this.ui.overlays().toast(content, 1_000L);
            assertEquals(0f, content.opacity(), "starts transparent");
            OpacityTest.this.frame(100L);
            assertTrue(content.opacity() > 0f && content.opacity() < 1f, "fading in: " + content.opacity());
            OpacityTest.this.frame(100L);
            assertEquals(1f, content.opacity());
            OpacityTest.this.frame(600L);
            assertTrue(toast.isOpen());
            OpacityTest.this.frame(100L);
            assertTrue(content.opacity() < 1f, "fading out before its time is up: " + content.opacity());
            assertTrue(toast.isOpen());
            OpacityTest.this.frame(100L);
            assertFalse(toast.isOpen(), "closed when the fade ends");
        }

        @Test
        void aToastWithoutAFadeKeepsItsLook() {
            OpacityTest.this.test.show(OpacityTest.this.ui.label(Text.of("Base")));
            final Swatch content = new Swatch(OpacityTest.this.ui, 0xFFFF0000);
            final Overlay toast = OpacityTest.this.ui.overlays().toast(content, 1_000L);
            assertEquals(1f, content.opacity());
            OpacityTest.this.frame(1_000L);
            assertFalse(toast.isOpen());
        }

        @Test
        void aShortToastFadesOverHalfItsTimeEachWay() {
            OpacityTest.this.test = new TestHost(OpacityTest.this.platform, Themes.flat().toBuilder()
                    .metric(Overlays.STYLE_TOAST_FADE, 500f).build(), 300, 200);
            OpacityTest.this.ui = OpacityTest.this.test.factory();
            OpacityTest.this.test.show(OpacityTest.this.ui.label(Text.of("Base")));
            final Swatch content = new Swatch(OpacityTest.this.ui, 0xFFFF0000);
            final Overlay toast = OpacityTest.this.ui.overlays().toast(content, 200L);
            OpacityTest.this.frame(100L);
            assertTrue(content.opacity() > 0.9f, "fully in at half its time: " + content.opacity());
            OpacityTest.this.frame(100L);
            assertFalse(toast.isOpen());
        }
    }
}
