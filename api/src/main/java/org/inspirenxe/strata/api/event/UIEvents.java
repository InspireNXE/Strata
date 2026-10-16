package org.inspirenxe.strata.api.event;

/**
 * The event types every component can receive.
 */
public final class UIEvents {
    public static final EventType<PointerEvent> POINTER_ENTER = EventType.direct("pointer_enter", PointerEvent.class);
    public static final EventType<PointerEvent> POINTER_EXIT = EventType.direct("pointer_exit", PointerEvent.class);
    public static final EventType<PointerEvent> POINTER_MOVE = EventType.bubbling("pointer_move", PointerEvent.class);
    public static final EventType<PointerButtonEvent> POINTER_PRESS = EventType.bubbling("pointer_press", PointerButtonEvent.class);
    public static final EventType<PointerButtonEvent> POINTER_RELEASE = EventType.bubbling("pointer_release", PointerButtonEvent.class);
    /**
     * A press and release on the same component without a drag in between.
     */
    public static final EventType<PointerButtonEvent> CLICK = EventType.bubbling("click", PointerButtonEvent.class);
    public static final EventType<DragEvent> DRAG_START = EventType.bubbling("drag_start", DragEvent.class);
    public static final EventType<DragEvent> DRAG = EventType.bubbling("drag", DragEvent.class);
    public static final EventType<DragEvent> DRAG_END = EventType.bubbling("drag_end", DragEvent.class);
    public static final EventType<ScrollEvent> SCROLL = EventType.bubbling("scroll", ScrollEvent.class);
    public static final EventType<KeyEvent> KEY = EventType.bubbling("key", KeyEvent.class);
    public static final EventType<CharEvent> CHAR = EventType.bubbling("char", CharEvent.class);
    public static final EventType<FocusEvent> FOCUS_GAINED = EventType.direct("focus_gained", FocusEvent.class);
    public static final EventType<FocusEvent> FOCUS_LOST = EventType.direct("focus_lost", FocusEvent.class);

    private UIEvents() {
    }
}
