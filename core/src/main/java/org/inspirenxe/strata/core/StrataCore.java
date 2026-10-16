package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenStack;
import org.inspirenxe.strata.spi.UiHost;

/**
 * Where adapters get Strata's implementation. Developers never call this; they receive a
 * {@link org.inspirenxe.strata.api.Ui} from the host.
 */
public final class StrataCore {

    private StrataCore() {
    }

    /**
     * A host whose screens close themselves when they ask to.
     */
    public static UiHost createHost(final Platform platform, final Theme theme) {
        return new CoreHost(platform, theme, null);
    }

    /**
     * @param closeRequested runs when the open screen asks to close; the adapter decides what closing means (on
     *                       Minecraft, returning to the previous screen)
     */
    public static UiHost createHost(final Platform platform, final Theme theme, final Runnable closeRequested) {
        return new CoreHost(platform, theme, closeRequested);
    }

    public static ScreenStack createScreenStack(final Platform platform, final Theme theme) {
        return new CoreScreenStack(platform, theme);
    }
}
