package org.inspirenxe.strata.api;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.ButtonBehavior;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CheckBoxBehavior;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.SlotGrid;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.ImageBehavior;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.LabelBehavior;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.SliderBehavior;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.field.TextBoxBehavior;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.extension.ComponentBehavior;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.function.Function;

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
     * Returns the overlays of the host this factory belongs to, for opening popups and dialogs over its screen.
     *
     * @return the host's overlays
     */
    Overlays overlays();

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
     * Makes a {@link ScrollPanel} that scrolls vertically.
     *
     * @return a new scroll panel
     */
    ScrollPanel scrollPanel();

    /**
     * Makes a {@link Label}.
     *
     * @param text the text; {@code \n} starts a new line
     * @return a new label
     */
    Label label(String text);

    /**
     * Makes a {@link Label} decorated with a behavior.
     *
     * @param text the text
     * @param behavior the behavior
     * @return a new label
     */
    Label label(String text, LabelBehavior behavior);

    /**
     * Makes an {@link Image}, 16 by 16 units until {@linkplain Image#setImageSize resized}.
     *
     * @param sprite the sprite to draw
     * @return a new image
     */
    Image image(SpriteId sprite);

    /**
     * Makes an {@link Image} decorated with a behavior.
     *
     * @param sprite the sprite to draw
     * @param behavior the behavior
     * @return a new image
     */
    Image image(SpriteId sprite, ImageBehavior behavior);

    /**
     * Makes an {@link Image} of an engine visual, at the visual's natural size.
     *
     * @param visual the visual to draw
     * @return a new image
     */
    Image image(Visual visual);

    /**
     * Makes a {@link Button}.
     *
     * @param text the caption
     * @return a new button
     */
    Button button(String text);

    /**
     * Makes a {@link Button} decorated with a behavior.
     *
     * @param text the caption
     * @param behavior the behavior
     * @return a new button
     */
    Button button(String text, ButtonBehavior behavior);

    /**
     * Makes an unchecked {@link CheckBox}.
     *
     * @param text the caption
     * @return a new check box
     */
    CheckBox checkBox(String text);

    /**
     * Makes an unchecked {@link CheckBox} decorated with a behavior.
     *
     * @param text the caption
     * @param behavior the behavior
     * @return a new check box
     */
    CheckBox checkBox(String text, CheckBoxBehavior behavior);

    /**
     * Makes a {@link Slider} over the range 0 to 1, at 0, with no step.
     *
     * @return a new slider
     */
    Slider slider();

    /**
     * Makes a {@link Slider} decorated with a behavior, over the range 0 to 1, at 0, with no step.
     *
     * @param behavior the behavior
     * @return a new slider
     */
    Slider slider(SliderBehavior behavior);

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
     */
    TextBox textBox(TextBoxBehavior behavior);

    /**
     * Makes an empty {@link ListView}.
     *
     * @param <T> the type of the items
     * @param cellFactory makes the component for an item's row
     * @return a new list
     */
    <T> ListView<T> listView(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Makes a {@link ComboBox} with no options.
     *
     * @param <T> the type of the options
     * @param labelFunction returns the text for an option
     * @return a new combo box
     */
    <T> ComboBox<T> comboBox(Function<? super T, String> labelFunction);

    /**
     * Makes an empty {@link Window}.
     *
     * @param title the title
     * @return a new window
     */
    Window window(String title);

    /**
     * Makes a {@link MessageBox}. Show it with {@link Overlays#dialog}.
     *
     * @param title the window's title
     * @param message the message; it wraps, and {@code \n} starts a new line
     * @param buttons which buttons it has
     * @return a new message box
     */
    MessageBox messageBox(String title, String message, MessageBox.Buttons buttons);

    /**
     * Makes a {@link TabBar} with no tabs.
     *
     * @return a new tab bar
     */
    TabBar tabBar();

    /**
     * Makes an empty {@link ProgressBar}.
     *
     * @return a new progress bar
     */
    ProgressBar progressBar();

    /**
     * Makes a {@link Separator}.
     *
     * @param orientation {@link Orientation#HORIZONTAL} for a line across, {@link Orientation#VERTICAL} for one
     *                    down
     * @return a new separator
     */
    Separator separator(Orientation orientation);

    /**
     * Makes an empty {@link RadioGroup}. Make its buttons with {@link RadioGroup#add}.
     *
     * @param <T> the type of the values the buttons stand for
     * @return a new group
     */
    <T> RadioGroup<T> radioGroup();

    /**
     * Makes a {@link ToggleSwitch} that is off.
     *
     * @param text the caption
     * @return a new switch
     */
    ToggleSwitch toggleSwitch(String text);

    /**
     * Makes a {@link Spinner} counting whole numbers from 0 to 100, at 0.
     *
     * @return a new spinner
     */
    Spinner spinner();

    /**
     * Makes an empty {@link TextArea} that wraps long lines.
     *
     * @return a new text area
     */
    TextArea textArea();

    /**
     * Makes a {@link TableView} with no columns and no items.
     *
     * @param <T> the type of the items
     * @return a new table
     */
    <T> TableView<T> tableView();

    /**
     * Makes a {@link TreeView} with no roots, in which every item is a leaf until
     * {@linkplain TreeView#setChildren told} otherwise.
     *
     * @param <T> the type of the items
     * @param cellFactory makes the component for an item's row
     * @return a new tree
     */
    <T> TreeView<T> treeView(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Makes an empty, closed {@link Menu}.
     *
     * @return a new menu
     */
    Menu menu();

    /**
     * Makes a {@link MenuBar} with no menus.
     *
     * @return a new menu bar
     */
    MenuBar menuBar();

    /**
     * Makes a collapsed {@link Expander} with no content.
     *
     * @param title the header's title
     * @return a new expander
     */
    Expander expander(String title);

    /**
     * Makes an empty {@link SplitPane} with the divider in the middle.
     *
     * @param orientation {@link Orientation#HORIZONTAL} for side by side, {@link Orientation#VERTICAL} for one
     *                    above the other
     * @return a new split pane
     */
    SplitPane splitPane(Orientation orientation);

    /**
     * Makes a {@link SlotGrid} with no slots, {@value SlotGrid#DEFAULT_COLUMNS} columns wide.
     *
     * @param <T> the type of the items
     * @param cellFactory makes the component shown in a filled slot
     * @return a new slot grid
     */
    <T> SlotGrid<T> slotGrid(Function<? super T, ? extends Component<?>> cellFactory);

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
     * Creates the peer of a new container or composite. Called by
     * {@link org.inspirenxe.strata.api.extension.CustomComposite}'s constructor; do not call it yourself.
     *
     * @param owner the container being constructed
     * @param hooks how the peer reaches the container's protected methods
     * @param behavior the container's behavior, or {@code null}
     * @return the new peer
     */
    ContainerPeer createContainerPeer(CustomComponent<?> owner, ComponentHooks hooks, ComponentBehavior<?> behavior);
}
