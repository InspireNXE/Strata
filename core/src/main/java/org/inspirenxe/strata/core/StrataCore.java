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

import org.inspirenxe.strata.api.appearance.Appearance;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.appearance.AppearancesImpl;
import org.inspirenxe.strata.core.host.PlatformScreens;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.host.ScreenStackImpl;
import org.inspirenxe.strata.core.text.TextFactoryImpl;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.ScreenStack;
import org.inspirenxe.strata.spi.TextSource;
import org.inspirenxe.strata.spi.ThemeSource;
import org.inspirenxe.strata.spi.Translator;

/**
 * Where engine adapters get Strata's implementation. Developers never call this; screens receive a
 * {@link org.inspirenxe.strata.api.ComponentFactory} from their host.
 */
public final class StrataCore {

    private StrataCore() {
    }

    /**
     * Sets the translator that {@linkplain Text#translatable translated text} reads through, for the whole process.
     * Open hosts lay out again on their next layout or render. Adapters call this once when they start, and may call
     * it again to replace the translator.
     *
     * @param translator the translator
     */
    public static void setTranslator(final Translator translator) {
        Translations.setTranslator(translator);
    }

    /**
     * Sets the platform's screen controller for the whole process: the one {@code Strata.screens()} returns, and the
     * one handles open screens with when their host has no opener of its own ({@link ScreenHost#setOpener}).
     * Adapters call this when they load, before any other code can ask for it, and may call it again to replace the
     * controller. The controller's {@code open(screen)} must use the default theme the adapter's
     * {@link ThemeSource#defaultTheme()} names, which the interface's default method does by passing
     * {@code ThemeRef.platform()}.
     *
     * @param controller the controller, or {@code null} to remove it, after which {@code Strata.screens()} throws
     *     {@link IllegalStateException}
     */
    public static void setScreenController(final ScreenController controller) {
        PlatformScreens.set(controller);
    }

    /**
     * Registers an engine type's default appearance beneath every other registration for the type, so anything
     * registered for it, earlier or later, takes precedence. Adapters call this when they start, for the engine
     * objects their visuals and text can show.
     *
     * @param <T> the type
     * @param type the engine type
     * @param appearance the appearance
     */
    public static <T> void registerDefaultAppearance(final Class<T> type, final Appearance<? super T> appearance) {
        AppearancesImpl.INSTANCE.registerDefault(type, appearance);
    }

    /**
     * Wraps an engine's own text as Strata text, so every text operation works on it and it reads anew whenever it
     * is resolved.
     *
     * @param source the engine text
     * @return the text
     */
    public static Text text(final TextSource source) {
        return TextFactoryImpl.INSTANCE.of(source);
    }

    /**
     * Creates a host for one screen at a time. A screen that asks to close through its handle is closed by the
     * host itself.
     *
     * @param platform the engine's platform
     * @param theme the theme for every component the host shows
     * @return a new host, with no screen open and a size of zero until resized
     */
    public static ScreenHost createScreenHost(final Platform platform, final Theme theme) {
        return new ScreenHostImpl(platform, theme, null);
    }

    /**
     * Creates a host for one screen at a time, letting the adapter decide what closing means.
     *
     * @param platform the engine's platform
     * @param theme the theme for every component the host shows
     * @param closeRequested runs when the open screen asks to close through its handle; an engine with its own
     *                       screens may close the engine screen here, which in turn closes the host
     * @return a new host, with no screen open and a size of zero until resized
     */
    public static ScreenHost createScreenHost(final Platform platform, final Theme theme, final Runnable closeRequested) {
        return new ScreenHostImpl(platform, theme, closeRequested);
    }

    /**
     * Creates a stack of screens, for engines without a screen system of their own.
     *
     * @param platform the engine's platform
     * @param theme the theme for every screen pushed
     * @return a new, empty stack with a size of zero until resized
     */
    public static ScreenStack createScreenStack(final Platform platform, final Theme theme) {
        return new ScreenStackImpl(platform, theme);
    }

    /**
     * Creates a host for one screen at a time whose theme is resolved from a ref and follows the platform's
     * {@link org.inspirenxe.strata.spi.ThemeSource}, letting the adapter decide what closing means.
     *
     * @param platform the engine's platform
     * @param theme the theme ref for every component the host shows
     * @param closeRequested runs when the open screen asks to close through its handle, or {@code null} to have
     *                       the host close the screen itself
     * @return a new host, with no screen open and a size of zero until resized
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved
     */
    public static ScreenHost createScreenHost(final Platform platform, final ThemeRef theme,
                                              final Runnable closeRequested) {
        return new ScreenHostImpl(platform, theme, closeRequested);
    }

    /**
     * Creates a stack of screens whose theme is resolved from a ref and follows the platform's
     * {@link org.inspirenxe.strata.spi.ThemeSource}.
     *
     * @param platform the engine's platform
     * @param theme the theme ref for every screen pushed
     * @return a new, empty stack with a size of zero until resized
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved
     */
    public static ScreenStack createScreenStack(final Platform platform, final ThemeRef theme) {
        return new ScreenStackImpl(platform, theme);
    }

    /**
     * Resolves a theme ref against a platform's theme source once, without a host, with the themes and overrides
     * added in code. The format, the layers and the merge are described on {@link ThemeResolver}.
     *
     * @param platform the engine's platform
     * @param theme the ref
     * @return the theme
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved
     */
    public static Theme resolveTheme(final Platform platform, final ThemeRef theme) {
        return StrataCore.resolveTheme(platform.themes(), theme);
    }

    /**
     * Resolves a theme ref against a theme source once, without a host or a platform, with the themes and overrides
     * added in code. The format, the layers and the merge are described on {@link ThemeResolver}.
     *
     * @param source where to find base themes and theme files
     * @param theme the ref
     * @return the theme
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved
     */
    public static Theme resolveTheme(final ThemeSource source, final ThemeRef theme) {
        return new ThemeResolver(source).resolve(theme);
    }
}
