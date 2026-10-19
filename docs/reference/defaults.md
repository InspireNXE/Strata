# Defaults and timings

Every number Strata picks for you.

## Every component

| Setting | Default |
|---|---|
| Visible, enabled | yes |
| Focusable, tabbable | no, except buttons, check boxes, radio buttons, toggle switches, sliders, text boxes, text areas, lists (also inside tables and trees), combo boxes, tab bars and expanders. Menus are focusable but not tabbable. |
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

## Controls

| Control | Setting | Default |
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
| `CheckBox` | Box size | caption line height plus 8 (17 with a 9-unit line height) |
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
| | Selection | the first option once there are options |
| | Label function, key function | `toString`, the option itself |
| | Desired size | widest option plus 4, 4 and 4 of padding and a 7-wide arrow; one line plus 6 |
| | Open list rows | one line plus 4 |
| | Cursor | `HAND` |
| `MessageBox` | Message width | wraps at 280 |
| | Buttons | 64 wide, 4 apart, the first focused |
| `TabBar` | Tab size | text plus 8 on each side; one line plus 7 |
| | Spacing | 0 |
| | Selected | the first tab added |
| `ProgressBar` | Desired size | 100 wide, one line plus 4 |
| | Range, value | 0 to 1, 0 |
| `Separator` | Thickness | 1 |
| `RadioButton` | Box, gap and cursor | as `CheckBox` |
| | Mark inset | three tenths of the box size on each side |
| `ToggleSwitch` | Track | caption line height plus 4 tall, twice that wide |
| | Knob | a square 2 in from the track's edges |
| | Gap between track and caption | 4 |
| `Spinner` | Desired width | 60, of which the buttons take 12 |
| | Range, step, decimals, value | 0 to 100, 1, 0, 0 |
| | Page Up and Page Down | 10 steps |
| `TextArea` | Desired size | 160 wide, 4 lines plus padding tall |
| | Text padding | 4 on each side |
| | Wrapping | on |
| | Wheel | 3 lines a notch |
| | Caret after `setText` | at the start, scrolled to the top |
| `TableView` | Header height | 20 |
| | Auto column width | title plus 8, and 11 more when sortable |
| | Text cells | 4 in from the column's edges, cut short with `...` |
| | Sort | the set order; text columns sortable, ignoring case |
| `TreeView` | Indent | 10 a level |
| | Arrow column | 12 |
| | Children | none: every item is a leaf |
| `Menu` | Item height | one line plus 6 |
| | Gutters | 14 at each side of an item, 2 around the items |
| | Separator height | 5 |
| | Smallest width | 60 |
| `MenuBar` | Title size | text plus 8 on each side; one line plus 6 |
| `Expander` | Header | one line plus 6 tall |
| | Expanded | no |
| `SplitPane` | Divider | 4 thick |
| | Position | 0.5 |
| | Minimum side when dragging | 20 |
| `GridView` | Columns, cell size, spacing | 9, 18, 0 |
| | Content inset | 1 |
| | Selectable | no |
| | Activation | a left double click |

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
| Toast time | 3000 ms for text toasts |
| Toast position | 4 in from the top and right edges, stacked 4 apart |
| Toast padding | 6; text wraps at a third of the host's width |

The host synthesizes repeats only for engines whose platform reports `sendsKeyRepeat == false`. Swing sends its own.

## Text

| Setting | Value |
|---|---|
| `TextStyle.DEFAULT` | opaque white, no decoration, no shadow |
| Swing default font | logical Dialog, 9 units |

## Adapters

| Setting | Value |
|---|---|
| `StrataPanel` preferred size | 960 by 600 pixels |
| `StrataPanel` background | `0xFF1E1F22` |
| `StrataPanel` repaint rate | about 60 per second (every 16 ms) |
| Swing missing sprite | solid magenta |
