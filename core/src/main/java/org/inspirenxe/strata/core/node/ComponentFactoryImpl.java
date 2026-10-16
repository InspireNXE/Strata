package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ComponentBehavior;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ButtonBehavior;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.LabelBehavior;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.api.widget.TextBoxBehavior;
import org.inspirenxe.strata.core.layout.AbsolutePanelImpl;
import org.inspirenxe.strata.core.layout.GridPanelImpl;
import org.inspirenxe.strata.core.layout.AnchorPanelImpl;
import org.inspirenxe.strata.core.layout.StackPanelImpl;
import org.inspirenxe.strata.core.widget.ButtonImpl;
import org.inspirenxe.strata.core.widget.LabelImpl;
import org.inspirenxe.strata.core.widget.TextBoxImpl;
import org.inspirenxe.strata.spi.Platform;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The component factory for one host. It also guards the tree: {@link #nodeOf} is how core turns a public component
 * back into its node, and it rejects any component Strata did not make, or that another host's factory made.
 */
public final class ComponentFactoryImpl implements ComponentFactory {
    private final Supplier<Theme> theme;
    private final Environment environment;

    /**
     * Creates the factory for a host.
     *
     * @param theme supplies the host's current theme; read each time {@link #theme()} is called, so a theme change
     *     shows up without a new factory
     * @param platform the host's platform, which backs {@link #environment()}
     * @throws NullPointerException if {@code theme} is null
     */
    public ComponentFactoryImpl(final Supplier<Theme> theme, final Platform platform) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.environment = new EnvironmentImpl(platform);
    }

    /**
     * Returns the environment that every component made by this factory shares.
     *
     * @return the environment, backed by the host's platform
     */
    public Environment environment() {
        return this.environment;
    }

    @Override
    public Theme theme() {
        return this.theme.get();
    }

    @Override
    public AnchorPanel anchorPanel() {
        return new AnchorPanelImpl(this);
    }

    @Override
    public StackPanel stackPanel() {
        return new StackPanelImpl(this, Orientation.VERTICAL);
    }

    @Override
    public StackPanel stackPanel(final Orientation orientation) {
        return new StackPanelImpl(this, orientation);
    }

    @Override
    public GridPanel gridPanel() {
        return new GridPanelImpl(this);
    }

    @Override
    public AbsolutePanel absolutePanel() {
        return new AbsolutePanelImpl(this);
    }

    @Override
    public Label label(final String text) {
        return new LabelImpl(this, text, null);
    }

    @Override
    public Label label(final String text, final LabelBehavior behavior) {
        return new LabelImpl(this, text, Objects.requireNonNull(behavior, "behavior"));
    }

    @Override
    public Button button(final String text) {
        return new ButtonImpl(this, text, null);
    }

    @Override
    public Button button(final String text, final ButtonBehavior behavior) {
        return new ButtonImpl(this, text, Objects.requireNonNull(behavior, "behavior"));
    }

    @Override
    public TextBox textBox() {
        return new TextBoxImpl(this, null);
    }

    @Override
    public TextBox textBox(final TextBoxBehavior behavior) {
        return new TextBoxImpl(this, Objects.requireNonNull(behavior, "behavior"));
    }

    @Override
    public ComponentPeer createPeer(final CustomComponent<?> owner, final ComponentHooks hooks,
                                    final ComponentBehavior<?> behavior) {
        return new Node(this, owner, hooks, behavior);
    }

    @Override
    public ContainerPeer createContainerPeer(final CustomComponent<?> owner, final ComponentHooks hooks,
                                             final ComponentBehavior<?> behavior) {
        return new ContainerNode(this, owner, hooks, behavior);
    }

    /**
     * Returns the node behind a component. Every core path that takes a public component goes through here.
     *
     * @param component the component
     * @return its node
     * @throws NullPointerException if {@code component} is null
     * @throws IllegalArgumentException if the component has no core node behind it (it implements a component
     *     interface directly), or another host's factory made it
     */
    public Node nodeOf(final Component<?> component) {
        Objects.requireNonNull(component, "component");
        if (!(component instanceof CustomComponent<?> custom) || !(custom.peer() instanceof Node node)) {
            throw new IllegalArgumentException(component.getClass().getName() + " has no Strata implementation behind it. "
                    + "Make components with ComponentFactory, or extend CustomComponent; do not implement component interfaces directly.");
        }
        if (node.factory() != this) {
            throw new IllegalArgumentException(component + " was made by a different ComponentFactory; components cannot move between hosts");
        }
        return node;
    }
}
