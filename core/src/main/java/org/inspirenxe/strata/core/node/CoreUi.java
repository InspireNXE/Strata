package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ComponentBehavior;
import org.inspirenxe.strata.api.extension.ComponentHooks;
import org.inspirenxe.strata.api.extension.ComponentPeer;
import org.inspirenxe.strata.api.extension.ContainerPeer;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Grid;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.Panel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ButtonBehavior;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.LabelBehavior;
import org.inspirenxe.strata.core.layout.AbsolutePanelImpl;
import org.inspirenxe.strata.core.layout.GridImpl;
import org.inspirenxe.strata.core.layout.PanelImpl;
import org.inspirenxe.strata.core.layout.StackPanelImpl;
import org.inspirenxe.strata.core.widget.ButtonImpl;
import org.inspirenxe.strata.core.widget.LabelImpl;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The component factory for one host. Also the gatekeeper: {@link #nodeOf} is how core turns a public component
 * back into its node, and it rejects anything Strata did not make, or that another host's factory made.
 */
public final class CoreUi implements Ui {
    private final Supplier<Theme> theme;

    public CoreUi(final Supplier<Theme> theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public Theme theme() {
        return this.theme.get();
    }

    @Override
    public Panel panel() {
        return new PanelImpl(this);
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
    public Grid grid() {
        return new GridImpl(this);
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
     * The node behind a component.
     *
     * @throws IllegalArgumentException if Strata did not make the component, or another host's {@code Ui} did
     */
    public Node nodeOf(final Component<?> component) {
        Objects.requireNonNull(component, "component");
        if (!(component instanceof CustomComponent<?> custom) || !(custom.peer() instanceof Node node)) {
            throw new IllegalArgumentException(component.getClass().getName() + " has no Strata implementation behind it. "
                    + "Make components with Ui, or extend CustomComponent; do not implement component interfaces directly.");
        }
        if (node.ui() != this) {
            throw new IllegalArgumentException(component + " was made by a different Ui; components cannot move between hosts");
        }
        return node;
    }
}
