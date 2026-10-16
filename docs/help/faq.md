# FAQ

## General

### What engines does Strata support?

Swing (Java2D) on any desktop JVM, and Minecraft 26.3 on NeoForge and Fabric. Other engines need an adapter, which is a small amount of code; see [Writing an adapter](../platforms/writing-an-adapter.md).

### Which Minecraft versions?

26.3 only. The mod metadata accepts `[26.3, 26.4)`. Versions before 26.x will not be supported: 26.3 changed the GUI and input APIs (`GuiGraphicsExtractor`, SDL scancodes) that the adapter is built on.

### Is it ready for production?

Not yet. Strata is at 0.1.0, unreleased, and the API will change. Nothing has been run inside a Minecraft client yet. See [Status and limitations](status.md).

### Is it a port of MalisisCore?

No. Strata takes ideas from MalisisCore (a retained component tree, anchors, cancellable events) and from Wintergreen (measure-and-arrange layout, click and drag handling, focus, typed events, the text box), but it is new code with no engine dependency. The [design document](../DESIGN.md) records where each idea comes from and what Strata does differently.

### What license?

MIT.

## Writing screens

### Why can't I write `new Button()`?

Components are interfaces with a hidden implementation. You get them from the `ComponentFactory` the host passes to your screen, so every component belongs to the host that shows it. See [Core concepts](../getting-started/concepts.md#the-factory-and-the-host).

### Why is there no static way to get a factory?

Mod loaders give each mod its own class loader, and one game can show several hosts at once. Passing the factory keeps every tree tied to the host that shows it, and leaves no global state behind when a screen closes.

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

### Is there a scroll panel, list, check box, slider or dropdown?

Not yet. The built-ins are `Label`, `Button`, `TextBox` and four panels. The extending guide builds a [check box](../guides/extending.md#write-a-new-component) as a complete example.

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

## Minecraft

### How do I open a Strata screen?

`Minecraft.getInstance().gui.setScreen(new StrataScreen(title, screen, MinecraftThemes.vanilla()))`, on the client thread. See [Opening a screen](../platforms/minecraft.md#opening-a-screen).

### Why does closing my screen go back to the game instead of the previous screen?

That is vanilla's default `onClose()`. Subclass `StrataScreen` and override it. See [Returning to a parent screen](../platforms/minecraft.md#returning-to-a-parent-screen).

### Why does Escape close my screen while I'm typing?

The text box does not consume Escape, so vanilla gets it. Consume it in a root handler, or override `shouldCloseOnEsc()`. See [Escape](../platforms/minecraft.md#escape).

### Why do I get no `CHAR` events in my custom component?

Minecraft 26.3 sends characters only while a text field reports focus. Override `acceptsTextInput()` to return true. See [Typed characters need text input mode](../platforms/minecraft.md#typed-characters-need-text-input-mode).

### Why doesn't Shift+scroll or Ctrl+drag work in game?

Minecraft sends no modifiers with pointer moves, scrolls or characters. See [Modifier keys](../platforms/differences.md#modifier-keys).

### Why doesn't Command+C work in a text box on my Mac?

Strata's text box uses Control on every platform for now. Use Control+C.

### Does the world keep running behind a Strata screen?

In single player it pauses, as with most vanilla screens. Override `isPauseScreen()` to return false. See [Pausing](../platforms/minecraft.md#pausing).

### Where do I put my sprites?

`assets/<modid>/textures/gui/sprites/<path>.png`, named `SpriteId.of("<modid>", "<path>")`. Add a `.png.mcmeta` for nine-slices. See [Your own sprites](../platforms/minecraft.md#your-own-sprites).

### Can I put vanilla buttons in a Strata screen, or Strata components in a vanilla screen?

Not yet. Both directions are planned as hosting adapters. See [Not available yet](../platforms/minecraft.md#not-available-yet).

### Can I draw on the HUD?

Not yet. HUD layers are planned.

### Does Strata need Fabric API?

No. The Fabric jar depends only on Fabric Loader, Minecraft and Java.

### Does Strata use mixins?

No. It hosts screens through vanilla's own `Screen` class.

### Does it work on a dedicated server?

Strata is client-only. Keep classes that touch it out of server code.

## Swing

### What does the scale argument of `StrataPanel` mean?

Pixels per UI unit. At 2, a 100-unit button is 200 pixels wide, like Minecraft at GUI scale 2.

### Why does text look different from Minecraft?

Swing draws with an AWT font, Dialog 9 by default, not Minecraft's pixel font. Widths differ too, so leave slack around text. See [Platform differences](../platforms/differences.md#text).

### My sprite draws as a magenta square.

The image was not found on the classpath at `assets/<namespace>/textures/gui/sprites/<path>.png`, or could not be read. See [Sprites](../platforms/swing.md#sprites).

### Can I render a screen without a window?

Yes, with `SwingSnapshot.render`, which works headless. See [Rendering without a display](../platforms/swing.md#rendering-without-a-display).
