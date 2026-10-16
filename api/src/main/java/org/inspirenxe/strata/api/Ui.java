package org.inspirenxe.strata.api;

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

/**
 * Makes components. Each host has its own {@code Ui}, handed to {@link org.inspirenxe.strata.api.screen.Screen#build}
 * and to {@link CustomComponent} constructors. A component can only join a tree built by the {@code Ui} that made
 * it.
 *
 * <p>There is deliberately no static way to get a {@code Ui}: mod loaders layer class loaders, and passing the
 * factory keeps every tree tied to the host that shows it.</p>
 */
public interface Ui {

    /**
     * The theme of the host this factory belongs to.
     */
    Theme theme();

    Panel panel();

    StackPanel stackPanel();

    StackPanel stackPanel(Orientation orientation);

    Grid grid();

    AbsolutePanel absolutePanel();

    Label label(String text);

    Label label(String text, LabelBehavior behavior);

    Button button(String text);

    Button button(String text, ButtonBehavior behavior);

    /**
     * For {@link CustomComponent}'s constructor; not for direct use.
     */
    ComponentPeer createPeer(CustomComponent<?> owner, ComponentHooks hooks, ComponentBehavior<?> behavior);

    /**
     * For {@link org.inspirenxe.strata.api.extension.CustomContainer}'s constructor; not for direct use.
     */
    ContainerPeer createContainerPeer(CustomComponent<?> owner, ComponentHooks hooks, ComponentBehavior<?> behavior);
}
