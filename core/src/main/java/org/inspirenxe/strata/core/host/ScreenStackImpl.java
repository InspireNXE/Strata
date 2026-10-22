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
package org.inspirenxe.strata.core.host;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.node.Root;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link ScreenStack}. It keeps one {@link ScreenHostImpl} per stacked screen, so each screen has its own
 * tree, focus and factory. A screen that asks to close through its handle is removed from the stack, wherever it
 * is in it.
 *
 * <p>Pushed screens take the stack's theme and follow {@link #setTheme}. Screens opened with
 * {@link #open(Screen, ThemeRef)}, or from a screen's handle, keep the theme they were opened in. A screen opened
 * from a handle goes straight above the screen whose handle it was, so closing it shows that screen again.</p>
 *
 * <p>Only the top screen gets input. A screen that another covers hears that the pointer left, so it drops its hover,
 * tooltip and cursor, and the engine's text input stops if its focused field had it; when it is on top again, text
 * input starts again for that field. The platform's text input is shared by every screen, so only the top one may
 * hold it.</p>
 */
public final class ScreenStackImpl implements ScreenStack {
    private final Platform platform;
    private final List<Entry> entries;
    private Theme theme;
    private ThemeRef themeRef;
    private float width;
    private float height;
    // Negative until set, so each screen follows its theme
    private long tooltipDelay = -1L;
    private float pixelsPerUnit = 1f;

    /**
     * Creates an empty stack of size 0 by 0. Adapters use {@code StrataCore} instead.
     *
     * @param platform the engine's platform, shared by every screen's host
     * @param theme the theme for every screen pushed
     */
    public ScreenStackImpl(final Platform platform, final Theme theme) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.entries = new ArrayList<>();
    }

    /**
     * Creates an empty stack of size 0 by 0 whose screens resolve their theme from a ref. Adapters use
     * {@code StrataCore} instead.
     *
     * @param platform the engine's platform, shared by every screen's host
     * @param theme the theme ref for every screen pushed
     * @throws org.inspirenxe.strata.api.theme.ThemeException if the ref cannot be resolved
     */
    public ScreenStackImpl(final Platform platform, final ThemeRef theme) {
        this(platform, new ThemeResolver(platform.themes()).resolve(theme));
        this.themeRef = theme;
    }

    @Override
    public void push(final Screen screen) {
        this.insert(this.entries.size(), screen, null);
    }

    @Override
    public ScreenHandle open(final Screen screen, final ThemeRef theme) {
        return this.insert(this.entries.size(), screen, Objects.requireNonNull(theme, "theme"));
    }

    /**
     * Opens a screen at a place in the stack.
     *
     * @param index where the screen goes, counted from the bottom
     * @param screen the screen
     * @param own the theme it keeps, or {@code null} for the stack's
     * @return the screen's handle
     */
    private ScreenHandle insert(final int index, final Screen screen, final ThemeRef own) {
        Objects.requireNonNull(screen, "screen");
        final ScreenHostImpl[] self = new ScreenHostImpl[1];
        final Runnable close = () -> this.remove(self[0]);
        final ScreenHostImpl host;
        if (own != null) {
            host = new ScreenHostImpl(this.platform, own, close);
        } else if (this.themeRef != null) {
            host = new ScreenHostImpl(this.platform, this.themeRef, close);
        } else {
            host = new ScreenHostImpl(this.platform, this.theme, close);
        }
        self[0] = host;
        host.setOpener(new Above(host));
        host.resize(this.width, this.height);
        host.setPixelsPerUnit(this.pixelsPerUnit);
        if (this.tooltipDelay >= 0L) {
            host.setTooltipDelay(this.tooltipDelay);
        }
        final Entry entry = new Entry(host, own != null);
        final ScreenHostImpl covered = this.topHost();
        // In the stack before it opens, so a screen can open another above itself from opened()
        this.entries.add(index, entry);
        if (this.topHost() == host) {
            // Before the new screen opens, so a text field it focuses keeps the engine's text input
            this.covered(covered);
        }
        try {
            return host.openScreen(screen);
        } catch (final RuntimeException e) {
            this.entries.remove(entry);
            try {
                // closed() for a screen whose opened() threw, and the host lets go of its tree
                host.close();
            } catch (final RuntimeException suppressed) {
                e.addSuppressed(suppressed);
            }
            this.uncovered(covered);
            throw e;
        }
    }

    @Override
    public Optional<Screen> pop() {
        if (this.entries.isEmpty()) {
            return Optional.empty();
        }
        final ScreenHostImpl host = this.entries.removeLast().host();
        final Optional<Screen> screen = host.screen();
        try {
            host.close();
        } finally {
            this.uncovered(this.topHost());
        }
        return screen;
    }

    private void remove(final ScreenHostImpl host) {
        final boolean wasTop = this.topHost() == host;
        if (this.entries.removeIf(entry -> entry.host() == host)) {
            try {
                host.close();
            } finally {
                if (wasTop) {
                    this.uncovered(this.topHost());
                }
            }
        }
    }

    private ScreenHostImpl topHost() {
        return this.entries.isEmpty() ? null : this.entries.getLast().host();
    }

    /**
     * Tells a host that another screen now covers it and takes the input: the pointer has left it, so it drops its
     * hover, tooltip and cursor, and the engine's text input stops if its focused field had it.
     */
    private void covered(final ScreenHostImpl host) {
        if (host != null) {
            host.pointerLeft();
            host.root().suspendTextInput();
        }
    }

    /**
     * Tells a host that it is on top again: the engine's text input starts again if its focused field takes text.
     */
    private void uncovered(final ScreenHostImpl host) {
        if (host != null && this.topHost() == host) {
            host.root().resumeTextInput();
        }
    }

    private int indexOf(final ScreenHostImpl host) {
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.entries.get(i).host() == host) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Optional<Screen> top() {
        return this.entries.isEmpty() ? Optional.empty() : this.entries.getLast().host().screen();
    }

    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public void clear() {
        while (!this.entries.isEmpty()) {
            this.pop();
        }
    }

    /**
     * Asks each screen from the top down through its handle's {@code requestClose()}, which removes a screen that
     * agrees from the stack.
     */
    @Override
    public boolean requestClose() {
        while (!this.entries.isEmpty()) {
            final ScreenHostImpl host = this.topHost();
            if (!host.handle().map(ScreenHandle::requestClose).orElse(true)) {
                return false;
            }
            // A host whose screen closed while it opened has no handle, and stays until removed here
            this.remove(host);
        }
        return true;
    }

    @Override
    public Text title() {
        final ScreenHostImpl top = this.topHost();
        return top == null ? Text.empty() : top.title();
    }

    /**
     * Gives every screen that follows the stack's theme a new theme. Every such screen is checked first, so a theme
     * that lacks a key one of them needs changes no screen and leaves the stack's theme as it was.
     */
    @Override
    public void setTheme(final Theme theme) {
        Objects.requireNonNull(theme, "theme");
        this.checkTheme(theme);
        this.theme = theme;
        this.themeRef = null;
        for (final Entry entry : this.entries) {
            if (!entry.ownTheme()) {
                entry.host().setTheme(theme);
            }
        }
    }

    /**
     * Resolves a theme ref and gives the result to every screen that follows the stack's theme, as
     * {@link #setTheme(Theme)} does, checking every such screen first.
     */
    @Override
    public void setTheme(final ThemeRef theme) {
        final Theme resolved = new ThemeResolver(this.platform.themes()).resolve(theme);
        this.checkTheme(resolved);
        for (final Entry entry : this.entries) {
            if (!entry.ownTheme()) {
                entry.host().setTheme(theme);
            }
        }
        this.theme = resolved;
        this.themeRef = theme;
    }

    private void checkTheme(final Theme theme) {
        for (final Entry entry : this.entries) {
            if (!entry.ownTheme()) {
                entry.host().checkTheme(theme);
            }
        }
    }

    @Override
    public void resize(final float width, final float height) {
        this.width = width;
        this.height = height;
        for (final Entry entry : this.entries) {
            entry.host().resize(width, height);
        }
    }

    @Override
    public void setPixelsPerUnit(final float pixelsPerUnit) {
        Root.checkPixelsPerUnit(pixelsPerUnit);
        this.pixelsPerUnit = pixelsPerUnit;
        for (final Entry entry : this.entries) {
            entry.host().setPixelsPerUnit(pixelsPerUnit);
        }
    }

    @Override
    public void setTooltipDelay(final long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("Tooltip delay must not be negative: " + millis);
        }
        this.tooltipDelay = millis;
        for (final Entry entry : this.entries) {
            entry.host().setTooltipDelay(millis);
        }
    }

    @Override
    public void flush() {
        for (final Entry entry : List.copyOf(this.entries)) {
            entry.host().flush();
        }
    }

    @Override
    public void render(final Canvas canvas) {
        int first = 0;
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            if (this.entries.get(i).host().screen().map(Screen::hidesBackground).orElse(false)) {
                first = i;
                break;
            }
        }
        for (int i = first; i < this.entries.size(); i++) {
            if (i > first) {
                canvas.nextLayer();
            }
            this.entries.get(i).host().render(canvas);
        }
    }

    private Optional<ScreenHostImpl> input() {
        return this.entries.isEmpty() ? Optional.empty() : Optional.of(this.entries.getLast().host());
    }

    @Override
    public boolean pointerMoved(final float x, final float y, final Modifiers modifiers) {
        return this.input().map(host -> host.pointerMoved(x, y, modifiers)).orElse(false);
    }

    @Override
    public boolean pointerButton(final float x, final float y, final MouseButton button, final boolean pressed,
                                 final Modifiers modifiers) {
        return this.input().map(host -> host.pointerButton(x, y, button, pressed, modifiers)).orElse(false);
    }

    @Override
    public boolean scroll(final float x, final float y, final float scrollX, final float scrollY, final ScrollKind kind,
                          final Modifiers modifiers) {
        return this.input().map(host -> host.scroll(x, y, scrollX, scrollY, kind, modifiers)).orElse(false);
    }

    @Override
    public void pointerLeft() {
        this.input().ifPresent(ScreenHostImpl::pointerLeft);
    }

    @Override
    public boolean key(final Key key, final KeyAction action, final Modifiers modifiers) {
        return this.input().map(host -> host.key(key, action, modifiers)).orElse(false);
    }

    @Override
    public boolean charTyped(final int codePoint, final Modifiers modifiers) {
        return this.input().map(host -> host.charTyped(codePoint, modifiers)).orElse(false);
    }

    /**
     * One stacked screen's host, and whether it keeps a theme of its own.
     *
     * @param host the host
     * @param ownTheme true if the screen was opened in a theme of its own, which {@link #setTheme} leaves alone
     */
    private record Entry(ScreenHostImpl host, boolean ownTheme) {
    }

    /**
     * What a stacked screen's handle opens screens with: straight above that screen.
     */
    private final class Above implements ScreenController {
        private final ScreenHostImpl host;

        Above(final ScreenHostImpl host) {
            this.host = host;
        }

        @Override
        public ScreenHandle open(final Screen screen, final ThemeRef theme) {
            final int index = ScreenStackImpl.this.indexOf(this.host);
            if (index < 0) {
                throw new IllegalStateException("The screen has left the stack and cannot open another");
            }
            return ScreenStackImpl.this.insert(index + 1, screen, theme);
        }
    }
}
