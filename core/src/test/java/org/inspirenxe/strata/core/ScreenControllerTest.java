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
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.host.PlatformScreens;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opening screens: the controller an adapter hands over, {@code Strata.screens()}, handles that open screens above
 * their own, and {@code ThemeRef.platform()}.
 */
class ScreenControllerTest {
    private static final ThemeId ENGINE = ThemeId.of("engine", "native");
    private static final Paint ENGINE_BUTTON = Paint.solid(0xFF102030);

    @AfterEach
    void removeController() {
        StrataCore.setScreenController(null);
    }

    /**
     * A source whose default theme is the flat theme with one paint changed.
     */
    private static ThemeResolverTest.MapSource engineSource() {
        return new ThemeResolverTest.MapSource()
                .base(ENGINE, Themes.flat().toBuilder().paint(Button.STYLE_NORMAL, ENGINE_BUTTON).build())
                .defaultTheme(ENGINE);
    }

    @Nested
    class HandOver {
        @Test
        void screensThrowsUntilAControllerIsSet() {
            final IllegalStateException error = assertThrows(IllegalStateException.class, Strata::screens);
            assertEquals(PlatformScreens.MISSING, error.getMessage());
            assertTrue(error.getMessage().contains("StrataPanel.setDefault()"));
        }

        @Test
        void screensReturnsTheControllerHandedOver() {
            final Recorder controller = new Recorder();
            StrataCore.setScreenController(controller);
            assertSame(controller, Strata.screens());

            final Recorder replacement = new Recorder();
            StrataCore.setScreenController(replacement);
            assertSame(replacement, Strata.screens());

            StrataCore.setScreenController(null);
            assertThrows(IllegalStateException.class, Strata::screens);
        }

        @Test
        void openWithoutAThemeAsksForThePlatformTheme() {
            final Recorder controller = new Recorder();
            StrataCore.setScreenController(controller);
            final Screen screen = ui -> ui.anchorPanel();

            Strata.screens().open(screen);

            assertEquals(List.of(screen), controller.screens);
            assertEquals(List.of(ThemeRef.platform()), controller.themes);
        }

        @Test
        void aHostWithoutAnOpenerOpensThroughThePlatformController() {
            final Recorder controller = new Recorder();
            StrataCore.setScreenController(controller);
            final TestHost test = new TestHost(100f, 100f);
            final ScreenHandle handle = test.host().openScreen(ui -> ui.anchorPanel());
            final Screen next = ui -> ui.anchorPanel();
            final ThemeRef theme = ThemeRef.of(Themes.FLAT);

            handle.open(next, theme);

            assertEquals(List.of(next), controller.screens);
            assertEquals(List.of(theme), controller.themes);
        }

        @Test
        void aHostWithAnOpenerOpensThroughIt() {
            final Recorder platform = new Recorder();
            final Recorder opener = new Recorder();
            StrataCore.setScreenController(platform);
            final TestHost test = new TestHost(100f, 100f);
            test.host().setOpener(opener);
            final ScreenHandle handle = test.host().openScreen(ui -> ui.anchorPanel());

            handle.open(ui -> ui.anchorPanel());

            assertEquals(1, opener.screens.size());
            assertEquals(List.of(ThemeRef.platform()), opener.themes);
            assertTrue(platform.screens.isEmpty());
        }

        @Test
        void aClosedScreensHandleCannotOpen() {
            final Recorder controller = new Recorder();
            StrataCore.setScreenController(controller);
            final TestHost test = new TestHost(100f, 100f);
            final ScreenHandle handle = test.host().openScreen(ui -> ui.anchorPanel());
            assertSame(handle, test.host().handle().orElseThrow());

            test.host().close();

            assertTrue(test.host().handle().isEmpty());
            assertThrows(IllegalStateException.class, () -> handle.open(ui -> ui.anchorPanel()));
            assertTrue(controller.screens.isEmpty());
        }

        @Test
        void aReopenedScreenGetsANewHandle() {
            final TestHost test = new TestHost(100f, 100f);
            final Screen screen = ui -> ui.anchorPanel();
            final ScreenHandle first = test.host().openScreen(screen);
            final ScreenHandle second = test.host().openScreen(screen);

            assertThrows(IllegalStateException.class, () -> first.open(ui -> ui.anchorPanel()));
            assertSame(second, test.host().handle().orElseThrow());
        }

        @Test
        void screensHaveAnEmptyTitleByDefault() {
            final Screen screen = ui -> ui.anchorPanel();
            assertEquals(Text.empty(), screen.title());
        }
    }

    @Nested
    class Stacks {
        private final TestPlatform platform = new TestPlatform();
        private final ScreenStack stack;

        Stacks() {
            this.platform.setThemes(engineSource());
            this.stack = StrataCore.createScreenStack(this.platform, Themes.flat());
            this.stack.resize(100f, 100f);
        }

        @Test
        void aHandleOpensAboveItsScreenAndClosingReturnsToIt() {
            final Counting first = new Counting();
            this.stack.push(first);
            final Component<?> tree = first.built.get(0);

            final Counting second = new Counting();
            final ScreenHandle opened = first.handle.open(second);

            assertSame(second, this.stack.top().orElseThrow());
            assertSame(opened, second.handle);
            assertEquals(0, first.closes);

            opened.close();

            assertSame(first, this.stack.top().orElseThrow());
            assertEquals(1, second.closes);
            assertEquals(0, first.closes);
            assertEquals(List.of(tree), first.built);
        }

        @Test
        void aCoveredScreensHandleOpensStraightAboveIt() {
            final Counting bottom = new Counting();
            this.stack.push(bottom);
            final Counting top = new Counting();
            bottom.handle.open(top);
            final Counting middle = new Counting();

            bottom.handle.open(middle);

            assertSame(top, this.stack.top().orElseThrow());
            this.stack.pop();
            assertSame(middle, this.stack.top().orElseThrow());
            middle.handle.close();
            assertSame(bottom, this.stack.top().orElseThrow());
        }

        @Test
        void aScreenCanOpenAnotherFromOpened() {
            final Counting child = new Counting();
            final Counting parent = new Counting() {
                @Override
                public void opened(final ScreenHandle handle) {
                    super.opened(handle);
                    handle.open(child);
                }
            };

            this.stack.push(parent);

            assertSame(child, this.stack.top().orElseThrow());
            this.stack.pop();
            assertSame(parent, this.stack.top().orElseThrow());
        }

        @Test
        void aRemovedScreensHandleCannotOpen() {
            final Counting screen = new Counting();
            this.stack.push(screen);
            this.stack.pop();

            assertThrows(IllegalStateException.class, () -> screen.handle.open(new Counting()));
        }

        @Test
        void openWithoutAThemeUsesThePlatformDefault() {
            final Counting screen = new Counting();

            final ScreenHandle handle = this.stack.open(screen);

            assertSame(screen, this.stack.top().orElseThrow());
            assertEquals(ENGINE_BUTTON, handle.factory().theme().paint(Button.STYLE_NORMAL).orElseThrow());
        }

        @Test
        void screensOpenedInAThemeKeepItWhenTheStackChangesTheme() {
            final Counting pushed = new Counting();
            this.stack.push(pushed);
            final Counting opened = new Counting();
            this.stack.open(opened, ThemeRef.of(ENGINE));
            final Paint changed = Paint.solid(0xFF0000FF);

            this.stack.setTheme(Themes.flat().toBuilder().paint(Button.STYLE_NORMAL, changed).build());

            assertEquals(changed, pushed.handle.factory().theme().paint(Button.STYLE_NORMAL).orElseThrow());
            assertEquals(ENGINE_BUTTON, opened.handle.factory().theme().paint(Button.STYLE_NORMAL).orElseThrow());
        }

        @Test
        void aScreenThatFailsToOpenIsNotStacked() {
            final Screen broken = ui -> {
                throw new IllegalArgumentException("broken");
            };

            assertThrows(IllegalArgumentException.class, () -> this.stack.open(broken));

            assertTrue(this.stack.isEmpty());
        }
    }

    @Nested
    class PlatformTheme {
        private final ThemeRegistryImpl registry = new ThemeRegistryImpl();

        private Theme resolve(final ThemeResolverTest.MapSource source, final ThemeRef ref) {
            return new ThemeResolver(source, this.registry).resolve(ref);
        }

        @Test
        void resolvesAsTheSourcesDefaultTheme() {
            final Theme theme = this.resolve(engineSource(), ThemeRef.platform());
            assertEquals(ENGINE_BUTTON, theme.paint(Button.STYLE_NORMAL).orElseThrow());
        }

        @Test
        void isTheFlatThemeWhereTheSourceNamesNone() {
            final Theme theme = this.resolve(new ThemeResolverTest.MapSource(), ThemeRef.platform());
            assertEquals(Themes.flat().paints(), theme.paints());
        }

        @Test
        void takesOverridesForThePlatformAboveTheDefaultThemesOwn() {
            final Paint engine = Paint.solid(0xFF000001);
            final Paint platform = Paint.solid(0xFF000002);
            this.registry.override(ENGINE, ThemeOverrides.of()
                    .with(Button.STYLE_NORMAL, engine)
                    .with(Button.STYLE_HOVERED, engine));
            this.registry.override(Themes.PLATFORM, ThemeOverrides.of().with(Button.STYLE_NORMAL, platform));

            final Theme theme = this.resolve(engineSource(), ThemeRef.platform());

            assertEquals(platform, theme.paint(Button.STYLE_NORMAL).orElseThrow());
            assertEquals(engine, theme.paint(Button.STYLE_HOVERED).orElseThrow());
            assertEquals(ThemeResolver.CODE_LAYER, theme.layer(Button.STYLE_NORMAL).orElseThrow());
        }

        @Test
        void overridesForThePlatformDoNotReachTheDefaultThemeByItsId() {
            this.registry.override(Themes.PLATFORM, ThemeOverrides.of()
                    .with(Button.STYLE_NORMAL, Paint.solid(0xFF000002)));

            final Theme theme = this.resolve(engineSource(), ThemeRef.of(ENGINE));

            assertEquals(ENGINE_BUTTON, theme.paint(Button.STYLE_NORMAL).orElseThrow());
        }

        @Test
        void aRefsOwnOverridesWin() {
            final Paint mine = Paint.solid(0xFF000003);
            this.registry.override(Themes.PLATFORM, ThemeOverrides.of()
                    .with(Button.STYLE_NORMAL, Paint.solid(0xFF000002)));

            final Theme theme = this.resolve(engineSource(), ThemeRef.platform().override(Button.STYLE_NORMAL, mine));

            assertEquals(mine, theme.paint(Button.STYLE_NORMAL).orElseThrow());
        }

        @Test
        void themeFilesCanNameItAsTheirParent() {
            final ThemeId shop = ThemeId.of("mod", "shop");
            final ThemeResolverTest.MapSource source = engineSource()
                    .file(shop, """
                            { "parent": "strata:platform", "paints": { "button.hovered": "#000004" } }""");

            final Theme theme = this.resolve(source, ThemeRef.of(shop));

            assertEquals(ENGINE_BUTTON, theme.paint(Button.STYLE_NORMAL).orElseThrow());
            assertEquals(Paint.solid(0xFF000004), theme.paint(Button.STYLE_HOVERED).orElseThrow());
        }

        @Test
        void aDefaultThatIsThePlatformItselfThrows() {
            final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource().defaultTheme(Themes.PLATFORM);
            assertThrows(ThemeException.class, () -> this.resolve(source, ThemeRef.platform()));
        }

        @Test
        void aDefaultWhoseParentIsThePlatformThrows() {
            final ThemeResolverTest.MapSource source = new ThemeResolverTest.MapSource()
                    .file(ENGINE, """
                            { "parent": "strata:platform" }""")
                    .defaultTheme(ENGINE);
            assertThrows(ThemeException.class, () -> this.resolve(source, ThemeRef.platform()));
        }

        @Test
        void cannotBeDefinedInCode() {
            assertThrows(ThemeException.class,
                    () -> this.registry.define(Themes.PLATFORM, Themes.FLAT, ThemeOverrides.of()));
        }

        @Test
        void aHostFollowsTheDefaultWhenTheSourceChanges() {
            final TestPlatform platform = new TestPlatform();
            final ThemeResolverTest.MapSource source = engineSource();
            platform.setThemes(source);
            final ScreenHostImpl host = (ScreenHostImpl) StrataCore.createScreenHost(platform, ThemeRef.platform(),
                    null);
            assertEquals(ENGINE_BUTTON, host.theme().paint(Button.STYLE_NORMAL).orElseThrow());

            source.defaultTheme(Themes.FLAT).bump();
            host.layout();

            assertEquals(Themes.flat().paint(Button.STYLE_NORMAL).orElseThrow(), host.theme().paint(Button.STYLE_NORMAL).orElseThrow());
        }
    }

    /**
     * A controller that records what it was asked to open and opens nothing.
     */
    private static final class Recorder implements ScreenController {
        private final List<Screen> screens = new ArrayList<>();
        private final List<ThemeRef> themes = new ArrayList<>();

        @Override
        public ScreenHandle open(final Screen screen, final ThemeRef theme) {
            this.screens.add(screen);
            this.themes.add(theme);
            return new TestHost(10f, 10f).host().openScreen(screen);
        }
    }

    /**
     * A screen that keeps its handle and counts its trees and closes.
     */
    private static class Counting implements Screen {
        private final List<Component<?>> built = new ArrayList<>();
        private ScreenHandle handle;
        private int closes;

        @Override
        public Component<?> build(final ComponentFactory factory) {
            final Component<?> tree = factory.anchorPanel();
            this.built.add(tree);
            return tree;
        }

        @Override
        public void opened(final ScreenHandle handle) {
            this.handle = handle;
        }

        @Override
        public void closed() {
            this.closes++;
        }
    }
}
