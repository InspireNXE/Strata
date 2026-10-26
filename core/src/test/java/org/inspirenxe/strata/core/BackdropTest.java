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

import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.Capabilities;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Frosted glass: a blurred backdrop with a tint over it.
 */
class BackdropTest {
    private static final int TINT = 0xC0101014;
    private static final Capabilities BLURS = new Capabilities(false, false, false, true, false, true);
    private static final Capabilities PLAIN = new Capabilities(false, false, false, true, false);

    private static List<String> paint(final Capabilities capabilities, final Paint paint, final Rect rect) {
        final BackdropLog canvas = new BackdropLog();
        paint.paint(new CanvasPainter().begin(canvas, capabilities, 1f), rect);
        return canvas.lines();
    }

    @Test
    void aCanvasThatBlursGetsTheBackdropWithItsRadiusLimited() {
        assertEquals(List.of("backdrop 0,0 40x20 r10.0 b8.0 #C0101014"),
                paint(BLURS, Paint.backdrop(8, TINT, 30), new Rect(0, 0, 40, 20)));
    }

    @Test
    void aCanvasThatCannotBlurGetsTheTintAsARoundedFill() {
        assertEquals(List.of("roundrect 0,0 40x20 r6.0 #C0101014"),
                paint(PLAIN, Paint.backdrop(8, TINT, 6), new Rect(0, 0, 40, 20)));
        assertEquals(List.of("fill 0,0 40x20 #C0101014"), paint(PLAIN, Paint.backdrop(8, TINT), new Rect(0, 0, 40, 20)));
    }

    @Test
    void noBlurIsJustTheTint() {
        assertEquals(List.of("roundrect 0,0 40x20 r4.0 #C0101014"),
                paint(BLURS, Paint.backdrop(0, TINT, 4), new Rect(0, 0, 40, 20)));
    }

    @Test
    void anEmptyRectangleDrawsNothing() {
        assertEquals(List.of(), paint(BLURS, Paint.backdrop(8, TINT, 4), new Rect(0, 0, 0, 20)));
    }

    @Test
    void blurAndRadiusMustBeFiniteAndNotNegative() {
        assertThrows(IllegalArgumentException.class, () -> Paint.backdrop(-1, TINT));
        assertThrows(IllegalArgumentException.class, () -> Paint.backdrop(Float.POSITIVE_INFINITY, TINT));
        assertThrows(IllegalArgumentException.class, () -> Paint.backdrop(4, TINT, Float.NaN));
    }

    @Test
    void aPanelBackgroundIsDrawnAsFrostedGlass() {
        final TestPlatform platform = new TestPlatform();
        platform.setDrawsShapes(true);
        platform.setBlursBackdrop(true);
        final TestHost test = new TestHost(platform, Themes.flat(), 100, 100);
        final AbsolutePanel panel = test.factory().absolutePanel().setBackground(Paint.backdrop(6, TINT, 5));
        test.show(panel.setSize(60, 40));
        final BackdropLog canvas = new BackdropLog();
        test.host().render(canvas);
        assertTrue(canvas.lines().contains("backdrop 0,0 60x40 r5.0 b6.0 #C0101014"), canvas.lines()::toString);
    }

    @Test
    void themeFilesNameABackdropWithATintAndARadius() {
        final ThemeId id = ThemeId.of("test", "glass");
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource()
                .base(id, Theme.builder("glass").build())
                .file(id, """
                        { "paints": {
                            "panel": { "backdrop": 8, "tint": "#C0101014", "radius": 6 },
                            "bare": { "backdrop": 4 }
                        } }""");
        final Theme theme = new ThemeResolver(source, new ThemeRegistryImpl()).resolve(ThemeRef.of(id));
        assertEquals(Paint.backdrop(8, TINT, 6), theme.paint("panel").orElseThrow());
        assertEquals(Paint.backdrop(4, 0x00000000), theme.paint("bare").orElseThrow());
    }

    /**
     * A draw log that logs backdrops and rounded fills.
     */
    static final class BackdropLog extends DrawLog {
        @Override
        public void backdrop(final Rect rect, final float radius, final float blur, final int tint) {
            this.lines().add("backdrop " + DrawLog.rect(rect) + " r" + radius + " b" + blur + " "
                    + String.format("#%08X", tint));
        }

        @Override
        public void fillRoundRect(final Rect rect, final float radius, final int argb) {
            this.lines().add("roundrect " + DrawLog.rect(rect) + " r" + radius + " " + String.format("#%08X", argb));
        }
    }
}
