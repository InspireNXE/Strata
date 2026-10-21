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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;

import java.util.Optional;

/**
 * Shows one {@link Screen} at a time. Implemented by Strata and created with {@code StrataCore.createScreenHost};
 * adapters drive it. Engines that keep their own stack of screens use one host per engine screen. Do not implement
 * this interface.
 *
 * <h2>Requirements for callers (adapters)</h2>
 * <ul>
 *     <li>Call {@link #resize} before the first {@link #render}, and again whenever the area changes.</li>
 *     <li>Call {@link #render} once per frame. It builds pending screens, runs layout if needed, draws, and asks for
 *     the cursor.</li>
 *     <li>Call {@link #close()} when the engine screen goes away, so the Strata screen's {@code closed()} runs.</li>
 *     <li>Call everything on the UI thread.</li>
 * </ul>
 */
public interface ScreenHost extends InputSink {

    /**
     * Returns the factory for components shown by this host, the one screens receive in {@link Screen#build}.
     *
     * @return the factory
     */
    ComponentFactory factory();

    /**
     * Closes the current screen, if any, then builds and opens this one at the current size.
     *
     * @param screen the screen to show
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the screen's tree needs; no
     *                                                         screen is open afterwards
     * @throws IllegalArgumentException if the screen's {@code build} returns a component that is already in a tree,
     *                                  that implements a component interface directly, or that another host's
     *                                  factory made
     */
    void open(Screen screen);

    /**
     * Closes the current screen: calls its {@code closed()} and detaches its tree. Does nothing if none is open.
     */
    void close();

    /**
     * Returns the screen being shown.
     *
     * @return the open screen, or empty
     */
    Optional<Screen> screen();

    /**
     * Returns the handle of the open screen, the one passed to its {@link Screen#opened}.
     *
     * @return the handle, or empty if no screen is open
     */
    Optional<ScreenHandle> handle();

    /**
     * Sets what the open screen's handle opens screens with, through {@link ScreenHandle#open}. The adapter's
     * controller must put the new screen above this host's screen and show this host's screen again, unchanged,
     * when the new one closes. It is called on the UI thread with the screen and theme the handle was given, and
     * its result is returned from the handle.
     *
     * <p>Until one is set, a handle opens screens through the platform's controller, the one
     * {@code StrataCore.setScreenController} was given. A host inside a {@link ScreenStack} has the stack's own,
     * which puts the new screen straight above this one.</p>
     *
     * @param opener what to open screens above this host's screen with, or {@code null} to use the platform's
     *     controller
     */
    void setOpener(ScreenController opener);

    /**
     * Returns the theme every component in this host uses.
     *
     * @return the theme
     */
    Theme theme();

    /**
     * Replaces the theme. Components re-read their style keys and a new layout runs.
     *
     * @param theme the new theme
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the open tree needs; the old
     *                                                         theme stays
     */
    void setTheme(Theme theme);

    /**
     * Replaces the theme with one resolved from the platform's {@link ThemeSource}. The host resolves the ref
     * again whenever {@link ThemeSource#version()} changes. A reload that fails, or that leaves out keys the open
     * tree needs, is logged and the host keeps its current theme.
     *
     * @param theme the theme to resolve
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved now, or the result
     *                                                         lacks keys the open tree needs; the old theme stays
     */
    void setTheme(ThemeRef theme);

    /**
     * Returns the ref the theme was resolved from.
     *
     * @return the ref, or empty if the theme was set as a {@link Theme}
     */
    Optional<ThemeRef> themeRef();

    /**
     * Sets the size of the area the host fills, in UI units. A change schedules a layout.
     *
     * @param width the width
     * @param height the height
     */
    void resize(float width, float height);

    /**
     * Sets how long the pointer must rest on a component before its tooltip shows, in place of the theme's. Until
     * this is called, the host uses the theme's
     * {@value org.inspirenxe.strata.api.component.Tooltip#STYLE_DELAY} metric, or
     * {@link org.inspirenxe.strata.api.component.Tooltip#DEFAULT_DELAY_MILLIS} when the theme has none. A waiting
     * tooltip shows on the first {@link #render} after the delay has passed on the platform's clock.
     *
     * @param millis the delay in milliseconds; 0 shows tooltips as soon as the pointer arrives
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    void setTooltipDelay(long millis);

    /**
     * Builds a pending screen and brings bound components up to date without laying out or drawing: every binding
     * whose states changed since the last frame, from any thread, sets its component, and bindings those changes
     * mark dirty run too, until none is left. {@link #layout} and {@link #render} do this first; tests call it to see
     * a state change reach a component. A cycle of bindings that never settles is cut off after a fixed number of
     * rounds, with an error logged through {@link System.Logger}, and its remaining updates are dropped.
     */
    void flush();

    /**
     * Builds a pending screen, {@linkplain #flush() flushes} bindings and runs layout now if anything changed.
     * {@link #render} does this itself; call it to read bounds before the first frame.
     */
    void layout();

    /**
     * Draws a frame: rebuilds the screen if it asked to be, {@linkplain #flush() flushes} bindings, synthesizes due
     * key repeats, runs layout if needed, paints the tree and requests the cursor.
     *
     * @param canvas the engine canvas for this frame
     */
    void render(Canvas canvas);

    /**
     * Returns the component with keyboard focus.
     *
     * @return the focused component, or empty
     */
    Optional<Component<?>> focused();

    /**
     * Returns the deepest component under the pointer, as of the last pointer input.
     *
     * @return the hovered component, or empty
     */
    Optional<Component<?>> hovered();
}
