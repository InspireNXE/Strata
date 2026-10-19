# Extending Strata

There are three ways to extend Strata. Pick the lightest one that does the job.

| Way | Use it to | Cost |
|---|---|---|
| [Handle a built-in's events](#change-a-built-in-through-its-events) | Add drawing, change sizes, or filter, rewrite or veto what the user does to any control | A lambda |
| [Compose](#compose-built-ins) | Package several components as one reusable piece | A plain class |
| [Write a new component](#write-a-new-component) or [layout](#write-a-new-layout) | Make something that does not exist yet | A subclass of `CustomComponent` or `CustomContainer` |

Strata's own controls and panels are written the third way, on the same public base classes, so you have everything they have.

![A custom switch and a custom wrapping layout](../assets/images/extending-custom.png)

## Change a built-in through its events

Everything a control does goes through [events](events.md), so most changes to a built-in need no new class, only a handler:

- To stop or change what the user does, handle the control's Pre event: cancel it, or set another value.
- To react, handle its Post event.
- To draw on top, handle `Component.PAINTED`; to draw instead, cancel `Component.PAINT`; to change the size it asks for, handle `Component.MEASURED`.

Filter what a text box accepts, whether the user types or pastes:

```java
--8<-- "extending/ExtendingExamples.java:filter"
```

Rewrite a value on its way in:

```java
--8<-- "extending/ExtendingExamples.java:rewrite"
```

Ask before a window closes. Calling `window.close()` from code fires no `Closable.CLOSE`, so the dialog can close it without asking again:

```java
--8<-- "extending/ExtendingExamples.java:veto"
```

Draw on top of a button:

```java
--8<-- "extending/ExtendingExamples.java:paint"
```

Change how big a label wants to be:

```java
--8<-- "extending/ExtendingExamples.java:measure"
```

A handler you want on many controls is a plain field or method, so reuse costs nothing:

```java
--8<-- "extending/ExtendingExamples.java:reuse"
```

Each control lists its events with its theme keys in [Controls](controls.md), and every event type's Javadoc lists the input that fires it.

## Compose built-ins

When the piece is just existing components arranged a certain way, you need no Strata base class at all. Build the parts in a container and hand out the container:

```java
--8<-- "extending/ExtendingExamples.java:compose"
```

The class is not a `Component`, so you add `field.component()` rather than `field`. To make a composite that is a component itself, so it can be passed anywhere a component can, extend `CustomContainer` and add the parts as its children.

## Write a new component

Subclass `CustomComponent<T>`. To let other code see your component as a type of its own, declare an interface that extends `Component<YourInterface>` and implement it. This example builds a small switch; Strata's own [`ToggleSwitch`](controls.md#toggleswitch) is written the same way in core.

```java
--8<-- "extending/ToggleSwitch.java:interface"
```

```java
--8<-- "extending/ToggleSwitchImpl.java:impl"
```

Using it looks like using a built-in:

```java
--8<-- "extending/ExtendingExamples.java:use"
```

Its required keys must be in the theme:

```java
--8<-- "extending/ExtendingExamples.java:theme"
```

### Events for your own control

Declare your control's events as constants on its interface, a Pre request for each change the user can make and a Post report for each change from any source, and fire them from the implementation:

```java
--8<-- "extending/ToggleSwitch.java:events"
```

```java
--8<-- "extending/ToggleSwitchImpl.java:events"
```

| Made with | Kind | Fire it with |
|---|---|---|
| `EventType.request(name)` | Pre, no value | `request(type)`, which returns whether to go ahead |
| `EventType.valueRequest(name)` | Pre, with a value | `request(type, value, apply)`, which calls `apply` with the final value unless canceled |
| `EventType.report(name)` | Post, no value | `report(type)` |
| `EventType.valueReport(name)` | Post, with a value | `report(type, value, previous)` |

Fire requests only for what the user does, and reports after every change, whoever made it, as the built-ins do. With no handler for a type on the path, `request` and `report` fire nothing and allocate nothing, and `request(type, value, apply)` just calls `apply`. `isHandled(type)` tells you whether anything listens, for work only an event needs. If your component is a `CustomComposite`, the events of the controls you build it from stop at it, so fire your own types for what the outside should hear.

### The hooks

| Hook | Called | Default |
|---|---|---|
| `measure(context, availableWidth, availableHeight)` | During layout. Return the content's natural size. Required. | |
| `paint(painter, bounds)` | Every frame, after the background and before a container's children | Draws nothing |
| `requiredStyleKeys()` | When the component joins a host, and when the theme changes | None |
| `onApplyTheme(theme)` | On attach and whenever the theme changes, before the next layout | Nothing |
| `acceptsTextInput()` | When it gains focus | false |
| `snapshotState()` | When an event with [snapshots](snapshots.md#your-own-components) captures it | null |
| `onClick(event)` and the other `on...` hooks | The default-behavior step of each input event, after the handlers, unless canceled | Nothing |
| `onOtherEvent(type, event)` | For event types not in `EventTypes` | Nothing |

### Rules

- Pass the factory of the host that will show the component. A component cannot move between hosts.
- `measure` must be pure layout. Compute and return a size, and cache what `paint` needs. Strata has already removed the margin and applied the maximum size; it applies the minimum and any fixed size to what you return. The available size may be infinite, and a negative result counts as zero. Do not change other components or call `invalidateLayout()` from it.
- `paint` draws inside the bounds it is given and must not change layout state. It runs every frame, so avoid allocating there. The painter is valid only during the call.
- Call `invalidateLayout()` whenever something `measure` reads changes. Changes that only affect drawing need nothing: the next frame draws them.
- Read time, the clipboard and text sizes through `environment()`, never from `System.currentTimeMillis()` or AWT, so the component works on every engine and in tests.
- Event hooks run only while the component is attached, and a hidden or disabled component gets no new pointer or keyboard input.
- Consume the events you act on, so ancestors and the engine do not act on them too.
- Everything runs on the UI thread. Exceptions thrown from hooks propagate to the engine's call into Strata.

Every `Component` method is `final` on `CustomComponent` and delegates to Strata's half of the component, its peer, so your component takes part in layout, focus, hit testing and events like any other. Return `this.self()` from your own setters to keep chaining typed.

### Painting

The `Painter` a component draws with:

| Method | Draws |
|---|---|
| `fill(rect, argb)` | A solid rectangle |
| `fillGradient(rect, top, bottom)` | A vertical gradient |
| `sprite(id, rect, tint)` | An image stretched over the rectangle |
| `nineSlice(id, rect, border, tint)` | A nine-slice image |
| `text(span, x, y)` | One line of text, top-left at the point |
| `clipped(rect, draw)` | Runs `draw` with drawing limited to the rectangle, intersected with any clip in place |
| `translated(dx, dy, draw)` | Runs `draw` with every coordinate shifted |

Coordinates are host coordinates in UI units. Later calls draw over earlier ones. Clipping and translation take a callback, so the engine's state is always restored, even if the callback throws. Any `Paint` from a theme draws itself with `paint.paint(painter, rect)`.

### The environment

`environment()` gives a component what it may ask of the engine outside layout:

| Method | Returns |
|---|---|
| `text()` | The engine's text metrics, the same ones layout uses |
| `millis()` | The host's clock in milliseconds. Only differences mean anything. |
| `clipboardText()` | The clipboard text, or empty |
| `setClipboardText(text)` | Puts text on the clipboard. Engines without one ignore it. |

It works before the component is attached.

## Write a new layout

Subclass `CustomContainer<T>`. It is a `CustomComponent` that also measures and places children. This flow layout wraps its children into rows like words in a paragraph:

```java
--8<-- "extending/WrapPanel.java:wrap"
```

Everything required of a custom component applies, plus:

- In `measure`, measure every child, hidden or not, with `context.measure(child, width, height)`. It returns the child's desired size plus its margin, or zero for a hidden child. A child you do not measure keeps a stale size.
- In `arrange`, give every child a slot with `context.arrange(child, slot)`, hidden children included. The slot includes the child's margin; the child shrinks it by the margin and aligns itself inside. `context.outerSize(child)` returns what the child asked for in the last measure. A child you do not arrange keeps stale bounds.
- Handle the padding yourself: take it off the space you offer children, add it to the size you return, and place children inside `contentBox()`.
- Keep per-child data, such as a grid cell, in your own map, and drop it in `onChildRemoved`.
- Leave drawing the children to Strata. Your `paint` draws under them, and they are clipped to your bounds while `clipsChildren()` is true.

## Things you cannot do

- Implement `Component`, `Container` or a control interface directly. Strata rejects such classes.
- Subclass the built-in controls. Their implementations are hidden in `core`. Handle their events, compose them, or write your own.
- Draw arbitrary geometry, such as lines or textured triangles. The painter offers rectangles, gradients, sprites and text only, because every engine can draw those. See [Status and limitations](../help/status.md).
- Use `ComponentFactory.createPeer`, `createContainerPeer`, `CustomComponent.peer()` or anything in `api.extension.peer`. They are plumbing between the base classes and core.
