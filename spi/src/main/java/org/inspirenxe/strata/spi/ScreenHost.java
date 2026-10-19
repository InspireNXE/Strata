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
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;

import java.util.Optional;

/**
 * Shows one {@link Screen} at a time. Implemented by Strata and created with {@code StrataCore.createScreenHost};
 * adapters drive it. Engines that keep their own stack of screens use one host per engine screen.
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
     * @throws IllegalArgumentException if the screen's {@code build} returns a component it may not use
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
     * Builds a pending screen and runs layout now if anything changed. {@link #render} does this itself; call it to
     * read bounds before the first frame.
     */
    void layout();

    /**
     * Draws a frame: rebuilds the screen if it asked to be, synthesizes due key repeats, runs layout if needed,
     * paints the tree and requests the cursor.
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
