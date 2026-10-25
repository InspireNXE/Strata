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
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
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
 *     <li>Call {@link #setPixelsPerUnit} when the engine knows its density and whenever it changes, such as when
 *     the window moves to another display or the user zooms, together with {@link #resize}, since the area in UI
 *     units changes with it.</li>
 *     <li>Call {@link #render} once per frame. It builds pending screens, runs layout if needed, draws, and asks for
 *     the cursor. An adapter whose engine does not draw every frame may render on demand instead: whenever
 *     {@link #needsFrame()} is true, which the {@linkplain #setFrameRequester frame requester} announces, and
 *     whenever the platform's clock reaches {@link #nextFrameNanos()}. Either way it renders no sooner than
 *     {@link #frameIntervalNanos()} after its last render began, so the host is drawn at most
 *     {@link #frameRate()} times a second.</li>
 *     <li>Call {@link #close()} when the engine screen goes away, so the Strata screen's {@code closed()} runs.</li>
 *     <li>When the user asks to close the engine screen, with its close button or a key the engine closes screens
 *     with, call {@link #requestClose()} first and close the engine screen only if it returns true.</li>
 *     <li>If the engine shows a title, show {@link #title()}, and compare it with the one shown on each frame, since
 *     the screen can change it.</li>
 *     <li>Call everything on the UI thread.</li>
 * </ul>
 */
public interface ScreenHost extends InputSink {
    /**
     * The frame rate of a host, and of a stack, until {@link #setFrameRate} changes it.
     */
    int DEFAULT_FRAME_RATE = 30;

    /**
     * The lowest frame rate {@link #setFrameRate} accepts.
     */
    int MIN_FRAME_RATE = 1;

    /**
     * The highest frame rate {@link #setFrameRate} accepts.
     */
    int MAX_FRAME_RATE = 1000;

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
     * Closes the current screen: calls its {@code closed()} and detaches its tree. Does nothing if none is open. The
     * screen is not asked first; see {@link #requestClose()}.
     */
    void close();

    /**
     * Asks the open screen whether it may close, for the engine's or the window's own close button and keys. The
     * host calls the screen's {@link Screen#closeRequested} with its handle and, if it agrees, closes it as
     * {@link #close()} does. The runnable given to {@code StrataCore.createScreenHost} does not run: the adapter asked,
     * so it closes its own screen when this returns true.
     *
     * @return true if no screen is open or the open screen agreed and was closed; false if it refused and stays open
     */
    boolean requestClose();

    /**
     * Returns the open screen's title as of now: its {@link Screen#title()} as read when it opened, or the last
     * title it set with {@link ScreenHandle#setTitle} since. The text is returned as it is, so an adapter resolves it
     * again when translations change.
     *
     * @return the title, or {@link Text#empty()} if no screen is open
     */
    Text title();

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
     * Replaces the theme. Components re-read their style keys and a new layout runs. The host stops following a ref
     * set before with {@link #setTheme(ThemeRef)}: {@link #themeRef()} becomes empty, and a reload of the theme
     * source no longer changes the theme.
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
     * Sets how many device pixels one UI unit covers on the display, which components read from
     * {@link org.inspirenxe.strata.api.extension.Environment#pixelsPerUnit()} and painters from
     * {@link org.inspirenxe.strata.api.extension.Painter#pixelsPerUnit()}. It is 1 until set. A change lays every
     * component out again, as a change of language does; the size in UI units is set apart, with {@link #resize}.
     * A game adapter passes its GUI scale times the window's content scale; a desktop adapter its zoom times the
     * display's scale.
     *
     * @param pixelsPerUnit device pixels per UI unit
     * @throws IllegalArgumentException if the value is not finite and greater than 0
     */
    void setPixelsPerUnit(float pixelsPerUnit);

    /**
     * Returns the size the open screen's content wants in a given space, for a toolkit that asks an embedded widget
     * for its preferred size, such as Swing's {@code pack()}. The host builds a pending screen and flushes bindings,
     * then measures the content with the space offered, as a parent measures a child, without arranging it or
     * changing its bounds; the next layout measures it again at the host's own size. Overlays and the tooltip are
     * left out, since they place themselves in whatever size the host gets.
     *
     * @param availableWidth the width offered, in UI units; {@link Float#POSITIVE_INFINITY} for no limit
     * @param availableHeight the height offered, in UI units; {@link Float#POSITIVE_INFINITY} for no limit
     * @return the content's desired size, margin included, or {@link Size#ZERO} if no screen is open
     * @throws IllegalArgumentException if either value is negative or NaN
     */
    Size preferredSize(float availableWidth, float availableHeight);

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
     * Returns whether something changed since the last {@link #render}, so the next frame would draw something
     * different: input this host dispatched, a layout invalidation, a binding queued or a task handed to the UI
     * thread from any thread, an overlay opening or closing, a change of look a component reported, or a screen
     * opening or closing. A new host needs a frame. Any thread may call this.
     *
     * @return true if a frame is needed
     */
    boolean needsFrame();

    /**
     * Returns when the next timed change is due, such as a caret's next blink, a tooltip's delay running out, a
     * toast's expiry, the next synthesized key repeat, a caption or progress bar that moves with the clock, or an
     * animation's next step. An adapter that renders on demand renders when the platform's clock reaches it, even if
     * {@link #needsFrame()} is false. Call it on the UI thread, after a render.
     *
     * @return the {@link Clock#nanoTime()} reading at which a frame is due, possibly already past, or
     *     {@link Long#MAX_VALUE} if nothing timed is waiting
     */
    long nextFrameNanos();

    /**
     * Returns whether a render at a time would draw something other than what the last {@link #render} drew: when
     * {@link #needsFrame()} is true, when something that moves with the clock, such as a running animation, asked for
     * the next frame, when a timed change {@link #nextFrameNanos()} counts is due by then, or when the theme source,
     * the theme registry, the translations or the platform's text metrics changed. Unlike {@link #nextFrameNanos()},
     * it does not hold motion to the {@linkplain #frameRate() frame rate}. An engine that draws every frame anyway may
     * show what it drew for the last render again in place of rendering while this is false, as long as nothing of
     * its own that the host drew with changed, such as its sprites or its fonts. Call it on the UI thread, after a
     * render. It allocates nothing.
     *
     * @param nanoTime the platform clock's {@link Clock#nanoTime()} reading for the frame
     * @return true if a render is needed to draw the frame
     */
    boolean needsRender(long nanoTime);

    /**
     * Sets what runs when {@link #needsFrame()} turns from false to true, and runs it at once if a frame is already
     * needed. It runs once per change from clean to dirty, not again before the next {@link #render}. It can run on
     * any thread, such as the thread that set a state, so it must only schedule a frame, for example by asking the
     * engine to repaint, and must not call into the host. An adapter that renders every frame anyway can leave it
     * unset.
     *
     * @param requester the requester, or {@code null} for none
     */
    void setFrameRequester(Runnable requester);

    /**
     * Sets how many frames a second the host asks for at most. {@link #nextFrameNanos()} is never sooner than one
     * interval after the last {@link #render} began, and things that move with the clock, such as a running
     * animation or a sliding caption, ask for the next frame one interval ahead. Every motion reads the clock when it
     * draws, so a lower rate shows the same motion in fewer steps. An adapter that renders on demand holds frames that
     * {@link #needsFrame()} asks for to the same rate. An engine that draws every frame anyway is not held to it.
     * It is {@link #DEFAULT_FRAME_RATE} until set. A change asks for a frame. Any thread may call this.
     *
     * @param framesPerSecond frames a second, from {@link #MIN_FRAME_RATE} to {@link #MAX_FRAME_RATE}
     * @throws IllegalArgumentException if the rate is out of that range
     */
    void setFrameRate(int framesPerSecond);

    /**
     * Returns how many frames a second the host asks for at most.
     *
     * @return the rate {@link #setFrameRate} set, or {@link #DEFAULT_FRAME_RATE}
     */
    int frameRate();

    /**
     * Returns the time between two frames at the {@linkplain #frameRate() frame rate}: one second divided by the rate,
     * rounded to the nearest nanosecond. An adapter that renders on demand renders no sooner than this after its last
     * render began.
     *
     * @return the interval in nanoseconds of the {@link Clock}
     */
    long frameIntervalNanos();

    /**
     * Returns the component with keyboard focus.
     *
     * @return the focused component, or empty
     */
    Optional<Component<?>> focused();

    /**
     * Returns the deepest visible component under the pointer, as of the last pointer input. It may be disabled, or
     * inside a disabled component, and then hears no pointer events.
     *
     * @return the hovered component, or empty
     */
    Optional<Component<?>> hovered();
}
