package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.node.Node;

import java.util.List;

/**
 * A host for tests: build components with {@link #ui()} first, then {@link #show} the tree.
 */
public final class TestHost {
    private final TestPlatform platform;
    private final CoreHost host;

    public TestHost(final float width, final float height) {
        this(new TestPlatform(), Themes.flat(), width, height);
    }

    public TestHost(final TestPlatform platform, final Theme theme, final float width, final float height) {
        this.platform = platform;
        this.host = (CoreHost) StrataCore.createHost(platform, theme);
        this.host.resize(width, height);
    }

    public TestPlatform platform() {
        return this.platform;
    }

    public CoreHost host() {
        return this.host;
    }

    public Ui ui() {
        return this.host.ui();
    }

    /**
     * Opens a screen whose tree is {@code content}, and lays it out.
     */
    public TestHost show(final Component<?> content) {
        this.host.open(ui -> content);
        this.host.layout();
        return this;
    }

    public void click(final float x, final float y) {
        this.host.pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    public Component<?> focused() {
        return this.host.focused().orElse(null);
    }

    public List<Component<?>> tabOrder() {
        return this.host.root().tabOrder().stream().<Component<?>>map(Node::facade).toList();
    }
}
