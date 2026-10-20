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
package org.inspirenxe.strata.api;

import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableColumn;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.CheckMenuItem;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.menu.MenuItem;
import org.inspirenxe.strata.api.control.menu.MenuSeparator;
import org.inspirenxe.strata.api.control.menu.SubmenuItem;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.Tab;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.peer.ComponentHooks;
import org.inspirenxe.strata.api.extension.peer.ComponentPeer;
import org.inspirenxe.strata.api.extension.peer.ContainerPeer;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.Spacer;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.Text;
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
     * The margin, in UI units, on each side of the text in a row's default cell when the theme sets no
     * {@value org.inspirenxe.strata.api.control.collection.ListView#STYLE_CELL_PADDING}; see
     * {@link #cellLabel(String)}.
     */
    float CELL_PADDING = 4f;


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
     * Makes a {@link Spacer} with no size, for taking the room left in a stack panel through a
     * {@linkplain StackPanel#setWeight weight}.
     *
     * @return a new spacer
     */
    Spacer spacer();

    /**
     * Makes a {@link Spacer} with a fixed size, as {@code spacer().setSize(width, height)} does.
     *
     * @param width the width in UI units
     * @param height the height in UI units
     * @return a new spacer
     */
    default Spacer spacer(final float width, final float height) {
        return this.spacer().setSize(width, height);
    }

    /**
     * Makes a {@link Label} with no text, as {@code label("")} does.
     *
     * @return a new label
     */
    default Label label() {
        return this.label("");
    }

    /**
     * Makes a {@link Label}.
     *
     * @param text the text; {@code \n} starts a new line
     * @return a new label
     */
    Label label(Text text);

    /**
     * Does the same as {@link #label(Text)} with literal text.
     *
     * @param text the text; {@code \n} starts a new line
     * @return a new label
     */
    default Label label(final String text) {
        return this.label(Text.of(text));
    }

    /**
     * Makes the label a list, tree or table row shows for text by default: vertically centered, cut short with an
     * ellipsis when it does not fit, and with the theme's
     * {@value org.inspirenxe.strata.api.control.collection.ListView#STYLE_CELL_PADDING} as its margin, or
     * {@value #CELL_PADDING} units on the left and right without it. The margin follows the theme until
     * {@link Label#setMargin} is called. Use it in your own cell factories so text rows line up with the built-in
     * ones.
     *
     * @param text the text
     * @return a new label
     */
    Label cellLabel(Text text);

    /**
     * Does the same as {@link #cellLabel(Text)} with literal text.
     *
     * @param text the text
     * @return a new label
     */
    default Label cellLabel(final String text) {
        return this.cellLabel(Text.of(text));
    }

    /**
     * Returns the appearances registered for the process, the same registry as
     * {@link Strata#appearances()}.
     *
     * @return the registry
     */
    default Appearances appearances() {
        return Strata.appearances();
    }

    /**
     * Makes the row a list, tree or grid shows for a value by default: the value's icon, if its appearance has one,
     * and its name in a {@linkplain #cellLabel(Text) cell label}. The icon's margin is the left, top and bottom of
     * the cell padding the label uses, so the two line up. The row shows the value's tooltip, asked anew each time
     * the pointer arrives.
     *
     * @param value the value
     * @return a new row
     */
    Component<?> cell(Object value);

    /**
     * Makes an {@link Image} that draws nothing until it is given a {@linkplain Image#setSprite sprite} or a
     * {@linkplain Image#setVisual visual}, 16 by 16 units until {@linkplain Image#setImageSize resized}.
     *
     * @return a new image
     */
    Image image();

    /**
     * Makes an {@link Image}, 16 by 16 units until {@linkplain Image#setImageSize resized}.
     *
     * @param sprite the sprite to draw
     * @return a new image
     */
    Image image(SpriteId sprite);

    /**
     * Makes an {@link Image} of an engine visual, at the visual's natural size.
     *
     * @param visual the visual to draw
     * @return a new image
     */
    Image image(Visual visual);

    /**
     * Makes an {@link Image} of a value's icon, with the value's tooltip; see {@link Image#setValue}.
     *
     * @param value the value
     * @return a new image
     */
    default Image image(final Object value) {
        return this.image().setValue(value);
    }

    /**
     * Makes a {@link Button} with no caption, as {@code button("")} does.
     *
     * @return a new button
     */
    default Button button() {
        return this.button("");
    }

    /**
     * Makes a {@link Button}.
     *
     * @param text the caption
     * @return a new button
     */
    Button button(Text text);

    /**
     * Does the same as {@link #button(Text)} with literal text.
     *
     * @param text the caption
     * @return a new button
     */
    default Button button(final String text) {
        return this.button(Text.of(text));
    }

    /**
     * Makes an unchecked {@link CheckBox} with no caption, as {@code checkBox("")} does.
     *
     * @return a new checkbox
     */
    default CheckBox checkBox() {
        return this.checkBox("");
    }

    /**
     * Makes an unchecked {@link CheckBox}.
     *
     * @param text the caption
     * @return a new checkbox
     */
    CheckBox checkBox(Text text);

    /**
     * Does the same as {@link #checkBox(Text)} with literal text.
     *
     * @param text the caption
     * @return a new checkbox
     */
    default CheckBox checkBox(final String text) {
        return this.checkBox(Text.of(text));
    }

    /**
     * Makes a {@link Slider} over the range 0 to 1, at 0, with no step.
     *
     * @return a new slider
     */
    Slider slider();

    /**
     * Makes an empty {@link TextBox}.
     *
     * @return a new text box
     */
    TextBox textBox();

    /**
     * Makes an empty {@link PasswordTextBox} that the user may reveal, with no length limit and {@code *} as its
     * mask.
     *
     * @return a new password box
     */
    PasswordTextBox passwordTextBox();

    /**
     * Makes an empty {@link ListView} whose rows show each item's {@linkplain #cell(Object) appearance}. Set other
     * cells with {@code setCellFactory}.
     *
     * @param <T> the type of the items
     * @return a new list
     */
    default <T> ListView<T> listView() {
        return this.listView(this::cell);
    }

    /**
     * Makes an empty {@link ListView}.
     *
     * @param <T> the type of the items
     * @param cellFactory makes the component for an item's row
     * @return a new list
     */
    <T> ListView<T> listView(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Makes a {@link ComboBox} with no options that shows each option by its appearance's name.
     *
     * @param <T> the type of the options
     * @return a new combo box
     */
    default <T> ComboBox<T> comboBox() {
        return this.comboBox(Function.identity());
    }

    /**
     * Makes a {@link ComboBox} with no options.
     *
     * @param <T> the type of the options
     * @param labelFunction returns what to show for an option: a {@link Text}, or any other value, shown by its
     *     appearance's name
     * @return a new combo box
     */
    <T> ComboBox<T> comboBox(Function<? super T, ?> labelFunction);

    /**
     * Makes an empty {@link Window} with no title, as {@code window("")} does.
     *
     * @return a new window
     */
    default Window window() {
        return this.window("");
    }

    /**
     * Makes an empty {@link Window}.
     *
     * @param title the title
     * @return a new window
     */
    Window window(Text title);

    /**
     * Does the same as {@link #window(Text)} with literal text.
     *
     * @param title the title
     * @return a new window
     */
    default Window window(final String title) {
        return this.window(Text.of(title));
    }

    /**
     * Makes a {@link MessageBox} with no title, no message and an OK button, as
     * {@code messageBox("", "", MessageBox.Buttons.OK)} does. Show it with {@link Overlays#dialog}.
     *
     * @return a new message box
     */
    default MessageBox messageBox() {
        return this.messageBox("", "", MessageBox.Buttons.OK);
    }

    /**
     * Makes a {@link MessageBox}. Show it with {@link Overlays#dialog}.
     *
     * @param title the window's title
     * @param message the message; it wraps, and {@code \n} starts a new line
     * @param buttons which buttons it has
     * @return a new message box
     */
    MessageBox messageBox(Text title, Text message, MessageBox.Buttons buttons);

    /**
     * Does the same as {@link #messageBox(Text, Text, MessageBox.Buttons)} with literal text.
     *
     * @param title the window's title
     * @param message the message; it wraps, and {@code \n} starts a new line
     * @param buttons which buttons it has
     * @return a new message box
     */
    default MessageBox messageBox(final String title, final String message, final MessageBox.Buttons buttons) {
        return this.messageBox(Text.of(title), Text.of(message), buttons);
    }

    /**
     * Makes a {@link TabBar} with no tabs.
     *
     * @return a new tab bar
     */
    TabBar tabBar();

    /**
     * Makes an enabled {@link Tab} with no text, as {@code tab("")} does.
     *
     * @return a new tab
     */
    default Tab tab() {
        return this.tab("");
    }

    /**
     * Makes an enabled {@link Tab} with no tooltip.
     *
     * @param text the tab's text
     * @return a new tab
     */
    Tab tab(Text text);

    /**
     * Does the same as {@link #tab(Text)} with literal text.
     *
     * @param text the tab's text
     * @return a new tab
     */
    default Tab tab(final String text) {
        return this.tab(Text.of(text));
    }

    /**
     * Makes an empty {@link ProgressBar}.
     *
     * @return a new progress bar
     */
    ProgressBar progressBar();

    /**
     * Makes a horizontal {@link Separator}, as {@code separator(Orientation.HORIZONTAL)} does.
     *
     * @return a new separator
     */
    default Separator separator() {
        return this.separator(Orientation.HORIZONTAL);
    }

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
     * Makes a {@link ToggleSwitch}, off, with no caption, as {@code toggleSwitch("")} does.
     *
     * @return a new switch
     */
    default ToggleSwitch toggleSwitch() {
        return this.toggleSwitch("");
    }

    /**
     * Makes a {@link ToggleSwitch} that is off.
     *
     * @param text the caption
     * @return a new switch
     */
    ToggleSwitch toggleSwitch(Text text);

    /**
     * Does the same as {@link #toggleSwitch(Text)} with literal text.
     *
     * @param text the caption
     * @return a new switch
     */
    default ToggleSwitch toggleSwitch(final String text) {
        return this.toggleSwitch(Text.of(text));
    }

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
     * Makes a {@link TableColumn} with no title, as {@code tableColumn("")} does.
     *
     * @param <T> the type of the table's items
     * @return a new column
     */
    default <T> TableColumn<T> tableColumn() {
        return this.tableColumn("");
    }

    /**
     * Makes a {@link TableColumn}, weighted 1, that shows {@link String#valueOf(Object)} of each item as text and is
     * not sortable.
     *
     * @param <T> the type of the table's items
     * @param title the header title
     * @return a new column
     */
    <T> TableColumn<T> tableColumn(Text title);

    /**
     * Does the same as {@link #tableColumn(Text)} with literal text.
     *
     * @param <T> the type of the table's items
     * @param title the header title
     * @return a new column
     */
    default <T> TableColumn<T> tableColumn(final String title) {
        return this.tableColumn(Text.of(title));
    }

    /**
     * Makes a {@link TreeView} with no roots and no children function, whose cells are labels showing
     * {@link String#valueOf(Object)} of each item. Set other cells with {@code setCellFactory}.
     *
     * @param <T> the type of the items
     * @return a new tree
     */
    default <T> TreeView<T> treeView() {
        return this.treeView(this::cell);
    }

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
     * Makes an enabled {@link MenuItem} with no text and an action that does nothing, as {@code menuItem("")} does.
     *
     * @return a new item
     */
    default MenuItem menuItem() {
        return this.menuItem("");
    }

    /**
     * Makes an enabled {@link MenuItem} with an action that does nothing.
     *
     * @param text the item's text
     * @return a new item
     */
    MenuItem menuItem(Text text);

    /**
     * Does the same as {@link #menuItem(Text)} with literal text.
     *
     * @param text the item's text
     * @return a new item
     */
    default MenuItem menuItem(final String text) {
        return this.menuItem(Text.of(text));
    }

    /**
     * Makes an enabled, unchecked {@link CheckMenuItem} with no text and an action that does nothing, as
     * {@code checkMenuItem("")} does.
     *
     * @return a new item
     */
    default CheckMenuItem checkMenuItem() {
        return this.checkMenuItem("");
    }

    /**
     * Makes an enabled, unchecked {@link CheckMenuItem} with an action that does nothing.
     *
     * @param text the item's text
     * @return a new item
     */
    CheckMenuItem checkMenuItem(Text text);

    /**
     * Does the same as {@link #checkMenuItem(Text)} with literal text.
     *
     * @param text the item's text
     * @return a new item
     */
    default CheckMenuItem checkMenuItem(final String text) {
        return this.checkMenuItem(Text.of(text));
    }

    /**
     * Makes an enabled {@link SubmenuItem} with no text and no menu, as {@code submenuItem("")} does.
     *
     * @return a new item
     */
    default SubmenuItem submenuItem() {
        return this.submenuItem("");
    }

    /**
     * Makes an enabled {@link SubmenuItem} with no menu.
     *
     * @param text the item's text, or a menu bar's title
     * @return a new item
     */
    SubmenuItem submenuItem(Text text);

    /**
     * Does the same as {@link #submenuItem(Text)} with literal text.
     *
     * @param text the item's text, or a menu bar's title
     * @return a new item
     */
    default SubmenuItem submenuItem(final String text) {
        return this.submenuItem(Text.of(text));
    }

    /**
     * Makes a {@link MenuSeparator}.
     *
     * @return a new separator
     */
    MenuSeparator menuSeparator();

    /**
     * Makes a collapsed {@link Expander} with no title and no content, as {@code expander("")} does.
     *
     * @return a new expander
     */
    default Expander expander() {
        return this.expander("");
    }

    /**
     * Makes a collapsed {@link Expander} with no content.
     *
     * @param title the header's title
     * @return a new expander
     */
    Expander expander(Text title);

    /**
     * Does the same as {@link #expander(Text)} with literal text.
     *
     * @param title the header's title
     * @return a new expander
     */
    default Expander expander(final String title) {
        return this.expander(Text.of(title));
    }

    /**
     * Makes an empty, side by side {@link SplitPane}, as {@code splitPane(Orientation.HORIZONTAL)} does.
     *
     * @return a new split pane
     */
    default SplitPane splitPane() {
        return this.splitPane(Orientation.HORIZONTAL);
    }

    /**
     * Makes an empty {@link SplitPane} with the divider in the middle.
     *
     * @param orientation {@link Orientation#HORIZONTAL} for side by side, {@link Orientation#VERTICAL} for one
     *                    above the other
     * @return a new split pane
     */
    SplitPane splitPane(Orientation orientation);

    /**
     * Makes a {@link GridView} with no items, whose cells show each item's {@linkplain #cell(Object) appearance}. Set
     * other cells with {@code setCellFactory}.
     *
     * @param <T> the type of the items
     * @return a new grid
     */
    default <T> GridView<T> gridView() {
        return this.gridView(this::cell);
    }

    /**
     * Makes a {@link GridView} with no cells, {@value GridView#DEFAULT_COLUMNS} columns wide.
     *
     * @param <T> the type of the items
     * @param cellFactory makes the component shown in a filled cell
     * @return a new grid
     */
    <T> GridView<T> gridView(Function<? super T, ? extends Component<?>> cellFactory);

    /**
     * Makes a {@link CycleButton} with no label and no values that shows each value as
     * {@link String#valueOf(Object)}.
     *
     * @param <T> the type of the values
     * @return a new cycle button
     */
    default <T> CycleButton<T> cycleButton() {
        return this.cycleButton("");
    }

    /**
     * Makes a {@link CycleButton} with no values that shows each value as {@link String#valueOf(Object)}.
     *
     * @param <T> the type of the values
     * @param text the label shown before the value
     * @return a new cycle button
     */
    default <T> CycleButton<T> cycleButton(final Text text) {
        return this.cycleButton(text, Function.identity());
    }

    /**
     * Does the same as {@link #cycleButton(Text)} with literal text.
     *
     * @param <T> the type of the values
     * @param text the label shown before the value
     * @return a new cycle button
     */
    default <T> CycleButton<T> cycleButton(final String text) {
        return this.cycleButton(Text.of(text));
    }

    /**
     * Makes a {@link CycleButton} with no values.
     *
     * @param <T> the type of the values
     * @param text the label shown before the value
     * @param valueLabelFunction returns what to show for a value: a {@link Text}, or any other value, shown by its
     *     appearance's name
     * @return a new cycle button
     */
    <T> CycleButton<T> cycleButton(Text text, Function<? super T, ?> valueLabelFunction);

    /**
     * Does the same as {@link #cycleButton(Text, Function)} with literal text.
     *
     * @param <T> the type of the values
     * @param text the label shown before the value
     * @param valueLabelFunction returns what to show for a value: a {@link Text}, or any other value, shown by its
     *     appearance's name
     * @return a new cycle button
     */
    default <T> CycleButton<T> cycleButton(final String text, final Function<? super T, ?> valueLabelFunction) {
        return this.cycleButton(Text.of(text), valueLabelFunction);
    }

    /**
     * Makes a {@link Slider} over the range 0 to 1, at 0, with no step, running the given way.
     *
     * @param orientation {@link Orientation#HORIZONTAL} for a slider across, {@link Orientation#VERTICAL} for one up
     * @return a new slider
     */
    default Slider slider(final Orientation orientation) {
        return this.slider().setOrientation(orientation);
    }

    /**
     * Makes a vertical {@link ScrollBar} with nothing to scroll. Give it lengths with
     * {@link ScrollBar#setExtent(double, double)}.
     *
     * @return a new scroll bar
     */
    ScrollBar scrollBar();

    /**
     * Makes a {@link ScrollBar} with nothing to scroll, running the given way.
     *
     * @param orientation {@link Orientation#VERTICAL} for content that scrolls up and down,
     *                    {@link Orientation#HORIZONTAL} for content that scrolls sideways
     * @return a new scroll bar
     */
    default ScrollBar scrollBar(final Orientation orientation) {
        return this.scrollBar().setOrientation(orientation);
    }

    /**
     * Makes a {@link BusyIndicator} with no caption.
     *
     * @return a new busy indicator
     */
    BusyIndicator busyIndicator();

    /**
     * Makes a {@link BusyIndicator} with a caption above its dots.
     *
     * @param text the caption
     * @return a new busy indicator
     */
    default BusyIndicator busyIndicator(final Text text) {
        return this.busyIndicator().setText(text);
    }

    /**
     * Does the same as {@link #busyIndicator(Text)} with literal text.
     *
     * @param text the caption
     * @return a new busy indicator
     */
    default BusyIndicator busyIndicator(final String text) {
        return this.busyIndicator(Text.of(text));
    }

    /**
     * Makes a {@link ColorPicker} set to opaque white, with the alpha strip hidden.
     *
     * @return a new color picker
     */
    ColorPicker colorPicker();

    /**
     * Creates the peer of a new leaf component. Called by {@link CustomComponent}'s constructor; do not call it
     * yourself.
     *
     * @param owner the component being constructed
     * @param hooks how the peer reaches the component's protected methods
     * @return the new peer
     */
    ComponentPeer createPeer(CustomComponent<?> owner, ComponentHooks hooks);

    /**
     * Creates the peer of a new container or composite. Called by
     * {@link org.inspirenxe.strata.api.extension.CustomComposite}'s constructor; do not call it yourself.
     *
     * @param owner the container being constructed
     * @param hooks how the peer reaches the container's protected methods
     * @return the new peer
     */
    ContainerPeer createContainerPeer(CustomComponent<?> owner, ComponentHooks hooks);
}
