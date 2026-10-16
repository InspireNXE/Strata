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

## Default behavior of the built-ins

| Type | `Button` | `TextBox` | `Label`, panels |
|---|---|---|---|
| `POINTER_PRESS` | | Places the caret, or selects a word (2 clicks) or everything (3 clicks). Shift extends. | |
| `CLICK` | Presses on a left click and consumes it | | |
| `DRAG` | | Extends the selection, by word if the press was a double click | |
| `KEY` | Presses on Enter, keypad Enter or Space (`PRESS` only) and consumes it | Edits and moves the caret; consumes what it uses and every printable key without Control or Alt | |
| `CHAR` | | Inserts the character and consumes it | |
| `FOCUS_GAINED` | | Moves the caret to the end | |

Anything not listed does nothing by default and is not consumed. Disabled widgets do nothing.
