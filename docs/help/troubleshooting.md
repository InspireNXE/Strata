# Troubleshooting

## Errors

### `ThemeException: Theme '...' is missing style keys: ...`

The theme lacks keys that components in the tree require. The message lists every missing key. Add them to the theme, usually by deriving from a built-in theme with `toBuilder()`. For your own components, the keys are whatever `requiredStyleKeys()` returns. It is thrown when a screen opens, when a component is added to a tree on screen, and when the host's theme changes. See [Themes](../guides/theming.md#required-and-optional-keys).

### `IllegalArgumentException` when adding a component

`Container.add` rejects a child that:

- already has a parent: remove it from the old parent first;
- is a screen's root, the container itself, or one of the container's ancestors;
- was made by another host's factory: build each screen's components from the factory that screen received;
- has no Strata implementation: it implements a component interface directly instead of extending `CustomComponent`.

The same rules apply to the component `Screen.build` returns.

### `IllegalStateException: No snapshot was taken for this event`

A handler called `event.snapshot()` or `currentSnapshot()` on an event that has no snapshot of that component. Register the handler with `onWithSnapshot` instead of `on`. See [Event snapshots](../guides/snapshots.md#when-there-is-no-snapshot).

### `IllegalArgumentException: Sprite id needs a namespace`

`SpriteId.parse` was given text without a colon. Write `"examplemod:button"`, not `"button"`.

### An exception from my handler crashes the game or the Swing thread

Strata does not catch exceptions from handlers or hooks. They propagate out of the input or frame call that caused them. Catch your own errors.

## Nothing shows up

| Check | Why |
|---|---|
| Is the root anchored with `Anchor.FILL`? | Without it, the root takes its desired size at the top left, which may be tiny. |
| Does the component have a size? | An empty container measures as just its padding. Give it children or a size. |
| Is it visible? | Hidden components take no space and are not drawn. |
| Is a clipping ancestor too small? | Children are clipped to their container's bounds by default. |
| Are your colors opaque? | `0xFF0000` has alpha 0 and draws nothing. Write `0xFFFF0000`. |
| On Swing, was the screen pushed? | `panel.screens().push(screen)`. |
| On Minecraft, did `setScreen` run on the client thread? | Use `Minecraft.getInstance().execute(...)` from other threads. |

## Layout looks wrong

| Symptom | Fix |
|---|---|
| A stretched child is not stretching | Its parent has no width to give. Fix the width somewhere above it. |
| A child ignores its alignment in a `StackPanel` | Along the stack's direction, each child gets exactly its size. Use a `GridPanel` to distribute space. |
| A label does not wrap | Turn wrapping on and give it a width. See [Wrapping text](../guides/layout.md#wrapping-text). |
| Text is cut off in Minecraft but fine in Swing | Minecraft's font is wider in places and rounds to whole pixels. Leave slack around text, or let it measure. |
| Things are a pixel off in Minecraft | Fractional positions round outward to whole GUI pixels. Use whole numbers. |
| A `GridPanel` child lands in the wrong cell | Cells past the defined rows and columns move to the last ones. Define enough tracks. |
| A custom container's children have stale positions | It must measure and arrange every child, hidden ones included. See [Write a new layout](../guides/extending.md#write-a-new-layout). |

## Input problems

| Symptom | Cause and fix |
|---|---|
| Clicks do nothing on a button | It is disabled, hidden, covered by a later sibling, or clipped away. Clicks on a disabled component go to its ancestor. |
| A container's handler misses clicks on its buttons | Buttons consume the clicks they act on. Handle them on the button. |
| Typing does nothing in a custom component (Minecraft) | Override `acceptsTextInput()` to return true. |
| Typing does nothing at all | Nothing has focus, and characters only go to the focused component. Click the field or call `requestFocus()`. |
| `requestFocus()` returns false | The component is not attached, not focusable, hidden or disabled, or an ancestor is hidden or disabled. |
| Tab skips a component | It is not focusable and tabbable, or it or an ancestor is hidden or disabled. Check its tab index too. |
| A shortcut on the root never fires | A focused component consumed the key first. A text box consumes printable keys without Control or Alt. |
| Modifier checks never pass in game | Minecraft sends no modifiers with moves, scrolls or characters. |
| Escape closes the Minecraft screen | Consume Escape in a root handler or override `shouldCloseOnEsc()`. |
| Vanilla widgets in a `StrataScreen` never get clicks | Strata claims every press that hits one of its components, and a `FILL` root covers the screen. Mixing them is not supported yet. |

## Sprites

| Symptom | Cause |
|---|---|
| Magenta square (Swing) | The image is not on the classpath at `assets/<namespace>/textures/gui/sprites/<path>.png`. |
| Purple and black checkerboard (Minecraft) | The same path is missing from your mod's resources, or the game needs a resource reload. |
| Nine-slice stretches in Minecraft | The sprite has no `.png.mcmeta` with `nine_slice` scaling. |
| Nine-slice looks different in Swing and Minecraft | See [Nine-slices](../platforms/differences.md#nine-slices). |

## Building Strata

### "Unable to establish loopback connection" on Windows

The JDK's NIO pipe falls back to Unix domain sockets in `%TEMP%`, which some machines block. Point it at a local directory before running Gradle:

```bash
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$PWD/.gradle/udstmp"
```

### "Unsupported class file major version" or toolchain errors

Strata needs JDK 25. Set `JAVA_HOME` to a JDK 25 installation, or let Gradle's toolchain support download one.

### The build fails on a Javadoc warning

`strata-documented` runs Javadoc with every doclint check and fails on any warning, including a missing comment. Document the new public or protected member. The error names the file and line.

### "Cannot find a variant" or loader attribute errors in a mod project

The shared projects (`api`, `spi`, `core`, `mc-common`) are compiled into each loader jar and carry a loader attribute. Depend on `strata-mc-neoforge` or `strata-mc-fabric`, not on the shared projects. See [Installation](../getting-started/installation.md#minecraft-mods).

### The documentation site fails to build

`mkdocs build` is strict. A link to a missing page, or a snippet that names a missing file or section, fails it. Run `./gradlew siteJavadoc` first so the Javadoc links resolve. See [Contributing](../project/contributing.md#the-documentation-site).
