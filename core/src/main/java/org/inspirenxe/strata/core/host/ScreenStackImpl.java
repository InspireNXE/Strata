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

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
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
 */
public final class ScreenStackImpl implements ScreenStack {
    private final Platform platform;
    private final List<ScreenHostImpl> hosts;
    private Theme theme;
    private ThemeRef themeRef;
    private float width;
    private float height;
    private long tooltipDelay = Tooltip.DEFAULT_DELAY_MILLIS;

    /**
     * Creates an empty stack of size 0 by 0. Adapters use {@code StrataCore} instead.
     *
     * @param platform the engine's platform, shared by every screen's host
     * @param theme the theme for every screen pushed
     */
    public ScreenStackImpl(final Platform platform, final Theme theme) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.hosts = new ArrayList<>();
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
        Objects.requireNonNull(screen, "screen");
        final ScreenHostImpl[] self = new ScreenHostImpl[1];
        final Runnable close = () -> this.remove(self[0]);
        final ScreenHostImpl host = this.themeRef != null
                ? new ScreenHostImpl(this.platform, this.themeRef, close)
                : new ScreenHostImpl(this.platform, this.theme, close);
        self[0] = host;
        host.resize(this.width, this.height);
        host.setTooltipDelay(this.tooltipDelay);
        host.open(screen);
        this.hosts.add(host);
    }

    @Override
    public Optional<Screen> pop() {
        if (this.hosts.isEmpty()) {
            return Optional.empty();
        }
        final ScreenHostImpl host = this.hosts.removeLast();
        final Optional<Screen> screen = host.screen();
        host.close();
        return screen;
    }

    private void remove(final ScreenHostImpl host) {
        if (this.hosts.remove(host)) {
            host.close();
        }
    }

    @Override
    public Optional<Screen> top() {
        return this.hosts.isEmpty() ? Optional.empty() : this.hosts.getLast().screen();
    }

    @Override
    public boolean isEmpty() {
        return this.hosts.isEmpty();
    }

    @Override
    public void clear() {
        while (!this.hosts.isEmpty()) {
            this.pop();
        }
    }

    @Override
    public void setTheme(final Theme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.themeRef = null;
        for (final ScreenHostImpl host : this.hosts) {
            host.setTheme(theme);
        }
    }

    @Override
    public void setTheme(final ThemeRef theme) {
        final Theme resolved = new ThemeResolver(this.platform.themes()).resolve(theme);
        for (final ScreenHostImpl host : this.hosts) {
            host.setTheme(theme);
        }
        this.theme = resolved;
        this.themeRef = theme;
    }

    @Override
    public void resize(final float width, final float height) {
        this.width = width;
        this.height = height;
        for (final ScreenHostImpl host : this.hosts) {
            host.resize(width, height);
        }
    }

    @Override
    public void setTooltipDelay(final long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("Tooltip delay must not be negative: " + millis);
        }
        this.tooltipDelay = millis;
        for (final ScreenHostImpl host : this.hosts) {
            host.setTooltipDelay(millis);
        }
    }

    @Override
    public void render(final Canvas canvas) {
        int first = 0;
        for (int i = this.hosts.size() - 1; i >= 0; i--) {
            if (this.hosts.get(i).screen().map(Screen::hidesBackground).orElse(false)) {
                first = i;
                break;
            }
        }
        for (int i = first; i < this.hosts.size(); i++) {
            if (i > first) {
                canvas.nextLayer();
            }
            this.hosts.get(i).render(canvas);
        }
    }

    private Optional<ScreenHostImpl> input() {
        return this.hosts.isEmpty() ? Optional.empty() : Optional.of(this.hosts.getLast());
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
}
