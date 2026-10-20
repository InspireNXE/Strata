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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.control.pane.TabBarImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.inspirenxe.strata.spi.ThemeSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeInsetsTest {
    private static final ThemeId MOD = ThemeId.of("mod", "look");

    @Nested
    class Values {
        @Test
        void aThemeHoldsInsetsBesideItsPaints() {
            final Theme theme = Theme.builder("t").insets("a.padding", Insets.of(3)).build();
            assertEquals(Optional.of(Insets.of(3)), theme.insets("a.padding"));
            assertEquals(Insets.of(1), theme.insetsOr("b.padding", Insets.of(1)));
            assertTrue(theme.has("a.padding"));
            assertEquals(Set.of(), theme.missing(List.of("a.padding")));
            assertEquals(Map.of("a.padding", Insets.of(3)), theme.toBuilder().build().insets());
        }

        @Test
        void filesAndOverridesSetInsets() {
            final Theme theme = resolve("{ \"parent\": \"strata:flat\", \"insets\": { \"a\": 2, \"b\": [1, 2],"
                    + " \"c\": [1, 2, 3, 4] } }", ThemeRef.of(MOD).override("c", Insets.of(9)));
            assertEquals(Insets.of(2), theme.insets("a").orElseThrow());
            assertEquals(Insets.of(1, 2), theme.insets("b").orElseThrow());
            // The ref's override wins over the file
            assertEquals(Insets.of(9), theme.insets("c").orElseThrow());
        }

        @Test
        void aBadValueRejectsTheFile() {
            // The only file is skipped, so the id has nothing to resolve
            assertThrows(ThemeException.class,
                    () -> resolve("{ \"insets\": { \"a\": [1, 2, 3] } }", ThemeRef.of(MOD)));
        }

        private static Theme resolve(final String file, final ThemeRef ref) {
            return new ThemeResolver(new ThemeSource() {
                @Override
                public Optional<Theme> base(final ThemeId id) {
                    return Optional.empty();
                }

                @Override
                public List<ThemeDocument> documents(final ThemeId id) {
                    return id.equals(MOD) ? List.of(new ThemeDocument("test", file)) : List.of();
                }

                @Override
                public long version() {
                    return 0;
                }
            }).resolve(ref);
        }
    }

    @Nested
    class TabPadding {
        /**
         * Shows "Buy" (selected) and "Sell" in a theme, and returns where each caption's top lands.
         */
        private Map<String, Float> captions(final Theme theme, final float[] height) {
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            final TabBar tabs = test.factory().tabBar().addTab("Buy").addTab("Sell");
            test.show(test.factory().absolutePanel().add(tabs, 0, 0));
            final Captions canvas = new Captions();
            test.host().render(canvas);
            height[0] = tabs.bounds().height();
            return canvas.tops;
        }

        @Test
        void theDefaultKeepsTheOldLook() {
            final float[] height = new float[1];
            final Map<String, Float> tops = this.captions(Themes.flat(), height);
            assertEquals(TestPlatform.LINE_HEIGHT + 7f, height[0]);
            assertEquals(4f, tops.get("Buy"));
            assertEquals(4f, tops.get("Sell"));
            assertEquals(new Insets(8, 4, 8, 3), TabBarImpl.DEFAULT_PADDING);
        }

        @Test
        void unselectedTabsCanSitTheirTextLower() {
            final float[] height = new float[1];
            final Map<String, Float> tops = this.captions(Themes.flat().toBuilder()
                    .insets(TabBar.STYLE_PADDING, new Insets(8, 5, 8, 2))
                    .insets(TabBar.STYLE_PADDING_SELECTED, TabBarImpl.DEFAULT_PADDING)
                    .build(), height);
            assertEquals(TestPlatform.LINE_HEIGHT + 7f, height[0]);
            assertEquals(4f, tops.get("Buy"));
            assertEquals(5f, tops.get("Sell"));
        }

        @Test
        void tabsSizeForTheLargerPadding() {
            final float[] height = new float[1];
            final Map<String, Float> tops = this.captions(Themes.flat().toBuilder()
                    .insets(TabBar.STYLE_PADDING_SELECTED, new Insets(8, 6, 8, 4))
                    .build(), height);
            // The selected padding is 10 tall and the unselected default 7, so every tab is a line plus 10
            assertEquals(TestPlatform.LINE_HEIGHT + 10f, height[0]);
            assertEquals(6f, tops.get("Buy"));
            // The default centers its line in the 3 units it does not use: 4 plus half of 3
            assertEquals(5.5f, tops.get("Sell"));
        }
    }

    /**
     * Records the top of each text span drawn.
     */
    private static final class Captions implements Canvas {
        private final Map<String, Float> tops = new HashMap<>();

        @Override
        public void fill(final Rect rect, final int argb) {
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.tops.put(span.text(), y);
        }

        @Override
        public void pushClip(final Rect rect) {
        }

        @Override
        public void popClip() {
        }

        @Override
        public void pushTranslation(final float dx, final float dy) {
        }

        @Override
        public void pushScale(final float scale) {
        }

        @Override
        public void popTransform() {
        }

        @Override
        public void visual(final Visual visual, final Rect rect, final int tint) {
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }
}
