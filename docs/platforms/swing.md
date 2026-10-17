# Swing

The Swing adapter draws Strata with Java2D and takes input from AWT. It is the fastest way to work on a screen, since there is no game to start, and `SwingSnapshot` renders screens to images without a display for tests and documentation. Every image on this site was rendered that way.

## Showing screens

`StrataPanel` is a `JPanel` that holds a stack of Strata screens:

```java
--8<-- "swing/SwingExamples.java:window"
```

| Constructor argument | Meaning |
|---|---|
| `SwingPlatform` | Text metrics, clipboard and clock. Its text metrics also draw the text. |
| `Theme` | The theme for every screen pushed |
| `scale` | Pixels per UI unit, which must be positive |

`panel.screens()` is the [screen stack](../guides/screens.md#stacks-of-screens): push to show a screen, pop to close the top one. Only the top screen gets input.

Use the panel on the Swing event dispatch thread only, the same as any Swing component.

## How Swing maps onto Strata

- One UI unit is `scale` pixels. Drawing is scaled up and pointer positions scaled down by the same factor, and the screens are resized whenever the panel is.
- The panel repaints about 60 times a second while it is shown, and stops when it is removed from its window.
- Its preferred size is 960 by 600 pixels, and its own background is `0xFF1E1F22` where no screen draws.
- It takes keyboard focus when clicked, and turns off Swing's own Tab handling so Strata's tab order applies.
- AWT reports held keys as repeated presses. The panel turns them into `REPEAT`s, and forgets held keys when it loses focus.
- Characters come from AWT's key-typed events, with surrogate pairs joined into one code point and control characters dropped.
- Shift plus the vertical wheel scrolls horizontally. A whole wheel rotation counts as `WHEEL`, a fractional one as `PRECISE`.
- Cursors map to AWT's predefined cursors. AWT has no "not allowed" cursor, so `NOT_ALLOWED` shows the default arrow.

## Fonts

`SwingTextMetrics` picks the fonts. One base font is derived into plain, bold, italic and bold italic, and its size is in UI units:

```java
--8<-- "swing/SwingExamples.java:font"
```

`SwingTextMetrics.defaults()` is the logical Dialog font at 9 units. Widths are fractional advance widths with antialiasing and fractional metrics on, the same settings the panel draws with. The line height is the font's height. Underline, strikethrough and the shadow are drawn by the adapter and do not change widths.

Other adapters use other fonts with other widths. Layouts built on measured text adapt; layouts that assume widths will not.

## Sprites

`SwingSprites` loads images from the classpath at `assets/<namespace>/sprites/<path>.png`. Pass a `SpriteResolver` to `SwingSprites`, and that to `StrataPanel` or `SwingSnapshot.render`, to read another layout. Images are loaded once and cached, including the fact that one is missing. A missing or unreadable image draws as solid magenta.

The panel and `SwingSnapshot` look images up through the class loader that loaded the Swing adapter. In an ordinary application, where everything shares one class path, that finds images in your own jar. Under a plugin system that gives each plugin its own class loader, it may not.

Tints multiply each channel of the image, and tinted copies are cached per sprite and tint. Nine-slice borders are in the image's pixels; borders wider than the rectangle shrink in proportion so nothing draws outside it. Swing stretches the edges and the center of a nine-slice.

## Clipboard

`SwingPlatform` uses the system clipboard. Reads and writes that fail, because another program holds the clipboard or it holds no text, read as empty and are dropped. In a headless JVM there is no clipboard at all.

## Rendering without a display

`SwingSnapshot.render` opens a screen on a fresh stack, draws one frame into an image and closes it again:

```java
--8<-- "swing/SwingExamples.java:snapshot"
```

Any pixel the screen does not draw stays transparent. The screen's `closed()` runs before `render` returns, and a theme missing a required key throws `ThemeException`. Set `-Djava.awt.headless=true` on build servers. Strata's own Swing tests and the sandbox's `--snapshot` option use it.

Snapshots are a good way to test layouts: render a screen and compare pixels, or check that it renders at all with each of your themes.

## The sandbox

The repository's `sandbox` module is a demo screen written against the API only:

```bash
./gradlew :sandbox:run
./gradlew :sandbox:run --args="--snapshot sandbox.png"
```
