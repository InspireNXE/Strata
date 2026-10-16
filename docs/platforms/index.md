# Platforms

Strata runs wherever an adapter exists. Two ship with it:

| Adapter | Module | Show a screen with | One UI unit is |
|---|---|---|---|
| [Minecraft 26.3](minecraft.md), NeoForge and Fabric | `mc-common`, packaged by `mc-neoforge` and `mc-fabric` | `StrataScreen`, a vanilla `Screen` | One GUI pixel |
| [Swing](swing.md) | `canvas-swing` | `StrataPanel`, a `JPanel` | `scale` pixels |

Screens written against the API run on both unchanged. A few behaviors differ, mostly in fonts, modifier keys and nine-slices; [Platform differences](differences.md) lists them all.

To run Strata on another engine, see [Writing an adapter](writing-an-adapter.md).
