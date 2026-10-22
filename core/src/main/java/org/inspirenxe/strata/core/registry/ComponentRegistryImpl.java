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
package org.inspirenxe.strata.core.registry;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.component.registry.ComponentLibrary;
import org.inspirenxe.strata.api.component.registry.ComponentMaker;
import org.inspirenxe.strata.api.component.registry.ComponentRegistry;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.control.button.DropDownButton;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
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
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.field.TimePicker;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.NavigationView;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.Spacer;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.WrapPanel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * The core {@link ComponentRegistry}. Each type and each id keeps a stack of registrations, newest on top, and every
 * registration has a sequence number. A lookup by type takes the top of the type's own stack, else, of the tops of
 * every stack whose type is a subtype of it, the one with the highest sequence number. Strata's own controls and
 * panels are registered first, so a library's registration for a subtype wins over them.
 *
 * <p>The first call to any public method registers the {@link ComponentLibrary} services seen by Strata's class
 * loader and by the calling thread's context class loader. A library is registered at most once per class. When one
 * throws, or cannot be loaded, the registrations it made are removed and the error is logged at
 * {@link System.Logger.Level#ERROR}. Every method holds the registry's lock, including while a library registers.</p>
 */
public final class ComponentRegistryImpl implements ComponentRegistry {
    /**
     * The one registry.
     */
    public static final ComponentRegistryImpl INSTANCE = new ComponentRegistryImpl();

    private static final System.Logger LOGGER = System.getLogger(ComponentRegistryImpl.class.getName());

    private final Map<Class<?>, Deque<Entry>> byType = new HashMap<>();
    private final Map<ComponentId, Deque<Entry>> byId = new HashMap<>();
    private final Set<Class<?>> builtInTypes = new HashSet<>();
    private final Set<Class<?>> libraries = new HashSet<>();
    private long sequence;
    private boolean loaded;

    /**
     * One registration: what {@link ComponentFactory#create(Class)} needs to make the component and check it.
     *
     * @param id the id, or {@code null} for a registration by type alone
     * @param type the registered type
     * @param maker the maker
     * @param sequence the order of registration, higher for newer
     */
    public record Entry(ComponentId id, Class<?> type, ComponentMaker<? extends Component<?>> maker, long sequence) {
        /**
         * Returns the id if there is one, else the type's name, for messages.
         *
         * @return a name for the registration
         */
        public String describe() {
            return this.id == null ? this.type.getName() : this.id + " (" + this.type.getName() + ")";
        }
    }

    private ComponentRegistryImpl() {
        this.builtIn("anchor_panel", AnchorPanel.class, ComponentFactory::anchorPanel);
        this.builtIn("stack_panel", StackPanel.class, ComponentFactory::stackPanel);
        this.builtIn("grid_panel", GridPanel.class, ComponentFactory::gridPanel);
        this.builtIn("wrap_panel", WrapPanel.class, ComponentFactory::wrapPanel);
        this.builtIn("absolute_panel", AbsolutePanel.class, ComponentFactory::absolutePanel);
        this.builtIn("scroll_panel", ScrollPanel.class, ComponentFactory::scrollPanel);
        this.builtIn("spacer", Spacer.class, ComponentFactory::spacer);
        this.builtIn("label", Label.class, ComponentFactory::label);
        this.builtIn("image", Image.class, ComponentFactory::image);
        this.builtIn("avatar", Avatar.class, ComponentFactory::avatar);
        this.builtIn("info_bar", InfoBar.class, ComponentFactory::infoBar);
        this.builtIn("progress_bar", ProgressBar.class, ComponentFactory::progressBar);
        this.builtIn("busy_indicator", BusyIndicator.class, ComponentFactory::busyIndicator);
        this.builtIn("separator", Separator.class, ComponentFactory::separator);
        this.builtIn("button", Button.class, ComponentFactory::button);
        this.builtIn("drop_down_button", DropDownButton.class, ComponentFactory::dropDownButton);
        this.builtIn("cycle_button", CycleButton.class, ComponentFactory::cycleButton);
        this.builtIn("check_box", CheckBox.class, ComponentFactory::checkBox);
        this.builtIn("toggle_switch", ToggleSwitch.class, ComponentFactory::toggleSwitch);
        this.builtIn("text_box", TextBox.class, ComponentFactory::textBox);
        this.builtIn("password_text_box", PasswordTextBox.class, ComponentFactory::passwordTextBox);
        this.builtIn("text_area", TextArea.class, ComponentFactory::textArea);
        this.builtIn("spinner", Spinner.class, ComponentFactory::spinner);
        this.builtIn("slider", Slider.class, ComponentFactory::slider);
        this.builtIn("color_picker", ColorPicker.class, ComponentFactory::colorPicker);
        this.builtIn("calendar_view", CalendarView.class, ComponentFactory::calendarView);
        this.builtIn("date_picker", DatePicker.class, ComponentFactory::datePicker);
        this.builtIn("time_picker", TimePicker.class, ComponentFactory::timePicker);
        this.builtIn("date_time_picker", DateTimePicker.class, ComponentFactory::dateTimePicker);
        this.builtIn("list_view", ListView.class, ComponentFactory::listView);
        this.builtIn("combo_box", ComboBox.class, ComponentFactory::comboBox);
        this.builtIn("table_view", TableView.class, ComponentFactory::tableView);
        this.builtIn("tree_view", TreeView.class, ComponentFactory::treeView);
        this.builtIn("grid_view", GridView.class, ComponentFactory::gridView);
        this.builtIn("menu", Menu.class, ComponentFactory::menu);
        this.builtIn("menu_bar", MenuBar.class, ComponentFactory::menuBar);
        this.builtIn("flyout", Flyout.class, ComponentFactory::flyout);
        this.builtIn("tab_bar", TabBar.class, ComponentFactory::tabBar);
        this.builtIn("expander", Expander.class, ComponentFactory::expander);
        this.builtIn("split_pane", SplitPane.class, ComponentFactory::splitPane);
        this.builtIn("scroll_bar", ScrollBar.class, ComponentFactory::scrollBar);
        this.builtIn("navigation_view", NavigationView.class, ComponentFactory::navigationView);
        this.builtIn("window", Window.class, ComponentFactory::window);
        this.builtIn("message_box", MessageBox.class, ComponentFactory::messageBox);
    }

    @Override
    public <T extends Component<?>> Subscription register(final Class<T> type, final ComponentMaker<? extends T> maker) {
        return this.add(null, type, maker);
    }

    @Override
    public <T extends Component<?>> Subscription register(final ComponentId id, final Class<T> type,
            final ComponentMaker<? extends T> maker) {
        Objects.requireNonNull(id, "id");
        if (id.namespace().equals(ComponentId.STRATA)) {
            throw new IllegalArgumentException("The " + ComponentId.STRATA + " namespace is Strata's own: " + id);
        }
        return this.add(id, type, maker);
    }

    @Override
    public <T> Optional<ComponentMaker<? extends Component<?>>> find(final Class<T> type) {
        return this.entry(type).map(Entry::maker);
    }

    @Override
    public Optional<ComponentMaker<? extends Component<?>>> find(final ComponentId id) {
        return this.entry(id).map(Entry::maker);
    }

    @Override
    public boolean has(final Class<?> type) {
        return this.entry(type).isPresent();
    }

    @Override
    public boolean has(final ComponentId id) {
        return this.entry(id).isPresent();
    }

    @Override
    public synchronized Set<ComponentId> ids() {
        this.loadOnce();
        final Set<ComponentId> ids = new HashSet<>();
        this.byId.forEach((id, stack) -> {
            if (!stack.isEmpty()) {
                ids.add(id);
            }
        });
        return Set.copyOf(ids);
    }

    @Override
    public synchronized void load(final ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        this.loadOnce();
        this.loadFrom(loader);
    }

    /**
     * Returns the registration {@code create(type)} uses.
     *
     * @param type the type asked for
     * @return the registration for the type, else the newest for a subtype of it, or empty if there is none
     */
    public synchronized Optional<Entry> entry(final Class<?> type) {
        Objects.requireNonNull(type, "type");
        this.loadOnce();
        final Deque<Entry> own = this.byType.get(type);
        if (own != null && !own.isEmpty()) {
            return Optional.of(own.peekFirst());
        }
        Entry best = null;
        for (final Deque<Entry> stack : this.byType.values()) {
            final Entry top = stack.peekFirst();
            if (top != null && type.isAssignableFrom(top.type()) && (best == null || top.sequence() > best.sequence())) {
                best = top;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Returns the newest registration under an id.
     *
     * @param id the id
     * @return the registration, or empty if there is none
     */
    public synchronized Optional<Entry> entry(final ComponentId id) {
        Objects.requireNonNull(id, "id");
        this.loadOnce();
        final Deque<Entry> stack = this.byId.get(id);
        return stack == null ? Optional.empty() : Optional.ofNullable(stack.peekFirst());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void builtIn(final String path, final Class<?> type, final ComponentMaker maker) {
        this.builtInTypes.add(type);
        this.push(new Entry(ComponentId.of(ComponentId.STRATA, path), type, maker, this.sequence++));
    }

    private synchronized Subscription add(final ComponentId id, final Class<?> type,
            final ComponentMaker<? extends Component<?>> maker) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(maker, "maker");
        if (this.builtInTypes.contains(type)) {
            throw new IllegalArgumentException(type.getName() + " is one of Strata's own controls; register a type of your own");
        }
        this.loadOnce();
        final Entry entry = new Entry(id, type, maker, this.sequence++);
        this.push(entry);
        return () -> {
            synchronized (ComponentRegistryImpl.this) {
                this.byType.get(type).removeFirstOccurrence(entry);
                if (id != null) {
                    this.byId.get(id).removeFirstOccurrence(entry);
                }
            }
        };
    }

    private void push(final Entry entry) {
        this.byType.computeIfAbsent(entry.type(), key -> new ArrayDeque<>()).push(entry);
        if (entry.id() != null) {
            this.byId.computeIfAbsent(entry.id(), key -> new ArrayDeque<>()).push(entry);
        }
    }

    private void loadOnce() {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        this.loadFrom(ComponentRegistryImpl.class.getClassLoader());
        final ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            this.loadFrom(context);
        }
    }

    private void loadFrom(final ClassLoader loader) {
        final List<ServiceLoader.Provider<ComponentLibrary>> found = new ArrayList<>();
        try {
            ServiceLoader.load(ComponentLibrary.class, loader).stream().forEach(found::add);
        } catch (final ServiceConfigurationError error) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not list the component libraries in " + loader, error);
        }
        for (final ServiceLoader.Provider<ComponentLibrary> provider : found) {
            if (this.libraries.add(provider.type())) {
                this.registerLibrary(provider);
            }
        }
    }

    private void registerLibrary(final ServiceLoader.Provider<ComponentLibrary> provider) {
        final List<Subscription> made = new ArrayList<>();
        final ComponentRegistry recording = new Recording(made);
        try {
            provider.get().register(recording);
        } catch (final RuntimeException | ServiceConfigurationError | LinkageError error) {
            made.forEach(Subscription::unsubscribe);
            LOGGER.log(System.Logger.Level.ERROR, "Component library " + provider.type().getName()
                    + " failed to register; its components are not available", error);
        }
    }

    /**
     * What a library registers with: the registry, remembering each subscription so a failed library's
     * registrations can be removed.
     */
    private final class Recording implements ComponentRegistry {
        private final List<Subscription> made;

        Recording(final List<Subscription> made) {
            this.made = made;
        }

        @Override
        public <T extends Component<?>> Subscription register(final Class<T> type, final ComponentMaker<? extends T> maker) {
            final Subscription subscription = ComponentRegistryImpl.this.register(type, maker);
            this.made.add(subscription);
            return subscription;
        }

        @Override
        public <T extends Component<?>> Subscription register(final ComponentId id, final Class<T> type,
                final ComponentMaker<? extends T> maker) {
            final Subscription subscription = ComponentRegistryImpl.this.register(id, type, maker);
            this.made.add(subscription);
            return subscription;
        }

        @Override
        public <T> Optional<ComponentMaker<? extends Component<?>>> find(final Class<T> type) {
            return ComponentRegistryImpl.this.find(type);
        }

        @Override
        public Optional<ComponentMaker<? extends Component<?>>> find(final ComponentId id) {
            return ComponentRegistryImpl.this.find(id);
        }

        @Override
        public boolean has(final Class<?> type) {
            return ComponentRegistryImpl.this.has(type);
        }

        @Override
        public boolean has(final ComponentId id) {
            return ComponentRegistryImpl.this.has(id);
        }

        @Override
        public Set<ComponentId> ids() {
            return ComponentRegistryImpl.this.ids();
        }

        @Override
        public void load(final ClassLoader loader) {
            ComponentRegistryImpl.this.load(loader);
        }
    }
}
