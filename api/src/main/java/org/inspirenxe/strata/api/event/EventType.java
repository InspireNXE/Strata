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
package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.event.control.ControlEvent;
import org.inspirenxe.strata.api.event.control.RequestEvent;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.event.input.InputEvents;

import java.util.Objects;

/**
 * A typed key for one kind of event, used to register handlers with {@link EventTarget#onEvent}. Input types are in
 * {@link InputEvents}, and each control declares its own as constants on its interface.
 *
 * <p>Types compare by identity: each call to a factory makes a type equal only to itself, even when another has the
 * same name and event class. Keep each type in a constant, and register handlers and fire events with that
 * constant. Two libraries that both declare {@code valueReport("changed")} get two types, and a handler for one
 * never hears the other. The name is only for debugging and need not be unique, though a prefixed name such as
 * {@code "gauge.changed"} reads better in logs.</p>
 *
 * @param <E> the event interface handlers receive
 */
public final class EventType<E extends Event> {
    private final String name;
    private final Class<E> eventClass;
    private final boolean bubbles;

    private EventType(final String name, final Class<E> eventClass, final boolean bubbles) {
        this.name = Objects.requireNonNull(name, "name");
        this.eventClass = Objects.requireNonNull(eventClass, "eventClass");
        this.bubbles = bubbles;
    }

    /**
     * Creates a type whose events travel from the target up through its ancestors, to the top of the tree it is in:
     * the screen's content, or an overlay's content.
     *
     * @param <E> the event interface
     * @param name a readable name
     * @param eventClass the event interface
     * @return the new type
     */
    public static <E extends Event> EventType<E> bubbling(final String name, final Class<E> eventClass) {
        return new EventType<>(name, eventClass, true);
    }

    /**
     * Creates a type whose events reach only their target.
     *
     * @param <E> the event interface
     * @param name a readable name
     * @param eventClass the event interface
     * @return the new type
     */
    public static <E extends Event> EventType<E> direct(final String name, final Class<E> eventClass) {
        return new EventType<>(name, eventClass, false);
    }

    /**
     * Creates a bubbling type for a control's request without a value, such as a press. Like every request and
     * report, its events stop at the first composite above the control, unless they come from content the composite
     * holds for its user. Fire it with {@code CustomComponent.request}.
     *
     * @param <C> the control type
     * @param name a readable name
     * @return the new type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <C> EventType<RequestEvent<C>> request(final String name) {
        return new EventType<>(name, (Class) RequestEvent.class, true);
    }

    /**
     * Creates a bubbling type for a control's request that carries a value, such as an edit. Its events stop at the
     * first composite above the control, as {@link #request} says. Fire it with {@code CustomComponent.request}.
     *
     * @param <C> the control type
     * @param <V> the value type
     * @param name a readable name
     * @return the new type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <C, V> EventType<ValueRequestEvent<C, V>> valueRequest(final String name) {
        return new EventType<>(name, (Class) ValueRequestEvent.class, true);
    }

    /**
     * Creates a bubbling type for a control's report without a value, such as a press having happened. Its events
     * stop at the first composite above the control, as {@link #request} says. Fire it with
     * {@code CustomComponent.report}.
     *
     * @param <C> the control type
     * @param name a readable name
     * @return the new type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <C> EventType<ControlEvent<C>> report(final String name) {
        return new EventType<>(name, (Class) ControlEvent.class, true);
    }

    /**
     * Creates a bubbling type for a control's report that carries a value, such as the text after an edit. Its
     * events stop at the first composite above the control, as {@link #request} says. Fire it with
     * {@code CustomComponent.report}.
     *
     * @param <C> the control type
     * @param <V> the value type
     * @param name a readable name
     * @return the new type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <C, V> EventType<ValueEvent<C, V>> valueReport(final String name) {
        return new EventType<>(name, (Class) ValueEvent.class, true);
    }

    /**
     * Returns the type's name.
     *
     * @return the name given to the factory, for debugging
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns the event interface handlers of this type receive.
     *
     * @return the event interface
     */
    public Class<E> eventClass() {
        return this.eventClass;
    }

    /**
     * Returns whether events of this type travel up through the target's ancestors.
     *
     * @return true for a bubbling type, false for a direct one
     */
    public boolean bubbles() {
        return this.bubbles;
    }

    /**
     * Compares by identity.
     *
     * @param other the object to compare with
     * @return true only if {@code other} is this type
     */
    @Override
    public boolean equals(final Object other) {
        return this == other;
    }

    /**
     * Returns the identity hash code, to match {@link #equals}.
     *
     * @return the identity hash code
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Returns the type's name, for debugging.
     *
     * @return for example {@code EventType[click]}
     */
    @Override
    public String toString() {
        return "EventType[" + this.name + "]";
    }
}
