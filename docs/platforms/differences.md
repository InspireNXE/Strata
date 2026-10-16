# Platform differences

Strata hides most engine differences. This page lists the ones that remain between the Minecraft and Swing adapters, so you can build a screen in Swing and know how it will behave in game.

## At a glance

| Topic | Minecraft 26.3 | Swing |
|---|---|---|
| One UI unit | One GUI pixel (follows GUI scale) | `scale` pixels, fixed when the panel is made |
| Rounding | Fills and clips round outward to whole GUI pixels; text to the nearest pixel | None. Java2D draws fractional coordinates. |
| Font | The player's font, from resource packs | An AWT font, Dialog 9 by default |
| Text widths | Whole GUI pixels; bold adds one per character | Fractional |
| Line height | 9 for the default font | The AWT font's height |
| Modifiers with pointer moves, scrolls, characters | Never reported (`NONE`) | Reported |
| Modifiers with button presses and keys | Reported | Reported |
| Edit shortcuts on macOS | Text box uses Control, vanilla uses Command | Control |
| Key repeat | Held keys repeat at the system rate | Same |
| Typed characters | Only while a text-input component has focus | Always |
| Horizontal scrolling | From the device, flipped to Strata's direction | Shift plus the vertical wheel |
| Escape | Closes the screen unless consumed | Does nothing unless you handle it |
| Clipboard | Vanilla's | The system clipboard; none when headless |
| Missing sprite | Vanilla's missing-texture pattern | Magenta |
| Nine-slice slicing | From the sprite's `.png.mcmeta`; the `border` argument is ignored | From the `border` argument, in image pixels |
| Nine-slice edges and center | Tiled, unless the `.mcmeta` sets `stretch_inner` | Stretched |
| `NOT_ALLOWED` cursor | Shown | Shows the default arrow |
| Screen stack | Vanilla's; one Strata screen per `StrataScreen` | `StrataPanel.screens()` |
| `hidesBackground()` | No effect | Skips drawing screens below |
| Pausing | Single-player pauses unless `isPauseScreen()` is false | Not applicable |
| UI thread | The render thread | The event dispatch thread |

The sections below cover the differences most likely to affect a screen.

## Units and rounding

On Minecraft, everything snaps to whole GUI pixels when drawn, while layout itself stays in floats. A panel at x = 10.4 with width 20.2 fills from pixel 10 to pixel 31, never shrinking. Text snaps to the nearest pixel. On Swing, the same panel is drawn at its fractional position. Design with whole numbers and the two look alike.

The GUI scale changes how many screen pixels a unit is, not how many units fit. A screen at GUI scale 4 has half as many units across as at GUI scale 2. Use anchors, stretching and weighted grids rather than fixed positions, so layouts fit both.

## Text

Minecraft's pixel font and an AWT font have different widths, and Minecraft's widths are whole pixels. Text that just fits in Swing may wrap or overflow in game. Never size components by guessing text widths. Let components measure, and leave slack in fixed widths around text.

## Modifier keys

Minecraft passes no modifiers with pointer moves, scrolls or typed characters. A Shift+scroll or Ctrl+drag check works in Swing and never fires in game. Button presses and key events carry modifiers on both. If you need modifier state during a drag, remember it from the `POINTER_PRESS` that started it.

On macOS, vanilla uses Command for edit shortcuts, but Strata's text box checks Control on every platform. A platform shortcut modifier is planned. Until then, Mac players use Control+C, X, V and A in Strata text boxes.

## Typed characters

Minecraft 26.3 delivers characters only while the screen has reported a focused text field. Strata does this automatically for components whose `acceptsTextInput()` is true. A custom component that reads `CHAR` without that override works in Swing and receives nothing in game.

## Scrolling

Minecraft reports horizontal scrolling from devices that have it, and the adapter flips its sign to Strata's convention: positive toward the left. Swing treats Shift plus the vertical wheel as horizontal scrolling. Minecraft's amounts already include the player's scroll sensitivity, so a sensitivity other than 1 turns notched wheels into fractional, `PRECISE` scrolls.

## Escape and closing

In Minecraft, an Escape that no Strata component consumes reaches vanilla, which closes the screen. Swing has no such convention. If your screen should close on Escape everywhere, handle it yourself on the root and call `handle.close()`. If it should never close on Escape, consume the key.

## Nine-slices

Vanilla reads slicing from the sprite's `.png.mcmeta`, where the borders are in the units of the `.mcmeta`'s `width` and `height`, and it tiles the edges and center. Swing uses the paint's `border`, in the image's own pixels, and stretches. For both to match:

- give the image the same pixel size as the `.mcmeta`'s `width` and `height`;
- use the same border in the `.mcmeta` and in `Paint.nineSlice`;
- either set `"stretch_inner": true`, or design edges and center that look the same tiled or stretched, such as flat colors.

## Screens and stacks

Minecraft keeps its own screen stack, so a Strata dialog over another Strata screen is a second `StrataScreen`, which must [override `onClose()`](minecraft.md#returning-to-a-parent-screen) to return to the first. Vanilla decides what is drawn behind it, and `hidesBackground` does nothing. In Swing, both are on the panel's stack and the stack draws them.
