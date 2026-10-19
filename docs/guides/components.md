# Components

Everything on screen is a `Component`. This page covers what all components share. The built-in controls add their own behavior on top, described in [Controls](controls.md).

## Getting components

There are exactly two ways to get a component:

- From the `ComponentFactory` a screen receives: `anchorPanel()`, `stackPanel()`, `stackPanel(Orientation)`, `gridPanel()`, `absolutePanel()`, `label(text)`, `button(text)` and `textBox()`. To change what a built-in does, handle its [events](events.md).
- By subclassing `CustomComponent` or `CustomContainer`. See [Extending Strata](extending.md).

Each factory call returns a new component with default settings, not attached to anything.

!!! danger "Do not implement the component interfaces"
    A class that implements `Component`, `Button` or any other component interface directly has no Strata implementation behind it. Adding it to a container, or returning it from `Screen.build`, throws `IllegalArgumentException`. Subclass `CustomComponent` instead.

## The type parameter

`Component<T extends Component<T>>` is self-typed: every setter returns `T`, the type you see the component as. That is what makes chaining work without casts:

```java
Button save = factory.button("Save").setEnabled(false).setTabIndex(2).onEvent(Button.PRESSED, e -> this.save());
```

Setters inherited from `Component` return the specific type: `setEnabled` on a `Button` returns `Button`, and `setPadding` on a `StackPanel` returns `StackPanel`. When a parameter or field only needs "some component", use `Component<?>`.

## The tree

| Method | What it does |
|---|---|
| `parent()` | The container this component was added to, or `null` for a root or a detached component |
| `Container.add(child)` | Adds at the end, so the child is drawn on top of earlier ones |
| `Container.remove(child)` | Removes it. It loses focus, hover and press state and is detached. Does nothing if it is not a child. |
| `Container.clear()` | Removes every child |
| `Container.children()` | A live, unmodifiable view in drawing order |

`add` throws `IllegalArgumentException` if the child already has a parent, is a screen's root, is the container itself or one of its ancestors, has no Strata implementation, or was made by another factory. Remove a component from its parent before adding it elsewhere.

`add` can also throw `ThemeException`: when the container is already on screen, the child is checked against the host's theme right away. See [Themes](theming.md#required-and-optional-keys).

### Attachment

A component is attached while it is in a tree that a host is showing. `isAttached()` tells you. Some things only happen while attached:

- Hover, focus and press state change only while attached.
- `theme()` is `null` until the component is first attached.
- `requestFocus()` fails on a detached component.
- Custom components receive event hooks only while attached.

Adding a child to an attached container attaches the child, and the child's theme hook runs before the next layout.

Removing a component does not remove its event handlers. Close the subscriptions, or use an [owner token](events.md#removing-handlers), if the component will be reused.

## State flags

| Flag | Default | Effect |
|---|---|---|
| `setVisible(false)` | visible | Not drawn, cannot be hovered or focused, takes no space in layout, and its bounds become empty |
| `setEnabled(false)` | enabled | Still drawn (built-in controls draw a disabled look) and still takes space, but gets no pointer or keyboard input and cannot be focused |
| `setFocusable(true)` | buttons, check boxes, sliders and text boxes only | Can take keyboard focus by a click or `requestFocus()` |
| `setTabbable(true)` | buttons, check boxes, sliders and text boxes only | Tab and Shift+Tab stop here. Only matters if it is also focusable. |
| `setTabIndex(n)` | 0 | Tab visits lower indexes first, then tree order among equal indexes. Negative values are allowed. |

Hiding or disabling a component also hides or disables everything inside it, and clears focus if the focused component is inside.

!!! note "Disabled components pass the pointer to their ancestors"
    The pointer cannot hit a disabled or hidden component or anything inside it. A click on a disabled button therefore lands on the nearest enabled ancestor under the pointer, usually the panel behind it. If that ancestor has a click handler, it runs.

Read-only state:

| Method | True when |
|---|---|
| `isHovered()` | The pointer is over this component and it is the deepest one there. Its ancestors are not hovered. |
| `isPressed()` | A mouse button went down on it and has not come up yet, even if the pointer has moved away |
| `isFocused()` | It has keyboard focus. At most one component per host does. |

## Ids

`setId(String)` attaches a free-form name used only for debugging, in `toString()` and in event handlers. Ids need not be unique, and Strata never looks components up by id.

## Cursors

`setCursor(CursorKind)` picks the mouse cursor shown while the pointer is over the component. A component with no cursor of its own uses its nearest ancestor's, and the default is `ARROW`. Buttons, check boxes and sliders show `HAND`, and text boxes show `TEXT`.

| `CursorKind` | Meaning |
|---|---|
| `ARROW` | Default pointer |
| `TEXT` | I-beam, for editable text |
| `HAND` | Pointing hand, for clickable things |
| `CROSSHAIR` | Precise picking |
| `RESIZE_HORIZONTAL`, `RESIZE_VERTICAL`, `RESIZE_ALL` | Resizing and moving |
| `NOT_ALLOWED` | Something that cannot be used now |

Engines without a shape show the closest one they have. Swing has no "not allowed" cursor and shows the default arrow.

## Backgrounds

`setBackground(Paint)` draws a paint over the component's bounds before anything else of the component. It never changes layout. The default is `Paint.NONE`. Backgrounds are set per component; themes do not set them. See [Paints](theming.md#paints).

## Reading geometry

| Method | Returns |
|---|---|
| `bounds()` | Where the component sits as of the last layout, in the host's coordinates. `Rect.ZERO` before the first layout, and empty while hidden. |
| `desiredWidth()`, `desiredHeight()` | What it asked for in the last measure, margin excluded |

Bounds are in host coordinates, not relative to the parent. Pointer events give you `localX()` and `localY()` for positions relative to the current component.

Bounds are updated by layout, which runs before the next frame. Right after you change something, `bounds()` still holds the old value. Adapters can force a layout with `ScreenHost.layout()`, but screens and components should not need to.

## Threading and timing

- Call components only from the engine's UI thread.
- Setters take effect on the next frame. Changes that affect size schedule a layout; you never call layout or repaint.
- `invalidateLayout()` exists for custom components whose content changes size. Built-in setters call it for you.
