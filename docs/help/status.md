# Status and limitations

Strata is at version 0.1.0 and has not been released. This page lists what is tested, what has only been tested indirectly, known issues, and what is planned.

## What is tested

| Area | How |
|---|---|
| Layout, input, focus, events, snapshots, extension, the widgets, scrolling, lists, overlays and tooltips | Unit tests in `core` against a test platform with a manual clock |
| Building real screens | Mock-ups of Almura's Exchange and Store screens in the sandbox, rendered in a dozen states by `--mocks` |
| The SPI contracts | Contract tests run on the test platform, on the Swing adapter, and on the example adapter in these docs |
| The Swing adapter | Every contract, sprite, tint and nine-slice drawing on generated images, key and button mapping, a panel driven with synthetic AWT events, end-to-end snapshots |
| Minecraft input mapping | Tested against every key constant in the real 26.3 `InputConstants` |
| The Minecraft canvas | Tested against a mocked `GuiGraphicsExtractor`, including item visuals made from real item stacks |
| Minecraft item tooltips | Styling tested against real chat components |
| Loader packaging | The processed mod metadata and the NeoForge entry point |
| These docs | Every Java example compiles, and the example adapter and snapshot timing are tested |

## What is not tested

Nothing has run inside a Minecraft client yet. Minecraft drawing (item rendering included), vanilla's handling of small nine-slices, text input focus and mod loading on both loaders are covered only by compiling against the real classes and by unit tests with mocks. Publishing to Maven and depending on Strata from a separate mod project have not been tried.

## Known issues

- On macOS, the text box's shortcuts use Control, not Command, so they do not match the rest of the system or vanilla.
- A key held while focus moves keeps repeating into the newly focused component. Holding Backspace while clicking into another text box deletes text there.
- The Minecraft adapter cannot change a `StrataScreen`'s theme after it is created.

## Not built yet

- Widgets: multi-line text area, vertical slider, tree. Behavior interfaces for the newer widgets.
- Inline sprites in text. `SpriteSpan` and `InlineElement` are reserved.
- Tooltip images, such as a bundle's contents, in Minecraft item tooltips.
- Firing custom event types.
- Minecraft HUD layers, through `RegisterGuiLayersEvent` on NeoForge and `HudElementRegistry` on Fabric.
- Vanilla widgets inside Strata (`MinecraftUi.wrap`) and Strata inside vanilla screens (`StrataElement`).
- Narration and other accessibility support.
- Controller navigation.
- Drawing beyond fills, gradients, sprites and text.
- A public Maven repository.

## Open design questions

These are recorded in the [design document](../DESIGN.md#8-decisions-and-open-questions):

- Custom geometry on Fabric: an accessor mixin on `GuiGraphicsExtractor`, or staying with fills, sprites and text.
- A platform shortcut modifier: Command on macOS, Control elsewhere.
- How Minecraft item tooltip images should draw.

## Compatibility promise

None yet. Until 1.0, any release may change the API. Once there are releases, the [changelog](https://github.com/InspireNXE/Strata/releases) will list breaking changes.
