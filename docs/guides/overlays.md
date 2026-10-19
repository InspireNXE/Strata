# Overlays and tooltips

Some things draw above a screen instead of inside it: a drop-down's list, a confirmation box, an item's tooltip. Strata keeps them out of the screen's tree, so no container clips them and they never push the layout around.

Each host has one `Overlays`, from `factory.overlays()`. Overlays stack in the order they open, each above the screen and the overlays before it.

## Popups

A popup shows a component next to an area, usually the bounds of the component that opened it.

```java
--8<-- "overlays/OverlayExamples.java:popup"
```

The popup takes its desired size. It sits below the area with left edges lined up, above it when there is more room above, and moves left as needed to stay inside the host. A press anywhere outside the popup closes it, and that press goes nowhere else, so clicking another button while a menu is open only closes the menu. Escape closes it too. The [`ComboBox`](controls.md#combobox) is built this way.

## Dialogs

A dialog is modal. Its content is laid out in the whole host, where its own alignment places it, so a centered [window](controls.md#window) stays centered when the host resizes.

```java
--8<-- "overlays/OverlayExamples.java:dialog"
```

While a dialog is open:

- Nothing below it gets the pointer, focus or Tab. Keys go to it when nothing in it has focus.
- The screen below stays visible, under the theme's `overlay.backdrop` paint if it has one.
- Focus moves to the first tabbable component in it when it opens, and back to where it was when it closes.
- An Escape press that nothing consumed closes it. A window closes its dialog itself, telling its close listeners first.

A [`MessageBox`](controls.md#messagebox) is a ready-made dialog for confirmations:

```java
--8<-- "overlays/OverlayExamples.java:message-box"
```

## Toasts

A toast is a short notice that closes itself, such as "Listing created".

```java
factory.overlays().toast("Listing created");
factory.overlays().toast(myNoticePanel, 5000);
```

Toasts stack down the top-right corner, 4 units in from the edges and 4 apart. A text toast shows for 3 seconds and wraps at a third of the host's width; a component toast shows for the time given and draws its own background. The time runs on the platform's clock, and a toast closes on the first frame after it runs out. A click on a toast that nothing in it consumed closes it early. Presses elsewhere and Escape leave toasts alone, and they take no focus.

A text toast draws on `toast.background` with `toast.text`, falling back to the tooltip keys.

## Working with open overlays

| Call | Does |
|---|---|
| `overlay.close()` | Closes it and detaches its content. Closing twice does nothing. |
| `overlay.onEvent(Overlay.CLOSED, handler)` | Runs once it has closed, however that happened |
| `overlays.of(component)` | Finds the overlay a component is shown in, for a button inside a dialog that closes it |
| `overlays.open()` | Every open overlay, bottom first |
| `overlays.closeAll()` | Closes them all, top first, toasts included |

Content must be new: a component already in a tree is refused. Closing or rebuilding the screen closes every overlay.

Dialogs are overlays inside the screen's host, so they need no engine screen of their own, and the screen behind keeps updating.

## Tooltips

Any component can have a tooltip. It shows while the pointer rests on the component or anything inside it that has none of its own, follows the pointer 12 units right of and above it (left of it near the right edge), and stays inside the host. It hides while a drag is in progress. Tooltips take no input.

```java
--8<-- "overlays/OverlayExamples.java:tooltips"
```

| Kind | Made with | Drawn |
|---|---|---|
| Plain text | `setTooltip(String)`, `Tooltip.text` | In the `tooltip.text` style, `\n` for new lines |
| Styled lines | `Tooltip.lines(lines)` | Each span in its own style |
| A component | `Tooltip.of(factory -> component)` | Built again each time the tooltip shows, with no box of its own |

Text tooltips draw on the `tooltip.background` paint with 4 units of padding, or on a dark box with a purple edge when the theme lacks the key.

Adapters can make styled-lines tooltips from the engine's own tooltip text, for objects such as an engine visual.

## Held modifiers

`EventTypes.MODIFIERS_CHANGED` fires at the screen's content and each open overlay whenever the held modifiers change, by a modifier key or by input that arrives with different ones. Use it for controls that change while a key is held:

```java
--8<-- "overlays/OverlayExamples.java:modifiers"
```

A component reads the current set at any time from `environment().modifiers()`.
