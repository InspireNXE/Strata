# Widgets

Strata has three built-in widgets. Each is an interface in `org.inspirenxe.strata.api.widget`, made by the factory, and each has a matching behavior interface for [decorating](extending.md#decorate-a-built-in-with-a-behavior) it. Their theme keys are listed on each widget below and in the [theme key reference](../reference/theme-keys.md).

## Label

Text the user reads but does not edit.

```java
Label title = factory.label("Settings").setStyle(TextStyle.DEFAULT.withBold(true));
Label help = factory.label(longText).setWrap(true);
help.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
```

| Method | Default | Notes |
|---|---|---|
| `setText(text)` | | `\n` starts a new line. A new layout runs if the text changed. |
| `setWrap(boolean)` | off | Also break between words to fit the width offered. See [Wrapping text](layout.md#wrapping-text). |
| `setStyle(TextStyle)` | `null` | Overrides the theme's `label.text` for this label. `null` goes back to the theme. |
| `setTextAlignment(HorizontalAlignment)` | `LEFT` | How each line sits within the label's width. It only shows when the label is wider than its text, for example when stretched. `STRETCH` acts as `LEFT`. |

With wrapping on, a single word wider than a whole line is broken between characters. The desired size is the widest line by the number of lines times the line height.

A label is not focusable and has no default behavior for input, so clicks and keys pass through to its ancestors. That makes labels handy click targets for a container-level handler; see [Handling events on a container](events.md#handling-events-on-a-container).

`style()` returns the style the label draws with: the one you set, or the theme's.

## Button

A push button with a text caption.

```java
Button save = factory.button("Save").onAction(this::save);
```

| Method | Notes |
|---|---|
| `setText(text)` | One line. A new layout runs. |
| `onAction(Runnable)` | Adds an action. Actions run in the order added. An action may add more; they run from the next press. |
| `press()` | Runs the actions as if clicked. Does nothing while disabled. Fires no events. |
| `setTextPadding(Insets)` | Space between the caption and the edges. 8 left and right, 5 top and bottom by default. |

A button presses on a left click, or on a press of Enter, keypad Enter or Space while it has focus. Held keys that repeat do not press it again. It consumes those inputs. A disabled button does neither and draws its disabled face.

It is focusable and tabbable, shows the hand cursor, and has a minimum size of 20 by 20. Its desired size is the caption plus the text padding. The caption is centered in whatever size the button ends up.

| Face | When |
|---|---|
| `button.normal` | Idle |
| `button.hovered` | Hovered or focused |
| `button.pressed` | Pressed with the pointer still over it |
| `button.disabled` | Disabled |
| `button.focus` (optional) | A one-unit ring drawn inside the edge while focused |

To veto a press, cancel its `CLICK` in a `BEFORE` handler. To react to other buttons, such as a right click, handle `CLICK` yourself; see [Clicks](events.md#clicks).

## TextBox

A single-line text field ported from Wintergreen's.

```java
TextBox name = factory.textBox()
        .setMaxLength(32)
        .onTextChanged(text -> preview.setText("Hello, " + text));
name.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
```

### Settings

| Method | Default | Notes |
|---|---|---|
| `setText(text)` | empty | Removes line breaks, cuts to the length limit and puts the caret at the end. Listeners run if the text changed. |
| `setMaxLength(n)` | `Integer.MAX_VALUE` | Counted in code points. Longer text is cut at once, and listeners run. Negative throws. |
| `setPassword(boolean)` | off | Draws every code point as the mask character and disables copy and cut. `text()` still returns the real text. |
| `setMaskCodePoint(cp)` | `'*'` | The mask character |
| `setTextPadding(Insets)` | 4 left and right, 5 top and bottom | Space between the text and the edges |
| `onTextChanged(listener)` | | Called with the new text after every change: typing, deleting, cutting, pasting, `setText` and `setMaxLength` cutting. In the order added. |

### Reading and selecting

| Method | Returns |
|---|---|
| `text()` | The text, never null |
| `caret()` | The caret, a char index from 0 to `text().length()` |
| `selectionStart()`, `selectionEnd()` | The selected range. Equal when nothing is selected. |
| `selectedText()` | The selected text, or empty |
| `select(anchor, caret)` | Selects a range. The anchor stays put and the caret is the moving end, as after Shift+arrow. Out-of-range indexes are clamped. |
| `selectAll()` | Selects everything, caret at the end |

Indexes count UTF-16 chars, like `String` indexes, but never fall inside a surrogate pair: editing always works on whole code points, so an emoji is deleted in one go. An index inside a pair given to `select` moves to the pair's start.

### Mouse

- A click puts the caret at the nearest character boundary. Shift+click extends the selection to it.
- A drag selects by character. A drag that starts with a double click selects whole words.
- A double click selects the word, or the run of spaces, under the pointer. Double-clicking the first letter of a word selects that word.
- A triple click selects everything.

### Keyboard

| Keys | Action |
|---|---|
| Left, Right | Move one code point. With a selection and no Shift, collapse it to its start or end. |
| Home, End | Move to the start or end |
| Ctrl+Left, Ctrl+Right | Move by word |
| Backspace, Delete | Delete the selection, or one code point |
| Ctrl+Backspace, Ctrl+Delete | Delete the selection, or one word |
| Shift with any movement | Extend the selection |
| Ctrl+A | Select all |
| Ctrl+C, Ctrl+X | Copy, cut. Nothing happens in password mode. |
| Ctrl+V | Paste. Line breaks in the pasted text are removed. |
| Typing | Replaces the selection. Text past the length limit is dropped. |

Shortcuts need Control held without Alt, so AltGr combinations, which many layouts report as Control plus Alt, type their characters instead of triggering shortcuts. On macOS, see the [platform note on Command](../platforms/differences.md#modifier-keys).

### Focus and the engine

The box is focusable and tabbable and shows the text cursor. Focus by Tab or `requestFocus()` puts the caret at the end; focus by a click puts it at the click. While focused, it:

- asks the engine for text input, which Minecraft 26.3 needs before it sends any characters;
- consumes every key press that types a character (any printable key without Control or Alt), so engine shortcuts on those keys, such as Minecraft's inventory or chat keys, do not fire while you type;
- consumes the editing keys and shortcuts in the table above;
- does not consume Tab, Escape, Enter, Up, Down, Page Up or Page Down. Tab moves focus, Escape reaches the engine (so a Minecraft screen still closes), and Enter bubbles to ancestors, where you can handle it as "submit".

A disabled box ignores input.

### Drawing

The caret is one unit wide and blinks every half second on the host's clock. It shows again after each edit or move. When the text is wider than the box, it scrolls sideways to keep the caret in view.

| Key | Required | Used for |
|---|---|---|
| `textbox.normal` | yes | Background when not focused |
| `textbox.focused` | yes | Background when focused |
| `textbox.disabled` | yes | Background when disabled |
| `textbox.text` | no | Text style. Falls back to the default text style. |
| `textbox.text.disabled` | no | Text when disabled. Falls back to `textbox.text`. |
| `textbox.selection` | no | Behind selected text. Falls back to translucent blue. |
| `textbox.caret` | no | The caret. Falls back to light gray. |

### Size

The desired size is 120 units wide and one line plus the text padding tall, with a minimum of 20 by 20. Stretch it or set a width to make it wider.

### Common recipes

Submit on Enter:

```java
--8<-- "events/EventExamples.java:submit"
```

Accept only digits: decorate `onChar`, as in [this behavior example](extending.md#decorate-a-built-in-with-a-behavior).

React to each change: `onTextChanged`. React only when the user leaves the field: handle `FOCUS_LOST`.

## Widgets that do not exist yet

There is no check box, slider, list, scroll panel, dropdown, tooltip, multi-line text area or image widget yet. You can build any of them on `CustomComponent`; the [extending guide](extending.md) builds a check box. [Status and limitations](../help/status.md) lists what is planned.
