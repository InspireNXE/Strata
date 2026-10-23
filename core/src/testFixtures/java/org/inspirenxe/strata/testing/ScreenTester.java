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
package org.inspirenxe.strata.testing;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.input.ClickCounter;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.ContainerNode;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.node.Root;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;

import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Opens a {@link Screen} for a test, without an engine or a display, and drives it as a person would.
 *
 * <pre>{@code
 * try (ScreenTester tester = ScreenTester.open(new ShopScreen(shop))) {
 *     tester.click(tester.byId("quantity", TextBox.class)).type("3")
 *           .click(tester.find(Button.class, Matching.text("Buy")));
 *     assertEquals("29.97", tester.byId("total", Label.class).text().plain());
 * }
 * }</pre>
 *
 * <h2>The platform</h2>
 * <p>Text is measured as if monospaced: {@value #CHAR_WIDTH} units wide per code point, {@value #MONO_CHAR_WIDTH} in a
 * monospaced style, and {@value #LINE_HEIGHT} units per line. The clock stands still until {@link #advance} moves it,
 * so delays such as a tooltip's never pass on their own and animations move only as it does ({@link #settle()} runs
 * them to their end), and the wall clock always reads
 * {@link TesterOptions#now()}. The clipboard is held in memory, and the engine sends its own key repeats, so a key
 * repeats only when the test presses it again.</p>
 *
 * <h2>Input</h2>
 * <p>Input goes through the {@link org.inspirenxe.strata.spi.InputSink} an engine adapter uses, with no modifiers held
 * unless {@link #press} names some, so every event is the user's and controls fire their requests as they would for
 * a person. The pointer acts at the center of its target. An action that a person could not perform fails with an
 * {@link AssertionError} that says why: its target is not on this screen, is hidden or inside a hidden component,
 * is disabled or inside a disabled one (except for {@link #scroll}, which a disabled component passes on), has no
 * size, or has its center clipped out of view or covered by something else, such as an overlay. Each action lays
 * out first, so bounds are current, and runs a {@link #frame()} after.</p>
 *
 * <h2>Finding components</h2>
 * <p>{@link #find} and {@link #findAll} walk the screen's tree and then each open overlay's, in tree order, parents
 * before children, the private parts of controls included; the tooltip is left out. Hidden and disabled components
 * are found too, so a test can check that they are.</p>
 *
 * <p>Translated text reads through {@link TesterOptions#translator()}, which the tester sets for the whole process
 * while it is open and puts back when it closes, so testers that run at the same time must use the same one. Use a
 * tester on one thread only.</p>
 */
public final class ScreenTester implements AutoCloseable {
    /**
     * How wide the tester measures a code point, in UI units.
     */
    public static final float CHAR_WIDTH = 6f;
    /**
     * How wide the tester measures a code point in a monospaced style, in UI units.
     */
    public static final float MONO_CHAR_WIDTH = 7f;
    /**
     * How tall the tester measures a line of text, in UI units.
     */
    public static final float LINE_HEIGHT = 9f;

    /**
     * How far apart {@link #settle()} runs its frames, in milliseconds: an animation's frame (design 4.47).
     */
    public static final long SETTLE_STEP_MILLIS = 16L;
    /**
     * How long {@link #settle()} lets animations run before it fails, in milliseconds.
     */
    public static final long SETTLE_LIMIT_MILLIS = 60_000L;

    private final TesterPlatform platform;
    private final ScreenHostImpl host;
    private final TextRecorder canvas;
    private final Translator previousTranslator;
    private final float width;
    private final float height;
    private boolean closed;
    // The last press, so a click right after it on the same spot is not read as a double click
    private MouseButton lastButton;
    private long lastPressMillis;
    private float lastPressX;
    private float lastPressY;

    private ScreenTester(final Screen screen, final TesterOptions options) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(options, "options");
        this.platform = new TesterPlatform(Clock.fixed(options.now().toInstant(), options.now().getZone()));
        this.canvas = new TextRecorder();
        this.width = options.width();
        this.height = options.height();
        this.previousTranslator = Translations.translator();
        StrataCore.setTranslator(options.translator());
        ScreenHostImpl opened = null;
        try {
            opened = (ScreenHostImpl) StrataCore.createScreenHost(this.platform, options.theme());
            opened.resize(options.width(), options.height());
            opened.open(screen);
        } catch (final RuntimeException e) {
            try {
                if (opened != null) {
                    opened.close();
                }
            } finally {
                StrataCore.setTranslator(this.previousTranslator);
            }
            throw e;
        }
        this.host = opened;
        this.frame();
    }

    /**
     * Opens a screen with the {@linkplain TesterOptions#DEFAULT default options}: 800 by 600 units in
     * {@link org.inspirenxe.strata.api.theme.Themes#flat()}, and renders a first frame.
     *
     * @param screen the screen
     * @return the tester, which the test closes when done
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the screen's tree needs
     * @throws IllegalArgumentException if the screen's {@code build} returns a component already in a tree or made
     *     by another factory
     */
    public static ScreenTester open(final Screen screen) {
        return new ScreenTester(screen, TesterOptions.DEFAULT);
    }

    /**
     * Opens a screen with the given size, theme, translator and wall clock, and renders a first frame.
     *
     * @param screen the screen
     * @param options how to show it
     * @return the tester, which the test closes when done
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the theme lacks keys the screen's tree needs
     * @throws IllegalArgumentException if the screen's {@code build} returns a component already in a tree or made
     *     by another factory
     */
    public static ScreenTester open(final Screen screen, final TesterOptions options) {
        return new ScreenTester(screen, options);
    }

    /**
     * Returns the factory of the host showing the screen, the one its {@code build} got, for making components a
     * test adds itself.
     *
     * @return the factory
     */
    public ComponentFactory factory() {
        return this.host.factory();
    }

    // ------------------------------------------------------------------
    // Finding
    // ------------------------------------------------------------------

    /**
     * Finds the one component of a type that matches a condition.
     *
     * @param <T> the type
     * @param type the type, usually a control interface such as {@code Button.class}
     * @param match the condition, such as {@link Matching#text}
     * @return the component
     * @throws AssertionError if no component or more than one matches; the message lists what was found
     */
    public <T> T find(final Class<T> type, final Predicate<? super T> match) {
        final List<T> found = this.findAll(type, match);
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new AssertionError("No " + type.getSimpleName() + " matches; the screen shows "
                    + ScreenTester.describe(this.findAll(type, any -> true)));
        }
        throw new AssertionError(found.size() + " components match where one should: " + ScreenTester.describe(found));
    }

    /**
     * Finds every component of a type that matches a condition.
     *
     * @param <T> the type
     * @param type the type
     * @param match the condition
     * @return the matches in tree order, possibly none
     */
    public <T> List<T> findAll(final Class<T> type, final Predicate<? super T> match) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(match, "match");
        this.requireOpen();
        final List<T> found = new ArrayList<>();
        for (final Node top : this.tops()) {
            ScreenTester.collect(top, type, match, found);
        }
        return found;
    }

    /**
     * Finds the one component of a type with the given {@linkplain Component#id() id}.
     *
     * @param <T> the type
     * @param id the id
     * @param type the type
     * @return the component
     * @throws AssertionError if no component of the type has the id, or more than one has
     */
    public <T extends Component<?>> T byId(final String id, final Class<T> type) {
        Objects.requireNonNull(id, "id");
        final List<T> found = this.findAll(type, component -> component.id().filter(id::equals).isPresent());
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new AssertionError("No " + type.getSimpleName() + " has the id \"" + id + "\"; the screen shows "
                    + ScreenTester.describe(this.findAll(type, any -> true)));
        }
        throw new AssertionError(found.size() + " components have the id \"" + id + "\": " + ScreenTester.describe(found));
    }

    private List<Node> tops() {
        final Root root = this.host.root();
        final List<Node> tops = new ArrayList<>();
        if (root.content() != null) {
            tops.add(root.content());
        }
        for (final Overlay overlay : root.overlays().open()) {
            tops.add(this.nodeOf(overlay.content()));
        }
        return tops;
    }

    private static <T> void collect(final Node node, final Class<T> type, final Predicate<? super T> match,
                                    final List<T> found) {
        final Component<?> component = node.facade();
        if (type.isInstance(component) && match.test(type.cast(component))) {
            found.add(type.cast(component));
        }
        if (node instanceof ContainerNode container) {
            for (final Node child : container.childNodes()) {
                ScreenTester.collect(child, type, match, found);
            }
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    /**
     * Clicks a component with the left button: moves the pointer to its center, then presses and releases. A click
     * that would follow another press on the same spot quickly enough to count as a double click first moves the
     * clock past the double-click time, so two calls are two single clicks; use {@link #doubleClick} for a double
     * click.
     *
     * @param target the component
     * @return this tester
     * @throws AssertionError if a person could not click the component; see the class description
     */
    public ScreenTester click(final Component<?> target) {
        return this.clickWith(target, MouseButton.LEFT, 1);
    }

    /**
     * Double-clicks a component with the left button: two presses and releases at its center with no time between
     * them.
     *
     * @param target the component
     * @return this tester
     * @throws AssertionError if a person could not click the component
     */
    public ScreenTester doubleClick(final Component<?> target) {
        return this.clickWith(target, MouseButton.LEFT, 2);
    }

    /**
     * Clicks a component with the right button, as {@link #click} does with the left.
     *
     * @param target the component
     * @return this tester
     * @throws AssertionError if a person could not click the component
     */
    public ScreenTester rightClick(final Component<?> target) {
        return this.clickWith(target, MouseButton.RIGHT, 1);
    }

    private ScreenTester clickWith(final Component<?> target, final MouseButton button, final int clicks) {
        final float[] at = this.reachable(target, "click", true);
        final float x = at[0];
        final float y = at[1];
        this.separate(button, x, y);
        this.host.pointerMoved(x, y, Modifiers.NONE);
        for (int i = 0; i < clicks; i++) {
            this.host.pointerButton(x, y, button, true, Modifiers.NONE);
            this.host.pointerButton(x, y, button, false, Modifiers.NONE);
        }
        this.pressed(button, x, y);
        return this.frame();
    }

    /**
     * Drags a component with the left button: presses at its center, moves halfway and then the whole way, and
     * releases there. A move shorter than the drag threshold, four units on both axes, is a press and release
     * that starts no drag.
     *
     * @param target the component to press
     * @param dx how far to move right, in UI units; negative moves left
     * @param dy how far to move down, in UI units; negative moves up
     * @return this tester
     * @throws AssertionError if a person could not press the component
     */
    public ScreenTester drag(final Component<?> target, final float dx, final float dy) {
        final float[] at = this.reachable(target, "drag", true);
        final float x = at[0];
        final float y = at[1];
        this.separate(MouseButton.LEFT, x, y);
        this.host.pointerMoved(x, y, Modifiers.NONE);
        this.host.pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
        this.pressed(MouseButton.LEFT, x, y);
        this.host.pointerMoved(x + dx * 0.5f, y + dy * 0.5f, Modifiers.NONE);
        this.host.pointerMoved(x + dx, y + dy, Modifiers.NONE);
        this.host.pointerButton(x + dx, y + dy, MouseButton.LEFT, false, Modifiers.NONE);
        return this.frame();
    }

    /**
     * Drags one component onto another with the left button: presses at the source's center, moves past the drag
     * threshold, then to the target's center, and releases there. A source that starts a transfer, such as a list
     * whose rows drag or a text box pressed inside its selection, drops it on the target, as for a person.
     *
     * @param source the component to press
     * @param target the component to release over
     * @return this tester
     * @throws AssertionError if a person could not press the source or point at the target
     */
    public ScreenTester dragTo(final Component<?> source, final Component<?> target) {
        final float[] from = this.reachable(source, "drag", true);
        final float[] to = this.reachable(target, "drop on", false);
        this.separate(MouseButton.LEFT, from[0], from[1]);
        this.host.pointerMoved(from[0], from[1], Modifiers.NONE);
        this.host.pointerButton(from[0], from[1], MouseButton.LEFT, true, Modifiers.NONE);
        this.pressed(MouseButton.LEFT, from[0], from[1]);
        final float step = Root.DRAG_THRESHOLD + 1f;
        final float dx = to[0] - from[0];
        final float dy = to[1] - from[1];
        final float length = (float) Math.hypot(dx, dy);
        if (length > step) {
            this.host.pointerMoved(from[0] + dx / length * step, from[1] + dy / length * step, Modifiers.NONE);
        } else {
            this.host.pointerMoved(from[0] + step, from[1], Modifiers.NONE);
        }
        this.host.pointerMoved(to[0], to[1], Modifiers.NONE);
        this.host.pointerButton(to[0], to[1], MouseButton.LEFT, false, Modifiers.NONE);
        return this.frame();
    }

    /**
     * Drops a transfer from another application on a component, as files dragged from the desktop: the drag enters
     * at the component's center, allowing every effect, and drops there.
     *
     * @param target the component to drop on
     * @param transfer what is dropped
     * @return the effect the component under the point took, or empty when nothing took it
     * @throws AssertionError if a person could not point at the component
     */
    public Optional<DropEffect> drop(final Component<?> target, final Transfer transfer) {
        Objects.requireNonNull(transfer, "transfer");
        final float[] at = this.reachable(target, "drop on", false);
        this.host.dragEntered(at[0], at[1], transfer, EnumSet.allOf(DropEffect.class), Modifiers.NONE);
        final Optional<DropEffect> taken = this.host.dropped(at[0], at[1], transfer, Modifiers.NONE);
        this.frame();
        return taken;
    }

    /**
     * Turns the wheel over a component: moves the pointer to its center and scrolls there. A disabled component
     * passes the scroll on to its nearest enabled ancestor, as it would for a person.
     *
     * @param target the component to scroll over
     * @param dy how far, in wheel notches; positive turns the wheel away from the user, toward the top of the
     *     content, and negative toward its end
     * @return this tester
     * @throws AssertionError if a person could not point at the component
     */
    public ScreenTester scroll(final Component<?> target, final float dy) {
        final float[] at = this.reachable(target, "scroll", false);
        this.host.pointerMoved(at[0], at[1], Modifiers.NONE);
        this.host.scroll(at[0], at[1], 0f, dy, ScrollKind.WHEEL, Modifiers.NONE);
        return this.frame();
    }

    /**
     * Types text: one typed character for each code point, in order, to whatever has focus. Click or focus a field
     * first.
     *
     * @param text the text
     * @return this tester
     */
    public ScreenTester type(final String text) {
        Objects.requireNonNull(text, "text");
        this.requireOpen();
        this.host.layout();
        text.codePoints().forEach(codePoint -> this.host.charTyped(codePoint, Modifiers.NONE));
        return this.frame();
    }

    /**
     * Presses and releases a key with modifiers held, such as {@code press(Key.A, Modifier.CONTROL)}.
     *
     * @param key the key
     * @param modifiers the modifiers held while it is pressed
     * @return this tester
     */
    public ScreenTester press(final Key key, final Modifier... modifiers) {
        Objects.requireNonNull(key, "key");
        this.requireOpen();
        final Modifiers held = Modifiers.of(modifiers);
        this.host.layout();
        this.host.key(key, KeyAction.PRESS, held);
        this.host.key(key, KeyAction.RELEASE, held);
        return this.frame();
    }

    /**
     * Moves the clock forward and runs a {@link #frame()}, so waiting tooltips show and toasts whose time ran out
     * close.
     *
     * @param millis how far, in milliseconds
     * @return this tester
     * @throws IllegalArgumentException if {@code millis} is negative
     */
    public ScreenTester advance(final long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("The clock cannot go back: " + millis);
        }
        this.requireOpen();
        this.platform.advance(millis);
        return this.frame();
    }

    /**
     * Runs frames {@value #SETTLE_STEP_MILLIS} milliseconds apart until no animation of the screen runs, so motion a
     * theme turns on, a toast's fade or the screen's own animations come to rest (design 4.51). With nothing animating
     * it runs no frame and leaves the clock alone.
     *
     * @return this tester
     * @throws AssertionError if animations still run after {@value #SETTLE_LIMIT_MILLIS} milliseconds, as when one
     *     keeps starting another
     */
    public ScreenTester settle() {
        this.requireOpen();
        final Root root = this.host.root();
        for (long waited = 0L; root.animator().isAnimating(); waited += SETTLE_STEP_MILLIS) {
            if (waited >= SETTLE_LIMIT_MILLIS) {
                throw new AssertionError("Animations were still running after " + SETTLE_LIMIT_MILLIS + " ms");
            }
            this.advance(SETTLE_STEP_MILLIS);
        }
        return this;
    }

    /**
     * Runs a frame as an engine does: rebuilds the screen if it asked to be, brings bound components up to date,
     * lays out and renders, which records the text {@link #drawnText()} returns.
     *
     * @return this tester
     */
    public ScreenTester frame() {
        this.requireOpen();
        this.canvas.clear();
        this.host.render(this.canvas);
        return this;
    }

    /**
     * Moves the clock past the double-click time if a press of this button on this spot would continue a run.
     */
    private void separate(final MouseButton button, final float x, final float y) {
        if (button == this.lastButton
                && this.platform.clock().millis() - this.lastPressMillis <= ClickCounter.MULTI_CLICK_MILLIS
                && Math.abs(x - this.lastPressX) <= ClickCounter.MULTI_CLICK_DISTANCE
                && Math.abs(y - this.lastPressY) <= ClickCounter.MULTI_CLICK_DISTANCE) {
            this.platform.advance(ClickCounter.MULTI_CLICK_MILLIS + 1L);
        }
    }

    private void pressed(final MouseButton button, final float x, final float y) {
        this.lastButton = button;
        this.lastPressMillis = this.platform.clock().millis();
        this.lastPressX = x;
        this.lastPressY = y;
    }

    /**
     * Lays out and returns the center of a component a person could act on there, or fails saying why not.
     */
    private float[] reachable(final Component<?> target, final String action, final boolean enabled) {
        Objects.requireNonNull(target, "target");
        this.requireOpen();
        this.host.layout();
        final Root root = this.host.root();
        final Node node;
        try {
            node = this.nodeOf(target);
        } catch (final IllegalArgumentException e) {
            throw ScreenTester.cannot(action, target, "is not on this screen");
        }
        if (node.root() != root) {
            throw ScreenTester.cannot(action, target, "is not on this screen");
        }
        for (Component<?> component = target; component != null; component = component.parent().orElse(null)) {
            if (!component.isVisible()) {
                throw ScreenTester.cannot(action, target, component == target ? "is hidden"
                        : "is inside hidden " + ScreenTester.describe(component));
            }
        }
        if (enabled) {
            for (Component<?> component = target; component != null; component = component.parent().orElse(null)) {
                if (!component.isEnabled()) {
                    throw ScreenTester.cannot(action, target, component == target ? "is disabled"
                            : "is inside disabled " + ScreenTester.describe(component));
                }
            }
        }
        final Rect bounds = node.bounds();
        if (bounds.isEmpty()) {
            throw ScreenTester.cannot(action, target, "has no size");
        }
        final float x = bounds.x() + bounds.width() * 0.5f;
        final float y = bounds.y() + bounds.height() * 0.5f;
        final Node hit = root.hitTest(x, y);
        if (hit != null && ScreenTester.isWithin(hit, node)) {
            return new float[] {x, y};
        }
        final String center = "has its center " + ScreenTester.point(x, y);
        if (!(x >= 0f && y >= 0f && x < this.width && y < this.height)) {
            throw ScreenTester.cannot(action, target, center + " outside the screen");
        }
        if (hit != null && ScreenTester.isWithin(node, hit)) {
            throw ScreenTester.cannot(action, target, center + " clipped out of view by "
                    + ScreenTester.describe(hit.facade()));
        }
        final ContainerNode clipper = ScreenTester.clipper(node, x, y);
        if (clipper != null) {
            throw ScreenTester.cannot(action, target, center + " clipped out of view by "
                    + ScreenTester.describe(clipper.facade()));
        }
        throw ScreenTester.cannot(action, target, hit != null ? center + " covered by "
                + ScreenTester.describe(hit.facade()) : center + " under a dialog's backdrop");
    }

    /**
     * Returns the nearest ancestor that clips its children and does not reach the point.
     */
    private static ContainerNode clipper(final Node node, final float x, final float y) {
        for (ContainerNode ancestor = node.parentNode(); ancestor != null; ancestor = ancestor.parentNode()) {
            if (ancestor.clipsChildren() && !ancestor.bounds().contains(x, y)) {
                return ancestor;
            }
        }
        return null;
    }

    private static boolean isWithin(final Node node, final Node ancestor) {
        for (Node candidate = node; candidate != null; candidate = candidate.parentNode()) {
            if (candidate == ancestor) {
                return true;
            }
        }
        return false;
    }

    private Node nodeOf(final Component<?> component) {
        return ((ComponentFactoryImpl) this.host.factory()).nodeOf(component);
    }

    private static AssertionError cannot(final String action, final Component<?> target, final String why) {
        return new AssertionError("Cannot " + action + " " + ScreenTester.describe(target) + ": it " + why);
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /**
     * Returns the component with keyboard focus.
     *
     * @return the focused component, or empty
     */
    public Optional<Component<?>> focused() {
        this.requireOpen();
        return this.host.focused();
    }

    /**
     * Returns the open overlays: popups, dialogs, menus and toasts, bottom first.
     *
     * @return a copy, possibly empty
     */
    public List<Overlay> overlays() {
        this.requireOpen();
        return this.host.factory().overlays().open();
    }

    /**
     * Returns the text the last frame drew, one entry for each run of text drawn, in drawing order: the screen's
     * tree, then each overlay, then the tooltip. Opening the tester and every action run a frame, so this is current
     * after each.
     *
     * @return a copy, possibly empty
     */
    public List<String> drawnText() {
        this.requireOpen();
        return this.canvas.text();
    }

    /**
     * Closes the screen, so its {@code closed()} runs and its tree detaches, and puts back the translator the
     * process had before the tester opened. Does nothing the second time.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        try {
            this.host.close();
        } finally {
            StrataCore.setTranslator(this.previousTranslator);
        }
    }

    private void requireOpen() {
        if (this.closed) {
            throw new IllegalStateException("The tester is closed");
        }
    }

    // ------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------

    private static String describe(final List<?> components) {
        if (components.isEmpty()) {
            return "none of that type";
        }
        final StringBuilder out = new StringBuilder();
        for (final Object component : components) {
            out.append(out.isEmpty() ? "" : ", ").append(ScreenTester.describe((Component<?>) component));
        }
        return out.toString();
    }

    private static String describe(final Component<?> component) {
        String name = component.getClass().getSimpleName();
        if (name.endsWith("Impl")) {
            name = name.substring(0, name.length() - "Impl".length());
        }
        if (name.isEmpty()) {
            name = "component";
        }
        final StringBuilder out = new StringBuilder(name);
        component.id().ifPresent(id -> out.append(" #").append(id));
        if (component instanceof final HasText<?> text) {
            out.append(" \"").append(text.text().plain()).append('"');
        }
        final Rect bounds = component.bounds();
        out.append(" at ").append(ScreenTester.point(bounds.x(), bounds.y())).append(' ')
                .append(ScreenTester.number(bounds.width())).append('x').append(ScreenTester.number(bounds.height()));
        return out.toString();
    }

    private static String point(final float x, final float y) {
        return ScreenTester.number(x) + "," + ScreenTester.number(y);
    }

    private static String number(final float value) {
        return value == Math.rint(value) && Math.abs(value) < 1e9f ? Integer.toString((int) value)
                : String.format(Locale.ROOT, "%.2f", value);
    }
}
