# Input and focus

Components never see engine input codes. Each adapter maps its engine's keys, buttons and modifiers to Strata's types in `org.inspirenxe.strata.api.input` before handing them over, so a component written against the API behaves the same on every engine.

## Keys

`Key` names physical keys by their position on a US keyboard. `Key.A` is the key labeled A on a US layout, whatever the user's own layout prints on it, so on a French AZERTY keyboard it is the key labeled Q. Use keys for shortcuts and navigation, and [characters](#characters) for text.

The constants cover letters, digits (`NUM_0` to `NUM_9`), punctuation, `SPACE`, `ENTER`, `TAB`, `BACKSPACE`, `ESCAPE`, `INSERT`, `DELETE`, arrows, `HOME`, `END`, `PAGE_UP`, `PAGE_DOWN`, the lock keys, `PRINT_SCREEN`, `PAUSE`, `F1` to `F12`, the keypad (`KP_0` to `KP_9`, `KP_ENTER` and friends), left and right modifiers, and `MENU`. Keys an adapter cannot identify arrive as `Key.UNKNOWN`. On Minecraft that includes F13 to F24 and the keypad comma.

A few helpers: `isEnter()` is true for `ENTER` and `KP_ENTER`, and `isPrintable()` for keys that usually type a character.

!!! warning "Store keys by name"
    The order of the `Key` constants carries no meaning and may change between versions. Save a key binding as `key.name()` and load it with `Key.valueOf(name)`, never by ordinal.

### Key actions

Each `KEY` event has a `KeyAction`:

| Action | Meaning |
|---|---|
| `PRESS` | The key went down |
| `REPEAT` | It is still held and repeating |
| `RELEASE` | It came up |

`action().isDown()` is true for presses and repeats, the moments when a held key should act again, such as moving a caret.

### Key repeat

Strata delivers repeats on every engine. Engines differ in how they report a held key, and the adapter evens that out:

- If the engine sends real repeats, the adapter passes them on.
- If the engine reports a held key as more presses (AWT and Minecraft do), the adapter uses the SPI's `HeldKeys` to turn the second and later presses into `REPEAT`.
- If the engine sends no repeats at all, the host makes them: after 400 ms, then every 33 ms, for Left, Right, Up, Down, Backspace, Delete, Home, End, Page Up and Page Down only. These timings follow the host's clock and need `render` to be called every frame.

Both shipped adapters fall in the second group, so you see the user's system repeat rate there.

## Modifiers

`Modifier` is `SHIFT`, `CONTROL`, `ALT` or `SUPER`. The left and right keys of a kind count as the same modifier; use `Key.LEFT_SHIFT` and so on to tell them apart. `ALT` includes AltGr. `SUPER` is the Windows key, or Command on macOS.

`Modifiers` is an immutable set packed into an int, so events carry it without allocating:

```java
Modifiers held = event.modifiers();
held.shift();                    // also control(), alt(), superKey()
held.has(Modifier.CONTROL);
held.isEmpty();
Modifiers.of(Modifier.CONTROL, Modifier.SHIFT);
Modifiers.NONE.with(Modifier.ALT).without(Modifier.ALT);
```

Some engines do not report modifiers with every kind of input. Minecraft gives none with pointer moves, scrolls or typed characters, and those events report `Modifiers.NONE` there. See [Platform differences](../platforms/differences.md#modifier-keys).

## Shortcuts

`KeyEvent.is(key, required, policy)` is true for a press or repeat of `key` whose held modifiers satisfy the policy:

| `ModifierPolicy` | Matches when | Ctrl+S matches Ctrl+Shift+S? |
|---|---|---|
| `EXACT` | Exactly the required modifiers are held | no |
| `AT_LEAST` | The required modifiers are held, and maybe others | yes |
| `IGNORE` | Always, whatever is held | yes |
| `FORBID` | No modifier is held; `required` is ignored | no |

```java
--8<-- "events/EventExamples.java:shortcut"
```

Put screen-wide shortcuts on the root, as above. Keys bubble from the focused component, so a shortcut on the root fires unless something focused consumed the key first. A focused text box consumes plain typing keys, but not Control combinations it does not use, so Ctrl+S still reaches the root while the user is typing.

## Characters

`CHAR` events carry text the user typed, as Unicode code points, after the keyboard layout and any input method have done their work. Characters outside the Basic Multilingual Plane, such as emoji, arrive as one event. `CharEvent.text()` gives the character as a string.

Characters go only to the focused component and are dropped when nothing has focus. Adapters never send control characters.

A key press and its character are separate events: typing "a" produces a `KEY` press of `Key.A`, a `CHAR` of `'a'`, and a `KEY` release. Use `KEY` for what the key does and `CHAR` for what it types.

### Text input mode

Some engines send no characters at all until a text field asks for them. Minecraft 26.3 is one, as are other SDL-based engines. Others use the signal to show an on-screen keyboard or input method window.

Strata handles this through focus. When a component that accepts text input gains focus, the host tells the engine to start text input, at the component's bounds. When focus leaves the last such component, it tells the engine to stop. The built-in text box accepts text input; a custom component opts in by overriding `acceptsTextInput()` to return true.

## Focus

At most one component per host has keyboard focus. Keys go to it, or to the screen's root when nothing has focus. Characters go only to it.

A component can take focus when it is attached, focusable, visible and enabled, and every ancestor is visible and enabled. Built-in buttons, check boxes, sliders and text boxes are focusable; labels and panels are not, until you call `setFocusable(true)`.

Focus moves when:

- a mouse button goes down: focus moves to the pressed component or its nearest focusable ancestor, or is cleared if there is none. This happens just before `POINTER_PRESS` fires, so a click on empty space clears focus;
- Tab or Shift+Tab is pressed and nothing consumed it;
- code calls `requestFocus()`, which returns false and changes nothing if the component cannot take focus.

Focus is cleared when the focused component, or an ancestor, is hidden, disabled or removed. A screen that is rebuilt starts with nothing focused.

There is no public method to clear focus from a component. Requesting focus on another component moves it.

## Tab order

Tab moves to the next component in tab order and Shift+Tab to the previous one, wrapping at both ends. With nothing focused, Tab goes to the first and Shift+Tab to the last.

The order holds every component that is focusable and tabbable, with all its ancestors visible and enabled. It is sorted by tab index, lowest first, and components with equal indexes keep tree order: depth first, children in the order they were added. A focused component that is not in the order, for example one focused by a click with tabbing turned off, counts as "nothing focused".

Ctrl+Tab and Alt+Tab do not move focus, so they stay free for the engine or the operating system. A component that consumes Tab keeps focus where it is; nothing built in does.

```java
first.setTabIndex(1);
second.setTabIndex(2);
skipMe.setTabbable(false);   // still focusable by clicking
```

## Pointer input

Pointer positions are in UI units in host coordinates. Mouse buttons are `LEFT`, `RIGHT`, `MIDDLE`, `BACK` (button 4), `FORWARD` (button 5) and `UNKNOWN`. Clicks, drags, hover and scrolling are covered in [Events](events.md#clicks).

## What reaches the engine

Every input call into the host returns whether Strata used it, and adapters pass unused input on to the engine:

| Input | Counts as used when |
|---|---|
| Pointer move | The pointer is over a component, or a drag is in progress |
| Button press | It hit a component |
| Button release | It ended a press that started on a component |
| Scroll | A component consumed it |
| Key | A component consumed it, or Tab moved focus |
| Character | The focused component consumed it |

A screen whose root fills the host is "a component" everywhere, so it claims every press. On Minecraft that means vanilla widgets added to the same vanilla screen would never be clicked. See [Minecraft](../platforms/minecraft.md#input).
