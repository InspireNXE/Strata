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
 * {@link InputEvents}, and each control declares its own as constants on its interface. Strata picks a component's
 * default behavior by comparing types by identity, so always use the constants rather than creating equal copies.
 *
 * @param <E> the event interface handlers receive
 * @param name a readable name, for debugging
 * @param eventClass the event interface handlers receive
 * @param bubbles whether the event travels up through the target's ancestors
 */
public record EventType<E extends Event>(String name, Class<E> eventClass, boolean bubbles) {
    /**
     * Checks the arguments.
     */
    public EventType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(eventClass, "eventClass");
    }

    /**
     * Creates a type whose events travel from the target up to the screen's root.
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
     * Creates a bubbling type for a control's request without a value, such as a press.
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
     * Creates a bubbling type for a control's request that carries a value, such as an edit.
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
     * Creates a bubbling type for a control's report without a value, such as a press having happened.
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
     * Creates a bubbling type for a control's report that carries a value, such as the text after an edit.
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
     * Returns the type's name, for debugging.
     *
     * @return for example {@code EventType[click]}
     */
    @Override
    public String toString() {
        return "EventType[" + this.name + "]";
    }
}
