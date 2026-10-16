package org.inspirenxe.strata.api.event;

/**
 * The event types Strata fires. Every component can receive every type; built-in widgets act on the ones listed
 * under "default behavior" below. Positions in pointer events are in the host's coordinates.
 */
public final class EventTypes {
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
     * focusable ancestor, or is cleared if there is none. Default behavior: a text box moves its caret or selects.
     */
    public static final EventType<PointerButtonEvent> POINTER_PRESS = EventType.bubbling("pointer_press", PointerButtonEvent.class);
    /**
     * A mouse button that went down on the target came up, wherever the pointer is now. Bubbles.
     */
    public static final EventType<PointerButtonEvent> POINTER_RELEASE = EventType.bubbling("pointer_release", PointerButtonEvent.class);
    /**
     * A press and release of the same button on the target, without a drag in between, with the pointer still over
     * the target at release and the target enabled. Bubbles. Fired after {@link #POINTER_RELEASE}. Default behavior:
     * a button runs its actions on a left click.
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
     * A key went down, repeated or came up. Fired at the focused component, or the screen's root when nothing has
     * focus. Bubbles. If nothing consumes a Tab press, focus moves to the next tabbable component (Shift+Tab: the
     * previous). Default behavior: a button presses on Enter or Space; a text box edits and moves its caret.
     */
    public static final EventType<KeyEvent> KEY = EventType.bubbling("key", KeyEvent.class);
    /**
     * A character was typed. Fired only at the focused component; dropped when nothing has focus. Bubbles. Default
     * behavior: a text box inserts it.
     */
    public static final EventType<CharEvent> CHAR = EventType.bubbling("char", CharEvent.class);
    /**
     * The target gained keyboard focus. Direct. Default behavior: a text box moves its caret to the end.
     */
    public static final EventType<FocusEvent> FOCUS_GAINED = EventType.direct("focus_gained", FocusEvent.class);
    /**
     * The target lost keyboard focus. Direct.
     */
    public static final EventType<FocusEvent> FOCUS_LOST = EventType.direct("focus_lost", FocusEvent.class);

    private EventTypes() {
    }
}
