# Theme keys

Every style key the built-in widgets read. A theme lacking a required key cannot show a tree that uses the widget; see [Themes](../guides/theming.md#required-and-optional-keys).

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
| `separator` | `Separator.STYLE_LINE` | paint | yes | |

## Overlays and tooltips

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `overlay.backdrop` | `Overlays.STYLE_BACKDROP` | paint | no | Nothing |
| `tooltip.background` | `Tooltip.STYLE_BACKGROUND` | paint | no | `0xF0100010` with a one-unit `0x805000FF` edge |
| `tooltip.text` | `Tooltip.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |

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
| `separator` | solid `0xFF3C3F44` |
| `overlay.backdrop` | solid `0x60000000` |
| `tooltip.text` | `0xFFF0F0F0` |

Text styles are otherwise plain: no bold, italic, underline, strikethrough or shadow. `tooltip.background` is not set, so tooltips use the fallback box.

### `MinecraftThemes.vanilla()`

| Key | Value |
|---|---|
| `button.normal` | nine-slice `minecraft:widget/button`, border 3 |
| `button.hovered` | nine-slice `minecraft:widget/button_highlighted`, border 3 |
| `button.pressed` | nine-slice `minecraft:widget/button_highlighted`, border 3 |
| `button.disabled` | nine-slice `minecraft:widget/button_disabled`, border 1 |
| `button.text` | `0xFFFFFFFF` with shadow |
| `button.text.disabled` | `0xFFA0A0A0` with shadow |
| `label.text` | `0xFFFFFFFF` with shadow |
| `checkbox.normal` | sprite `minecraft:widget/checkbox` |
| `checkbox.hovered` | sprite `minecraft:widget/checkbox_highlighted` |
| `checkbox.checked` | sprite `minecraft:widget/checkbox_selected` |
| `checkbox.checked.hovered` | sprite `minecraft:widget/checkbox_selected_highlighted` |
| `checkbox.text` | `0xFFFFFFFF` with shadow |
| `checkbox.text.disabled` | `0xFFA0A0A0` with shadow |
| `slider.track` | nine-slice `minecraft:widget/slider`, border 1 |
| `slider.track.focused` | nine-slice `minecraft:widget/slider_highlighted`, border 1 |
| `slider.thumb` | nine-slice `minecraft:widget/slider_handle`, border 2 (3 at the bottom) |
| `slider.thumb.hovered` | nine-slice `minecraft:widget/slider_handle_highlighted`, border 2 (3 at the bottom) |
| `slider.text` | `0xFFFFFFFF` with shadow |
| `slider.text.disabled` | `0xFFA0A0A0` with shadow |
| `textbox.normal` | nine-slice `minecraft:widget/text_field`, border 1 |
| `textbox.focused` | nine-slice `minecraft:widget/text_field_highlighted`, border 1 |
| `textbox.disabled` | nine-slice `minecraft:widget/text_field`, border 1 |
| `textbox.selection` | solid `0x800000FF` |
| `textbox.caret` | solid `0xFFD0D0D0` |
| `textbox.text` | `0xFFE0E0E0` with shadow |
| `textbox.text.disabled` | `0xFF707070` with shadow |
| `textbox.text.readonly` | `0xFFA0A0A0` with shadow |
| `scroll.thumb` | nine-slice `minecraft:widget/scroller`, border 1 |
| `scroll.track` | nine-slice `minecraft:widget/scroller_background`, border 1 |
| `list.row.hovered` | solid `0x30FFFFFF` |
| `list.row.selected` | solid `0x60FFFFFF` |
| `list.placeholder` | `0xFFA0A0A0` with shadow |
| `combobox.normal` | nine-slice `minecraft:widget/button`, border 3 |
| `combobox.hovered` | nine-slice `minecraft:widget/button_highlighted`, border 3 |
| `combobox.disabled` | nine-slice `minecraft:widget/button_disabled`, border 1 |
| `combobox.popup` | solid `0xF0101010` |
| `combobox.text` | `0xFFFFFFFF` with shadow |
| `combobox.text.disabled` | `0xFFA0A0A0` with shadow |
| `window.background` | nine-slice `minecraft:popup/background`, border 6 |
| `window.title.text` | `0xFFFFFFFF` with shadow |
| `window.close` | `0xFFA0A0A0` with shadow |
| `window.close.hovered` | `0xFFFFFFFF` with shadow |
| `tab.normal` | nine-slice `minecraft:widget/tab`, border 2 (0 at the bottom) |
| `tab.hovered` | nine-slice `minecraft:widget/tab_highlighted`, border 2 (0 at the bottom) |
| `tab.selected` | nine-slice `minecraft:widget/tab_selected`, border 2 (0 at the bottom) |
| `tab.text` | `0xFFA0A0A0` with shadow |
| `tab.text.selected` | `0xFFFFFFFF` with shadow |
| `tab.text.disabled` | `0xFF606060` with shadow |
| `progress.track` | solid `0xFF000000` |
| `progress.fill` | solid `0xFF008200` |
| `progress.text` | `0xFFFFFFFF` with shadow |
| `separator` | solid `0xB9FFFFFF` |
| `overlay.backdrop` | solid `0x60000000` |

`button.focus` and `checkbox.focus` are not set, so vanilla-themed buttons and check boxes show focus through their hovered sprites only. Vanilla's check box sprites include the check mark, so `checkbox.mark` is not set either, and vanilla has no disabled check box or slider sprites. Vanilla has no sprites for list rows, progress bars or separators, so they are plain colors, and the tooltip keys are not set, so tooltips use the fallback, which matches vanilla's classic tooltip colors. The borders match the sprites' `.png.mcmeta` files in 26.3. Minecraft reads the `.mcmeta`; other engines use the borders.

## Naming your own keys

Prefix keys for your own components with your mod or project id, such as `examplemod.switch.track`, so they never collide with Strata's keys or another mod's.
