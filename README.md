# Strata

Strata is a GUI library for Java games that does not depend on any engine. You build screens from a tree of components, and an adapter draws them and feeds them input on a particular engine. Strata has:

- two-pass layout with four containers: `AnchorPanel`, `StackPanel`, `GridPanel` and `AbsolutePanel`
- labels, images, buttons, check boxes, sliders and a single-line text box
- click counting, dragging, focus and tab order
- typed events that handlers can cancel
- themes that map style keys to colors, gradients and sprites

This repository has the Java2D/Swing adapter. The Minecraft adapter lives in the [Strata-Minecraft](https://github.com/InspireNXE/Strata-Minecraft) repository.

| Module | What it is |
|---|---|
| `api` | What developers compile against: interfaces, value records, the `ComponentFactory`, the controls (`Label`, `Image`, `Button`, `CheckBox`, `Slider`, `TextBox`, `ListView`, `ComboBox`, `Window`, `MessageBox`, `TabBar`, `ProgressBar`, `Separator`, `RadioButton`, `ToggleSwitch`, `Spinner`, `TextArea`, `TableView`, `TreeView`, `Menu`, `MenuBar`, `Expander`, `SplitPane`, `SlotGrid`), the containers (including `ScrollPanel`), overlays and tooltips, and the extension points (`CustomComponent`, `CustomComposite`, `CustomContainer`, `ComponentBehavior`, `Painter`, `Environment`). |
| `spi` | What engine adapters implement (`Canvas`, `Platform`) and drive (`ScreenHost`, `ScreenStack`), plus contract tests for adapters. |
| `core` | The implementation behind both. Only adapters use it directly. |
| `canvas-swing` | The Java2D/Swing adapter: `StrataPanel` and `SwingSnapshot`. |
| `sandbox` | A demo app written against the API only. |
| `docs-examples` | The code the documentation site shows, compiled and tested by the build. |

[docs/DESIGN.md](docs/DESIGN.md) covers the design and where its ideas come from (MalisisCore and Wintergreen). The Javadoc of `api` and `spi` is the contract, including what adapters must guarantee.

## Writing a screen

A screen builds its tree from the factory its host passes in:

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

You can extend Strata in three ways, from lightest to heaviest:

- Change a built-in through its events, with `onEvent(type, handler)` and an optional priority. Pre events (`TextBox.EDIT`, `Window.CLOSE`) can be canceled or rewritten, Post events (`TextBox.EDITED`) report every change, and drawing and sizing are events too (`Component.PAINTED`, `Component.MEASURED`).
- Compose built-ins inside a container and expose the group through an interface of your own.
- Write a new type by extending `CustomComponent<T>`, or `CustomContainer<T>` for a layout. The class can implement an interface of your own, so other code sees it as its own type.

Do not implement the component interfaces directly. Strata rejects such a class when it is added to a container.

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
