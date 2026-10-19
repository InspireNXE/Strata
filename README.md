# Strata

Strata is a GUI library for Java games that does not depend on any engine. You build screens from a tree of components, and an adapter draws them and feeds them input on a particular engine. Strata has:

- two-pass layout with four containers: `AnchorPanel`, `StackPanel`, `GridPanel` and `AbsolutePanel`
- labels, images, buttons, check boxes, sliders and a single-line text box
- click counting, dragging, focus and tab order
- typed events that handlers can cancel
- themes that map style keys to colors, gradients and sprites

This repository holds the library alone. Adapters live in their own repositories: the Java2D/Swing adapter in Strata-Swing and the Minecraft adapter in [Strata-Minecraft](https://github.com/InspireNXE/Strata-Minecraft).

| Module | What it is |
|---|---|
| `api` | What developers compile against: interfaces, value records, the `ComponentFactory`, the controls (`Label`, `Image`, `Button`, `CheckBox`, `Slider`, `TextBox`, `ListView`, `ComboBox`, `Window`, `MessageBox`, `TabBar`, `ProgressBar`, `Separator`, `RadioButton`, `ToggleSwitch`, `Spinner`, `TextArea`, `TableView`, `TreeView`, `Menu`, `MenuBar`, `Expander`, `SplitPane`, `GridView`) and the attributes they share (`Toggle`, `SingleSelection`, `TextInput` and others), the containers (including `ScrollPanel`), overlays and tooltips, and the extension points (`CustomComponent`, `CustomComposite`, `CustomContainer`, `Painter`, `Environment`). |
| `spi` | What engine adapters implement (`Canvas`, `Platform`) and drive (`ScreenHost`, `ScreenStack`), plus contract tests for adapters. |
| `core` | The implementation behind both. Only adapters use it directly. |

[docs/design-implemented.md](docs/design-implemented.md) covers the design as built and where its ideas come from (MalisisCore and Wintergreen), [docs/design-pending.md](docs/design-pending.md) holds the proposals awaiting review and the open questions, and [docs/ROADMAP.md](docs/ROADMAP.md) tracks what is built and what is next. The Javadoc of `api` and `spi` is the contract, including what adapters must guarantee.

The documentation site (guides, reference, platform notes and the compiled examples the pages show) is in the Strata-Docs repository, a sibling of this one at `../Strata-Docs`. It builds against this checkout, so check the two out side by side. Nothing is published yet.

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

- Change a built-in through its events, with `onEvent(type, handler)` and an optional priority. Pre events (`TextInput.EDIT`, `Closable.CLOSE`) can be canceled or rewritten, Post events (`TextInput.EDITED`) report every change, and drawing and sizing are events too (`Component.PAINTED`, `Component.MEASURED`).
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

Every control has a page in the gallery, which runs on the Java2D adapter from Strata-Swing and lives in the Strata-Gallery repository, a sibling of this one at `../Strata-Gallery`. From there:

```bash
./gradlew run
```

## License

Strata is released under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
