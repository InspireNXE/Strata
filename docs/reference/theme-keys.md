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

## TextBox

| Key | Constant | Kind | Required | Fallback |
|---|---|---|---|---|
| `textbox.normal` | `TextBox.STYLE_NORMAL` | paint | yes | |
| `textbox.focused` | `TextBox.STYLE_FOCUSED` | paint | yes | |
| `textbox.disabled` | `TextBox.STYLE_DISABLED` | paint | yes | |
| `textbox.text` | `TextBox.STYLE_TEXT` | text style | no | `TextStyle.DEFAULT` |
| `textbox.text.disabled` | `TextBox.STYLE_TEXT_DISABLED` | text style | no | `textbox.text` |
| `textbox.selection` | `TextBox.STYLE_SELECTION` | paint | no | `0x806A9FFF`, translucent blue |
| `textbox.caret` | `TextBox.STYLE_CARET` | paint | no | `0xFFE0E0E0`, light gray |

## Panels

The four panels read no keys. Give them a background with `setBackground`.

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
| `textbox.normal` | solid `0xFF1B1C1F` |
| `textbox.focused` | solid `0xFF24262A` |
| `textbox.disabled` | solid `0xFF2A2A2A` |
| `textbox.selection` | solid `0x806A9FFF` |
| `textbox.caret` | solid `0xFFE0E0E0` |
| `textbox.text` | `0xFFE0E0E0` |
| `textbox.text.disabled` | `0xFF707070` |

Text styles are otherwise plain: no bold, italic, underline, strikethrough or shadow.

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
| `textbox.normal` | nine-slice `minecraft:widget/text_field`, border 1 |
| `textbox.focused` | nine-slice `minecraft:widget/text_field_highlighted`, border 1 |
| `textbox.disabled` | nine-slice `minecraft:widget/text_field`, border 1 |
| `textbox.selection` | solid `0x800000FF` |
| `textbox.caret` | solid `0xFFD0D0D0` |
| `textbox.text` | `0xFFE0E0E0` with shadow |
| `textbox.text.disabled` | `0xFF707070` with shadow |

`button.focus` is not set, so vanilla-themed buttons show focus through their hovered sprite only. The borders match the sprites' `.png.mcmeta` files in 26.3. Minecraft reads the `.mcmeta`; other engines use the borders.

## Naming your own keys

Prefix keys for your own components with your mod or project id, such as `examplemod.checkbox.box`, so they never collide with Strata's keys or another mod's.
