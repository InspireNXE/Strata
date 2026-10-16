package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.CoreUi;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.node.Root;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.UiHost;

import java.util.Objects;
import java.util.Optional;

public final class CoreHost implements UiHost {
    private final CoreUi ui;
    private final Root root;
    private final Runnable closeRequested;
    private Theme theme;
    private Screen screen;
    private boolean rebuild;

    CoreHost(final Platform platform, final Theme theme, final Runnable closeRequested) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.ui = new CoreUi(() -> this.theme);
        this.root = new Root(platform, theme, this.ui);
        this.closeRequested = closeRequested == null ? this::close : closeRequested;
    }

    /**
     * The node tree's root, for tests and for adapters that need core internals.
     */
    public Root root() {
        return this.root;
    }

    @Override
    public Ui ui() {
        return this.ui;
    }

    @Override
    public void open(final Screen screen) {
        Objects.requireNonNull(screen, "screen");
        this.close();
        this.root.setContent(this.ui.nodeOf(screen.build(this.ui)));
        this.screen = screen;
        this.rebuild = false;
        screen.opened(new Handle(screen));
    }

    @Override
    public void close() {
        if (this.screen == null) {
            return;
        }
        final Screen closing = this.screen;
        this.screen = null;
        closing.closed();
        this.root.setContent(null);
    }

    @Override
    public Optional<Screen> screen() {
        return Optional.ofNullable(this.screen);
    }

    @Override
    public Theme theme() {
        return this.theme;
    }

    @Override
    public void setTheme(final Theme theme) {
        this.root.setTheme(theme);
        this.theme = theme;
    }

    @Override
    public void resize(final float width, final float height) {
        this.root.resize(width, height);
    }

    @Override
    public void layout() {
        this.rebuildIfNeeded();
        this.root.layout();
    }

    @Override
    public void render(final Canvas canvas) {
        this.rebuildIfNeeded();
        this.root.render(canvas);
    }

    private void rebuildIfNeeded() {
        if (this.rebuild && this.screen != null) {
            this.rebuild = false;
            this.root.setContent(this.ui.nodeOf(this.screen.build(this.ui)));
        }
    }

    @Override
    public Optional<Component<?>> focused() {
        return Optional.ofNullable(this.root.focused()).map(Node::facade);
    }

    @Override
    public Optional<Component<?>> hovered() {
        return Optional.ofNullable(this.root.hovered()).map(Node::facade);
    }

    @Override
    public boolean pointerMoved(final float x, final float y, final Modifiers modifiers) {
        return this.root.pointerMoved(x, y, modifiers);
    }

    @Override
    public boolean pointerButton(final float x, final float y, final MouseButton button, final boolean pressed,
                                 final Modifiers modifiers) {
        return this.root.pointerButton(x, y, button, pressed, modifiers);
    }

    @Override
    public boolean scroll(final float x, final float y, final float scrollX, final float scrollY, final ScrollKind kind,
                          final Modifiers modifiers) {
        return this.root.scroll(x, y, scrollX, scrollY, kind, modifiers);
    }

    @Override
    public void pointerLeft() {
        this.root.pointerLeft();
    }

    @Override
    public boolean key(final Key key, final KeyAction action, final Modifiers modifiers) {
        return this.root.key(key, action, modifiers);
    }

    @Override
    public boolean charTyped(final int codePoint, final Modifiers modifiers) {
        return this.root.charTyped(codePoint, modifiers);
    }

    /**
     * Handed to one opened screen. Once that screen is closed or replaced, the handle does nothing.
     */
    private final class Handle implements ScreenHandle {
        private final Screen owner;

        Handle(final Screen owner) {
            this.owner = owner;
        }

        private boolean current() {
            return CoreHost.this.screen == this.owner;
        }

        @Override
        public void invalidate() {
            if (this.current()) {
                CoreHost.this.rebuild = true;
            }
        }

        @Override
        public void close() {
            if (this.current()) {
                CoreHost.this.closeRequested.run();
            }
        }

        @Override
        public Ui ui() {
            return CoreHost.this.ui;
        }
    }
}
