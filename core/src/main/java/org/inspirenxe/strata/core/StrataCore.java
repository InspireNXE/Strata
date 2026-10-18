package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.host.ScreenStackImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.ScreenStack;

/**
 * Where engine adapters get Strata's implementation. Developers never call this; screens receive a
 * {@link org.inspirenxe.strata.api.ComponentFactory} from their host.
 */
public final class StrataCore {

    private StrataCore() {
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
     * Resolves a theme ref against a platform's theme source once, without a host. The format and the merge are
     * described on {@link ThemeResolver}.
     *
     * @param platform the engine's platform
     * @param theme the ref
     * @return the theme
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved
     */
    public static Theme resolveTheme(final Platform platform, final ThemeRef theme) {
        return new ThemeResolver(platform.themes()).resolve(theme);
    }
}
