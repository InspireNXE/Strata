# Platform differences

Strata hides most engine differences, but the SPI leaves some decisions to each adapter. This page lists them, with what the Swing adapter does, so you can build a screen in Swing and know which parts may behave differently on another engine. Each adapter's own documentation says what it does in each case.

## At a glance

| Topic | The SPI allows | Swing |
|---|---|---|
| One UI unit | Whatever the adapter chooses, often one engine pixel times a user scale | `scale` pixels, fixed when the panel is made |
| Rounding | Rounding to the engine's pixel grid, as long as drawing and clipping agree | None. Java2D draws fractional coordinates. |
| Font and text widths | Any font, with whole or fractional widths | An AWT font, Dialog 9 by default, with fractional widths |
| Line height | Whatever the font reports | The AWT font's height |
| Modifiers with pointer moves, scrolls and characters | `NONE` when the engine does not report them | Reported |
| Modifiers with button presses and keys | Reported | Reported |
| Key repeat | Reported by the engine, or made by the host for navigation and delete keys | Held keys repeat at the system rate |
| Typed characters | Only after the adapter's `TextInputSink` starts text input, if the engine needs that | Always |
| Horizontal scrolling | From the device, in Strata's direction | Shift plus the vertical wheel |
| Escape | Passed on to the engine when no component consumes it | Does nothing unless you handle it |
| Clipboard | The engine's, or `Clipboard.NONE` | The system clipboard; none when headless |
| Missing sprite | Anything visible | Magenta |
| Nine-slice slicing | The `border` argument, or slicing the engine stores with the image | From the `border` argument, in image pixels |
| `NOT_ALLOWED` cursor | Any cursor the engine has | Shows the default arrow |
| Screen stack | `ScreenStack`, or one host per engine screen when the engine stacks screens itself | `StrataPanel.screens()` |
| UI thread | The engine's UI or render thread | The event dispatch thread |

The sections below cover the differences most likely to affect a screen.

## Units and rounding

Layout always runs in floats. An adapter for an engine that draws on whole pixels rounds when it draws, so a panel at x = 10.4 with width 20.2 may cover pixels 10 to 31 there and its exact fractional span in Swing. Design with whole numbers and the two look alike.

When an engine has a user scale setting, it changes how many screen pixels a unit is, which changes how many units fit across the screen. Use anchors, stretching and weighted grids rather than fixed positions, so layouts fit every size.

## Text

Fonts differ in width from one engine to the next, and some report whole-pixel widths. Text that just fits in Swing may wrap or overflow elsewhere. Never size components by guessing text widths. Let components measure, and leave slack in fixed widths around text.

## Modifier keys

Some engines pass no modifiers with pointer moves, scrolls or typed characters, and the adapter then reports `Modifiers.NONE`. A Shift+scroll or Ctrl+drag check that works in Swing may never fire there. Button presses and key events carry modifiers on every adapter. If you need modifier state during a drag, remember it from the `POINTER_PRESS` that started it.

Strata's text box checks Control for its edit shortcuts on every platform, including macOS. A platform shortcut modifier is planned.

## Typed characters

Some engines deliver characters only while the application reports a focused text field. Strata starts text input through the adapter's `TextInputSink` for components whose `acceptsTextInput()` is true. A custom component that reads `CHAR` without that override works in Swing and may receive nothing elsewhere.

## Scrolling

Adapters report horizontal scrolling from devices that have it, with the sign flipped where needed to Strata's convention: positive toward the left. Swing treats Shift plus the vertical wheel as horizontal scrolling. Some engines scale scroll amounts by a user sensitivity setting, so a notched wheel can arrive as fractional, `PRECISE` scrolls there.

## Escape and closing

An Escape that no component consumes goes back to the engine, and some engines close the current screen on it. Swing does nothing. If your screen should close on Escape everywhere, handle it on the root and call `handle.close()`. If it should never close on Escape, consume the key.

## Nine-slices

Swing slices with the paint's `border`, in the image's own pixels, and stretches the edges and center. An engine that stores slicing with the image may use that instead, ignore `border`, and tile instead of stretching. To look the same on both, keep the image's own slicing in step with the border you pass to `Paint.nineSlice`, and design edges and centers that look the same tiled or stretched, such as flat colors.

## Screens and stacks

In Swing, every screen sits on the panel's stack, which draws them and honors `hidesBackground()`. An engine with its own screen system may give each Strata screen its own host and engine screen instead. There, the engine decides what draws behind a screen and `hidesBackground()` has no effect. Dialogs drawn as [overlays](../guides/overlays.md) behave the same on every adapter, because they live inside one host.
