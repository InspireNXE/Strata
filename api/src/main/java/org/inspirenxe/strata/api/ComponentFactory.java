package org.inspirenxe.strata.api;

import org.inspirenxe.strata.api.extension.ComponentBehavior;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.ButtonBehavior;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.LabelBehavior;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.api.widget.TextBoxBehavior;

/**
 * Makes components. Every host has its own factory; a screen receives it in
 * {@link org.inspirenxe.strata.api.screen.Screen#build}, and custom components take it in their constructor.
 *
 * <p>A component belongs to the factory that made it and can only join trees shown by that factory's host. There
 * is deliberately no static way to get a factory: mod loaders layer class loaders, and passing the factory along
 * keeps every tree tied to the host that shows it.</p>
 *
 * <p>Each call returns a new component, detached and with default settings. Call factories on the UI thread
 * only.</p>
 *
 * <p>Strata provides the factory. Do not implement this interface.</p>
 */
public interface ComponentFactory {

    /**
     * Returns the theme of the host this factory belongs to. It may change while the host runs; components are
     * told through their theme hook.
     *
     * @return the host's current theme
     */
    Theme theme();

    /**
     * Makes an {@link AnchorPanel}.
     *
     * @return a new anchor panel
     */
    AnchorPanel anchorPanel();

    /**
     * Makes a vertical {@link StackPanel}.
     *
     * @return a new stack panel laying out children top to bottom
     */
    StackPanel stackPanel();

    /**
     * Makes a {@link StackPanel} with the given orientation.
     *
     * @param orientation the direction to lay out children
     * @return a new stack panel
     * @throws NullPointerException if {@code orientation} is null
     */
    StackPanel stackPanel(Orientation orientation);

    /**
     * Makes a {@link GridPanel} with no row or column definitions.
     *
     * @return a new grid panel
     */
    GridPanel gridPanel();

    /**
     * Makes an {@link AbsolutePanel}.
     *
     * @return a new absolute panel
     */
    AbsolutePanel absolutePanel();

    /**
     * Makes a {@link Label}.
     *
     * @param text the text; {@code \n} starts a new line
     * @return a new label
     * @throws NullPointerException if {@code text} is null
     */
    Label label(String text);

    /**
     * Makes a {@link Label} decorated with a behavior.
     *
     * @param text the text
     * @param behavior the behavior
     * @return a new label
     * @throws NullPointerException if an argument is null
     */
    Label label(String text, LabelBehavior behavior);

    /**
     * Makes a {@link Button}.
     *
     * @param text the caption
     * @return a new button
     * @throws NullPointerException if {@code text} is null
     */
    Button button(String text);

    /**
     * Makes a {@link Button} decorated with a behavior.
     *
     * @param text the caption
     * @param behavior the behavior
     * @return a new button
     * @throws NullPointerException if an argument is null
     */
    Button button(String text, ButtonBehavior behavior);

    /**
     * Makes an empty {@link TextBox}.
     *
     * @return a new text box
     */
    TextBox textBox();

    /**
     * Makes an empty {@link TextBox} decorated with a behavior.
     *
     * @param behavior the behavior
     * @return a new text box
     * @throws NullPointerException if {@code behavior} is null
     */
    TextBox textBox(TextBoxBehavior behavior);

    /**
     * Creates the peer of a new leaf component. Called by {@link CustomComponent}'s constructor; do not call it
     * yourself.
     *
     * @param owner the component being constructed
     * @param hooks how the peer reaches the component's protected methods
     * @param behavior the component's behavior, or {@code null}
     * @return the new peer
     */
    ComponentPeer createPeer(CustomComponent<?> owner, ComponentHooks hooks, ComponentBehavior<?> behavior);

    /**
     * Creates the peer of a new container. Called by {@link org.inspirenxe.strata.api.extension.CustomContainer}'s
     * constructor; do not call it yourself.
     *
     * @param owner the container being constructed
     * @param hooks how the peer reaches the container's protected methods
     * @param behavior the container's behavior, or {@code null}
     * @return the new peer
     */
    ContainerPeer createContainerPeer(CustomComponent<?> owner, ComponentHooks hooks, ComponentBehavior<?> behavior);
}
