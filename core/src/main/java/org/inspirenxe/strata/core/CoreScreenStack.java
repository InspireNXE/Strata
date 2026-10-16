package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One {@link CoreHost} per stacked screen, so each screen keeps its own tree, focus and factory.
 */
public final class CoreScreenStack implements ScreenStack {
    private final Platform platform;
    private final List<CoreHost> hosts;
    private Theme theme;
    private float width;
    private float height;

    CoreScreenStack(final Platform platform, final Theme theme) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.hosts = new ArrayList<>();
    }

    @Override
    public void push(final Screen screen) {
        Objects.requireNonNull(screen, "screen");
        final CoreHost[] self = new CoreHost[1];
        final CoreHost host = new CoreHost(this.platform, this.theme, () -> this.remove(self[0]));
        self[0] = host;
        host.resize(this.width, this.height);
        host.open(screen);
        this.hosts.add(host);
    }

    @Override
    public Optional<Screen> pop() {
        if (this.hosts.isEmpty()) {
            return Optional.empty();
        }
        final CoreHost host = this.hosts.removeLast();
        final Optional<Screen> screen = host.screen();
        host.close();
        return screen;
    }

    private void remove(final CoreHost host) {
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
        for (final CoreHost host : this.hosts) {
            host.setTheme(theme);
        }
    }

    @Override
    public void resize(final float width, final float height) {
        this.width = width;
        this.height = height;
        for (final CoreHost host : this.hosts) {
            host.resize(width, height);
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

    private Optional<CoreHost> input() {
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
        this.input().ifPresent(CoreHost::pointerLeft);
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
