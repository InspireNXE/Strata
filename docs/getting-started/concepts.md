# Core concepts

A handful of ideas explain almost everything Strata does. Each section links to the guide that covers it in depth.

## The module split

Strata has three layers, and you only ever compile against the first one.

| Layer | Package | Who uses it |
|---|---|---|
| API | `org.inspirenxe.strata.api` | Everyone writing screens and components. Interfaces and values only. |
| SPI | `org.inspirenxe.strata.spi` | People writing an engine adapter. |
| Core | `org.inspirenxe.strata.core` | Nobody directly. It implements both, and adapters create hosts from it. |

Each adapter adds one more package for its engine, such as `org.inspirenxe.strata.swing`. You use a few classes from your engine's adapter to show screens (`StrataPanel` on Swing) and nothing else from it.

Because the API is interfaces, the implementation can change between versions without breaking your code.

## Components and the tree

A UI is a tree of components. Containers (the four panels) hold children and decide where they go; leaves (labels, buttons, text boxes) draw themselves. Each component has at most one parent. A container draws its children in the order they were added, so later children are drawn on top and are hit by the pointer first.

Components are interfaces such as `Button` and `Label`. You get them from a factory, or by subclassing `CustomComponent`. Never implement a component interface yourself. Such a class has no implementation behind it, and adding it to a container throws `IllegalArgumentException`. See [Components](../guides/components.md).

## The factory and the host

A host shows a screen: it owns the tree, runs layout, routes input, tracks focus and draws. Every host has its own `ComponentFactory`, which a screen receives in `build`. A component belongs to the factory that made it and can only join trees in that factory's host. That is why there is no `Strata.button(...)` static method.

You rarely see a host. On Swing, `StrataPanel` holds a stack of them. An adapter for an engine with its own screen system may give each engine screen a host of its own.

## UI units

All positions and sizes are floats in UI units, with the origin at the top left of the host, x growing right and y growing down. The adapter decides how big a unit is:

- In Swing, one unit is `scale` pixels, a number you pass to `StrataPanel`.
- Other adapters usually tie a unit to the engine's pixels and the player's scale setting.

Layout happens in floats. An adapter for an engine that draws whole pixels rounds when it draws.

## Layout in two passes

Layout runs before a frame is drawn, and only after something changed:

1. Measure. Each parent offers its children some space, and each child reports the size it wants: its desired size.
2. Arrange. Each parent gives each child a slot, and the child places itself in the slot using its margin, alignment and size limits. The result is its bounds.

Setters that affect size schedule a new layout automatically. Margin is space outside a component; padding is space inside a container. See [Layout](../guides/layout.md).

## Events

Input reaches components as events. For every component an event reaches, three steps run in order: the component's `BEFORE` handlers, then its default behavior (a button pressing, a text box inserting a character) unless a handler canceled the event, then its `AFTER` handlers. Bubbling events then move on to the parent. Built-in controls consume the input they act on, which stops it there.

When an event finishes, the host tells the adapter whether it was handled. Unhandled input goes on to the engine, so an engine that closes screens on Escape still does. See [Events](../guides/events.md).

## Focus

At most one component per host has keyboard focus. Key events go to it, or to the screen's root when nothing has focus, and typed characters go only to it. Tab and Shift+Tab move focus between tabbable components. See [Input and focus](../guides/input-focus.md).

## Themes

A theme maps style keys, such as `button.hovered`, to paints (solid colors, gradients, sprites, nine-slices) and text styles. Every component in a host shares the host's theme. Controls mark some keys as required, and a host refuses to show a tree whose theme lacks any of them, listing every missing key at once. `Themes.flat()` works on any engine, and adapters can ship themes that match their engine's own look. See [Themes](../guides/theming.md).

## Threading

Strata is single-threaded and does no locking. Create, change and read components only on the engine's UI thread: such as the Swing event dispatch thread, or a game's render thread. Handlers run on that thread too.

## What Strata leaves out

- No static registries or global state. Each host is independent, and closing a screen releases its tree.
- No immediate-mode drawing. You describe a tree once and change it; Strata draws it every frame.
- No engine types in the API. A key is a `Key`, never an SDL, GLFW or AWT number.
- No reflection or mixins into the engine.
