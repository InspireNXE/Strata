# Writing an adapter

An adapter runs Strata on an engine. It implements two SPI interfaces, `Platform` and `Canvas`, creates a host from `core`, and forwards the engine's frames and input to it. This page builds a complete adapter for an imaginary engine. Its code is compiled and passes Strata's contract tests as part of the build.

You need this only to bring Strata to a new engine: LWJGL directly, libGDX, a game of your own, or an ImGui backend. To use Strata on Minecraft or Swing, see those pages instead.

## What an adapter depends on

```groovy
dependencies {
    implementation 'org.inspirenxe:strata-api:0.1.0-SNAPSHOT'
    implementation 'org.inspirenxe:strata-spi:0.1.0-SNAPSHOT'
    implementation 'org.inspirenxe:strata-core:0.1.0-SNAPSHOT'
    // The contract tests
    testImplementation testFixtures('org.inspirenxe:strata-spi:0.1.0-SNAPSHOT')
}
```

`core` is the one place an adapter touches the implementation, through `StrataCore`. Screens written for your engine still compile against `api` only.

## Rules for every adapter

- Call hosts on one thread, the engine's UI or render thread. Strata does no locking.
- Coordinates are UI units with the origin at the top left of the host's area. Choose what a unit is, such as screen pixels or scaled pixels, and convert drawing and input the same way.
- Never let engine types into Strata calls. Map keys to `Key`, buttons to `MouseButton`, modifiers to `Modifiers`.
- Run the contract tests.

## Text metrics

`TextMetrics` measures text the way your engine draws it. Its requirements, from the Javadoc:

- A span drawn at x must end at x plus its measured width, give or take a unit of rounding. Text boxes place the caret from these numbers.
- The same span always gives the same result, with no side effects.
- Width is 0 for an empty span and never negative.
- Appending a code point never makes a span narrower.
- Line height is positive.
- Be fast, or cache: layout measures every label line, and a text box measures every prefix of its text.

```java
--8<-- "adapter/MonospaceTextMetrics.java:metrics"
```

## The platform

`Platform` is everything a host needs besides drawing:

```java
--8<-- "adapter/ToyPlatform.java:platform"
```

| Method | Requirements |
|---|---|
| `textMetrics()` | Must match what the canvas's `text` draws |
| `clipboard()` | Never throws. A busy or missing clipboard reads as empty and ignores writes. Use `Clipboard.NONE` if the engine has none. Text written reads back unchanged until something else changes it. |
| `clock()` | Monotonic, never backward, advancing in real time. `Clock.SYSTEM` is `System.nanoTime`. Click counting, caret blinking and synthesized key repeat stall on a clock that stops. |
| `textInput()` | Told when a text field gains and loses focus. `start` and `stop` may arrive repeatedly; a second `start` moves the input area. After `start`, the adapter must forward typed characters. Use `TextInputSink.NONE` if the engine always sends characters. |
| `capabilities()` | Constant for the platform's life. `sendsKeyRepeat` is true when the adapter delivers `REPEAT` itself. |

Return the same objects every time; hosts may keep what they get.

## The canvas

`Canvas` is what the engine draws for Strata:

```java
--8<-- "adapter/RecordingCanvas.java:canvas"
```

Requirements:

- Coordinates are UI units in the current transform. Round to the engine's pixel grid if it needs whole pixels, the same way for drawing and clipping.
- Colors are ARGB. Alpha 0 draws nothing; partial alpha blends over what is already drawn.
- Later calls draw over earlier ones. An engine that batches or sorts must keep this order, at least across `nextLayer()`.
- Calls may be recorded and replayed later, as Minecraft does. Nothing requires immediate drawing.
- Clips and transforms are stacks. Strata balances every push with a pop within a frame, in reverse order, even when a component throws. A clip applies in the transform current when it was pushed and intersects the clip already in place.
- Never throw for missing resources. Draw something visible instead, so the problem shows on screen.
- A nine-slice smaller than its borders shrinks the borders rather than drawing outside its rectangle. An engine that knows a sprite's slicing natively may ignore the `border` argument.
- `text` puts the top-left of the line box at the point, not the baseline, applies every style the engine supports, and lines up with the platform's text metrics.
- `requestCursor` is called at most once per frame, after drawing, while the pointer is over a component. In a frame without a request, show the default cursor.

## Driving the host

Create a host with `StrataCore`:

| Factory | For |
|---|---|
| `createScreenHost(platform, theme)` | One screen at a time. A screen that asks to close is closed by the host. |
| `createScreenHost(platform, theme, closeRequested)` | The same, but you decide what closing means. Minecraft uses it to close the vanilla screen. |
| `createScreenStack(platform, theme)` | A stack of screens, for engines with no screen system |

Then forward the engine's callbacks:

```java
--8<-- "adapter/ToyAdapter.java:adapter"
```

The host's duties for you:

- Call `resize` before the first `render`, and again whenever the area changes.
- Call `render` once per frame. It builds pending screens, synthesizes key repeats if needed, runs layout, draws and requests the cursor.
- Send every pointer movement, during drags too: drags are recognized from moves.
- Send each button's press and release. A release with no matching press is ignored.
- Send keys with the right `KeyAction`. If the engine reports repeats as more presses, use `HeldKeys` to turn the second and later presses into `REPEAT`, and call `HeldKeys.clear()` when the window loses focus.
- Send typed characters separately through `charTyped`, as code points, after the keyboard layout and input method. Never send control characters.
- Call `pointerLeft()` when the pointer leaves the area, and `close()` when the engine screen goes away so the Strata screen's `closed()` runs.
- Pass input on to the engine when the host returns false. See [What reaches the engine](../guides/input-focus.md#what-reaches-the-engine).
- Use Strata's directions: vertical scroll is positive away from the user, horizontal scroll positive toward the left.

## Contract tests

The SPI's test fixtures hold `TextMetricsContract`, `PlatformContract` and `CanvasContract`. Extend each with your implementation:

```java
--8<-- "AdapterContractTests.java:contracts"
```

`CanvasContract` needs a `Surface` per check. A canvas that cannot read back its pixels, like this one, runs the checks that need no pixels: every call is accepted with ordinary and edge-case arguments, and deep clip and transform stacks unwind. A canvas that draws to readable pixels, such as an offscreen buffer, returns `readable() == true` and implements `argb(x, y)`. Then the pixel checks run too: fills cover exactly their rectangle, alpha 0 draws nothing, later draws cover earlier ones, clips limit and restore, nested clips intersect, translations shift and restore, gradients run top to bottom, missing sprites stay visible and inside their rectangle, nine-slices stay inside small rectangles, and text stays within its measured width. Override `metrics()` to enable the text check. Surfaces use one UI unit per pixel on a transparent background.

`PlatformContract` has `hasClipboard()` and `clockRunsOnItsOwn()` to override for a headless clipboard or a hand-driven clock.

## Naming

Strata's adapters name classes after the engine and the SPI role they fill (`SwingCanvas`, `MinecraftPlatform`), and the engine-native component a developer adds to their engine is `Strata` plus the engine's base class (`StrataPanel`, `StrataScreen`). Following that keeps adapters easy to recognize.
