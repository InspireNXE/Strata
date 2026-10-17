# Platforms

Strata runs wherever an adapter exists. This repository ships one:

| Adapter | Module | Show a screen with | One UI unit is |
|---|---|---|---|
| [Swing](swing.md) | `canvas-swing` | `StrataPanel`, a `JPanel` | `scale` pixels |

The Minecraft adapter lives in the [Strata-Minecraft](https://github.com/InspireNXE/Strata-Minecraft) repository.

Screens written against the API run on every adapter unchanged. A few behaviors differ between engines, mostly in fonts, modifier keys and nine-slices; [Platform differences](differences.md) lists what an adapter may decide for itself.

To run Strata on another engine, see [Writing an adapter](writing-an-adapter.md).
