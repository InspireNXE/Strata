# Strata

Strata is a GUI library for Java games that does not depend on any engine. You build screens from a tree of components, and an adapter draws them and feeds them input on a particular engine. Strata has:

- two-pass layout with four containers: `AnchorPanel`, `StackPanel`, `GridPanel` and `AbsolutePanel`
- labels, images, buttons, check boxes, sliders and a single-line text box
- click counting, dragging, focus and tab order
- typed events that handlers can cancel
- themes that map style keys to colors, gradients and sprites

There are adapters for Java2D/Swing and for Minecraft 26.3 on NeoForge and Fabric.

| Module | What it is |
|---|---|
| `api` | What developers compile against: interfaces, value records, the `ComponentFactory`, the widgets (`Label`, `Image`, `Button`, `CheckBox`, `Slider`, `TextBox`, `ListView`, `ComboBox`, `Window`, `MessageBox`, `TabBar`, `ProgressBar`, `Separator`), the containers (including `ScrollPanel`), overlays and tooltips, and the extension points (`CustomComponent`, `CustomComposite`, `CustomContainer`, `ComponentBehavior`, `Painter`, `Environment`). |
| `spi` | What engine adapters implement (`Canvas`, `Platform`) and drive (`ScreenHost`, `ScreenStack`), plus contract tests for adapters. |
| `core` | The implementation behind both. Only adapters use it directly. |
| `canvas-swing` | The Java2D/Swing adapter: `StrataPanel` and `SwingSnapshot`. |
| `sandbox` | A demo app written against the API only. |
| `mc-common` | The Minecraft 26.3 adapter: `StrataScreen`, `MinecraftCanvas` and `MinecraftThemes.vanilla()`. |
| `mc-neoforge`, `mc-fabric` | One `strata` mod jar per loader, containing `api`, `spi`, `core` and `mc-common`. |

[docs/DESIGN.md](docs/DESIGN.md) covers the design and where its ideas come from (MalisisCore and Wintergreen). The Javadoc of `api` and `spi` is the contract, including what adapters must guarantee.

## Writing a screen

A screen builds its tree from the factory its host passes in:

```java
public final class SettingsScreen implements Screen {
    @Override
    public Component<?> build(final ComponentFactory factory) {
        final Label status = factory.label("Ready");
        final Button save = factory.button("Save").onAction(() -> status.setText("Saved"));
        final StackPanel column = factory.stackPanel().setSpacing(4).setPadding(8).add(status).add(save);
        return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
    }
}
```

You can extend Strata in three ways, from lightest to heaviest:

- Decorate a built-in with a behavior, as in `factory.button("Save", new ButtonBehavior() { ... })`. Each hook gets a `next` handle that runs the built-in behavior, much like calling `super`.
- Compose built-ins inside a container and expose the group through an interface of your own.
- Write a new type by extending `CustomComponent<T>`, or `CustomContainer<T>` for a layout. The class can implement an interface of your own, so other code sees it as its own type.

Do not implement the component interfaces directly. Strata rejects such a class when it is added to a container.

## Showing it in Minecraft

`StrataScreen` is a vanilla `Screen`, so you open it like any other:

```java
Minecraft.getInstance().gui.setScreen(
        new StrataScreen(Component.literal("Settings"), new SettingsScreen(), MinecraftThemes.vanilla()));
```

Input that Strata does not use goes on to vanilla, so Escape still closes the screen.

## Showing it in Swing

`StrataPanel` is a `JPanel` that shows a stack of screens. The last argument is the number of pixels per UI unit:

```java
StrataPanel panel = new StrataPanel(new SwingPlatform(SwingTextMetrics.defaults()), Themes.flat(), 2f);
panel.screens().push(new SettingsScreen());
frame.setContentPane(panel);
```

## Building

Building needs JDK 25.

```bash
./gradlew build
```

To run the sandbox in a window:

```bash
./gradlew :sandbox:run
```

To render one frame to a PNG instead, which works without a display:

```bash
./gradlew :sandbox:run --args="--snapshot sandbox.png"
```
