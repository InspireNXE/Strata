# Status and limitations

Strata is at version 0.1.0 and has not been released. This page lists what is tested, what has only been tested indirectly, known issues, and what is planned.

## What is tested

| Area | How |
|---|---|
| Layout, input, focus, events, snapshots, extension, the controls, scrolling, lists, overlays and tooltips | Unit tests in `core` against a test platform with a manual clock |
| Building real screens | Mock-ups of Almura's Exchange and Store screens in the sandbox, rendered in a dozen states by `--mocks` |
| The SPI contracts | Contract tests run on the test platform, on the Swing adapter, and on the example adapter in these docs |
| The Swing adapter | Every contract, sprite, tint and nine-slice drawing on generated images, key and button mapping, a panel driven with synthetic AWT events, end-to-end snapshots |
| These docs | Every Java example compiles, and the example adapter and snapshot timing are tested |

## What is not tested

Publishing to Maven and depending on Strata from a separate project have not been tried. Adapters in other repositories list their own status.

## Known issues

- On macOS, the text box's shortcuts use Control, not Command, so they do not match the rest of the system.
- A key held while focus moves keeps repeating into the newly focused component. Holding Backspace while clicking into another text box deletes text there.

## Not built yet

- Controls: multiple selection in lists, tables and trees, editable table cells.
- Inline sprites in text. `SpriteSpan` and `InlineElement` are reserved.
- Firing custom event types.
- Narration and other accessibility support.
- Controller navigation.
- Drawing beyond fills, gradients, sprites and text.
- A public Maven repository.

## Open design questions

These are recorded in the [design document](../DESIGN.md#8-decisions-and-open-questions):

- A platform shortcut modifier: Command on macOS, Control elsewhere.
- Whether `ListView`, `TableView` and `TreeView` need multiple selection.

## Compatibility promise

None yet. Until 1.0, any release may change the API. Once there are releases, the [changelog](https://github.com/InspireNXE/Strata/releases) will list breaking changes.
