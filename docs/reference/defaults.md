# Defaults and timings

Every number Strata picks for you.

## Every component

| Setting | Default |
|---|---|
| Visible, enabled | yes |
| Focusable, tabbable | no, except buttons, check boxes, sliders, text boxes, lists, combo boxes and tab bars |
| Tab index | 0 |
| Cursor | none (inherits; `ARROW` at the root) |
| Margin | `Insets.NONE` |
| Minimum size | 0 by 0 |
| Maximum size | `Float.POSITIVE_INFINITY` both ways |
| Fixed size | unset (`Float.NaN`) |
| Alignment | `LEFT`, `TOP`, except windows and message boxes (centered) and separators (stretched along their line) |
| Offset | 0, 0 |
| Background | `Paint.NONE` |
| Border | `Border.NONE` |
| Tooltip | none |
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
| `ScrollPanel` policies | horizontal `NEVER`, vertical `AUTO` |
| `ScrollPanel` scroll rate | 10 units a wheel notch or arrow key |
| `ScrollPanel` scroll bar | 6 wide, 2 from the viewport; thumb at least 32 long and at most the track less 8 |
| `Window` padding | 3 on every side |
| `Window` title bar | one line plus 3 above and below |
| `Window` close button | 12 wide, at the right end of the title bar |
| `Window` closable, movable | yes, yes |

## Widgets

| Widget | Setting | Default |
|---|---|---|
| `Label` | Wrapping | off |
| | Text alignment | `LEFT` |
| | Style | none, so the theme's `label.text` |
| | Spans | none: plain text |
| | Text scale | 1 |
| | Overflow | `VISIBLE` |
| `Button` | Text padding | 8 left and right, 5 top and bottom |
| | Minimum size | 20 by 20 |
| | Cursor | `HAND` |
| | Focus ring | one unit, inside the edge, if the theme has `button.focus` |
| `Image` | Image size and desired size | 16 by 16 |
| | Scaling | `STRETCH` |
| | Tint | `0xFFFFFFFF` |
| `CheckBox` | Box size | caption line height plus 8 (17 with Minecraft's font) |
| | Gap between box and caption | 4 |
| | Mark inset | a fifth of the box size on each side |
| | Cursor | `HAND` |
| `Slider` | Desired size | 150 by 20 |
| | Range | 0 to 1 |
| | Step | 0 (continuous) |
| | Key step | the step, or a hundredth of the range |
| | Page Up and Page Down | 10 key steps |
| | Thumb | 8 wide, the slider's full height |
| | Cursor | `HAND` |
| `TextBox` | Desired width | 120 |
| | Text padding | 4 left and right, 5 top and bottom |
| | Minimum size | 20 by 20 |
| | Maximum length | `Integer.MAX_VALUE` code points |
| | Mask character | `*` |
| | Caret width | 1 unit |
| | Caret blink | every 500 ms |
| | Cursor | `TEXT` |
| | Filter, read-only, select all on focus | none, off, off |
| `ListView` | Row height and spacing | 20 and 0 |
| | Selection | none; deselecting by pressing off; activation by double click |
| | Smallest desired width | 40 |
| | Wheel | one row and its spacing a notch |
| `ComboBox` | Rows shown before scrolling | 8 |
| | Desired size | widest option plus 4, 4 and 4 of padding and a 7-wide arrow; one line plus 6 |
| | Open list rows | one line plus 4 |
| | Cursor | `HAND` |
| `MessageBox` | Message width | wraps at 280 |
| | Buttons | 64 wide, 4 apart, the first focused |
| `TabBar` | Tab size | text plus 8 on each side; one line plus 7 |
| | Spacing | 0 |
| | Selected | the first tab added |
| `ProgressBar` | Desired size | 100 wide, one line plus 4 |
| | Value | 0 |
| `Separator` | Thickness | 1 |

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
| Tooltip position | 12 right of and 12 above the pointer, flipped left at the right edge, kept inside the host |
| Tooltip padding | 4, with 1 between lines |

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
