# Defaults and timings

Every number Strata picks for you.

## Every component

| Setting | Default |
|---|---|
| Visible, enabled | yes |
| Focusable, tabbable | no, except buttons and text boxes |
| Tab index | 0 |
| Cursor | none (inherits; `ARROW` at the root) |
| Margin | `Insets.NONE` |
| Minimum size | 0 by 0 |
| Maximum size | `Float.POSITIVE_INFINITY` both ways |
| Fixed size | unset (`Float.NaN`) |
| Alignment | `LEFT`, `TOP` |
| Background | `Paint.NONE` |
| Bounds before the first layout | `Rect.ZERO` |
| Id | `null` |

## Containers

| Setting | Default |
|---|---|
| Padding | `Insets.NONE` |
| Clips children | yes |
| `StackPanel` orientation | `VERTICAL` from `stackPanel()` |
| `StackPanel` spacing | 0 |
| `GridPanel` rows and columns | none, which acts as one auto cell |
| `GridPanel` spacing | 0 by 0 |
| `AbsolutePanel` position from plain `add` | (0, 0) |
| `GridPanel` cell from plain `add` | row 0, column 0, spanning 1 by 1 |

## Widgets

| Widget | Setting | Default |
|---|---|---|
| `Label` | Wrapping | off |
| | Text alignment | `LEFT` |
| | Style | none, so the theme's `label.text` |
| `Button` | Text padding | 8 left and right, 5 top and bottom |
| | Minimum size | 20 by 20 |
| | Cursor | `HAND` |
| | Focus ring | one unit, inside the edge, if the theme has `button.focus` |
| `TextBox` | Desired width | 120 |
| | Text padding | 4 left and right, 5 top and bottom |
| | Minimum size | 20 by 20 |
| | Maximum length | `Integer.MAX_VALUE` code points |
| | Mask character | `*` |
| | Caret width | 1 unit |
| | Caret blink | every 500 ms |
| | Cursor | `TEXT` |

## Input

| Rule | Value |
|---|---|
| Multi-click time | 500 ms between presses, inclusive |
| Multi-click distance | 4 units on each axis |
| Most clicks in a run | 3, then back to 1 |
| Drag threshold | more than 4 units on either axis |
| Synthesized key repeat delay | 400 ms |
| Synthesized key repeat interval | 33 ms |
| Keys the host repeats itself | Left, Right, Up, Down, Backspace, Delete, Home, End, Page Up, Page Down |

The host synthesizes repeats only for engines whose platform reports `sendsKeyRepeat == false`. Swing and Minecraft both send their own.

## Text

| Setting | Value |
|---|---|
| `TextStyle.DEFAULT` | opaque white, no decoration, no shadow |
| Minecraft line height | the font's, 9 for the default font |
| Swing default font | logical Dialog, 9 units |

## Adapters

| Setting | Value |
|---|---|
| Minecraft unit | one GUI pixel |
| `StrataPanel` preferred size | 960 by 600 pixels |
| `StrataPanel` background | `0xFF1E1F22` |
| `StrataPanel` repaint rate | about 60 per second (every 16 ms) |
| Swing missing sprite | solid magenta |
