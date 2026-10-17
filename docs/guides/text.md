# Text

Text in Strata is plain values: characters with a style. The engine adapter picks the font, draws the text and measures it. The same text is therefore a little wider on one engine than on another, and layouts should never assume character widths.

## TextStyle

A record of how a span of text looks:

| Field | Default (`TextStyle.DEFAULT`) |
|---|---|
| `color` | `0xFFFFFFFF`, opaque white. Alpha 0 draws nothing. |
| `bold`, `italic`, `underline`, `strikethrough` | off |
| `shadow` | off. A darker copy one unit down and to the right. |

`with` methods return modified copies:

```java
TextStyle heading = TextStyle.DEFAULT.withColor(0xFFFFD54F).withBold(true).withShadow(true);
```

Engines draw what they can:

| Style | Swing |
|---|---|
| Bold | A bold font derived from the base font |
| Italic | An italic font derived from the base font |
| Underline, strikethrough | Drawn by the adapter |
| Shadow | Drawn by the adapter |

Other adapters use the engine's own text styles where it has them.

## TextSpan

A `TextSpan` is a string with a `TextStyle`. `TextSpan.of(text)` uses the default style, and `withText` and `withStyle` make copies.

As far as engines are concerned, a span is one line. Break text into lines before drawing it, as `Label` does.

## Measuring

`TextMetrics` measures text the way the engine draws it:

| Method | Returns |
|---|---|
| `width(span)` | How wide the span is drawn, in UI units. 0 for empty text. |
| `lineHeight(style)` | The distance from one line's top to the next line's top |

Components get it from `MeasureContext.text()` during measure and from `environment().text()` anywhere else. The widths match drawing, so a text box can place its caret between characters.

| Engine | Widths | Line height |
|---|---|---|
| Swing | Fractional advance widths of the AWT font | The font's height: ascent, descent and leading |

Engines with pixel fonts often report whole-pixel widths, and some make bold text wider.

## Drawing text in a custom component

```java
@Override
protected Size measure(MeasureContext context, float availableWidth, float availableHeight) {
    this.span = new TextSpan(this.caption, this.style);
    this.lineHeight = context.text().lineHeight(this.style);
    return new Size(context.text().width(this.span), this.lineHeight);
}

@Override
protected void paint(Painter painter, Rect bounds) {
    painter.text(this.span, bounds.x(), bounds.y());
}
```

`Painter.text(span, x, y)` puts the top-left corner of the line box at the point: `y` is the top of the line, not the baseline. Build spans during measure and keep them for painting, so painting does not allocate. The [switch example](extending.md#write-a-new-component) does this.

## Wrapping

`Label` wraps greedily by word when wrapping is on: it fills each line with as many words as fit the available width, and breaks a single word wider than a whole line between characters. It always breaks at `\n`. See [Wrapping text](layout.md#wrapping-text) for giving it a width.

## Mixed styles

A label made with `setSpans` draws its spans one after another, each in its own style, and wraps them by the same rules, so a piece of a span keeps the span's style on any line. Use it for text like a white item name followed by a gray count, or a gold price followed by `/ea`. `setTextScale` draws a label larger or smaller, and `TextOverflow.ELLIPSIS` ends a line that does not fit with `...`, in the style of the last character kept. See [Label](widgets.md#label).

Wrapping is part of the label, not a public utility. A custom component that needs wrapped text can measure and break the text itself, or contain a wrapping label.

## Fonts

Strata has no font API, so each adapter picks its fonts. Swing draws with the font given to `SwingTextMetrics`. `SwingTextMetrics.defaults()` is the logical Dialog font at 9 units. See [Swing](../platforms/swing.md#fonts). Other adapters usually draw with the engine's own font.

## Not supported yet

- Inline images in a line of text. `SpriteSpan` and `InlineElement` are reserved for it, but nothing draws them yet.
- Markup such as BBCode. Avoid an engine's own formatting codes in text too: Strata does not interpret them, and the engine's text drawing may, so the same text would look different on each engine.
- Right-to-left text and complex shaping beyond what the engine's own text drawing does.
- Multi-line editing.
