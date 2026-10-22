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

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Themes and overrides added in code: where they apply among the layers, closing them, definitions, and hosts
 * that follow them.
 */
class ThemeRegistryTest {
    private static final ThemeId ENGINE = ThemeId.of("engine", "native");
    private static final ThemeId MOD = ThemeId.of("mod", "shop");
    private static final String FLAT_FILE = "{ \"parent\": \"strata:flat\", \"paints\": { \"a\": \"#000001\" } }";

    private final ThemeRegistryImpl registry = new ThemeRegistryImpl();

    private Theme resolve(final ThemeResolverTest.MapSource source, final ThemeRef ref) {
        return new ThemeResolver(source, this.registry).resolve(ref);
    }

    @Test
    void strataThemesIsCoresRegistry() {
        assertSame(ThemeRegistryImpl.INSTANCE, Strata.themes());
    }

    @Test
    void overridesBuildNewValues() {
        final ThemeOverrides empty = ThemeOverrides.of();
        final ThemeOverrides one = empty.with("a", Paint.solid(0xFF000001));
        final ThemeOverrides two = one.with("t", TextStyle.DEFAULT.withBold(true)).with("i", Insets.of(2));
        assertTrue(empty.paints().isEmpty());
        assertEquals(Map.of("a", Paint.solid(0xFF000001)), one.paints());
        assertTrue(one.textStyles().isEmpty());
        assertEquals(Map.of("t", TextStyle.DEFAULT.withBold(true)), two.textStyles());
        assertEquals(Map.of("i", Insets.of(2)), two.insets());
    }

    @Test
    void aRefTakesEveryKeyOfSomeOverrides() {
        final ThemeRef ref = ThemeRef.of(MOD)
                .override("a", Paint.solid(0xFF000001))
                .override(ThemeOverrides.of()
                        .with("a", Paint.solid(0xFF000002))
                        .with("t", TextStyle.DEFAULT.withItalic(true))
                        .with("i", Insets.of(3)));
        assertEquals(Map.of("a", Paint.solid(0xFF000002)), ref.paints());
        assertEquals(Map.of("t", TextStyle.DEFAULT.withItalic(true)), ref.textStyles());
        assertEquals(Map.of("i", Insets.of(3)), ref.insets());
    }

    @Test
    void overridesMergeKeyByKeyAndARefHoldsThem() {
        final ThemeOverrides merged = ThemeOverrides.of()
                .with("a", Paint.solid(0xFF000001))
                .with("m", 2f)
                .with(ThemeOverrides.of().with("a", Paint.solid(0xFF000002)).with("b", Paint.solid(0xFF000003)));
        assertEquals(Map.of("a", Paint.solid(0xFF000002), "b", Paint.solid(0xFF000003)), merged.paints());
        assertEquals(Map.of("m", 2f), merged.metrics());

        final ThemeRef ref = new ThemeRef(MOD, merged);
        assertSame(merged, ref.overrides());
        assertEquals(ref, ThemeRef.of(MOD).override(merged));
        assertEquals(new ThemeRef(MOD, merged.paints(), Map.of(), Map.of(), merged.metrics()), ref);
    }

    @Test
    void overridesApplyAboveFilesInTheOrderTheyWereAdded() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource()
                .file(MOD, "pack", FLAT_FILE);
        this.registry.override(MOD, ThemeOverrides.of()
                .with("a", Paint.solid(0xFF000002))
                .with("b", Paint.solid(0xFF000002)));
        this.registry.override(MOD, ThemeOverrides.of()
                .with("b", Paint.solid(0xFF000003))
                .with("t", TextStyle.DEFAULT.withBold(true)));
        final Theme theme = this.resolve(source, ThemeRef.of(MOD));
        assertEquals(Paint.solid(0xFF000002), theme.paint("a").orElseThrow());
        assertEquals(Paint.solid(0xFF000003), theme.paint("b").orElseThrow());
        assertEquals(Optional.of(ThemeResolver.CODE_LAYER), theme.layer("a"));
        assertEquals(Optional.of(ThemeResolver.CODE_LAYER), theme.layer("b"));
        // A text style from code replaces the whole style
        assertEquals(TextStyle.DEFAULT.withBold(true), theme.textStyle("t").orElseThrow());
    }

    @Test
    void aRefsOverridesWinOverTheRegistry() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource().file(MOD, FLAT_FILE);
        this.registry.override(MOD, ThemeOverrides.of().with("a", Paint.solid(0xFF000002)));
        final Theme theme = this.resolve(source, ThemeRef.of(MOD).override("a", Paint.solid(0xFF000003)));
        assertEquals(Paint.solid(0xFF000003), theme.paint("a").orElseThrow());
        assertEquals(Optional.of(ThemeResolver.REF_LAYER), theme.layer("a"));
    }

    @Test
    void overridesWaitForTheirId() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource();
        this.registry.override(MOD, ThemeOverrides.of().with("a", Paint.solid(0xFF000002)));
        assertThrows(ThemeException.class, () -> this.resolve(source, ThemeRef.of(MOD)));
        source.file(MOD, FLAT_FILE);
        assertEquals(Paint.solid(0xFF000002), this.resolve(source, ThemeRef.of(MOD)).paint("a").orElseThrow());
    }

    @Test
    void closingAnOverrideRestoresWhatWasBeneath() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource().file(MOD, "pack", FLAT_FILE);
        final long before = this.registry.version();
        final Subscription subscription =
                this.registry.override(MOD, ThemeOverrides.of().with("a", Paint.solid(0xFF000002)));
        final long added = this.registry.version();
        assertNotEquals(before, added);

        subscription.close();
        final long closed = this.registry.version();
        assertNotEquals(added, closed);
        final Theme theme = this.resolve(source, ThemeRef.of(MOD));
        assertEquals(Paint.solid(0xFF000001), theme.paint("a").orElseThrow());
        assertEquals(Optional.of("pack"), theme.layer("a"));

        subscription.close();
        assertEquals(closed, this.registry.version());
    }

    @Test
    void equalOverridesCloseOneAtATime() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource().file(MOD, FLAT_FILE);
        final ThemeOverrides overrides = ThemeOverrides.of().with("a", Paint.solid(0xFF000002));
        final Subscription first = this.registry.override(MOD, overrides);
        this.registry.override(MOD, overrides);
        first.close();
        first.close();
        assertEquals(Paint.solid(0xFF000002), this.resolve(source, ThemeRef.of(MOD)).paint("a").orElseThrow());
    }

    @Test
    void aDefinedThemeStartsFromItsParentAndFollowsFiles() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource()
                .file(MOD, "pack", "{ \"parent\": \"engine:native\", \"paints\": { \"b\": \"#000009\" } }");
        this.registry.define(MOD, Themes.FLAT, ThemeOverrides.of().with("a", Paint.solid(0xFF000002)));
        final Theme theme = this.resolve(source, ThemeRef.of(MOD));
        assertEquals("mod:shop", theme.name());
        assertEquals(Themes.flat().paint(Button.STYLE_NORMAL), theme.paint(Button.STYLE_NORMAL));
        assertEquals(Optional.of(ThemeResolver.BASE_LAYER), theme.layer(Button.STYLE_NORMAL));
        assertEquals(Paint.solid(0xFF000002), theme.paint("a").orElseThrow());
        assertEquals(Optional.of(ThemeResolver.CODE_LAYER), theme.layer("a"));
        // The pack's keys apply, and its parent, which names no theme, is ignored
        assertEquals(Paint.solid(0xFF000009), theme.paint("b").orElseThrow());
        assertEquals(Optional.of("pack"), theme.layer("b"));
    }

    @Test
    void aDefinedThemeInheritsItsParentsOverrides() {
        this.registry.override(Themes.FLAT, ThemeOverrides.of().with(Button.STYLE_NORMAL, Paint.solid(0xFF000004)));
        this.registry.define(MOD, Themes.FLAT, ThemeOverrides.of());
        final Theme theme = this.resolve(new ThemeResolverTest.MapSource(), ThemeRef.of(MOD));
        assertEquals(Paint.solid(0xFF000004), theme.paint(Button.STYLE_NORMAL).orElseThrow());
    }

    @Test
    void anIdCanBeDefinedOnceAtATime() {
        final Subscription first = this.registry.define(MOD, Themes.FLAT, ThemeOverrides.of());
        assertThrows(ThemeException.class, () -> this.registry.define(MOD, Themes.FLAT, ThemeOverrides.of()));
        assertThrows(ThemeException.class, () -> this.registry.define(Themes.FLAT, MOD, ThemeOverrides.of()));

        first.close();
        this.registry.define(MOD, Themes.FLAT, ThemeOverrides.of().with("a", Paint.solid(0xFF000005)));
        first.close();
        assertEquals(Paint.solid(0xFF000005),
                this.resolve(new ThemeResolverTest.MapSource(), ThemeRef.of(MOD)).paint("a").orElseThrow());
    }

    @Test
    void definingAnIdTheAdapterGivesABaseFailsWhenItResolves() {
        final ThemeResolverTest.MapSource built = new ThemeResolverTest.MapSource()
                .base(ENGINE, Theme.builder("engine").paint("a", Paint.solid(0xFF000001)).build());
        final ThemeResolverTest.MapSource files = new ThemeResolverTest.MapSource().baseFile(ENGINE, "engine", "{}");
        this.registry.define(ENGINE, Themes.FLAT, ThemeOverrides.of());
        assertThrows(ThemeException.class, () -> this.resolve(built, ThemeRef.of(ENGINE)));
        assertThrows(ThemeException.class, () -> this.resolve(files, ThemeRef.of(ENGINE)));
    }

    @Test
    void aCycleThroughADefinitionFails() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource()
                .file(ENGINE, "{ \"parent\": \"mod:shop\" }");
        this.registry.define(MOD, ENGINE, ThemeOverrides.of());
        assertThrows(ThemeException.class, () -> this.resolve(source, ThemeRef.of(MOD)));
    }

    @Test
    void aHostFollowsTheRegistry() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource().file(MOD,
                "{ \"parent\": \"strata:flat\", \"paints\": { \"button.normal\": \"#FF0000\" } }");
        final TestPlatform platform = new TestPlatform();
        platform.setThemes(source);
        final ScreenHostImpl host = (ScreenHostImpl) StrataCore.createScreenHost(platform, ThemeRef.of(MOD), null);
        host.open(ui -> ui.button("Buy"));
        host.layout();
        assertEquals(Paint.solid(0xFFFF0000), host.theme().paint(Button.STYLE_NORMAL).orElseThrow());

        final Subscription subscription = Strata.themes().override(MOD, ThemeOverrides.of()
                .with(Button.STYLE_NORMAL, Paint.solid(0xFF00FF00)));
        try {
            host.layout();
            assertEquals(Paint.solid(0xFF00FF00), host.theme().paint(Button.STYLE_NORMAL).orElseThrow());
            assertEquals(Optional.of(ThemeResolver.CODE_LAYER), host.theme().layer(Button.STYLE_NORMAL));
        } finally {
            subscription.close();
        }
        host.layout();
        assertEquals(Paint.solid(0xFFFF0000), host.theme().paint(Button.STYLE_NORMAL).orElseThrow());
    }

    @Test
    void aHostKeepsItsThemeWhenADefinitionGoesAway() {
        final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource();
        final TestPlatform platform = new TestPlatform();
        platform.setThemes(source);
        final Subscription definition = Strata.themes().define(MOD, Themes.FLAT, ThemeOverrides.of()
                .with(Button.STYLE_NORMAL, Paint.solid(0xFF0000FF)));
        final ScreenHostImpl host;
        try {
            host = (ScreenHostImpl) StrataCore.createScreenHost(platform, ThemeRef.of(MOD), null);
            host.open(ui -> ui.button("Buy"));
            host.layout();
        } finally {
            definition.close();
        }
        final Theme before = host.theme();
        host.layout();
        assertSame(before, host.theme());
        assertFalse(host.themeRef().isEmpty());
    }
}
