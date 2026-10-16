package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.HorizontalAlignment;
import org.inspirenxe.strata.api.component.VerticalAlignment;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

/**
 * Strata's half of a {@link CustomComponent}: the state and bookkeeping every component needs. Only Strata's
 * implementation provides peers, through {@link org.inspirenxe.strata.api.Ui#createPeer}; the custom component's
 * {@code final} methods delegate here.
 */
public interface ComponentPeer {

    Container<?> parent();

    boolean isAttached();

    Theme theme();

    String id();

    void setId(String id);

    boolean isVisible();

    void setVisible(boolean visible);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isFocusable();

    void setFocusable(boolean focusable);

    boolean isTabbable();

    void setTabbable(boolean tabbable);

    int tabIndex();

    void setTabIndex(int tabIndex);

    CursorKind cursor();

    void setCursor(CursorKind cursor);

    boolean isHovered();

    boolean isPressed();

    boolean isFocused();

    boolean requestFocus();

    Insets margin();

    void setMargin(Insets margin);

    float minWidth();

    float maxWidth();

    float minHeight();

    float maxHeight();

    void setMinSize(float minWidth, float minHeight);

    void setMaxSize(float maxWidth, float maxHeight);

    void setWidth(float width);

    void setHeight(float height);

    HorizontalAlignment horizontalAlignment();

    VerticalAlignment verticalAlignment();

    void setAlignment(HorizontalAlignment horizontal, VerticalAlignment vertical);

    Paint background();

    void setBackground(Paint background);

    Rect bounds();

    float desiredWidth();

    float desiredHeight();

    void invalidateLayout();

    <E extends Event> Subscription on(EventType<E> type, Phase phase, EventHandler<? super E> handler, Object owner);

    void removeHandlers(Object owner);
}
