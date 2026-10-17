# FAQ

## General

### What engines does Strata support?

This repository has the Swing (Java2D) adapter, which runs on any desktop JVM. [Platforms](../platforms/index.md) lists the adapters that live elsewhere. Other engines need an adapter, which is a small amount of code; see [Writing an adapter](../platforms/writing-an-adapter.md).

### Is it ready for production?

Not yet. Strata is at 0.1.0, unreleased, and the API will change. See [Status and limitations](status.md).

### Is it a port of MalisisCore?

No. Strata takes ideas from MalisisCore (a retained component tree, anchors, cancellable events) and from Wintergreen (measure-and-arrange layout, click and drag handling, focus, typed events, the text box), but it is new code with no engine dependency. The [design document](../DESIGN.md) records where each idea comes from and what Strata does differently.

### What license?

MIT.

## Writing screens

### Why can't I write `new Button()`?

Components are interfaces with a hidden implementation. You get them from the `ComponentFactory` the host passes to your screen, so every component belongs to the host that shows it. See [Core concepts](../getting-started/concepts.md#the-factory-and-the-host).

### Why is there no static way to get a factory?

Plugin systems often give each plugin its own class loader, and one application can show several hosts at once. Passing the factory keeps every tree tied to the host that shows it, and leaves no global state behind when a screen closes.

### Can I implement `Component` or `Button` myself?

No. A class that implements a component interface directly is rejected with `IllegalArgumentException`. Extend `CustomComponent` or `CustomContainer`. See [Extending Strata](../guides/extending.md).

### Can I subclass the built-in widgets?

No, their implementations are hidden. Decorate them with a behavior, compose them, or write your own component. See [Extending Strata](../guides/extending.md).

### Do I need to call layout or repaint?

Never. Setters that affect size schedule a layout, and the host draws every frame.

### Why is `bounds()` zero or out of date?

Layout runs before the next frame, not when you call a setter. Before the first layout, bounds are `Rect.ZERO`. Read bounds in an event handler or a later frame.

### How do I make something fill the rest of a column?

Use a `GridPanel` with a `weighted` row or column. A `StackPanel` never shares out leftover space. See [Filling the remaining space](../guides/layout.md#filling-the-remaining-space).

### How do I center a dialog?

Put it in a root `AnchorPanel` anchored with `Anchor.FILL`, and give the dialog `Anchor.CENTER`.

### Why doesn't my label wrap?

It has no width to wrap to. Give it a fixed or maximum width, or stretch it in a parent that has one. See [Wrapping text](../guides/layout.md#wrapping-text).

### How do I update the screen when my data changes?

For small changes, call setters: `label.setText(...)`, `button.setEnabled(...)`. For structural changes, call `handle.invalidate()` to rebuild the tree, or `add` and `remove` children directly. See [Screens](../guides/screens.md#keep-data-on-the-screen-not-in-the-tree).

### Is there a scroll panel, list or dropdown?

Not yet. The built-ins are `Label`, `Image`, `Button`, `CheckBox`, `Slider`, `TextBox` and four panels. A scroll panel is next; see [Status and limitations](status.md). The extending guide builds a [switch](../guides/extending.md#write-a-new-component) as a complete example of writing your own.

### Can I use Strata from Kotlin or another JVM language?

Nothing prevents it. The API is plain Java interfaces and records. The examples are all Java.

## Events

### What is the difference between `target()` and `current()`?

`target()` is the component the event was fired at and never changes. `current()` is the component whose handlers are running, which moves up as the event bubbles. A handler on a container uses `target()` to see which child was involved. See [Target and current](../guides/events.md#target-and-current).

### Why doesn't my panel's click handler see clicks on its buttons?

Buttons consume the clicks that press them, and consuming stops bubbling. Put the handler on the button. See [the warning in Events](../guides/events.md#canceling-and-stopping).

### What's the difference between `cancel`, `stopPropagation` and `consume`?

`cancel` skips default behavior, `stopPropagation` stops bubbling, and `consume` does both. See [Canceling and stopping](../guides/events.md#canceling-and-stopping).

### My `AFTER` handler ran even though I canceled the event. Is that a bug?

No. Canceling skips the default behavior only. `AFTER` handlers always run.

### How do I tell a double click from a single click?

Read `clicks()` on the `CLICK` event. Strata delivers every click immediately with its count, so a double click arrives as a click with count 1 followed by one with count 2. See [Click counting](../guides/events.md#click-counting).

### How do I make a keyboard shortcut?

Register a `KEY` handler on the screen's root and use `event.is(key, modifiers, policy)`. See [Shortcuts](../guides/input-focus.md#shortcuts).

### What happens if I call `event.snapshot()` without registering with `onWithSnapshot`?

If no handler on the event's path asked for snapshots, it throws `IllegalStateException`, and `snapshotOf` returns empty. If some other handler on the path asked, the target's snapshot exists and `snapshot()` works. Register every handler that reads snapshots with `onWithSnapshot`. See [When there is no snapshot](../guides/snapshots.md#when-there-is-no-snapshot).

### Does a snapshot include a panel's children?

No. Snapshots are shallow. A child has its own snapshot only if it was captured.

### Can I fire my own events?

Not yet. You can create `EventType`s, but there is no public way to fire them. Use listener methods on your components instead.

### Do I need to remove handlers when a screen closes?

No. A closed screen's tree is dropped, along with its handlers. Remove handlers only from components you keep and reuse.

## Swing

### What does the scale argument of `StrataPanel` mean?

Pixels per UI unit. At 2, a 100-unit button is 200 pixels wide.

### Why does text look different from other adapters?

Swing draws with an AWT font, Dialog 9 by default, while other adapters use their engine's font. Widths differ too, so leave slack around text. See [Platform differences](../platforms/differences.md#text).

### My sprite draws as a magenta square.

The image was not found on the classpath at `assets/<namespace>/sprites/<path>.png` (or where your `SpriteResolver` points), or could not be read. See [Sprites](../platforms/swing.md#sprites).

### Can I render a screen without a window?

Yes, with `SwingSnapshot.render`, which works headless. See [Rendering without a display](../platforms/swing.md#rendering-without-a-display).
