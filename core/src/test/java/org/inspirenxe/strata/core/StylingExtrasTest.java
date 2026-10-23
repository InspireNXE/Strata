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
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.text.TextStylePatch;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Design 4.52: text style patches from code and typed metrics in theme files.
 */
class StylingExtrasTest {
    private static final ThemeId ENGINE = ThemeId.of("engine", "native");
    private static final TextStyle SHADOWED = TextStyle.DEFAULT.withShadow(true).withBold(true).withColor(0xFF111111);

    private static Theme resolve(final ThemeResolverTest.MapSource source, final ThemeRef ref) {
        return new ThemeResolver(source, new ThemeRegistryImpl()).resolve(ref);
    }

    private static ThemeResolverTest.MapSource engine() {
        return new ThemeResolverTest.MapSource().base(ENGINE, Theme.builder("engine").textStyle("t", SHADOWED).build());
    }

    @Nested
    class Patches {
        @Test
        void aPatchChangesOnlyWhatItSets() {
            final TextStylePatch patch = TextStylePatch.of().color(0xFFE0C060).italic(true);
            assertEquals(Set.of(TextStyle.Property.COLOR, TextStyle.Property.ITALIC), patch.properties());
            assertEquals(SHADOWED.withColor(0xFFE0C060).withItalic(true), patch.apply(SHADOWED));
            assertSame(SHADOWED, TextStylePatch.of().apply(SHADOWED));
        }

        @Test
        void aPatchCanTurnPropertiesOff() {
            assertEquals(SHADOWED.withShadow(false), TextStylePatch.of().shadow(false).apply(SHADOWED));
        }

        @Test
        void patchesThatChangeTheSameThingsAreEqual() {
            final TextStylePatch a = new TextStylePatch(SHADOWED.withColor(0xFF00FF00), Set.of(TextStyle.Property.COLOR));
            assertEquals(TextStylePatch.of().color(0xFF00FF00), a);
            assertEquals(TextStylePatch.of().color(0xFF00FF00).hashCode(), a.hashCode());
        }

        @Test
        void aWholeStylePatchGivesThatStyle() {
            assertEquals(SHADOWED, TextStylePatch.of(SHADOWED).apply(TextStyle.DEFAULT.withMonospace(true)));
        }

        @Test
        void thenLetsTheSecondWin() {
            final TextStylePatch both = TextStylePatch.of().color(0xFF000001).bold(false)
                    .then(TextStylePatch.of().color(0xFF000002).underline(true));
            assertEquals(SHADOWED.withColor(0xFF000002).withBold(false).withUnderline(true), both.apply(SHADOWED));
        }

        @Test
        void aRefPatchKeepsTheRestOfTheEngineStyle() {
            final Theme theme = resolve(engine(), ThemeRef.of(ENGINE).override("t", TextStylePatch.of().color(0xFFE0C060)));
            assertEquals(SHADOWED.withColor(0xFFE0C060), theme.textStyle("t").orElseThrow());
            assertEquals("ref", theme.layer("t").orElseThrow());
        }

        @Test
        void aRefStyleStillReplacesTheWholeStyle() {
            final Theme theme = resolve(engine(),
                    ThemeRef.of(ENGINE).override("t", TextStyle.DEFAULT.withColor(0xFFE0C060)));
            assertEquals(TextStyle.DEFAULT.withColor(0xFFE0C060), theme.textStyle("t").orElseThrow());
        }

        @Test
        void aStyleAndAPatchInOneLayerTakeTheStyleThenThePatch() {
            final ThemeOverrides overrides = ThemeOverrides.of()
                    .with("t", TextStylePatch.of().bold(true))
                    .with("t", TextStyle.DEFAULT.withColor(0xFF000005));
            final Theme theme = resolve(engine(), ThemeRef.of(ENGINE).override(overrides));
            assertEquals(TextStyle.DEFAULT.withColor(0xFF000005).withBold(true), theme.textStyle("t").orElseThrow());
        }

        @Test
        void patchesOfOneKeyCombine() {
            final ThemeRef ref = ThemeRef.of(ENGINE)
                    .override("t", TextStylePatch.of().color(0xFF000001))
                    .override("t", TextStylePatch.of().italic(true))
                    .override(ThemeOverrides.of().with("t", TextStylePatch.of().color(0xFF000002)));
            assertEquals(SHADOWED.withColor(0xFF000002).withItalic(true),
                    resolve(engine(), ref).textStyle("t").orElseThrow());
        }

        @Test
        void aPatchOfAKeyWithNoStylePatchesTheDefault() {
            final Theme theme = resolve(engine(), ThemeRef.of(ENGINE).override("u", TextStylePatch.of().bold(true)));
            assertEquals(TextStyle.DEFAULT.withBold(true), theme.textStyle("u").orElseThrow());
        }

        @Test
        void registryOverridesPatchToo() {
            final ThemeRegistryImpl registry = new ThemeRegistryImpl();
            registry.override(ENGINE, ThemeOverrides.of().with("t", TextStylePatch.of().strikethrough(true)));
            final Theme theme = new ThemeResolver(engine(), registry).resolve(ThemeRef.of(ENGINE));
            assertEquals(SHADOWED.withStrikethrough(true), theme.textStyle("t").orElseThrow());
            assertEquals("code", theme.layer("t").orElseThrow());
        }

        @Test
        void theOlderConstructorsSetNoPatches() {
            assertTrue(new ThemeOverrides(java.util.Map.of(), java.util.Map.of(), java.util.Map.of()).textPatches()
                    .isEmpty());
            assertTrue(ThemeRef.platform().textPatches().isEmpty());
        }
    }

    @Nested
    class InheritedText {
        private static final TextStyle LABEL = TextStyle.DEFAULT.withShadow(true).withColor(0xFFFFFFFF);
        private static final Theme THEME = Themes.flat().toBuilder().textStyle(Label.STYLE_TEXT, LABEL)
                .textStyle("fancy.text", TextStyle.DEFAULT).build();
        private final TestHost test = new TestHost(new TestPlatform(), THEME, 200, 100);
        private final ComponentFactory ui = this.test.factory();
        private final TextStylePatch gray = TextStylePatch.of().color(0xFF808080);

        /**
         * A component as a developer would write one, reading its text style when the theme is applied.
         */
        static final class Badge extends CustomComponent<Badge> {
            TextStyle style;

            Badge(final ComponentFactory ui) {
                super(ui);
            }

            @Override
            protected void onApplyTheme(final Theme theme) {
                this.style = theme.textStyleOr("fancy.text", TextStyle.DEFAULT);
            }

            @Override
            protected Size measure(final MeasureContext context, final float availableWidth,
                                   final float availableHeight) {
                return Size.ZERO;
            }
        }

        /**
         * A container as a developer would write one, stacking its children.
         */
        static final class Box extends CustomContainer<Box> {
            Box(final ComponentFactory ui) {
                super(ui);
            }

            @Override
            protected Size measure(final MeasureContext context, final float availableWidth,
                                   final float availableHeight) {
                for (final var child : this.children()) {
                    context.measure(child, availableWidth, availableHeight);
                }
                return Size.ZERO;
            }

            @Override
            protected void arrange(final ArrangeContext context, final Rect bounds) {
                for (final var child : this.children()) {
                    context.arrange(child, bounds);
                }
            }
        }

        @Test
        void aPanelPatchesTheTextOfTheControlsInsideIt() {
            final Label label = this.ui.label("Ana");
            final StackPanel panel = this.ui.stackPanel().add(this.ui.stackPanel().add(label));
            this.test.show(panel);
            assertEquals(LABEL, label.style());

            panel.setTextStyle(this.gray);
            assertEquals(Optional.of(this.gray), panel.textStyle());
            assertEquals(LABEL.withColor(0xFF808080), label.style());

            panel.setTextStyle(null);
            assertTrue(panel.textStyle().isEmpty());
            assertEquals(LABEL, label.style());
        }

        @Test
        void innerPatchesApplyLast() {
            final Label label = this.ui.label("Ana");
            final StackPanel inner = this.ui.stackPanel().add(label)
                    .setTextStyle(TextStylePatch.of().color(0xFF0000FF));
            final StackPanel outer = this.ui.stackPanel().add(inner)
                    .setTextStyle(TextStylePatch.of().color(0xFFFF0000).bold(true));
            this.test.show(outer);

            assertEquals(LABEL.withColor(0xFF0000FF).withBold(true), label.style());
        }

        @Test
        void aStyleSetInCodeWins() {
            final TextStyle own = TextStyle.DEFAULT.withItalic(true);
            final Label label = this.ui.label("Ana").setStyle(own);
            this.test.show(this.ui.stackPanel().add(label).setTextStyle(this.gray));

            assertEquals(own, label.style());
        }

        @Test
        void theTreeChangingFollowsThePatches() {
            final Label label = this.ui.label("Ana");
            final StackPanel gray = this.ui.stackPanel().setTextStyle(this.gray);
            final StackPanel plain = this.ui.stackPanel();
            this.test.show(this.ui.stackPanel().add(gray).add(plain));

            gray.add(label);
            assertEquals(LABEL.withColor(0xFF808080), label.style());
            gray.remove(label);
            plain.add(label);
            assertEquals(LABEL, label.style());
        }

        @Test
        void aNewThemeKeepsThePatch() {
            final Label label = this.ui.label("Ana");
            this.test.show(this.ui.stackPanel().add(label).setTextStyle(this.gray));
            this.test.host().setTheme(THEME.toBuilder().textStyle(Label.STYLE_TEXT, TextStyle.DEFAULT).build());

            assertEquals(TextStyle.DEFAULT.withColor(0xFF808080), label.style());
        }

        @Test
        void aDevelopersComponentAndContainerTakePartToo() {
            final Badge badge = new Badge(this.ui);
            final Box box = new Box(this.ui);
            box.add(badge);
            this.test.show(box);
            assertEquals(TextStyle.DEFAULT, badge.style);

            box.setTextStyle(TextStylePatch.of().underline(true));
            assertEquals(TextStyle.DEFAULT.withUnderline(true), badge.style);
        }

        @Test
        void otherKindsOfValueAreLeftAlone() {
            final Badge badge = new Badge(this.ui);
            final StackPanel panel = this.ui.stackPanel().add(badge).setTextStyle(this.gray);
            this.test.show(panel);
            assertEquals(THEME.paints(), new Probe(badge).theme().paints());
            assertEquals(THEME.metrics(), new Probe(badge).theme().metrics());
        }

        /**
         * Reads a component's theme from outside it, as only a subclass could.
         */
        private record Probe(Badge badge) {
            Theme theme() {
                return this.badge.peer().theme();
            }
        }
    }

    @Nested
    class Metrics {
        private Theme file(final String metrics) {
            return resolve(engine().file(ENGINE, "{ \"metrics\": " + metrics + " }"), ThemeRef.of(ENGINE));
        }

        @Test
        void flagsAreTrueAndFalse() {
            final Theme theme = this.file("{ \"on\": true, \"off\": false, \"number\": 2 }");
            assertEquals(1f, theme.metricOr("on", -1f));
            assertEquals(0f, theme.metricOr("off", -1f));
            assertTrue(theme.flagOr("on", false));
            assertFalse(theme.flagOr("off", true));
            assertTrue(theme.flagOr("number", false));
            assertTrue(theme.flagOr("missing", true));
        }

        @Test
        void durationsTakeAUnit() {
            final Theme theme = this.file("{ \"a\": \"500ms\", \"b\": \"0.25s\", \"c\": 750, \"d\": \"2s\" }");
            assertEquals(500L, theme.millisOr("a", 0L));
            assertEquals(250L, theme.millisOr("b", 0L));
            assertEquals(750L, theme.millisOr("c", 0L));
            assertEquals(2000f, theme.metricOr("d", 0f));
            assertEquals(42L, theme.millisOr("missing", 42L));
        }

        @Test
        void otherStringsRejectTheFile() {
            for (final String bad : new String[] {"\"500\"", "\"ms\"", "\"-5ms\"", "\"5 ms\"", "\"5min\"", "\"1e3ms\"",
                "\"Infinitys\"", "[1]"}) {
                final Theme theme = this.file("{ \"k\": " + bad + " }");
                assertTrue(theme.metric("k").isEmpty(), bad);
            }
        }

        @Test
        void flagsFromCode() {
            final Theme theme = resolve(engine(), ThemeRef.of(ENGINE).override("on", true)
                    .override(ThemeOverrides.of().with("off", false)));
            assertTrue(theme.flagOr("on", false));
            assertFalse(theme.flagOr("off", true));
            assertEquals(1f, theme.metricOr("on", 0f));
        }
    }
}
