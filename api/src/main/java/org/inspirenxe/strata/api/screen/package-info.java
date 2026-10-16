/**
 * Screens: the unit a host shows.
 *
 * <p>A {@link org.inspirenxe.strata.api.screen.Screen} builds a component tree when the host opens it, and can ask
 * for a rebuild or to be closed through the {@link org.inspirenxe.strata.api.screen.ScreenHandle} it receives.
 * Engine adapters decide how screens are opened: on Minecraft, wrap one in a {@code StrataScreen}; with Swing, push
 * it on a {@code StrataPanel}'s screen stack.</p>
 */
package org.inspirenxe.strata.api.screen;
