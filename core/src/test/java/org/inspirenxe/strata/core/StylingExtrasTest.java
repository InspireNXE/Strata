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

import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.text.TextStylePatch;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
