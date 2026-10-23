<p align="center"><img src=".github/assets/logo.svg" width="128" alt="Strata logo"></p>

# Strata

Strata is a GUI library for Java that works with any engine. You build screens from layout panels and a full set of controls, style them with themes, and a thin adapter draws them and feeds them input on your engine. The same screen runs everywhere Strata has an adapter.

- Five layout panels: anchor, stack, grid, absolute and scroll.
- Controls from labels and buttons to text fields, lists, tables, trees, menus, date pickers and dialogs.
- One event system for input and for every change to a control. Handlers can cancel or rewrite what the user asks for.
- Themes that map style keys to colors, gradients and sprites, written in code or JSON and resolved in layers.

## Platforms

| | Platform | Repository | Versions |
|---|---|---|---|
| <img src=".github/assets/platform-swing.svg" width="40" alt=""> | Java2D | [Strata-Swing](https://github.com/InspireNXE/Strata-Swing) | Java 25 |
| <img src=".github/assets/platform-minecraft.svg" width="40" alt=""> | Minecraft | [Strata-Minecraft](https://github.com/InspireNXE/Strata-Minecraft) | 26.3 on NeoForge, Forge, Fabric and SpongeVanilla |
| <img src=".github/assets/platform-engine.svg" width="40" alt=""> | Your engine | [Writing an adapter](https://inspirenxe.github.io/Strata-Docs/platforms/writing-an-adapter/) | |

Every artifact is published to Maven Central under the group `org.inspirenxe`. Until 0.1.0 is released, there are snapshots only, in Central's snapshot repository. [Installation](https://inspirenxe.github.io/Strata-Docs/getting-started/installation/) has the setup.

## A screen

```java
public final class SettingsScreen implements Screen {
    @Override
    public Component<?> build(final ComponentFactory factory) {
        final Label status = factory.label("Ready");
        final Button save = factory.button("Save").onEvent(Button.PRESSED, e -> status.setText("Saved"));
        final StackPanel column = factory.stackPanel().setSpacing(4).setPadding(8).add(status).add(save);
        return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
    }
}
```

Open it with `Strata.screens().open(new SettingsScreen())` on any platform.

## Modules

| Module | What it is |
|---|---|
| `api` | What developers compile against: the factory, controls, panels, events, themes and extension points. |
| `spi` | What engine adapters implement and drive, plus contract tests for adapters. |
| `core` | The implementation. Only adapters use it directly. |

## Building

Building needs JDK 25.

```bash
./gradlew build
```

Strata-Swing, Strata-Minecraft and Strata-Gallery build against a Strata checkout beside them when there is one, so a change here reaches them at once. Without one they take Strata from Maven Central. Strata-Docs builds its Javadoc and design pages from the checkouts, so it needs Strata and Strata-Swing beside it.

## Documentation

The [documentation site](https://inspirenxe.github.io/Strata-Docs/) has the guides, the reference and the Javadoc. Its source is in [Strata-Docs](https://github.com/InspireNXE/Strata-Docs). To try every control, run [Strata-Gallery](https://github.com/InspireNXE/Strata-Gallery).

## License

Strata is released under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
