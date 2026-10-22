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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
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
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.node.Node;
import org.inspirenxe.strata.core.node.Root;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.Objects;
import java.util.Optional;

/**
 * The core {@link ScreenHost}. It owns one factory and one {@link Root}, and passes engine input and frames to the
 * root. Opening a screen closes the one before it, so components from different screens never share a tree.
 *
 * <p>A screen's handle opens screens through the opener the adapter set with {@link #setOpener}, or else through
 * the platform's controller in {@link PlatformScreens}. A handle whose opening has ended throws from
 * {@code open}.</p>
 *
 * <p>A host given a {@link ThemeRef} compares the platform's {@link ThemeSource#version()} and the version of the
 * themes and overrides added in code ({@link ThemeRegistryImpl#version()}) at the start of every layout and render,
 * and resolves the ref again when either changed. If that fails, or the new theme lacks keys the open tree needs,
 * the host logs a warning and keeps its theme until the next change.</p>
 */
public final class ScreenHostImpl implements ScreenHost {
    private static final System.Logger LOGGER = System.getLogger(ScreenHostImpl.class.getName());

    private final ComponentFactoryImpl ui;
    private final Root root;
    private final Runnable closeRequested;
    private final ThemeSource themes;
    private final ThemeResolver resolver;
    private Theme theme;
    private ThemeRef themeRef;
    private long themeVersion;
    private long registryVersion;
    private long textVersion = Translations.version();
    private Screen screen;
    private Handle handle;
    // The open screen's title: read from it on open, replaced through its handle
    private Text title = Text.empty();
    private ScreenController opener;
    private boolean rebuild;

    /**
     * Creates a host with no screen open and a size of 0 by 0. Adapters use {@code StrataCore} instead.
     *
     * @param platform the engine's platform
     * @param theme the starting theme
     * @param closeRequested runs when the open screen's handle closes it, through {@code close()} or a
     *     {@code requestClose()} the screen agreed to, or {@code null} to have the host close the screen itself
     */
    public ScreenHostImpl(final Platform platform, final Theme theme, final Runnable closeRequested) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.themes = platform.themes();
        this.resolver = new ThemeResolver(this.themes);
        this.ui = new ComponentFactoryImpl(() -> this.theme, platform);
        this.root = new Root(platform, theme, this.ui);
        this.closeRequested = closeRequested == null ? this::close : closeRequested;
    }

    /**
     * Creates a host whose theme is resolved from a ref and follows the platform's theme source. Adapters use
     * {@code StrataCore} instead.
     *
     * @param platform the engine's platform
     * @param theme the theme to resolve
     * @param closeRequested runs when the open screen's handle closes it, through {@code close()} or a
     *     {@code requestClose()} the screen agreed to, or {@code null} to have the host close the screen itself
     * @throws ThemeException if the ref cannot be resolved
     */
    public ScreenHostImpl(final Platform platform, final ThemeRef theme, final Runnable closeRequested) {
        this(platform, Resolution.of(new ThemeResolver(platform.themes()), platform.themes(), theme), closeRequested);
    }

    private ScreenHostImpl(final Platform platform, final Resolution resolution, final Runnable closeRequested) {
        this(platform, resolution.theme(), closeRequested);
        this.themeRef = resolution.ref();
        this.themeVersion = resolution.sourceVersion();
        this.registryVersion = resolution.registryVersion();
    }

    /**
     * Returns the root of the node tree, for tests and for adapters that need core internals.
     *
     * @return the root
     */
    public Root root() {
        return this.root;
    }

    @Override
    public ComponentFactory factory() {
        return this.ui;
    }

    @Override
    public void open(final Screen screen) {
        this.openScreen(screen);
    }

    /**
     * Opens a screen as {@link #open} does and returns its handle, which stays the screen's handle even if the
     * screen closes again while it opens.
     *
     * @param screen the screen to show
     * @return the handle passed to the screen's {@code opened}
     * @throws ThemeException if the theme lacks keys the screen's tree needs; no screen is open afterwards
     */
    public ScreenHandle openScreen(final Screen screen) {
        Objects.requireNonNull(screen, "screen");
        this.close();
        this.root.setContent(this.ui.nodeOf(screen.build(this.ui)));
        final Handle opened = new Handle(screen);
        this.screen = screen;
        this.handle = opened;
        final Text title = screen.title();
        this.title = title != null ? title : Text.empty();
        this.root.setScreen(screen);
        this.rebuild = false;
        screen.opened(opened);
        return opened;
    }

    /**
     * Closes the open screen: calls its {@code closed()}, then empties the root. The root is emptied even when
     * {@code closed()} throws, so the old tree never stays shown in a host that has no screen.
     */
    @Override
    public void close() {
        if (this.screen == null) {
            return;
        }
        final Screen closing = this.screen;
        this.screen = null;
        this.handle = null;
        this.title = Text.empty();
        try {
            closing.closed();
        } finally {
            this.root.setContent(null);
            this.root.setScreen(null);
        }
    }

    /**
     * Asks the open screen whether it may close and closes it if it agrees. A screen that closes itself through its
     * handle while it is asked counts as agreeing.
     */
    @Override
    public boolean requestClose() {
        final Handle asked = this.handle;
        if (asked == null) {
            return true;
        }
        if (!asked.owner.closeRequested(asked)) {
            return false;
        }
        if (this.handle == asked) {
            this.close();
        }
        return true;
    }

    @Override
    public Text title() {
        return this.title;
    }

    @Override
    public Optional<Screen> screen() {
        return Optional.ofNullable(this.screen);
    }

    @Override
    public Optional<ScreenHandle> handle() {
        return Optional.ofNullable(this.handle);
    }

    @Override
    public void setOpener(final ScreenController opener) {
        this.opener = opener;
    }

    @Override
    public Theme theme() {
        return this.theme;
    }

    @Override
    public void setTheme(final Theme theme) {
        this.applyTheme(theme);
        this.themeRef = null;
    }

    @Override
    public void setTheme(final ThemeRef theme) {
        final Resolution resolution = Resolution.of(this.resolver, this.themes, Objects.requireNonNull(theme, "theme"));
        this.applyTheme(resolution.theme());
        this.themeRef = theme;
        this.themeVersion = resolution.sourceVersion();
        this.registryVersion = resolution.registryVersion();
    }

    /**
     * Checks a theme against the open tree and its overlays without changing anything, so a stack can check every
     * screen before it changes any.
     *
     * @param theme the theme
     * @throws ThemeException if the theme lacks keys the tree or an overlay needs
     */
    public void checkTheme(final Theme theme) {
        this.root.checkTheme(theme);
    }

    /**
     * Sets the host's theme, which the factory reports, and then gives it to the root, so components that read
     * {@code ComponentFactory.theme()} while the theme is applied see the new one. If the root refuses the theme,
     * the host keeps the old one.
     */
    private void applyTheme(final Theme theme) {
        Objects.requireNonNull(theme, "theme");
        final Theme previous = this.theme;
        this.theme = theme;
        boolean done = false;
        try {
            this.root.setTheme(theme);
            done = true;
        } finally {
            if (!done) {
                this.theme = previous;
            }
        }
    }

    @Override
    public Optional<ThemeRef> themeRef() {
        return Optional.ofNullable(this.themeRef);
    }

    /**
     * Resolves the ref again if the theme source or the registry changed since the last time.
     */
    private void refreshTheme() {
        if (this.themeRef == null) {
            return;
        }
        final long version = this.themes.version();
        final long registry = ThemeRegistryImpl.INSTANCE.version();
        if (version == this.themeVersion && registry == this.registryVersion) {
            return;
        }
        this.themeVersion = version;
        this.registryVersion = registry;
        try {
            this.applyTheme(this.resolver.resolve(this.themeRef));
        } catch (final ThemeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Keeping theme " + this.theme.name() + " after a reload: "
                    + e.getMessage());
        }
    }

    /**
     * Lays out every node again if translations changed since the last time.
     */
    private void refreshText() {
        final long version = Translations.version();
        if (version != this.textVersion) {
            this.textVersion = version;
            this.root.invalidateText();
        }
    }

    @Override
    public void resize(final float width, final float height) {
        this.root.resize(width, height);
    }

    @Override
    public void setPixelsPerUnit(final float pixelsPerUnit) {
        this.root.setPixelsPerUnit(pixelsPerUnit);
    }

    @Override
    public void setTooltipDelay(final long millis) {
        this.root.setTooltipDelay(millis);
    }

    @Override
    public void flush() {
        this.rebuildIfNeeded();
        // Step 2 of the frame, firing recorded continuous requests, goes here once coalescing is built (design 4.14)
        this.root.flushBindings();
    }

    @Override
    public void layout() {
        this.refreshTheme();
        this.refreshText();
        this.flush();
        this.root.layout();
    }

    @Override
    public void render(final Canvas canvas) {
        this.refreshTheme();
        this.refreshText();
        this.flush();
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
     * Handed to one opening of a screen. Once that opening ends, the handle does nothing, and {@link #open} throws.
     */
    private final class Handle implements ScreenHandle {
        private final Screen owner;

        Handle(final Screen owner) {
            this.owner = owner;
        }

        private boolean current() {
            return ScreenHostImpl.this.handle == this;
        }

        @Override
        public void invalidate() {
            if (this.current()) {
                ScreenHostImpl.this.rebuild = true;
            }
        }

        @Override
        public void close() {
            if (this.current()) {
                ScreenHostImpl.this.closeRequested.run();
            }
        }

        @Override
        public boolean requestClose() {
            if (!this.current() || !this.owner.closeRequested(this)) {
                return false;
            }
            // The screen may have closed itself while it was asked
            if (this.current()) {
                ScreenHostImpl.this.closeRequested.run();
            }
            return true;
        }

        @Override
        public boolean isOpen() {
            return this.current();
        }

        @Override
        public ScreenHandle setTitle(final Text title) {
            Objects.requireNonNull(title, "title");
            if (this.current()) {
                ScreenHostImpl.this.title = title;
            }
            return this;
        }

        @Override
        public ComponentFactory factory() {
            return ScreenHostImpl.this.ui;
        }

        @Override
        public ScreenHandle open(final Screen screen, final ThemeRef theme) {
            Objects.requireNonNull(screen, "screen");
            Objects.requireNonNull(theme, "theme");
            if (!this.current()) {
                throw new IllegalStateException("Screen " + this.owner + " has closed and cannot open another");
            }
            final ScreenController above = ScreenHostImpl.this.opener;
            return (above != null ? above : PlatformScreens.get()).open(screen, theme);
        }
    }

    /**
     * A ref resolved, with the versions read before resolving, so a change made while it resolved is seen next time.
     *
     * @param ref the ref
     * @param theme the theme it resolved to
     * @param sourceVersion the theme source's version
     * @param registryVersion the registry's version
     */
    private record Resolution(ThemeRef ref, Theme theme, long sourceVersion, long registryVersion) {
        private static Resolution of(final ThemeResolver resolver, final ThemeSource source, final ThemeRef ref) {
            final long sourceVersion = source.version();
            final long registryVersion = ThemeRegistryImpl.INSTANCE.version();
            return new Resolution(ref, resolver.resolve(ref), sourceVersion, registryVersion);
        }
    }
}
