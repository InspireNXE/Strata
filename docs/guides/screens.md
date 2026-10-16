# Screens

A `Screen` is one full screen of UI: a settings page, a dialog, an inventory. You implement it; a host calls it.

## Lifecycle

1. The host calls `build(factory)` for the component tree and shows it, sized to the host.
2. The host calls `opened(handle)` with a `ScreenHandle` the screen may keep.
3. After `handle.invalidate()`, the host calls `build` again before the next frame and replaces the whole tree. Focus and hover start over.
4. When the screen closes, the host calls `closed()` and then detaches the tree.

A screen object may be opened again after it closes. Each opening builds a new tree and gets a new handle.

`Screen` is a functional interface, so a lambda works for simple screens:

```java
panel.screens().push(factory -> factory.anchorPanel()
        .add(factory.label("Loading").setAnchor(Anchor.CENTER))
        .setAnchor(Anchor.FILL));
```

## Rules for `build`

- Return a new component with no parent, made with the factory passed in. A component from another factory, or one already shown elsewhere, fails with `IllegalArgumentException`.
- The host's theme must define every required style key of the tree's components, or opening fails with `ThemeException`. See [Themes](theming.md#required-and-optional-keys).
- Make the root fill the host with `Anchor.FILL` if it should cover the whole area. Otherwise it takes its desired size at the top left.
- Everything runs on the UI thread.

## Keep data on the screen, not in the tree

Because `build` can run more than once, treat the tree as disposable and the screen object as the owner of your data. Small changes, such as new label text or a disabled button, only need setters. Changes to the structure, such as adding or removing rows, are easiest with a rebuild:

![A list whose rows are rebuilt](../assets/images/screens-list.png)

```java
--8<-- "screens/ScreenExamples.java:rebuild"
```

Rebuilding is cheap for screens of ordinary size, but it resets focus, hover, text box carets and anything else that lives only in components. For a list the user is typing into, change the tree in place instead: `remove` and `add` on the container work at any time.

## The handle

| Method | Does |
|---|---|
| `invalidate()` | Asks for a rebuild before the next frame |
| `close()` | Asks the host to close the screen |
| `factory()` | The host's factory, the same one `build` receives |

A handle belongs to one opening. Once that opening ends, because the screen closed or the host opened another, its methods do nothing, so a stale handle is harmless.

```java
--8<-- "events/EventExamples.java:handle"
```

What closing means depends on the host. A Swing screen stack pops the screen. On Minecraft, `StrataScreen` calls vanilla's `onClose()`, which by default leaves the screen and returns to the game, not to the screen that opened it. To go back to a parent screen, override `onClose()`; see [Returning to a parent screen](../platforms/minecraft.md#returning-to-a-parent-screen).

## Stacks of screens

Engines without a screen system of their own, such as Swing, use a `ScreenStack`. `StrataPanel.screens()` returns one.

| Method | Does |
|---|---|
| `push(screen)` | Opens a screen on top, at the stack's size and theme |
| `pop()` | Closes and removes the top screen |
| `top()`, `isEmpty()` | Inspect the stack |
| `clear()` | Closes every screen, top first |
| `setTheme(theme)`, `resize(w, h)` | Apply to every screen, and to screens pushed later |

Only the top screen receives input. A screen that closes through its handle is removed from wherever it is in the stack. Each screen in a stack has its own host inside, with its own factory, focus and hover, so components cannot move between screens.

```java
--8<-- "screens/ScreenExamples.java:stack"
```

### Drawing the screens below

A stack draws from the topmost screen that hides the background, or from the bottom screen if none does, up to the top. By default `hidesBackground()` is false and screens below show through, which suits dialogs. A screen that covers everything should return true, so the stack skips drawing what is beneath it:

```java
--8<-- "screens/ScreenExamples.java:opaque"
```

Minecraft has its own screen stack. Each vanilla `StrataScreen` holds exactly one Strata screen, and `hidesBackground` has no effect there. Vanilla decides what is drawn behind a screen.

## Changing the theme

Screens cannot change their host's theme. Adapters can: `ScreenStack.setTheme` and `ScreenHost.setTheme` swap it, components re-read their keys, and a new layout runs. `StrataScreen` takes its theme in the constructor and has no setter yet. A new theme that lacks keys the open tree needs throws `ThemeException`, and the old theme stays.
