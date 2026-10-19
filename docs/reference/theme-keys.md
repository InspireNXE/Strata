# Theme keys

Every style key the built-in controls read. A theme lacking a required key cannot show a tree that uses the control; see [Themes](../guides/theming.md#required-and-optional-keys).

## Button

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `button.normal` | `Button.STYLE_NORMAL` | paint | yes | |
| `button.hovered` | `Button.STYLE_HOVERED` | paint | yes | |
| `button.pressed` | `Button.STYLE_PRESSED` | paint | yes | |
| `button.disabled` | `Button.STYLE_DISABLED` | paint | yes | |
| `button.focus` | `Button.STYLE_FOCUS` | paint | no | No focus ring |
| `button.text` | `Button.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `button.text.disabled` | `Button.STYLE_TEXT_DISABLED` | text style | no | `button.text` |

## Label

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `label.text` | `Label.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |

A style set with `Label.setStyle` overrides the key.

## CheckBox

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `checkbox.normal` | `CheckBox.STYLE_NORMAL` | paint | yes | |
| `checkbox.hovered` | `CheckBox.STYLE_HOVERED` | paint | yes | |
| `checkbox.checked` | `CheckBox.STYLE_CHECKED` | paint | yes | |
| `checkbox.checked.hovered` | `CheckBox.STYLE_CHECKED_HOVERED` | paint | yes | |
| `checkbox.disabled` | `CheckBox.STYLE_DISABLED` | paint | no | `checkbox.normal` |
| `checkbox.checked.disabled` | `CheckBox.STYLE_CHECKED_DISABLED` | paint | no | `checkbox.checked` |
| `checkbox.mark` | `CheckBox.STYLE_MARK` | paint | no | No mark |
| `checkbox.mark.disabled` | `CheckBox.STYLE_MARK_DISABLED` | paint | no | `checkbox.mark` |
| `checkbox.focus` | `CheckBox.STYLE_FOCUS` | paint | no | No focus ring |
| `checkbox.text` | `CheckBox.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `checkbox.text.disabled` | `CheckBox.STYLE_TEXT_DISABLED` | text style | no | `checkbox.text` |

## Slider

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `slider.track` | `Slider.STYLE_TRACK` | paint | yes | |
| `slider.track.focused` | `Slider.STYLE_TRACK_FOCUSED` | paint | no | `slider.track` |
| `slider.track.disabled` | `Slider.STYLE_TRACK_DISABLED` | paint | no | `slider.track` |
| `slider.thumb` | `Slider.STYLE_THUMB` | paint | yes | |
| `slider.thumb.hovered` | `Slider.STYLE_THUMB_HOVERED` | paint | yes | |
| `slider.thumb.disabled` | `Slider.STYLE_THUMB_DISABLED` | paint | no | `slider.thumb` |
| `slider.text` | `Slider.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `slider.text.disabled` | `Slider.STYLE_TEXT_DISABLED` | text style | no | `slider.text` |

## TextBox

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `textbox.normal` | `TextBox.STYLE_NORMAL` | paint | yes | |
| `textbox.focused` | `TextBox.STYLE_FOCUSED` | paint | yes | |
| `textbox.disabled` | `TextBox.STYLE_DISABLED` | paint | yes | |
| `textbox.text` | `TextBox.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `textbox.text.disabled` | `TextBox.STYLE_TEXT_DISABLED` | text style | no | `textbox.text` |
| `textbox.text.readonly` | `TextBox.STYLE_TEXT_READ_ONLY` | text style | no | `textbox.text.disabled` |
| `textbox.selection` | `TextBox.STYLE_SELECTION` | paint | no | `0x806A9FFF`, translucent blue |
| `textbox.caret` | `TextBox.STYLE_CARET` | paint | no | `0xFFE0E0E0`, light gray |

## ScrollPanel

`ListView` and `ComboBox` use these keys for their scroll bars too.

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `scroll.thumb` | `ScrollPanel.STYLE_THUMB` | paint | yes | |
| `scroll.thumb.hovered` | `ScrollPanel.STYLE_THUMB_HOVERED` | paint | no | `scroll.thumb` |
| `scroll.track` | `ScrollPanel.STYLE_TRACK` | paint | no | Nothing |

## ScrollBar

A standard scroll bar reads the `scroll.*` keys above (`ScrollBar.STYLE_TRACK`, `STYLE_THUMB` and `STYLE_THUMB_HOVERED` are the same strings), plus these.

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `scroll.thumb.disabled` | `ScrollBar.STYLE_THUMB_DISABLED` | paint | no | `scroll.thumb` |
| `scroll.slim.track` | `ScrollBar.STYLE_SLIM_TRACK` | paint | no | `scroll.track` |
| `scroll.slim.thumb` | `ScrollBar.STYLE_SLIM_THUMB` | paint | no | `scroll.thumb` |
| `scroll.slim.thumb.hovered` | `ScrollBar.STYLE_SLIM_THUMB_HOVERED` | paint | no | `scroll.slim.thumb` |
| `scroll.slim.thumb.disabled` | `ScrollBar.STYLE_SLIM_THUMB_DISABLED` | paint | no | `scroll.slim.thumb` |

## ListView

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `list.row.selected` | `ListView.STYLE_ROW_SELECTED` | paint | yes | |
| `list.row` | `ListView.STYLE_ROW` | paint | no | Nothing |
| `list.row.hovered` | `ListView.STYLE_ROW_HOVERED` | paint | no | `list.row` |
| `list.placeholder` | `ListView.STYLE_PLACEHOLDER` | text style | no | `TextStyle.DEFAULT` |

## ComboBox

The open list also uses the `ListView` and `ScrollPanel` keys.

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `combobox.normal` | `ComboBox.STYLE_NORMAL` | paint | yes | |
| `combobox.popup` | `ComboBox.STYLE_POPUP` | paint | yes | |
| `combobox.hovered` | `ComboBox.STYLE_HOVERED` | paint | no | `combobox.normal` |
| `combobox.disabled` | `ComboBox.STYLE_DISABLED` | paint | no | `combobox.normal` |
| `combobox.text` | `ComboBox.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `combobox.text.disabled` | `ComboBox.STYLE_TEXT_DISABLED` | text style | no | `combobox.text` |

## Window

`MessageBox` uses these keys, plus those of `Label` and `Button`.

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `window.background` | `Window.STYLE_BACKGROUND` | paint | yes | |
| `window.title` | `Window.STYLE_TITLE_BAR` | paint | no | Nothing |
| `window.title.text` | `Window.STYLE_TITLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `window.close` | `Window.STYLE_CLOSE` | text style | no | `window.title.text` |
| `window.close.hovered` | `Window.STYLE_CLOSE_HOVERED` | text style | no | `window.close` |

## TabBar

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `tab.normal` | `TabBar.STYLE_NORMAL` | paint | yes | |
| `tab.selected` | `TabBar.STYLE_SELECTED` | paint | yes | |
| `tab.hovered` | `TabBar.STYLE_HOVERED` | paint | no | `tab.normal` |
| `tab.disabled` | `TabBar.STYLE_DISABLED` | paint | no | `tab.normal` |
| `tab.text` | `TabBar.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `tab.text.selected` | `TabBar.STYLE_TEXT_SELECTED` | text style | no | `tab.text` |
| `tab.text.disabled` | `TabBar.STYLE_TEXT_DISABLED` | text style | no | `tab.text` |

## ProgressBar and Separator

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `progress.track` | `ProgressBar.STYLE_TRACK` | paint | yes | |
| `progress.fill` | `ProgressBar.STYLE_FILL` | paint | yes | |
| `progress.text` | `ProgressBar.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `progress.segment` | `ProgressBar.STYLE_SEGMENT` | paint | no | `progress.fill` |
| `separator` | `Separator.STYLE_LINE` | paint | yes | |

## BusyIndicator

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `busy.dot` | `BusyIndicator.STYLE_DOT` | paint | yes | |
| `busy.dot.active` | `BusyIndicator.STYLE_DOT_ACTIVE` | paint | yes | |
| `busy.text` | `BusyIndicator.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |

## RadioButton

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `radio.normal` | `RadioButton.STYLE_NORMAL` | paint | yes | |
| `radio.hovered` | `RadioButton.STYLE_HOVERED` | paint | yes | |
| `radio.selected` | `RadioButton.STYLE_SELECTED` | paint | yes | |
| `radio.selected.hovered` | `RadioButton.STYLE_SELECTED_HOVERED` | paint | yes | |
| `radio.disabled` | `RadioButton.STYLE_DISABLED` | paint | no | `radio.normal` |
| `radio.selected.disabled` | `RadioButton.STYLE_SELECTED_DISABLED` | paint | no | `radio.selected` |
| `radio.mark` | `RadioButton.STYLE_MARK` | paint | no | No mark |
| `radio.mark.disabled` | `RadioButton.STYLE_MARK_DISABLED` | paint | no | `radio.mark` |
| `radio.focus` | `RadioButton.STYLE_FOCUS` | paint | no | No focus ring |
| `radio.text` | `RadioButton.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `radio.text.disabled` | `RadioButton.STYLE_TEXT_DISABLED` | text style | no | `radio.text` |

## ToggleSwitch

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `toggle.track` | `ToggleSwitch.STYLE_TRACK` | paint | yes | |
| `toggle.track.on` | `ToggleSwitch.STYLE_TRACK_ON` | paint | yes | |
| `toggle.knob` | `ToggleSwitch.STYLE_KNOB` | paint | yes | |
| `toggle.track.disabled` | `ToggleSwitch.STYLE_TRACK_DISABLED` | paint | no | `toggle.track` |
| `toggle.knob.hovered` | `ToggleSwitch.STYLE_KNOB_HOVERED` | paint | no | `toggle.knob` |
| `toggle.knob.disabled` | `ToggleSwitch.STYLE_KNOB_DISABLED` | paint | no | `toggle.knob` |
| `toggle.focus` | `ToggleSwitch.STYLE_FOCUS` | paint | no | No focus ring |
| `toggle.text` | `ToggleSwitch.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `toggle.text.disabled` | `ToggleSwitch.STYLE_TEXT_DISABLED` | text style | no | `toggle.text` |

## Spinner and TextArea

A spinner's field and a text area read the `TextBox` keys, and a text area the `ScrollPanel` keys for its bar.

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `spinner.button` | `Spinner.STYLE_BUTTON` | paint | no | `button.normal` |
| `spinner.button.hovered` | `Spinner.STYLE_BUTTON_HOVERED` | paint | no | `button.hovered` |
| `spinner.button.disabled` | `Spinner.STYLE_BUTTON_DISABLED` | paint | no | `button.disabled` |

The spinner's arrows use the colors of `button.text` and `button.text.disabled`.

## TableView and TreeView

Both read the `ListView` and `ScrollPanel` keys for their rows.

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `table.header` | `TableView.STYLE_HEADER` | paint | yes | |
| `table.header.hovered` | `TableView.STYLE_HEADER_HOVERED` | paint | no | `table.header` |
| `table.header.text` | `TableView.STYLE_HEADER_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `table.grid` | `TableView.STYLE_GRID` | paint | no | Nothing |
| `tree.arrow` | `TreeView.STYLE_ARROW` | paint | no | solid `0xFFA0A0A0` |

## Menu and MenuBar

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `menu.background` | `Menu.STYLE_BACKGROUND` | paint | yes | |
| `menu.item.hovered` | `Menu.STYLE_ITEM_HOVERED` | paint | yes | |
| `menu.text` | `Menu.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `menu.text.disabled` | `Menu.STYLE_TEXT_DISABLED` | text style | no | `menu.text` |
| `menu.shortcut` | `Menu.STYLE_SHORTCUT` | text style | no | `menu.text.disabled` |
| `menu.separator` | `Menu.STYLE_SEPARATOR` | paint | no | `separator` |
| `menubar.background` | `MenuBar.STYLE_BACKGROUND` | paint | no | Nothing |
| `menubar.item.hovered` | `MenuBar.STYLE_ITEM_HOVERED` | paint | no | `menu.item.hovered` |
| `menubar.text` | `MenuBar.STYLE_TEXT` | text style | no | `menu.text` |

## Expander, SplitPane and GridView

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `expander.header` | `Expander.STYLE_HEADER` | paint | no | Nothing |
| `expander.header.hovered` | `Expander.STYLE_HEADER_HOVERED` | paint | no | `expander.header` |
| `expander.text` | `Expander.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `expander.text.disabled` | `Expander.STYLE_TEXT_DISABLED` | text style | no | `expander.text` |
| `split.divider` | `SplitPane.STYLE_DIVIDER` | paint | no | `separator` |
| `split.divider.hovered` | `SplitPane.STYLE_DIVIDER_HOVERED` | paint | no | `split.divider` |
| `grid.cell` | `GridView.STYLE_CELL` | paint | yes | |
| `grid.cell.hovered` | `GridView.STYLE_CELL_HOVERED` | paint | no | Nothing |
| `grid.cell.selected` | `GridView.STYLE_CELL_SELECTED` | paint | no | `grid.cell.hovered` |

## Overlays and tooltips

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `overlay.backdrop` | `Overlays.STYLE_BACKDROP` | paint | no | Nothing |
| `tooltip.background` | `Tooltip.STYLE_BACKGROUND` | paint | no | `0xF0100010` with a one-unit `0x805000FF` edge |
| `tooltip.text` | `Tooltip.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `toast.background` | `Overlays.STYLE_TOAST` | paint | no | `tooltip.background` |
| `toast.text` | `Overlays.STYLE_TOAST_TEXT` | text style | no | `tooltip.text` |

## Image and panels

`Image` and the four plain panels read no keys. Give them a background with `setBackground` and a border with `setBorder`.

## Values in the built-in themes

### `Themes.flat()`

| Key | Value |
|---|---|
| `button.normal` | solid `0xFF3C3F44` |
| `button.hovered` | solid `0xFF4E525A` |
| `button.pressed` | solid `0xFF2B2D31` |
| `button.disabled` | solid `0xFF2A2A2A` |
| `button.focus` | solid `0xFF6A9FFF` |
| `button.text` | `0xFFF0F0F0` |
| `button.text.disabled` | `0xFF808080` |
| `label.text` | `0xFFE0E0E0` |
| `checkbox.normal`, `checkbox.checked` | solid `0xFF1B1C1F` |
| `checkbox.hovered`, `checkbox.checked.hovered` | solid `0xFF2E3136` |
| `checkbox.disabled`, `checkbox.checked.disabled` | solid `0xFF2A2A2A` |
| `checkbox.mark` | solid `0xFF6A9FFF` |
| `checkbox.mark.disabled` | solid `0xFF505050` |
| `checkbox.focus` | solid `0xFF6A9FFF` |
| `checkbox.text` | `0xFFE0E0E0` |
| `checkbox.text.disabled` | `0xFF707070` |
| `slider.track` | solid `0xFF1B1C1F` |
| `slider.track.focused` | solid `0xFF24262A` |
| `slider.track.disabled` | solid `0xFF2A2A2A` |
| `slider.thumb` | solid `0xFF5A5E66` |
| `slider.thumb.hovered` | solid `0xFF6A9FFF` |
| `slider.thumb.disabled` | solid `0xFF3A3A3A` |
| `slider.text` | `0xFFF0F0F0` |
| `slider.text.disabled` | `0xFF707070` |
| `textbox.normal` | solid `0xFF1B1C1F` |
| `textbox.focused` | solid `0xFF24262A` |
| `textbox.disabled` | solid `0xFF2A2A2A` |
| `textbox.selection` | solid `0x806A9FFF` |
| `textbox.caret` | solid `0xFFE0E0E0` |
| `textbox.text` | `0xFFE0E0E0` |
| `textbox.text.disabled` | `0xFF707070` |
| `textbox.text.readonly` | `0xFFC5C5C5` |
| `scroll.thumb` | solid `0xFF5A5E66` |
| `scroll.thumb.hovered` | solid `0xFF7A7F88` |
| `scroll.track` | solid `0xFF1B1C1F` |
| `scroll.thumb.disabled` | solid `0xFF3A3A3A` |
| `scroll.slim.track` | solid `0xFF2B2D31` |
| `scroll.slim.thumb` | solid `0xFF8A8F98` |
| `scroll.slim.thumb.hovered` | solid `0xFFE0E0E0` |
| `scroll.slim.thumb.disabled` | solid `0xFF3A3A3A` |
| `list.row` | solid `0xFF1F2024` |
| `list.row.hovered` | solid `0xFF2E3136` |
| `list.row.selected` | solid `0xFF3A4D6E` |
| `list.placeholder` | `0xFF808080` |
| `combobox.normal` | solid `0xFF1B1C1F` |
| `combobox.hovered` | solid `0xFF2E3136` |
| `combobox.disabled` | solid `0xFF2A2A2A` |
| `combobox.popup` | solid `0xFF16171A` |
| `combobox.text` | `0xFFE0E0E0` |
| `combobox.text.disabled` | `0xFF707070` |
| `window.background` | solid `0xF0141518` |
| `window.title` | solid `0xFF2B2D31` |
| `window.title.text` | `0xFFFFFFFF` |
| `window.close` | `0xFFA0A0A0` |
| `window.close.hovered` | `0xFFFFFFFF` |
| `tab.normal` | solid `0xFF24262A` |
| `tab.hovered` | solid `0xFF3C3F44` |
| `tab.selected` | solid `0xFF3A4D6E` |
| `tab.disabled` | solid `0xFF1B1C1F` |
| `tab.text` | `0xFFA0A0A0` |
| `tab.text.selected` | `0xFFFFFFFF` |
| `tab.text.disabled` | `0xFF606060` |
| `progress.track` | solid `0xFF1B1C1F` |
| `progress.fill` | solid `0xFF2E7D32` |
| `progress.text` | `0xFFF0F0F0` |
| `progress.segment` | solid `0xFF43A047` |
| `busy.dot` | solid `0xFF5A5E66` |
| `busy.dot.active` | solid `0xFFE0E0E0` |
| `busy.text` | `0xFFE0E0E0` |
| `separator` | solid `0xFF3C3F44` |
| `radio.normal`, `radio.selected` | solid `0xFF1B1C1F` |
| `radio.hovered`, `radio.selected.hovered` | solid `0xFF2E3136` |
| `radio.disabled`, `radio.selected.disabled` | solid `0xFF2A2A2A` |
| `radio.mark`, `radio.focus` | solid `0xFF6A9FFF` |
| `radio.mark.disabled` | solid `0xFF505050` |
| `radio.text` | `0xFFE0E0E0` |
| `radio.text.disabled` | `0xFF707070` |
| `toggle.track` | solid `0xFF3C3F44` |
| `toggle.track.on` | solid `0xFF3A4D6E` |
| `toggle.track.disabled` | solid `0xFF2A2A2A` |
| `toggle.knob` | solid `0xFF8A8F98` |
| `toggle.knob.hovered` | solid `0xFFE0E0E0` |
| `toggle.knob.disabled` | solid `0xFF505050` |
| `toggle.focus` | solid `0xFF6A9FFF` |
| `toggle.text` | `0xFFE0E0E0` |
| `toggle.text.disabled` | `0xFF707070` |
| `table.header` | solid `0xFF2B2D31` |
| `table.header.hovered` | solid `0xFF3C3F44` |
| `table.header.text` | `0xFFC8C8C8` |
| `table.grid` | solid `0xFF2E3136` |
| `tree.arrow` | solid `0xFFA0A0A0` |
| `menu.background` | solid `0xFF16171A` |
| `menu.item.hovered` | solid `0xFF3A4D6E` |
| `menu.text` | `0xFFE0E0E0` |
| `menu.text.disabled` | `0xFF707070` |
| `menu.shortcut` | `0xFF909090` |
| `menu.separator` | solid `0xFF3C3F44` |
| `menubar.background` | solid `0xFF24262A` |
| `menubar.item.hovered` | solid `0xFF3C3F44` |
| `menubar.text` | `0xFFE0E0E0` |
| `expander.header` | solid `0xFF24262A` |
| `expander.header.hovered` | solid `0xFF2E3136` |
| `expander.text` | `0xFFE0E0E0` |
| `expander.text.disabled` | `0xFF707070` |
| `split.divider` | solid `0xFF2B2D31` |
| `split.divider.hovered` | solid `0xFF6A9FFF` |
| `grid.cell` | solid `0xFF1B1C1F` |
| `grid.cell.hovered` | solid `0x40FFFFFF` |
| `grid.cell.selected` | solid `0x606A9FFF` |
| `toast.background` | solid `0xF02B2D31` |
| `toast.text` | `0xFFF0F0F0` |
| `overlay.backdrop` | solid `0x60000000` |
| `tooltip.text` | `0xFFF0F0F0` |

Text styles are otherwise plain: no bold, italic, underline, strikethrough or shadow. `tooltip.background` is not set, so tooltips use the fallback box.

## Naming your own keys

Prefix keys for your own components with your project id, such as `examplemod.switch.track`, so they never collide with Strata's keys or another project's.
