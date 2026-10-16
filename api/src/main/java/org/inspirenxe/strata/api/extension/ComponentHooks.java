package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Set;

/**
 * The calls Strata's implementation makes into a {@link CustomComponent}'s protected methods. Each custom component
 * hands a private instance to its peer; nothing else should hold or call one.
 */
public interface ComponentHooks {

    Size measure(MeasureContext context, float availableWidth, float availableHeight);

    void arrange(ArrangeContext context, Rect bounds);

    void paint(Painter painter, Rect bounds);

    Set<String> requiredStyleKeys();

    void applyTheme(Theme theme);

    boolean acceptsTextInput();

    /**
     * Runs the component's default behavior for an event.
     */
    void handle(EventType<?> type, Event event);

    void childRemoved(Component<?> child);
}
