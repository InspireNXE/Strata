/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.api.event.input;

import org.inspirenxe.strata.api.event.EventType;

/**
 * The input event types Strata fires. Every component can receive every type; built-in controls act on the ones listed
 * under "default behavior" below. Positions in pointer events are in the host's coordinates.
 */
public final class InputEvents {
    /**
     * The pointer moved onto the target, which is now the deepest component under it. Direct: ancestors are not
     * told. Also fired when a component appears under a still pointer after a layout change, on the next pointer
     * input.
     */
    public static final EventType<PointerEvent> POINTER_ENTER = EventType.direct("pointer_enter", PointerEvent.class);
    /**
     * The pointer left the target, or the target stopped being the deepest component under it, or the pointer left
     * the host's area. Direct.
     */
    public static final EventType<PointerEvent> POINTER_EXIT = EventType.direct("pointer_exit", PointerEvent.class);
    /**
     * The pointer moved over the target. Bubbles. Fired after any {@link #POINTER_ENTER} or {@link #POINTER_EXIT}
     * for the same movement.
     */
    public static final EventType<PointerEvent> POINTER_MOVE = EventType.bubbling("pointer_move", PointerEvent.class);
    /**
     * A mouse button went down over the target. Bubbles. Before it fires, focus moves to the target or its nearest
     * focusable ancestor, or is cleared if there is none. A press outside an open popup that closes on such a press
     * only closes the popup, and fires nothing. Default behavior: a text box moves its caret or selects.
     */
    public static final EventType<PointerButtonEvent> POINTER_PRESS = EventType.bubbling("pointer_press", PointerButtonEvent.class);
    /**
     * A mouse button that went down on the target came up, wherever the pointer is now. Bubbles.
     */
    public static final EventType<PointerButtonEvent> POINTER_RELEASE = EventType.bubbling("pointer_release", PointerButtonEvent.class);
    /**
     * A press and release of the same button on the target, without a drag in between, with the pointer still over
     * the target at release and the target enabled. Bubbles. Fired after {@link #POINTER_RELEASE}. Default behavior:
     * a button presses on a left click.
     */
    public static final EventType<PointerButtonEvent> CLICK = EventType.bubbling("click", PointerButtonEvent.class);
    /**
     * The pointer moved more than 4 units with a button held that went down on the target. Bubbles. Positions are
     * those of the original press.
     */
    public static final EventType<DragEvent> DRAG_START = EventType.bubbling("drag_start", DragEvent.class);
    /**
     * The pointer moved during a drag. Bubbles. Fired on every move after {@link #DRAG_START}, wherever the pointer
     * is. Default behavior: a text box extends its selection.
     */
    public static final EventType<DragEvent> DRAG = EventType.bubbling("drag", DragEvent.class);
    /**
     * The button holding a drag came up. Bubbles. Replaces the {@link #CLICK} that release would otherwise cause.
     */
    public static final EventType<DragEvent> DRAG_END = EventType.bubbling("drag_end", DragEvent.class);
    /**
     * A wheel or trackpad scrolled over the target. Bubbles, so an outer scrolling container receives scrolls its
     * children do not consume.
     */
    public static final EventType<ScrollEvent> SCROLL = EventType.bubbling("scroll", ScrollEvent.class);
    /**
     * A key went down, repeated or came up. Fired at the focused component or, when nothing has focus, at the
     * content of the top dialog, or else at the screen's content. Bubbles. If nothing consumes a press or repeat, the
     * host runs the first enabled command whose shortcut matches it (see
     * {@link org.inspirenxe.strata.api.command.Shortcuts}), then, where the platform uses mnemonics, Alt with a letter
     * acts on the control whose caption has that mnemonic (see
     * {@link org.inspirenxe.strata.api.control.button.Button}), and then Enter or Escape presses a default or cancel
     * button. If nothing consumes an Escape press, the top overlay that is not a toast closes. If nothing consumes a Tab press or repeat without Control or Alt, focus
     * moves to the next tabbable component (with Shift, the previous). Default behavior: a button presses on Enter or
     * Space; a text box edits and moves its caret.
     */
    public static final EventType<KeyEvent> KEY = EventType.bubbling("key", KeyEvent.class);
    /**
     * A character was typed. Fired only at the focused component; dropped when nothing has focus. Bubbles. Default
     * behavior: a text box inserts it.
     */
    public static final EventType<CharEvent> CHAR = EventType.bubbling("char", CharEvent.class);
    /**
     * The held modifier keys changed, by a modifier key going down or up or by input arriving with different
     * modifiers. Fired at the screen's content and at the content of each open overlay. Direct. No default
     * behavior.
     */
    public static final EventType<ModifiersEvent> MODIFIERS_CHANGED = EventType.direct("modifiers_changed", ModifiersEvent.class);

    private InputEvents() {
    }
}
