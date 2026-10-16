package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.component.Component;

import java.util.Optional;

public interface FocusEvent extends Event {

    /**
     * The component losing focus (for a gain) or gaining it (for a loss), if any.
     */
    Optional<Component<?>> other();
}
