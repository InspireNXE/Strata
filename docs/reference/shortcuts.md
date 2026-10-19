# Keyboard shortcuts

Every key the built-in controls and hosts respond to. Control means Control without Alt. On macOS, see the [note on Command](../platforms/differences.md#modifier-keys).

## Hosts

| Keys | Action |
|---|---|
| Tab | Focus the next tabbable component, wrapping at the end, unless something consumed the key |
| Shift+Tab | Focus the previous one |
| Ctrl+Tab, Alt+Tab | Nothing; left to the engine |

## Button, while focused

| Keys | Action |
|---|---|
| Enter, keypad Enter, Space | Press (not on repeat) |

## CheckBox, while focused

| Keys | Action |
|---|---|
| Enter, keypad Enter, Space | Toggle (not on repeat) |

## Slider, while focused

| Keys | Action |
|---|---|
| Left, Down | Lower by the key step (repeats) |
| Right, Up | Raise by the key step (repeats) |
| Page Down, Page Up | Lower or raise by ten key steps |
| Home, End | Go to the minimum or maximum |

## TextBox, while focused

| Keys | Action |
|---|---|
| Left, Right | Move one code point; with a selection and no Shift, collapse it |
| Home, End | Move to the start or end |
| Ctrl+Left, Ctrl+Right | Move by word |
| Shift with any movement | Extend the selection |
| Backspace, Delete | Delete the selection, or one code point |
| Ctrl+Backspace, Ctrl+Delete | Delete the selection, or one word |
| Ctrl+A | Select all |
| Ctrl+C | Copy (not in password mode) |
| Ctrl+X | Cut (not in password mode) |
| Ctrl+V | Paste, with line breaks removed |

Not handled, so they bubble to ancestors and the engine: Tab, Escape, Enter, Up, Down, Page Up, Page Down, and every Control combination not listed above.

## Mouse in a TextBox

| Input | Action |
|---|---|
| Click | Put the caret at the nearest character boundary |
| Shift+click | Extend the selection to there |
| Double click | Select the word, or run of spaces, under the pointer |
| Triple click | Select everything |
| Drag | Select by character |
| Drag after a double click | Select by word |
