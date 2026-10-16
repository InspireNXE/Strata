package org.inspirenxe.strata.api.event;

@FunctionalInterface
public interface EventHandler<E extends Event> {

    void handle(E event);
}
