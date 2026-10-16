# Layout

Strata lays out a tree in two passes, the way WPF and Wintergreen do. This page explains the passes, every input a component has, and each of the four containers.

## The two passes

Layout runs before a frame is drawn, and only after something changed.

Measure
:   Each container offers each child some available space, possibly infinite, and asks how big it wants to be. The child answers with its desired size. Containers measure their children first, then decide their own desired size from them.

Arrange
:   Each container gives each child a slot: a rectangle, margin included. The child shrinks the slot by its margin and places itself inside by its alignment and size limits. The resulting rectangle is its bounds.

The screen's root gets the whole host as its slot.

A change that affects size, such as new label text, a new child or a different margin, marks the component and its ancestors for layout, and the whole tree is laid out again before the next frame. Nothing is measured every frame, and a change that only affects drawing, such as a background, causes no layout at all.

## Inputs every component has

| Input | Default | Meaning |
|---|---|---|
| Margin | `Insets.NONE` | Space kept clear around the component, outside its bounds |
| Minimum size | 0 by 0 | The smallest size it may take |
| Maximum size | infinite | The largest size it may take. It also limits the space offered when measuring, so text wraps within it. |
| Fixed width and height | unset (`NaN`) | Replaces the measured size and overrides `STRETCH` |
| Horizontal alignment | `LEFT` | Where it sits across its slot's width |
| Vertical alignment | `TOP` | Where it sits across its slot's height |

Containers add padding (`Insets.NONE` by default): space inside their edges that children stay out of.

### How a component's size is decided

For each axis:

1. If a fixed size is set, use it.
2. Otherwise, if the alignment is `STRETCH`, use the slot's size minus the margin.
3. Otherwise, use the desired size.
4. Then clamp to the minimum and maximum.

The minimum and maximum always win, even over a fixed size.

With any alignment other than `STRETCH`, a component keeps its size even if the slot is smaller and overflows it. If an ancestor clips its children, which containers do by default, the overflow is cut off.

### Alignment

| Horizontal | Vertical | Places the component |
|---|---|---|
| `LEFT` | `TOP` | Against the slot's left or top edge, after the margin |
| `CENTER` | `MIDDLE` | Centered in the slot |
| `RIGHT` | `BOTTOM` | Against the right or bottom edge, before the margin |
| `STRETCH` | `STRETCH` | Filling the slot minus the margin, unless a fixed size is set |

`setAlignment(horizontal, vertical)` sets both. `setAnchor(Anchor)` sets both at once by name:

| Anchor | Horizontal, vertical |
|---|---|
| `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT` | `LEFT`/`CENTER`/`RIGHT`, `TOP` |
| `MIDDLE_LEFT`, `CENTER`, `MIDDLE_RIGHT` | `LEFT`/`CENTER`/`RIGHT`, `MIDDLE` |
| `BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT` | `LEFT`/`CENTER`/`RIGHT`, `BOTTOM` |
| `FILL` | `STRETCH`, `STRETCH` |

Alignment is a request to the parent. Each container decides how big a slot to give, and some ignore alignment along one axis. The container sections below say which.

### Margin and padding

![A label with a margin inside a padded panel](../assets/images/layout-spacing.png)

```java
--8<-- "layout/LayoutExamples.java:spacing"
```

A container's background covers its padding, but a child's margin belongs to the child and is never painted by it. Insets can be negative: a negative margin lets a component overlap its neighbors, and layout clamps any size that would go below zero.

`Insets.of(all)` sets every side, `Insets.of(horizontal, vertical)` sets left and right to one value and top and bottom to another, and the record constructor `new Insets(left, top, right, bottom)` sets each side.

### Size limits in practice

![Natural, fixed, stretched and minimum sizes](../assets/images/layout-sizes.png)

```java
--8<-- "layout/LayoutExamples.java:sizes"
```

To go back to the measured size after fixing one, pass `Float.NaN` to `setWidth` or `setHeight`.

## AnchorPanel

Every child gets the whole content area (the bounds minus padding) as its slot, so children overlap and place themselves by their own anchor. Use it to pin things to edges, corners and the center of the same area. Its desired size is the largest child plus margin, plus the padding.

![An anchor panel with a title, a close button, a footer and a centered dialog](../assets/images/layout-anchor.png)

```java
--8<-- "layout/LayoutExamples.java:anchor"
```

The root of most screens is an anchor panel with `Anchor.FILL`.

## StackPanel

Children follow one another in a column (`Orientation.VERTICAL`, the default) or a row (`Orientation.HORIZONTAL`), with `setSpacing` units between neighbors and none before the first or after the last.

Along the orientation, each child gets exactly the space it asks for, so alignment along that axis has no effect. Across it, each child gets the full content size and aligns itself: in a column, `STRETCH` makes a child as wide as the column, and `CENTER` centers it. Hidden children take no space and no gap.

![A column with stretched, centered and right-aligned children](../assets/images/layout-stack.png)

```java
--8<-- "layout/LayoutExamples.java:stack"
```

!!! tip "A stack panel never shares out leftover space"
    In a column, a child cannot ask for "the rest of the height". If you need one child to fill what is left, use a `GridPanel` with a weighted row or column. See [Filling the remaining space](#filling-the-remaining-space).

## GridPanel

Children sit in the cells of rows and columns. Each row and column has a `GridLength`:

| Length | Size |
|---|---|
| `GridLength.auto()` | As big as the largest child that sits only in it |
| `GridLength.fixed(size)` | Exactly `size` units, whatever its children need |
| `GridLength.weighted(weight)` | A share of the space left after the fixed and auto tracks. Weight 2 gets twice the space of weight 1. |

Fixed and auto tracks are sized first. Whatever width or height remains is shared among weighted tracks in proportion to their weights. When the grid itself is measured without a size limit, weighted tracks take the smallest size that fits their children while keeping their proportions.

```java
--8<-- "layout/LayoutExamples.java:grid"
```

![A two-column login form](../assets/images/layout-grid.png)

- `add(child, row, column)` puts a child in one cell. `add(child, row, column, rowSpan, columnSpan)` lets it cover several. A spanning child's slot includes the spacing between the cells it covers.
- A child added with plain `add(child)` sits in row 0, column 0.
- Several children may share a cell. They overlap, like in an anchor panel.
- A child aligns itself within its cell. `STRETCH` fills the cell.
- Children spanning several auto tracks grow those tracks if they would not fit otherwise.
- A grid with no rows or columns defined acts as one auto cell.
- A cell outside the defined rows or columns is moved to the last row or column, and spans are cut to fit. A negative row or column, or a span below 1, throws `IllegalArgumentException`.
- `setSpacing(columnGap, rowGap)` puts gaps between tracks, never around the edge. Use padding for that.
- `columns(...)` and `rows(...)` replace the definitions; `addColumn` and `addRow` append one. `cellOf(child)` tells you where a child sits.

Weighted tracks make proportional layouts:

![A sidebar and a main area twice as wide, above a fixed status bar](../assets/images/layout-weighted.png)

```java
--8<-- "layout/LayoutExamples.java:weighted"
```

## AbsolutePanel

Each child sits at a fixed position measured from the top-left corner of the content area. Its slot is exactly its desired size plus margin at that position, so alignment has no effect. Children may overlap; later ones are drawn on top. The panel's desired size is just big enough to cover every child.

![Three buttons at fixed positions](../assets/images/layout-absolute.png)

```java
--8<-- "layout/LayoutExamples.java:absolute"
```

`add(child)` places a child at (0, 0). `move(child, x, y)` moves one later and does nothing if it is not a child. There is no getter for a child's position, so keep your own if you need it, as the [drag example](events.md#drags) does.

Use it for free placement, such as a board of draggable pieces. For forms and menus, the other containers adapt to text and screen size, so prefer them.

## Clipping

Containers clip their children to their own bounds by default. A clipped part of a child is neither drawn nor hit by the pointer, so a child scrolled out of view cannot be clicked. `setClipChildren(false)` lets children draw outside, for example a focus glow or a tooltip that hangs over the edge.

## Recipes

### Center something on the screen

Put it in a root anchor panel with `Anchor.FILL` and give it `Anchor.CENTER`. Give it a fixed width if it holds stretched children, so they have a width to stretch to.

### Filling the remaining space

Use a grid with a weighted track. For a toolbar with a button pushed to the far right, leave a weighted column empty:

![A toolbar with a gap that pushes Help to the right](../assets/images/layout-spacer.png)

```java
--8<-- "layout/LayoutExamples.java:spacer"
```

For a header, a body that takes the rest of the height and a footer, use rows `auto()`, `weighted(1)`, `auto()` and stretch the body.

### Wrapping text

A wrapping label needs a width to wrap to: a fixed width, a maximum width, or `STRETCH` inside a parent that has a width. Offered unlimited width, it breaks only at `\n`.

![A wrapping label in a fixed-width column](../assets/images/layout-wrap.png)

```java
--8<-- "layout/LayoutExamples.java:wrap"
```

### Equal-width buttons

Put them in a grid with equal weighted columns and give each `Anchor.FILL`, or give each the same fixed width.

## Common problems

| Symptom | Likely cause |
|---|---|
| A component is not visible at all | Its size is zero: a container with no children measures as just its padding. Or it is hidden, or outside a clipping ancestor, or the root is not `FILL` and is smaller than you think. |
| A stretched child is not stretching | Its parent has no width to give. The root must be `FILL`, or a parent between needs a fixed width or `STRETCH`. |
| A child is cut off | It is bigger than its slot and an ancestor clips. Give the ancestor more room, or turn clipping off. |
| Text does not wrap | See [Wrapping text](#wrapping-text). |
| Alignment along a stack's direction does nothing | A stack panel gives each child exactly its size along that axis. Use a grid. |
| `bounds()` is out of date | Layout runs before the next frame. Read bounds in a later frame or in an event handler. |

See [Troubleshooting](../help/troubleshooting.md) for more.

## Writing your own layout

Extend `CustomContainer`. The [extending guide](extending.md#write-a-new-layout) walks through a flow layout that wraps children into rows.
