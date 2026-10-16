# Events

Input reaches components as events. This page covers registering handlers, the order things run in, cancellation and bubbling, and how clicks, drags, hover, scrolling, keys and focus are turned into events. For comparing state before and after an event, see [Event snapshots](snapshots.md).

## Registering a handler

```java
component.on(EventTypes.CLICK, event -> { ... });                    // BEFORE phase
component.on(EventTypes.CLICK, Phase.AFTER, event -> { ... });       // AFTER phase
component.on(EventTypes.CLICK, Phase.BEFORE, event -> { ... }, owner); // tagged with an owner
```

Event types are typed keys. `EventTypes.CLICK` is an `EventType<PointerButtonEvent>`, so the handler receives a `PointerButtonEvent` without a cast. Strata's types are the constants in `EventTypes`; always use those constants, because types are compared by identity.

Handlers for the same type and phase on the same component run in the order they were registered. A handler may register or remove handlers while it runs; the change applies from the next event.

## What runs, in what order

For every component an event reaches:

1. The component's `BEFORE` handlers.
2. The component's default behavior, such as a button pressing or a text box inserting a character, unless the event has been canceled.
3. The component's `AFTER` handlers. They run even if the event was canceled.

A bubbling event then moves to the parent and repeats, up to the screen's root, unless something stopped it. A direct event stays at its target.

```java
--8<-- "events/EventExamples.java:veto"
```

```java
--8<-- "events/EventExamples.java:after"
```

Use `BEFORE` to veto or replace what a component does, and `AFTER` to react to what it did.

## Canceling and stopping

| Call | Effect |
|---|---|
| `cancel()` | Skips the default behavior of the current component and of every ancestor the event still reaches. Handlers keep running, and the event keeps bubbling. |
| `stopPropagation()` | The event goes no further up. The current component's remaining handlers and its default behavior still run. |
| `consume()` | Both: "handled here". |

`isCancelled()`, `isPropagationStopped()` and `isHandled()` (either one) report the state. Canceling from the default behavior or from an `AFTER` handler is too late for the current component, so it only affects ancestors.

When an event finishes, the host reports it to the adapter as consumed if it was canceled or stopped, and the adapter keeps that input from the engine. A handler that acts on input should call `consume()`, so ancestors and the engine do not act on it too.

!!! warning "Built-in widgets consume what they act on"
    A button consumes the clicks and keys that press it, and a text box consumes the keys and characters it edits with. Consuming stops propagation, so those events never reach the widget's ancestors. A handler on a panel does not see clicks on the buttons inside it. Put the handler on the button, in `BEFORE` if it must run first.

## Target and current

`target()` is the component the event was fired at. It is fixed for the whole delivery. `current()` is the component whose handlers or default behavior are running now. It starts equal to the target and moves to each ancestor as the event bubbles.

`PointerEvent.localX()` and `localY()` are relative to the current component, so they change as the event bubbles. `x()` and `y()` are in host coordinates and do not change.

### Handling events on a container

One handler on a container can serve all of its children, because events from children bubble up to it. This works for children that do not consume the event, such as labels:

```java
--8<-- "events/EventExamples.java:delegate"
```

The `target() != palette` check skips clicks on the panel's own padding, where the panel itself is the target.

## Event objects are short-lived

An event object is valid only while it is being delivered. Do not keep it to read later: Strata may reuse it, and `current()` changes as it bubbles. To keep the state a component had when the event fired, take a [snapshot](snapshots.md), which stays valid.

## Removing handlers

```java
--8<-- "events/EventExamples.java:remove"
```

A `Subscription` is `AutoCloseable`, so it also works in try-with-resources. Closing it twice does nothing. Owners are compared by identity; passing your screen object as the owner lets the screen remove everything it registered with one call per component.

Removing a component from its tree does not remove its handlers. That rarely matters, because a closed screen's tree is dropped along with them. It matters if you keep a component and reuse it.

## Exceptions

A handler that throws is not caught by Strata. The exception propagates out of the input call that caused the event, into the adapter and the engine. Catch your own errors in handlers.

## Clicks

A press, a release and a click are three events:

1. `POINTER_PRESS` fires at the deepest component under the pointer. Just before it fires, focus moves to that component or its nearest focusable ancestor, or is cleared if there is none.
2. `POINTER_RELEASE` fires at the component the press started on, wherever the pointer is now.
3. `CLICK` fires after the release, at the same component, if the pointer is still over it, the component is enabled, and no drag happened in between.

Each mouse button is tracked separately, so a right-button press during a left-button drag is its own press.

```java
--8<-- "events/EventExamples.java:buttons"
```

### Click counting

`clicks()` says where a press falls in a run of quick presses: 1 for a single click, 2 for a double, 3 for a triple, and then it starts again at 1. A press continues the run when it hits the same component with the same button within 500 ms and within 4 units on each axis of the previous press. A release and its click carry the count of their press.

Strata delivers every press immediately with its count. It never holds a single click back to wait and see whether a double follows, so a double click produces a click with `clicks() == 1` and then one with `clicks() == 2`. React to the count you care about.

## Drags

A press becomes a drag once the pointer moves more than 4 units from where the button went down, measured on either axis.

| Event | When | Positions |
|---|---|---|
| `DRAG_START` | The pointer first passes the threshold | Those of the original press. Deltas are zero. |
| `DRAG` | Every move after that, wherever the pointer is | The current position, with the movement since the previous drag event in `deltaX()` and `deltaY()` |
| `DRAG_END` | The button comes up | The release position. It replaces the `CLICK` that release would otherwise cause. |

Drag events go to the component the press started on, even when the pointer leaves it. `startX()` and `startY()` give the press position, and `clicks()` the press's click count, so a drag after a double click can select by word as text boxes do.

![A label that can be dragged](../assets/images/events-drag.png)

```java
--8<-- "events/EventExamples.java:drag"
```

If the pointer leaves the host during a drag, the press stays tracked, and the drag continues when the pointer comes back.

## Hover

Only the deepest component under the pointer is hovered. Hit testing respects clipping, visibility and enabled state: the pointer cannot hit a hidden or disabled component, or the part of a child cut off by a clipping parent.

| Event | Bubbles | When |
|---|---|---|
| `POINTER_ENTER` | no | The component became the deepest one under the pointer |
| `POINTER_EXIT` | no | It stopped being the deepest one, or the pointer left the host |
| `POINTER_MOVE` | yes | The pointer moved over it. Fired after any enter or exit for the same movement. |

Enter and exit do not bubble, so moving from a panel onto a button inside it gives the panel an exit and the button an enter. When layout moves a component under a still pointer, the enter and exit fire on the next pointer input, not at the moment of the layout.

## Scrolling

`SCROLL` fires at the component under the pointer and bubbles, so an outer container receives scrolls its children do not consume.

| Value | Meaning |
|---|---|
| `scrollY()` | Positive when the wheel turns away from the user, which scrolls toward the top of the content |
| `scrollX()` | Positive toward the left, the start of a row |
| `kind()` | `WHEEL` for a notched wheel (1 per notch), `PRECISE` for trackpads and smooth wheels, which report fractions |

Engines differ in how horizontal scrolling arrives; see [Platform differences](../platforms/differences.md#scrolling).

## Keys and characters

`KEY` carries a physical key (`key()`), an action (`PRESS`, `REPEAT` or `RELEASE`) and the held modifiers. It fires at the focused component, or at the screen's root when nothing has focus, and bubbles. If nothing consumes a Tab press, focus moves to the next tabbable component.

`CHAR` carries a typed character as a code point, after the keyboard layout and any input method. It fires only at the focused component and is dropped when nothing has focus. It bubbles.

A key handler on the root sees every key that nothing focused consumed, which makes it the place for screen-wide shortcuts:

```java
--8<-- "events/EventExamples.java:shortcut"
```

[Input and focus](input-focus.md) covers keys, modifiers, shortcut matching and key repeat in detail.

## Focus events

`FOCUS_LOST` and `FOCUS_GAINED` are direct. When focus moves from one component to another, the old one gets `FOCUS_LOST` first, then the new one gets `FOCUS_GAINED`, and both fire after the focus has already changed. `other()` is the component on the other side of the move, or empty when focus came from or went to nothing.

## Custom event types

`EventType.bubbling(name, eventClass)` and `EventType.direct(name, eventClass)` create new types, and `CustomComponent.onOtherEvent` is the hook for their default behavior. There is no public way to fire an event yet, though, so custom types are reserved for now. For component-specific notifications, use listeners as the widgets do (`onAction`, `onTextChanged`), as the [check box example](extending.md#write-a-new-component) does with `onChange`.

## All event types

| Type | Event interface | Bubbles | Fired at |
|---|---|---|---|
| `POINTER_ENTER` | `PointerEvent` | no | The newly hovered component |
| `POINTER_EXIT` | `PointerEvent` | no | The previously hovered component |
| `POINTER_MOVE` | `PointerEvent` | yes | The hovered component |
| `POINTER_PRESS` | `PointerButtonEvent` | yes | The component under the pointer |
| `POINTER_RELEASE` | `PointerButtonEvent` | yes | The component the press started on |
| `CLICK` | `PointerButtonEvent` | yes | The same, if still under the pointer and enabled, with no drag |
| `DRAG_START`, `DRAG`, `DRAG_END` | `DragEvent` | yes | The component the press started on |
| `SCROLL` | `ScrollEvent` | yes | The component under the pointer |
| `KEY` | `KeyEvent` | yes | The focused component, or the root |
| `CHAR` | `CharEvent` | yes | The focused component only |
| `FOCUS_GAINED`, `FOCUS_LOST` | `FocusEvent` | no | The component gaining or losing focus |

The [event type reference](../reference/event-types.md) adds each type's default behavior in the built-in widgets.
