# Themes

A theme decides how components look. It maps style keys, such as `button.hovered`, to paints and text styles. Every component in one host shares the host's theme.

## Built-in themes

| Theme | Where | Look |
|---|---|---|
| `Themes.flat()` | `api` | Dark, solid colors only. Works on any engine without image assets. |

It defines every key of every built-in widget. Adapters may ship themes of their own that use the engine's widget images.

![The flat theme](../assets/images/theme-flat.png)

## Deriving a theme

Themes are immutable. `toBuilder()` starts a builder holding all of a theme's keys, so a variant only lists what changes:

```java
--8<-- "theming/ThemeExamples.java:derive"
```

![The flat theme with green buttons](../assets/images/theme-derived.png)

## A theme from scratch

`Theme.builder(name)` starts empty. The name appears in error messages.

```java
--8<-- "theming/ThemeExamples.java:scratch"
```

![A light theme built from nothing](../assets/images/theme-light.png)

Labels and panels have no backgrounds in any theme. Set those per component with `setBackground`.

## Required and optional keys

Each widget lists the keys it reads as `STYLE_*` constants on its interface. Some are required, and the rest are optional with a documented fallback. The [theme key reference](../reference/theme-keys.md) lists them all.

A host checks the theme before it shows anything. When it opens a screen, it collects the required keys of every component in the tree, and if the theme lacks any of them it throws a `ThemeException` that lists every missing key at once, so one run shows everything to add. The same check runs when a component is added to a tree that is already on screen, and when the host's theme changes.

```java
--8<-- "theming/ThemeExamples.java:missing"
```

Your own components declare required keys through `requiredStyleKeys()`. Remember to add them to the theme you use, as the [switch example](extending.md#write-a-new-component) does.

A key may have a paint, a text style, or both; `has(key)` is true for either. Look keys up with `paint(key)` and `textStyle(key)`, which return `Optional`, or with `paintOr` and `textStyleOr`, which take a fallback.

## Paints

A `Paint` fills a rectangle. There are four kinds:

| Factory | Record | Draws |
|---|---|---|
| `Paint.solid(argb)` | `Paint.Solid` | One color |
| `Paint.gradient(top, bottom)` | `Paint.Gradient` | A vertical gradient |
| `Paint.sprite(id)` | `Paint.Sprite` | An image stretched over the rectangle |
| `Paint.nineSlice(id, border)` | `Paint.NineSlice` | An image cut into a 3 by 3 grid |

`Paint.NONE` draws nothing; it is the default background. The sprite factories make untinted paints. To tint, use the record constructor with an ARGB tint, which is multiplied with the image's pixels: `new Paint.NineSlice(id, border, 0xFFC0C0C0)` darkens it slightly. Opaque white leaves an image unchanged.

`paint.paint(painter, rect)` draws any paint, so custom components need not care which kind they hold.

### Colors

Colors are ARGB ints, `0xAARRGGBB`. The alpha byte comes first: `0xFF000000` is opaque black, and `0x80FFFFFF` is half-transparent white.

A color with alpha 0 draws nothing, so be careful with `0xRRGGBB` literals: `0xFF0000` has alpha 0 and is invisible. Write `0xFFFF0000` for opaque red.

### Sprites

A `SpriteId` names an image as `namespace:path`:

```java
--8<-- "theming/ThemeExamples.java:sprites"
```

Each adapter decides where the image comes from. The Swing adapter reads it from the classpath, by default at:

```text
assets/<namespace>/sprites/<path>.png
```

A `SpriteResolver` given to `SwingSprites` maps ids to any other layout. A missing image draws as magenta.

`SpriteId.parse("examplemod:button")` parses the text form. A missing namespace is an error, so a sprite never lands in another namespace by accident.

### Nine-slices

A nine-slice keeps its corners at their size, stretches its edges along one axis and its center both ways, so one small image can frame a rectangle of any size. `border` gives the width of each fixed edge in the image's pixels.

Some engines store the slicing with the image and ignore `border`. Keep the two in agreement so every engine draws the same. When a rectangle is smaller than the borders, the Swing adapter shrinks the borders to fit.

## Text styles

A `TextStyle` sets color, bold, italic, underline, strikethrough and a shadow. Themes map text keys to styles; labels can also take one directly. See [Text](text.md).

## Per-component overrides

Backgrounds and label text styles can be set per component, outside the theme:

```java
--8<-- "theming/ThemeExamples.java:local"
```

There is no per-component theme or per-subtree theme. To give one button a different face, decorate it with a behavior that paints something else, or write a component that reads its own keys.

## Themes by id

A `Theme` built in code is fixed. To let players restyle your screens with packs, name the theme by id instead and hand the host a `ThemeRef`:

```java
ThemeRef theme = ThemeRef.of(ThemeId.of("mymod", "shop"))
        .override(Button.STYLE_NORMAL, Paint.solid(0xFF2E7D32));
```

The host resolves the ref when it opens. It starts from the theme registered under the id, applies every theme file the engine finds for that id, and applies the ref's overrides last. `Themes.FLAT` (`strata:flat`) is registered on every engine, and adapters register their engine themes under ids of their own. An id with nothing registered is defined by its files alone and names its starting theme as `parent`.

When the engine reloads its assets, every host with a ref resolves it again and keeps the overrides. A reload whose result lacks a key the open screen needs is logged and ignored, so the screen keeps its old theme.

### Theme files

A theme file sets only the keys it changes. Where the files live depends on the engine: Swing reads `assets/<namespace>/themes/<path>.json` from the class path. A file whose id has a registered theme changes that theme key by key, and when several packs ship the same file, the higher-priority pack wins per key.

```json
{
  "parent": "strata:flat",
  "paints": {
    "button.normal": { "nine_slice": "mymod:button", "border": [3, 3, 3, 3] },
    "button.hovered": { "sprite": "mymod:button_hover", "tint": "#FFE0E0FF" },
    "button.focus": "none",
    "list.row": "#1F2024",
    "window.background": { "gradient": ["#F0101010", "#F0202020"] }
  },
  "text": {
    "button.text": { "color": "#FFFFFF", "shadow": true }
  }
}
```

| Value | Written as |
|---|---|
| Color | `"#RRGGBB"` (opaque) or `"#AARRGGBB"` |
| No paint | `"none"` |
| Sprite | `{ "sprite": "ns:path" }`, with an optional `"tint"` color |
| Nine-slice | `{ "nine_slice": "ns:path", "border": ... }`, with an optional `"tint"`. The border is one number, `[horizontal, vertical]` or `[left, top, right, bottom]` |
| Gradient | `{ "gradient": [top, bottom] }` |
| Text style | Any of `color`, `bold`, `italic`, `underline`, `strikethrough` and `shadow`. Fields you leave out keep the inherited style for that key |

A misspelled member, a bad color or broken JSON makes the whole file invalid. The engine logs it and skips that file, and the other packs still apply.

## Changing the theme at run time

The host owns the theme. Adapters can change it with `ScreenHost.setTheme` or `ScreenStack.setTheme`, which take either a `Theme` or a `ThemeRef`; components then re-read their keys in `onApplyTheme` and a new layout runs. See [Screens](screens.md#changing-the-theme).
