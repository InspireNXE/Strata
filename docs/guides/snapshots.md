# Event snapshots

A snapshot records a component's state at the moment an event fired, before any handler or default behavior ran. A handler can compare it with the live component to see what the event changed: the text before a key press, whether a box was checked before a click, where a component was before a drag.

## Asking for snapshots

Register the handler with `onWithSnapshot` instead of `on`. It takes the same arguments:

```java
--8<-- "snapshots/SnapshotExamples.java:compare"
```

Inside the handler:

| Method | Returns |
|---|---|
| `event.snapshot()` | The target's snapshot |
| `event.currentSnapshot()` | The snapshot of the component whose handler is running |
| `event.snapshotOf(component)` | Any component's snapshot, as an `Optional` |

## What is captured, and when

Snapshots are lazy. Before an event is delivered, Strata walks its path: the target and, if the type bubbles, the target's ancestors. If no component on that path has an `onWithSnapshot` handler for this event type, nothing is captured and nothing is allocated. Otherwise Strata snapshots:

- the target, always; and
- every component on the path that has an `onWithSnapshot` handler for this type.

Nothing else is captured. All snapshots are taken before the first handler runs, so even a handler at the root sees the target as it was before the target's own handlers and default behavior changed it.

### What "when the event fired" means

Snapshots are taken after the host has updated its own bookkeeping for the input and before the event reaches any handler. A few events change host state first, and their snapshots show the new state:

| Event | Already updated in the snapshot |
|---|---|
| `POINTER_PRESS` | Focus has moved to the target or its nearest focusable ancestor, and the target is pressed |
| `POINTER_RELEASE`, `CLICK`, `DRAG_END` | The target is no longer pressed, unless another button still holds it |
| `POINTER_ENTER`, `POINTER_EXIT` | The target is hovered, or no longer hovered |
| `FOCUS_GAINED`, `FOCUS_LOST` | Focus has already changed |

Everything a handler or a default behavior changes, such as text, caret, checked state or enabled state, is still in its old state.

### Shallow

Snapshots are shallow. A snapshot of a panel holds the panel's own state and nothing about its children. A child that was captured has its own snapshot.

```java
--8<-- "snapshots/SnapshotExamples.java:ancestor"
```

## What a snapshot holds

`ComponentSnapshot` is a record. It holds what the `Component` getters returned when it was taken:

| Group | Fields |
|---|---|
| Identity | `component()` (the live component), `id` |
| Flags | `visible`, `enabled`, `focusable`, `tabbable`, `tabIndex`, `cursor`, `hovered`, `pressed`, `focused` |
| Layout inputs | `margin`, `minWidth`, `maxWidth`, `minHeight`, `maxHeight`, `horizontalAlignment`, `verticalAlignment`, `background` |
| Layout results | `bounds`, `desiredWidth`, `desiredHeight` |
| Kind-specific | `state()`, or `state(Class)` for a typed `Optional` |

```java
--8<-- "snapshots/SnapshotExamples.java:common"
```

The `state` is what the component's kind adds. The built-ins record a nested `State` record:

| Component | State record | Fields |
|---|---|---|
| `TextBox` | `TextBox.State` | `text` (unmasked even in password mode), `caret`, `anchor`, `maxLength`, `password`, plus `selectionStart()`, `selectionEnd()` and `hasSelection()` |
| `Button` | `Button.State` | `text` |
| `Label` | `Label.State` | `text`, `style`, `wraps`, `textAlignment` |
| `StackPanel` | `StackPanel.State` | `orientation`, `spacing` |
| `GridPanel` | `GridPanel.State` | `columns`, `rows`, `columnSpacing`, `rowSpacing`. Not the children or their cells. |
| `AnchorPanel`, `AbsolutePanel` | none | `state()` is null |

`state(Class)` returns empty when there is no state or it is of another type, so it is safe to call on any snapshot.

## Snapshots outlive the event

Unlike the event, a snapshot stays valid after delivery and never changes. Keep it for undo, logging or animation:

```java
--8<-- "snapshots/SnapshotExamples.java:keep"
```

A snapshot holds a reference to its live component, so keeping one keeps the component reachable.

## Your own components

A `CustomComponent` adds its state by overriding `snapshotState()`:

```java
@Override
protected Object snapshotState() {
    return new State(this.checked, this.text);
}
```

Strata calls it only when the component is captured, before any handler runs. Return an immutable value, such as a record, and copy any mutable data: a live list would change along with the component. Record only the component's own state; a container must not include its children, which are captured on their own. Do not change any component from it. The default returns null. The [check box example](extending.md#write-a-new-component) shows a complete component with a snapshot state.

## When there is no snapshot

| Situation | `snapshot()` | `currentSnapshot()` | `snapshotOf(c)` |
|---|---|---|---|
| No `onWithSnapshot` handler for this type anywhere on the path | throws `IllegalStateException` | throws | empty |
| Some handler on the path asked, but not the current component's | works (the target is always captured) | throws, unless current is the target | empty for uncaptured components |
| Your own handler asked | works | works | present for the target and every component that asked |

So a handler registered with plain `on` can still read snapshots when another handler on the same path asked for them, but should not rely on it. Register every handler that reads snapshots with `onWithSnapshot`. A component that joins the path while the event is being delivered, for example because a handler moved the target into another container, has no snapshot.

## Cost

An event whose path has no `onWithSnapshot` handler costs nothing extra. An event that does have one allocates one snapshot per captured component, plus whatever `snapshotState()` builds. Register this way only the handlers that read snapshots, and prefer narrow event types: an `onWithSnapshot` handler for `POINTER_MOVE` on a root panel snapshots the hovered component on every mouse move.

## Why not always snapshot?

Taking a snapshot of every component an event reaches, on every event, would allocate on every pointer move. Lazy, shallow snapshots keep the common case free and still give a handler that asks everything it needs about its own component and the target. This is recorded in the [design](../DESIGN.md#8-decisions-and-open-questions).
