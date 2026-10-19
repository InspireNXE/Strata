package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.SlotGrid;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.Environment;
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
import org.inspirenxe.strata.core.control.button.ButtonImpl;
import org.inspirenxe.strata.core.control.button.CheckBoxImpl;
import org.inspirenxe.strata.core.control.button.RadioGroupImpl;
import org.inspirenxe.strata.core.control.button.ToggleSwitchImpl;
import org.inspirenxe.strata.core.control.collection.ComboBoxImpl;
import org.inspirenxe.strata.core.control.collection.ListViewImpl;
import org.inspirenxe.strata.core.control.collection.SlotGridImpl;
import org.inspirenxe.strata.core.control.collection.TableViewImpl;
import org.inspirenxe.strata.core.control.collection.TreeViewImpl;
import org.inspirenxe.strata.core.control.display.ImageImpl;
import org.inspirenxe.strata.core.control.display.LabelImpl;
import org.inspirenxe.strata.core.control.display.ProgressBarImpl;
import org.inspirenxe.strata.core.control.display.SeparatorImpl;
import org.inspirenxe.strata.core.control.field.SliderImpl;
import org.inspirenxe.strata.core.control.field.SpinnerImpl;
import org.inspirenxe.strata.core.control.field.TextAreaImpl;
import org.inspirenxe.strata.core.control.field.TextBoxImpl;
import org.inspirenxe.strata.core.control.menu.MenuBarImpl;
import org.inspirenxe.strata.core.control.menu.MenuImpl;
import org.inspirenxe.strata.core.control.pane.ExpanderImpl;
import org.inspirenxe.strata.core.control.pane.SplitPaneImpl;
import org.inspirenxe.strata.core.control.pane.TabBarImpl;
import org.inspirenxe.strata.core.control.window.MessageBoxImpl;
import org.inspirenxe.strata.core.control.window.WindowImpl;
import org.inspirenxe.strata.core.layout.AbsolutePanelImpl;
import org.inspirenxe.strata.core.layout.AnchorPanelImpl;
import org.inspirenxe.strata.core.layout.GridPanelImpl;
import org.inspirenxe.strata.core.layout.ScrollPanelImpl;
import org.inspirenxe.strata.core.layout.StackPanelImpl;
import org.inspirenxe.strata.spi.Platform;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The component factory for one host. It also guards the tree: {@link #nodeOf} is how core turns a public component
 * back into its node, and it rejects any component Strata did not make, or that another host's factory made.
 */
public final class ComponentFactoryImpl implements ComponentFactory {
    private final Supplier<Theme> theme;
    private final Environment environment;
    private Root root;

    /**
     * Creates the factory for a host.
     *
     * @param theme supplies the host's current theme; read each time {@link #theme()} is called, so a theme change
     *     shows up without a new factory
     * @param platform the host's platform, which backs {@link #environment()}
     */
    public ComponentFactoryImpl(final Supplier<Theme> theme, final Platform platform) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.environment = new EnvironmentImpl(platform, this);
    }

    /**
     * Ties the factory to the root of its host. {@link Root}'s constructor calls this.
     *
     * @param root the host's root
     * @throws IllegalStateException if the factory already has a root
     */
    void bindRoot(final Root root) {
        if (this.root != null) {
            throw new IllegalStateException("This factory already belongs to a root");
        }
        this.root = Objects.requireNonNull(root, "root");
    }

    /**
     * Returns the root of this factory's host.
     *
     * @return the root, or {@code null} before the host made it
     */
    public Root root() {
        return this.root;
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
    public Overlays overlays() {
        if (this.root == null) {
            throw new IllegalStateException("This factory has no host yet");
        }
        return this.root.overlays();
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
    public ScrollPanel scrollPanel() {
        return new ScrollPanelImpl(this);
    }

    @Override
    public Label label(final String text) {
        return new LabelImpl(this, text);
    }

    @Override
    public Image image(final SpriteId sprite) {
        return new ImageImpl(this, sprite);
    }

    @Override
    public Image image(final Visual visual) {
        return new ImageImpl(this, visual);
    }

    @Override
    public Button button(final String text) {
        return new ButtonImpl(this, text);
    }

    @Override
    public CheckBox checkBox(final String text) {
        return new CheckBoxImpl(this, text);
    }

    @Override
    public Slider slider() {
        return new SliderImpl(this);
    }

    @Override
    public TextBox textBox() {
        return new TextBoxImpl(this);
    }

    @Override
    public <T> ListView<T> listView(final Function<? super T, ? extends Component<?>> cellFactory) {
        return new ListViewImpl<>(this, cellFactory);
    }

    @Override
    public <T> ComboBox<T> comboBox(final Function<? super T, String> labelFunction) {
        return new ComboBoxImpl<>(this, labelFunction);
    }

    @Override
    public Window window(final String title) {
        return new WindowImpl(this, title);
    }

    @Override
    public MessageBox messageBox(final String title, final String message, final MessageBox.Buttons buttons) {
        return new MessageBoxImpl(this, Objects.requireNonNull(title, "title"), Objects.requireNonNull(message, "message"),
                Objects.requireNonNull(buttons, "buttons"));
    }

    @Override
    public TabBar tabBar() {
        return new TabBarImpl(this);
    }

    @Override
    public ProgressBar progressBar() {
        return new ProgressBarImpl(this);
    }

    @Override
    public Separator separator(final Orientation orientation) {
        return new SeparatorImpl(this, orientation);
    }

    @Override
    public <T> RadioGroup<T> radioGroup() {
        return new RadioGroupImpl<>(this);
    }

    @Override
    public ToggleSwitch toggleSwitch(final String text) {
        return new ToggleSwitchImpl(this, text);
    }

    @Override
    public Spinner spinner() {
        return new SpinnerImpl(this);
    }

    @Override
    public TextArea textArea() {
        return new TextAreaImpl(this);
    }

    @Override
    public <T> TableView<T> tableView() {
        return new TableViewImpl<>(this);
    }

    @Override
    public <T> TreeView<T> treeView(final Function<? super T, ? extends Component<?>> cellFactory) {
        return new TreeViewImpl<>(this, cellFactory);
    }

    @Override
    public Menu menu() {
        return new MenuImpl(this);
    }

    @Override
    public MenuBar menuBar() {
        return new MenuBarImpl(this);
    }

    @Override
    public Expander expander(final String title) {
        return new ExpanderImpl(this, title);
    }

    @Override
    public SplitPane splitPane(final Orientation orientation) {
        return new SplitPaneImpl(this, orientation);
    }

    @Override
    public <T> SlotGrid<T> slotGrid(final Function<? super T, ? extends Component<?>> cellFactory) {
        return new SlotGridImpl<>(this, cellFactory);
    }

    @Override
    public ComponentPeer createPeer(final CustomComponent<?> owner, final ComponentHooks hooks) {
        return new Node(this, owner, hooks);
    }

    @Override
    public ContainerPeer createContainerPeer(final CustomComponent<?> owner, final ComponentHooks hooks) {
        return new ContainerNode(this, owner, hooks);
    }

    /**
     * Returns the node behind a component. Every core path that takes a public component goes through here.
     *
     * @param component the component
     * @return its node
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
