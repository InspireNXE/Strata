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
package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.core.animation.AnimatorImpl;
import org.inspirenxe.strata.core.frame.UiThreadImpl;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.command.Shortcuts;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.input.KeyStroke;
import org.inspirenxe.strata.core.event.CauseStackImpl;
import org.inspirenxe.strata.core.input.KeyNames;
import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.component.registry.ComponentRegistry;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.control.button.DropDownButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.display.Avatar;
import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.DatePicker;
import org.inspirenxe.strata.api.control.field.DateTimePicker;
import org.inspirenxe.strata.api.control.field.TimePicker;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.menu.MenuItem;
import org.inspirenxe.strata.api.control.menu.MenuSeparator;
import org.inspirenxe.strata.api.control.menu.SubmenuItem;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.NavigationHeader;
import org.inspirenxe.strata.api.control.pane.NavigationItem;
import org.inspirenxe.strata.api.control.pane.NavigationSeparator;
import org.inspirenxe.strata.api.control.pane.NavigationView;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.Tab;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.state.Source;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.CustomComposite;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.Spacer;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.WrapPanel;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.control.button.ButtonImpl;
import org.inspirenxe.strata.core.control.button.CheckBoxImpl;
import org.inspirenxe.strata.core.control.button.CycleButtonImpl;
import org.inspirenxe.strata.core.control.button.DropDownButtonImpl;
import org.inspirenxe.strata.core.control.button.RadioGroupImpl;
import org.inspirenxe.strata.core.control.button.ToggleSwitchImpl;
import org.inspirenxe.strata.core.control.collection.ComboBoxImpl;
import org.inspirenxe.strata.core.control.collection.GridViewImpl;
import org.inspirenxe.strata.core.control.collection.ListViewImpl;
import org.inspirenxe.strata.core.control.collection.TableColumnImpl;
import org.inspirenxe.strata.core.control.collection.TableViewImpl;
import org.inspirenxe.strata.core.control.collection.TreeViewImpl;
import org.inspirenxe.strata.core.control.display.AvatarImpl;
import org.inspirenxe.strata.core.control.display.BusyIndicatorImpl;
import org.inspirenxe.strata.core.control.display.ImageImpl;
import org.inspirenxe.strata.core.control.display.InfoBarImpl;
import org.inspirenxe.strata.core.control.display.LabelImpl;
import org.inspirenxe.strata.core.control.display.ProgressBarImpl;
import org.inspirenxe.strata.core.control.display.SeparatorImpl;
import org.inspirenxe.strata.core.control.field.CalendarViewImpl;
import org.inspirenxe.strata.core.control.field.ColorPickerImpl;
import org.inspirenxe.strata.core.control.field.DatePickerImpl;
import org.inspirenxe.strata.core.control.field.DateTimePickerImpl;
import org.inspirenxe.strata.core.control.field.TimePickerImpl;
import org.inspirenxe.strata.core.control.field.SliderImpl;
import org.inspirenxe.strata.core.control.field.SpinnerImpl;
import org.inspirenxe.strata.core.control.field.PasswordTextBoxImpl;
import org.inspirenxe.strata.core.control.field.TextAreaImpl;
import org.inspirenxe.strata.core.control.field.TextBoxImpl;
import org.inspirenxe.strata.core.control.menu.CheckMenuItemImpl;
import org.inspirenxe.strata.core.control.menu.FlyoutImpl;
import org.inspirenxe.strata.core.control.menu.MenuBarImpl;
import org.inspirenxe.strata.core.control.menu.MenuImpl;
import org.inspirenxe.strata.core.control.menu.MenuItemImpl;
import org.inspirenxe.strata.core.control.menu.MenuSeparatorImpl;
import org.inspirenxe.strata.core.control.menu.SubmenuItemImpl;
import org.inspirenxe.strata.core.control.pane.ExpanderImpl;
import org.inspirenxe.strata.core.control.pane.NavigationHeaderImpl;
import org.inspirenxe.strata.core.control.pane.NavigationItemImpl;
import org.inspirenxe.strata.core.control.pane.NavigationSeparatorImpl;
import org.inspirenxe.strata.core.control.pane.NavigationViewImpl;
import org.inspirenxe.strata.core.control.pane.ScrollBarImpl;
import org.inspirenxe.strata.core.control.pane.SplitPaneImpl;
import org.inspirenxe.strata.core.control.pane.TabBarImpl;
import org.inspirenxe.strata.core.control.pane.TabImpl;
import org.inspirenxe.strata.core.control.window.MessageBoxImpl;
import org.inspirenxe.strata.core.control.window.WindowImpl;
import org.inspirenxe.strata.core.layout.AbsolutePanelImpl;
import org.inspirenxe.strata.core.layout.AnchorPanelImpl;
import org.inspirenxe.strata.core.layout.GridPanelImpl;
import org.inspirenxe.strata.core.layout.ScrollPanelImpl;
import org.inspirenxe.strata.core.layout.SpacerImpl;
import org.inspirenxe.strata.core.layout.StackPanelImpl;
import org.inspirenxe.strata.core.layout.WrapPanelImpl;
import org.inspirenxe.strata.core.registry.ComponentRegistryImpl;
import org.inspirenxe.strata.spi.Platform;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The component factory for one host. Everything its named methods make is a {@link BuiltInComponent};
 * {@link #create(Class)} and {@link #create(ComponentId)} make what the {@link ComponentRegistryImpl} holds, which
 * may be a library's {@link CustomComponent}, and check it before returning it. It also guards the tree:
 * {@link #nodeOf} is how core turns a public component back into its node, whether Strata made it or a developer
 * wrote it on {@link CustomComponent}, and it rejects any component without a node, or that another host's factory
 * made.
 */
public final class ComponentFactoryImpl implements ComponentFactory {
    /**
     * The cell padding when the theme sets no {@link ListView#STYLE_CELL_PADDING}:
     * {@value ComponentFactory#CELL_PADDING} units at the sides.
     */
    public static final Insets DEFAULT_CELL_PADDING = new Insets(CELL_PADDING, 0f, CELL_PADDING, 0f);

    // Between a cell's icon and its name
    private static final Text CELL_GAP = Text.of(" ");

    private final Supplier<Theme> theme;
    private final Environment environment;
    private final Platform platform;
    private final CauseStack platformCauses;
    private final UiThreadImpl uiThread;
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
        this.platform = platform;
        this.platformCauses = platform.causes().orElse(null);
        this.uiThread = UiThreadImpl.of(platform);
    }

    /**
     * Returns the UI thread of the host's platform, shared by every host made with that platform.
     *
     * @return the UI thread
     */
    @Override
    public UiThreadImpl uiThread() {
        return this.uiThread;
    }

    /**
     * Returns the platform's cause stack if it has one, or else the calling thread's own, which is the UI thread's
     * when called from it.
     *
     * @return the stack
     */
    @Override
    public CauseStack causes() {
        return this.platformCauses != null ? this.platformCauses : CauseStackImpl.forThread();
    }

    /**
     * Returns the host's platform, for what only core reads, such as the wall clock.
     *
     * @return the platform
     */
    public Platform platform() {
        return this.platform;
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
    public AnimatorImpl animator() {
        if (this.root == null) {
            throw new IllegalStateException("This factory has no host yet");
        }
        return this.root.animator();
    }

    @Override
    public <T> Subscription watch(final Source<T> source, final Consumer<? super T> listener) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(listener, "listener");
        if (this.root == null) {
            throw new IllegalStateException("This factory has no host yet");
        }
        return this.root.watch(source, listener);
    }

    @Override
    public Optional<Transfer> clipboard() {
        return this.environment.clipboard();
    }

    @Override
    public void setClipboard(final Transfer transfer) {
        this.environment.setClipboard(transfer);
    }

    @Override
    public Overlays overlays() {
        if (this.root == null) {
            throw new IllegalStateException("This factory has no host yet");
        }
        return this.root.overlays();
    }

    @Override
    public Shortcuts shortcuts() {
        if (this.root == null) {
            throw new IllegalStateException("This factory has no host yet");
        }
        return this.root.shortcuts();
    }

    /**
     * Names a stroke through {@link KeyNames}, with the platform's shortcut modifier.
     */
    @Override
    public Text keyText(final KeyStroke stroke) {
        return KeyNames.text(Objects.requireNonNull(stroke, "stroke"), this.platform.conventions().shortcutModifier());
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
    public WrapPanel wrapPanel() {
        return new WrapPanelImpl(this, Orientation.HORIZONTAL);
    }

    @Override
    public WrapPanel wrapPanel(final Orientation orientation) {
        return new WrapPanelImpl(this, orientation);
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
    public Spacer spacer() {
        return new SpacerImpl(this);
    }

    @Override
    public Label label(final Text text) {
        return new LabelImpl(this, text);
    }

    @Override
    public Label cellLabel(final Text text) {
        final Label label = this.label(text)
                .setOverflow(TextOverflow.ELLIPSIS)
                .setAnchor(Anchor.MIDDLE_LEFT);
        return this.cellPadded(label);
    }

    @Override
    public Component<?> cell(final Object value) {
        final Appearances appearances = this.appearances();
        final Text name = appearances.nameOf(value);
        final Optional<Visual> icon = appearances.iconOf(value);
        final Label row = this.cellLabel(icon.isEmpty() ? name : Text.concat(Text.visual(icon.get()), CELL_GAP, name));
        final Runnable ask = () -> appearances.tooltipOf(value).ifPresentOrElse(row::setTooltip, row::clearTooltip);
        ask.run();
        return row.onEvent(InputEvents.POINTER_ENTER, event -> ask.run());
    }

    /**
     * Gives a component the theme's cell padding as its margin, {@link ListView#STYLE_CELL_PADDING} or
     * {@link #DEFAULT_CELL_PADDING}, until its margin is set.
     *
     * @param component the label of a cell
     * @param <C> the component's type
     * @return the component
     */
    public <C extends Component<?>> C cellPadded(final C component) {
        this.nodeOf(component).setThemeMargin(theme -> theme.insetsOr(ListView.STYLE_CELL_PADDING, DEFAULT_CELL_PADDING));
        return component;
    }

    @Override
    public Image image() {
        return new ImageImpl(this);
    }

    @Override
    public Avatar avatar() {
        return new AvatarImpl(this);
    }

    @Override
    public InfoBar infoBar() {
        return new InfoBarImpl(this);
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
    public Button button(final Text text) {
        return new ButtonImpl(this, text);
    }

    @Override
    public DropDownButton dropDownButton(final Text text) {
        return new DropDownButtonImpl(this, text);
    }

    @Override
    public CheckBox checkBox(final Text text) {
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
    public PasswordTextBox passwordTextBox() {
        return new PasswordTextBoxImpl(this);
    }

    @Override
    public <T> ListView<T> listView(final Function<? super T, ? extends Component<?>> cellFactory) {
        return new ListViewImpl<T>(this, cellFactory).standalone();
    }

    @Override
    public <T> CycleButton<T> cycleButton(final Text text, final Function<? super T, ?> valueLabelFunction) {
        return new CycleButtonImpl<>(this, text, valueLabelFunction);
    }

    @Override
    public <T> ComboBox<T> comboBox(final Function<? super T, ?> labelFunction) {
        return new ComboBoxImpl<>(this, labelFunction);
    }

    @Override
    public Window window(final Text title) {
        return new WindowImpl(this, title);
    }

    @Override
    public MessageBox messageBox(final Text title, final Text message, final MessageBox.Buttons buttons) {
        return new MessageBoxImpl(this, Objects.requireNonNull(title, "title"), Objects.requireNonNull(message, "message"),
                Objects.requireNonNull(buttons, "buttons"));
    }

    @Override
    public TabBar tabBar() {
        return new TabBarImpl(this);
    }

    @Override
    public Tab tab(final Text text) {
        return new TabImpl(text);
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
    public ToggleSwitch toggleSwitch(final Text text) {
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
    public <T> TableColumn<T> tableColumn(final Text title) {
        return new TableColumnImpl<>(this, title);
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
    public Flyout flyout() {
        return new FlyoutImpl(this);
    }

    @Override
    public MenuItem menuItem(final Text text) {
        return new MenuItemImpl(text);
    }

    @Override
    public CheckMenuItem checkMenuItem(final Text text) {
        return new CheckMenuItemImpl(text);
    }

    @Override
    public SubmenuItem submenuItem(final Text text) {
        return new SubmenuItemImpl(text);
    }

    @Override
    public MenuSeparator menuSeparator() {
        return new MenuSeparatorImpl();
    }

    @Override
    public Expander expander(final Text title) {
        return new ExpanderImpl(this, title);
    }

    @Override
    public SplitPane splitPane(final Orientation orientation) {
        return new SplitPaneImpl(this, orientation);
    }

    @Override
    public <T> GridView<T> gridView(final Function<? super T, ? extends Component<?>> cellFactory) {
        return new GridViewImpl<>(this, cellFactory);
    }

    @Override
    public ScrollBar scrollBar() {
        return new ScrollBarImpl(this);
    }

    @Override
    public BusyIndicator busyIndicator() {
        return new BusyIndicatorImpl(this);
    }

    @Override
    public ColorPicker colorPicker() {
        return new ColorPickerImpl(this);
    }

    @Override
    public NavigationView navigationView() {
        return new NavigationViewImpl(this);
    }

    @Override
    public NavigationItem navigationItem(final Text text) {
        return new NavigationItemImpl(text);
    }

    @Override
    public NavigationHeader navigationHeader(final Text text) {
        return new NavigationHeaderImpl(text);
    }

    @Override
    public NavigationSeparator navigationSeparator() {
        return new NavigationSeparatorImpl();
    }

    @Override
    public CalendarView calendarView() {
        return new CalendarViewImpl(this);
    }

    @Override
    public DatePicker datePicker() {
        return new DatePickerImpl(this);
    }

    @Override
    public TimePicker timePicker() {
        return new TimePickerImpl(this);
    }

    @Override
    public DateTimePicker dateTimePicker() {
        return new DateTimePickerImpl(this);
    }

    /**
     * Makes the node behind a developer's {@link CustomComponent}.
     *
     * @param owner the component
     * @param hooks the component's hooks
     * @return a new {@link Node}
     */
    @Override
    public ComponentPeer createPeer(final CustomComponent<?> owner, final ComponentHooks hooks) {
        return new Node(this, owner, new ExtensionHooks(hooks));
    }

    /**
     * Makes the node behind a developer's {@link CustomComposite} or {@link CustomContainer}. The children of a
     * composite that is not a container are private parts, whose control events stop at it.
     *
     * @param owner the component
     * @param hooks the component's hooks
     * @return a new {@link ContainerNode}
     */
    @Override
    public ContainerPeer createContainerPeer(final CustomComponent<?> owner, final ComponentHooks hooks) {
        return new ContainerNode(this, owner, new ExtensionHooks(hooks),
                owner instanceof CustomComposite<?> && !(owner instanceof CustomContainer<?>));
    }

    @Override
    public ComponentRegistry components() {
        return ComponentRegistryImpl.INSTANCE;
    }

    @Override
    public <T> T create(final Class<T> type) {
        return this.tryCreate(type).orElseThrow(() -> new IllegalStateException("No component is registered for "
                + type.getName() + "; is the library that provides it installed?"));
    }

    @Override
    public <T> Optional<T> tryCreate(final Class<T> type) {
        return ComponentRegistryImpl.INSTANCE.entry(type).map(entry -> type.cast(this.make(entry, type)));
    }

    @Override
    public Component<?> create(final ComponentId id) {
        return this.tryCreate(id).orElseThrow(() -> new IllegalStateException("No component is registered as " + id
                + "; is the library that provides it installed?"));
    }

    @Override
    public Optional<Component<?>> tryCreate(final ComponentId id) {
        return ComponentRegistryImpl.INSTANCE.entry(id).map(entry -> this.make(entry, entry.type()));
    }

    /**
     * Calls a registration's maker and checks that the result is a component of the registered type and of the type
     * asked for, made with this factory.
     */
    private Component<?> make(final ComponentRegistryImpl.Entry entry, final Class<?> asked) {
        final Component<?> component = entry.maker().make(this);
        if (component == null) {
            throw new IllegalStateException("The maker registered for " + entry.describe() + " returned null");
        }
        if (!entry.type().isInstance(component) || !asked.isInstance(component)) {
            throw new IllegalStateException("The maker registered for " + entry.describe() + " returned a "
                    + component.getClass().getName() + ", which is not a " + entry.type().getName());
        }
        try {
            this.nodeOf(component);
        } catch (final IllegalArgumentException e) {
            throw new IllegalStateException("The maker registered for " + entry.describe()
                    + " must make its component with the factory it is given", e);
        }
        return component;
    }

    /**
     * Returns the node behind a component. Every core path that takes a public component goes through here. It
     * accepts Strata's own components, which extend {@link BuiltInComponent}, and a developer's, which extend
     * {@link CustomComponent} and have a node as their peer.
     *
     * @param component the component
     * @return its node
     * @throws IllegalArgumentException if the component has no core node behind it (it implements a component
     *     interface directly), or another host's factory made it
     */
    public Node nodeOf(final Component<?> component) {
        Objects.requireNonNull(component, "component");
        final Node node;
        if (component instanceof BuiltInComponent<?> builtIn) {
            node = builtIn.node();
        } else if (component instanceof CustomComponent<?> custom && custom.peer() instanceof Node peer) {
            node = peer;
        } else {
            throw new IllegalArgumentException(component.getClass().getName() + " has no Strata implementation behind it. "
                    + "Make components with ComponentFactory, or extend CustomComponent; do not implement component interfaces directly.");
        }
        if (node.factory() != this) {
            throw new IllegalArgumentException(component + " was made by a different ComponentFactory; components cannot move between hosts");
        }
        return node;
    }
}
