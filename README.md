# Strata

Strata is an engine-agnostic GUI library for Java games. It provides:

- a retained component tree with two-pass layout (`Panel`, `StackPanel`, `Grid`, `AbsolutePanel`)
- click counting and dragging
- focus and tab order
- typed events you can cancel
- theming by style key

Developers write against a pure API. Engines plug in through a small SPI.

| Module | What it is |
|---|---|
| `api` | What developers compile against: interfaces, value records, the `Ui` factory, and the extension points (`CustomComponent`, `CustomContainer`, `ComponentBehavior`, `Painter`). |
| `spi` | What engine adapters implement and drive: `Canvas`, `Platform`, `UiHost`, `ScreenStack`. |
| `core` | The implementation behind both. Only adapters use it directly. |
| `canvas-swing` | Java2D/Swing adapter. |
| `sandbox` | Demo app written against the API only. |
| `mc-common` | Minecraft 26.3 adapter: `StrataScreen`, `MinecraftCanvas`, `MinecraftThemes.vanilla()`. |
| `mc-neoforge`, `mc-fabric` | One mod jar (`strata`) per loader, containing `api`, `spi`, `core` and `mc-common`. |

The design, and where its ideas come from (MalisisCore and Wintergreen), is in [docs/DESIGN.md](docs/DESIGN.md).

## Writing a screen

```java
public final class SettingsScreen implements Screen {
    @Override
    public Component<?> build(final Ui ui) {
        final Label status = ui.label("Ready");
        final Button save = ui.button("Save").onAction(() -> status.setText("Saved"));
        return ui.stackPanel().setSpacing(4).setPadding(8).add(status).add(save);
    }
}
```

There are three ways to extend Strata:
- **Decorate a built-in:** `ui.button("Save", new ButtonBehavior() { ... })`. Each hook gets a `next` that runs the normal behavior.
- **Write a new component type:** extend `CustomComponent<T>`, or `CustomContainer<T>` for layouts. The component can implement your own interfaces.
- **Compose:** a `CustomContainer` that holds built-in children.

Implementing the component interfaces directly is not supported. Strata rejects such components when they are added to a tree.

## Using it in Minecraft

```java
Minecraft.getInstance().setScreen(
        new StrataScreen(Component.literal("Settings"), new SettingsScreen(), MinecraftThemes.vanilla()));
```

## Building

Building needs JDK 25.

```bash
./gradlew build
```

Run the sandbox in a window:

```bash
./gradlew :sandbox:run
```

Or render a single frame to a PNG, which works without a display:

```bash
./gradlew :sandbox:run --args="--snapshot sandbox.png"
```
