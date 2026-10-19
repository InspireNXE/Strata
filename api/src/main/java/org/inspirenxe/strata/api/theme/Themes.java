package org.inspirenxe.strata.api.theme;

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.SlotGrid;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.TextStyle;

/**
 * Themes that ship with Strata. Engine adapters may offer more.
 */
public final class Themes {

    /**
     * The id {@link #flat()} is registered under on every engine. Theme files can name it as their parent.
     */
    public static final ThemeId FLAT = ThemeId.of("strata", "flat");

    private Themes() {
    }

    /**
     * Returns a dark theme made of solid colors only, so it works on any engine without image assets. It defines
     * every key of every built-in control.
     *
     * @return the flat theme
     */
    public static Theme flat() {
        return Theme.builder(FLAT.toString())
                .paint(Button.STYLE_NORMAL, Paint.solid(0xFF3C3F44))
                .paint(Button.STYLE_HOVERED, Paint.solid(0xFF4E525A))
                .paint(Button.STYLE_PRESSED, Paint.solid(0xFF2B2D31))
                .paint(Button.STYLE_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(Button.STYLE_FOCUS, Paint.solid(0xFF6A9FFF))
                .textStyle(Button.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFF0F0F0))
                .textStyle(Button.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF808080))
                .textStyle(Label.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .paint(CheckBox.STYLE_NORMAL, Paint.solid(0xFF1B1C1F))
                .paint(CheckBox.STYLE_HOVERED, Paint.solid(0xFF2E3136))
                .paint(CheckBox.STYLE_CHECKED, Paint.solid(0xFF1B1C1F))
                .paint(CheckBox.STYLE_CHECKED_HOVERED, Paint.solid(0xFF2E3136))
                .paint(CheckBox.STYLE_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(CheckBox.STYLE_CHECKED_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(CheckBox.STYLE_MARK, Paint.solid(0xFF6A9FFF))
                .paint(CheckBox.STYLE_MARK_DISABLED, Paint.solid(0xFF505050))
                .paint(CheckBox.STYLE_FOCUS, Paint.solid(0xFF6A9FFF))
                .textStyle(CheckBox.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .textStyle(CheckBox.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .paint(Slider.STYLE_TRACK, Paint.solid(0xFF1B1C1F))
                .paint(Slider.STYLE_TRACK_FOCUSED, Paint.solid(0xFF24262A))
                .paint(Slider.STYLE_TRACK_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(Slider.STYLE_THUMB, Paint.solid(0xFF5A5E66))
                .paint(Slider.STYLE_THUMB_HOVERED, Paint.solid(0xFF6A9FFF))
                .paint(Slider.STYLE_THUMB_DISABLED, Paint.solid(0xFF3A3A3A))
                .textStyle(Slider.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFF0F0F0))
                .textStyle(Slider.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .paint(TextBox.STYLE_NORMAL, Paint.solid(0xFF1B1C1F))
                .paint(TextBox.STYLE_FOCUSED, Paint.solid(0xFF24262A))
                .paint(TextBox.STYLE_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(TextBox.STYLE_SELECTION, Paint.solid(0x806A9FFF))
                .paint(TextBox.STYLE_CARET, Paint.solid(0xFFE0E0E0))
                .textStyle(TextBox.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .textStyle(TextBox.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .textStyle(TextBox.STYLE_TEXT_READ_ONLY, TextStyle.DEFAULT.withColor(0xFFC5C5C5))
                .paint(ScrollPanel.STYLE_THUMB, Paint.solid(0xFF5A5E66))
                .paint(ScrollPanel.STYLE_THUMB_HOVERED, Paint.solid(0xFF7A7F88))
                .paint(ScrollPanel.STYLE_TRACK, Paint.solid(0xFF1B1C1F))
                .paint(ListView.STYLE_ROW, Paint.solid(0xFF1F2024))
                .paint(ListView.STYLE_ROW_HOVERED, Paint.solid(0xFF2E3136))
                .paint(ListView.STYLE_ROW_SELECTED, Paint.solid(0xFF3A4D6E))
                .textStyle(ListView.STYLE_PLACEHOLDER, TextStyle.DEFAULT.withColor(0xFF808080))
                .paint(ComboBox.STYLE_NORMAL, Paint.solid(0xFF1B1C1F))
                .paint(ComboBox.STYLE_HOVERED, Paint.solid(0xFF2E3136))
                .paint(ComboBox.STYLE_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(ComboBox.STYLE_POPUP, Paint.solid(0xFF16171A))
                .textStyle(ComboBox.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .textStyle(ComboBox.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .paint(Window.STYLE_BACKGROUND, Paint.solid(0xF0141518))
                .paint(Window.STYLE_TITLE_BAR, Paint.solid(0xFF2B2D31))
                .textStyle(Window.STYLE_TITLE_TEXT, TextStyle.DEFAULT.withColor(0xFFFFFFFF))
                .textStyle(Window.STYLE_CLOSE, TextStyle.DEFAULT.withColor(0xFFA0A0A0))
                .textStyle(Window.STYLE_CLOSE_HOVERED, TextStyle.DEFAULT.withColor(0xFFFFFFFF))
                .paint(TabBar.STYLE_NORMAL, Paint.solid(0xFF24262A))
                .paint(TabBar.STYLE_HOVERED, Paint.solid(0xFF3C3F44))
                .paint(TabBar.STYLE_SELECTED, Paint.solid(0xFF3A4D6E))
                .paint(TabBar.STYLE_DISABLED, Paint.solid(0xFF1B1C1F))
                .textStyle(TabBar.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFA0A0A0))
                .textStyle(TabBar.STYLE_TEXT_SELECTED, TextStyle.DEFAULT.withColor(0xFFFFFFFF))
                .textStyle(TabBar.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF606060))
                .paint(ProgressBar.STYLE_TRACK, Paint.solid(0xFF1B1C1F))
                .paint(ProgressBar.STYLE_FILL, Paint.solid(0xFF2E7D32))
                .textStyle(ProgressBar.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFF0F0F0))
                .paint(Separator.STYLE_LINE, Paint.solid(0xFF3C3F44))
                .paint(RadioButton.STYLE_NORMAL, Paint.solid(0xFF1B1C1F))
                .paint(RadioButton.STYLE_HOVERED, Paint.solid(0xFF2E3136))
                .paint(RadioButton.STYLE_SELECTED, Paint.solid(0xFF1B1C1F))
                .paint(RadioButton.STYLE_SELECTED_HOVERED, Paint.solid(0xFF2E3136))
                .paint(RadioButton.STYLE_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(RadioButton.STYLE_SELECTED_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(RadioButton.STYLE_MARK, Paint.solid(0xFF6A9FFF))
                .paint(RadioButton.STYLE_MARK_DISABLED, Paint.solid(0xFF505050))
                .paint(RadioButton.STYLE_FOCUS, Paint.solid(0xFF6A9FFF))
                .textStyle(RadioButton.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .textStyle(RadioButton.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .paint(ToggleSwitch.STYLE_TRACK, Paint.solid(0xFF3C3F44))
                .paint(ToggleSwitch.STYLE_TRACK_ON, Paint.solid(0xFF3A4D6E))
                .paint(ToggleSwitch.STYLE_TRACK_DISABLED, Paint.solid(0xFF2A2A2A))
                .paint(ToggleSwitch.STYLE_KNOB, Paint.solid(0xFF8A8F98))
                .paint(ToggleSwitch.STYLE_KNOB_HOVERED, Paint.solid(0xFFE0E0E0))
                .paint(ToggleSwitch.STYLE_KNOB_DISABLED, Paint.solid(0xFF505050))
                .paint(ToggleSwitch.STYLE_FOCUS, Paint.solid(0xFF6A9FFF))
                .textStyle(ToggleSwitch.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .textStyle(ToggleSwitch.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .paint(TableView.STYLE_HEADER, Paint.solid(0xFF2B2D31))
                .paint(TableView.STYLE_HEADER_HOVERED, Paint.solid(0xFF3C3F44))
                .textStyle(TableView.STYLE_HEADER_TEXT, TextStyle.DEFAULT.withColor(0xFFC8C8C8))
                .paint(TableView.STYLE_GRID, Paint.solid(0xFF2E3136))
                .paint(TreeView.STYLE_ARROW, Paint.solid(0xFFA0A0A0))
                .paint(Menu.STYLE_BACKGROUND, Paint.solid(0xFF16171A))
                .paint(Menu.STYLE_ITEM_HOVERED, Paint.solid(0xFF3A4D6E))
                .textStyle(Menu.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .textStyle(Menu.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .textStyle(Menu.STYLE_SHORTCUT, TextStyle.DEFAULT.withColor(0xFF909090))
                .paint(Menu.STYLE_SEPARATOR, Paint.solid(0xFF3C3F44))
                .paint(MenuBar.STYLE_BACKGROUND, Paint.solid(0xFF24262A))
                .paint(MenuBar.STYLE_ITEM_HOVERED, Paint.solid(0xFF3C3F44))
                .textStyle(MenuBar.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .paint(Expander.STYLE_HEADER, Paint.solid(0xFF24262A))
                .paint(Expander.STYLE_HEADER_HOVERED, Paint.solid(0xFF2E3136))
                .textStyle(Expander.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFE0E0E0))
                .textStyle(Expander.STYLE_TEXT_DISABLED, TextStyle.DEFAULT.withColor(0xFF707070))
                .paint(SplitPane.STYLE_DIVIDER, Paint.solid(0xFF2B2D31))
                .paint(SplitPane.STYLE_DIVIDER_HOVERED, Paint.solid(0xFF6A9FFF))
                .paint(SlotGrid.STYLE_SLOT, Paint.solid(0xFF1B1C1F))
                .paint(SlotGrid.STYLE_SLOT_HOVERED, Paint.solid(0x40FFFFFF))
                .paint(SlotGrid.STYLE_SLOT_SELECTED, Paint.solid(0x606A9FFF))
                .paint(Overlays.STYLE_TOAST, Paint.solid(0xF02B2D31))
                .textStyle(Overlays.STYLE_TOAST_TEXT, TextStyle.DEFAULT.withColor(0xFFF0F0F0))
                .paint(Overlays.STYLE_BACKDROP, Paint.solid(0x60000000))
                .textStyle(Tooltip.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFFF0F0F0))
                .build();
    }
}
