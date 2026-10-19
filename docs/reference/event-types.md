# Event types

Every type is a constant in `EventTypes`. All positions in pointer events are in host coordinates, in UI units. See [Events](../guides/events.md) for how delivery works.

## Pointer

| Type | Interface | Bubbles | Fired at | When |
|---|---|---|---|---|
| `POINTER_ENTER` | `PointerEvent` | no | The newly hovered component | It became the deepest component under the pointer, including when it appears under a still pointer after a layout change, on the next pointer input |
| `POINTER_EXIT` | `PointerEvent` | no | The previously hovered component | It stopped being the deepest one, or the pointer left the host |
| `POINTER_MOVE` | `PointerEvent` | yes | The hovered component | The pointer moved, after any enter or exit for the same move |
| `POINTER_PRESS` | `PointerButtonEvent` | yes | The deepest component under the pointer | A button went down. Focus has already moved. |
| `POINTER_RELEASE` | `PointerButtonEvent` | yes | The component the press started on | That button came up, wherever the pointer is |
| `CLICK` | `PointerButtonEvent` | yes | The same | After the release, if the pointer is still over it, it is enabled, and no drag happened |
| `DRAG_START` | `DragEvent` | yes | The component the press started on | The pointer moved more than 4 units on either axis with the button held. Positions are the press's. |
| `DRAG` | `DragEvent` | yes | The same | Every move after `DRAG_START`, wherever the pointer is |
| `DRAG_END` | `DragEvent` | yes | The same | The button came up. Replaces the `CLICK`. |
| `SCROLL` | `ScrollEvent` | yes | The component under the pointer | A wheel or trackpad scrolled |

## Keyboard and focus

| Type | Interface | Bubbles | Fired at | When |
|---|---|---|---|---|
| `KEY` | `KeyEvent` | yes | The focused component, or the root | A key went down, repeated or came up. An unconsumed Tab then moves focus. |
| `CHAR` | `CharEvent` | yes | The focused component only; dropped if none | A character was typed |
| `FOCUS_GAINED` | `FocusEvent` | no | The component gaining focus | After focus changed, and after the old component's `FOCUS_LOST` |
| `FOCUS_LOST` | `FocusEvent` | no | The component losing focus | After focus changed |
| `MODIFIERS_CHANGED` | `ModifiersEvent` | no | The screen's content and each open overlay's content | The held modifiers changed, by a modifier key or by input arriving with different modifiers |

## What the events carry

| Interface | Methods |
|---|---|
| `Event` | `target()`, `current()`, `cancel()`, `isCancelled()`, `stopPropagation()`, `isPropagationStopped()`, `consume()`, `isHandled()`, `snapshot()`, `currentSnapshot()`, `snapshotOf(component)` |
| `PointerEvent` | `x()`, `y()`, `localX()`, `localY()`, `modifiers()` |
| `PointerButtonEvent` | Pointer, plus `button()`, `clicks()` (1, 2 or 3) |
| `DragEvent` | Pointer, plus `button()`, `clicks()`, `startX()`, `startY()`, `deltaX()`, `deltaY()` |
| `ScrollEvent` | Pointer, plus `scrollX()` (positive toward the left), `scrollY()` (positive away from the user), `kind()` |
| `KeyEvent` | `key()`, `action()`, `modifiers()`, `is(key, required, policy)` |
| `CharEvent` | `codePoint()`, `text()`, `modifiers()` |
| `FocusEvent` | `other()`: the component on the other side of the move, if any |
| `ModifiersEvent` | `modifiers()`: the modifiers now held |

## Default behavior of the built-ins

| Type | `Button` | `CheckBox` | `Slider` | `TextBox` |
|---|---|---|---|---|
| `POINTER_PRESS` | | | Moves the value to the pointer on a left press and consumes it | Places the caret, or selects a word (2 clicks) or everything (3 clicks). Shift extends. |
| `CLICK` | Presses on a left click and consumes it | Toggles on a left click and consumes it | | |
| `DRAG_START`, `DRAG_END` | | | Consumes left-button drags | |
| `DRAG` | | | Follows the pointer and consumes it | Extends the selection, by word if the press was a double click |
| `KEY` | Presses on Enter, keypad Enter or Space (`PRESS` only) and consumes it | Toggles on Enter, keypad Enter or Space (`PRESS` only) and consumes it | Moves on arrows, Page Up, Page Down, Home and End, and consumes them | Edits and moves the caret; consumes what it uses and every printable key without Control or Alt |
| `CHAR` | | | | Inserts the character and consumes it |
| `FOCUS_GAINED` | | | | Moves the caret to the end |

`Label`, `Image` and the plain panels have no default behavior. The newer controls document theirs in the [controls guide](../guides/controls.md): a `ScrollPanel` scrolls on `SCROLL`, unused keys and drags of its bar; a `ListView` selects on `POINTER_PRESS`, activates on a double press or Enter, and moves on keys; a `ComboBox` opens on `CLICK` and changes on keys; a `Window` moves on drags of its title bar and closes on Escape; a `TabBar` selects on clicks and Left and Right; a `ScrollBar` pages on `POINTER_PRESS` on its track, follows drags of its thumb, and moves on `SCROLL` and, when made focusable, on keys. Each consumes what it acts on.

Anything not listed does nothing by default and is not consumed. Disabled controls do nothing.
