# Overlays and tooltips

Some things draw above a screen instead of inside it: a drop-down's list, a confirmation box, an item's tooltip. Strata keeps them out of the screen's tree, so no container clips them and they never push the layout around.

Each host has one `Overlays`, from `factory.overlays()`. Overlays stack in the order they open, each above the screen and the overlays before it.

## Popups

A popup shows a component next to an area, usually the bounds of the component that opened it.

```java
--8<-- "widgets/OverlayExamples.java:popup"
```

The popup takes its desired size. It sits below the area with left edges lined up, above it when there is more room above, and moves left as needed to stay inside the host. A press anywhere outside the popup closes it, and that press goes nowhere else, so clicking another button while a menu is open only closes the menu. Escape closes it too. The [`ComboBox`](widgets.md#combobox) is built this way.

## Dialogs

A dialog is modal. Its content is laid out in the whole host, where its own alignment places it, so a centered [window](widgets.md#window) stays centered when the host resizes.

```java
--8<-- "widgets/OverlayExamples.java:dialog"
```

While a dialog is open:

- Nothing below it gets the pointer, focus or Tab. Keys go to it when nothing in it has focus.
- The screen below stays visible, under the theme's `overlay.backdrop` paint if it has one.
- Focus moves to the first tabbable component in it when it opens, and back to where it was when it closes.
- An Escape press that nothing consumed closes it. A window closes its dialog itself, telling its close listeners first.

A [`MessageBox`](widgets.md#messagebox) is a ready-made dialog for confirmations:

```java
--8<-- "widgets/OverlayExamples.java:message-box"
```

## Working with open overlays

| Call | Does |
|---|---|
| `overlay.close()` | Closes it and detaches its content. Closing twice does nothing. |
| `overlay.onClosed(listener)` | Runs once it has closed, however that happened |
| `overlays.of(component)` | Finds the overlay a component is shown in, for a button inside a dialog that closes it |
| `overlays.open()` | Every open overlay, bottom first |
| `overlays.closeAll()` | Closes them all, top first |

Content must be new: a component already in a tree is refused. Closing or rebuilding the screen closes every overlay.

On Minecraft, dialogs are overlays inside one vanilla screen, so they do not need a screen of their own, and the screen behind keeps updating.

## Tooltips

Any component can have a tooltip. It shows while the pointer rests on the component or anything inside it that has none of its own, follows the pointer 12 units right of and above it (left of it near the right edge), and stays inside the host. It hides while a drag is in progress. Tooltips take no input.

```java
--8<-- "widgets/OverlayExamples.java:tooltips"
```

| Kind | Made with | Drawn |
|---|---|---|
| Plain text | `setTooltip(String)`, `Tooltip.text` | In the `tooltip.text` style, `\n` for new lines |
| Styled lines | `Tooltip.lines(lines)` | Each span in its own style |
| A component | `Tooltip.of(factory -> component)` | Built again each time the tooltip shows, with no box of its own |

Text tooltips draw on the `tooltip.background` paint with 4 units of padding, or on a dark box with a purple edge, like vanilla's classic tooltip, when the theme lacks the key.

On Minecraft, `MinecraftTooltips.item(stack)` makes a styled-lines tooltip from an item's vanilla tooltip; see [Item tooltips](../platforms/minecraft.md#item-tooltips).

## Held modifiers

`EventTypes.MODIFIERS_CHANGED` fires at the screen's content and each open overlay whenever the held modifiers change, by a modifier key or by input that arrives with different ones. Use it for controls that change while a key is held:

```java
--8<-- "widgets/OverlayExamples.java:modifiers"
```

A component reads the current set at any time from `environment().modifiers()`.
