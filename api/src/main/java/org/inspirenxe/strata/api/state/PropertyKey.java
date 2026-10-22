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
package org.inspirenxe.strata.api.state;

import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ControlEvent;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A property of a component that {@link org.inspirenxe.strata.api.component.Component#bind} can keep in step with
 * a {@link Source}. Keys are constants beside the events of the attribute or control they belong to, such as
 * {@link org.inspirenxe.strata.api.control.attribute.value.Toggle#SELECTED} beside {@code Toggle.TOGGLED}.
 *
 * <p>A key says how to read and write the property and, for a property the user can change, which reports tell of
 * a change. Bound to a {@link State}, a key with a report writes the user's changes back to the state. A developer's
 * own control declares keys for its properties the way it declares its events:</p>
 *
 * <pre>{@code
 * public static final EventType<ValueEvent<Rating, Integer>> RATED = EventType.valueReport("rating.rated");
 * public static final PropertyKey<Rating, Integer> STARS =
 *         PropertyKey.of("rating.stars", Rating::stars, Rating::setStars, RATED);
 * }</pre>
 *
 * <p>The writer is called on the UI thread with values that differ from what the reader returns. It must apply the
 * value as the control's setter does, reporting the change and firing no request. Between them, the reports must
 * fire after every change of the property, from the user or from code, with the control as their target, and the
 * reader must return the new value by then. Most properties have one report; one whose changes two events tell of,
 * such as a bar that reports opening and closing apart, lists both. A component holds one binding per key name, so
 * names must be unique among the keys a component can take; the convention is {@code "attribute.property"}.</p>
 *
 * <p>{@link #convert} makes a key for the same property with values of another type, such as a text box's text as
 * an {@code Integer}. Since the user can type what has no such value, a key also has a {@link #check()} that reads
 * the property as a {@link Conversion}; a key made with {@code of} always reads as valid.</p>
 *
 * @param <C> the type the key applies to, usually an attribute or control interface
 * @param <V> the type of the property's value
 * @param name a readable name, unique among the keys of one component
 * @param reader returns the property's current value
 * @param writer sets the property to a value
 * @param reports the reports that tell of a change, or empty for a property only code changes
 * @param check reads the property as a conversion: valid with the value {@code reader} returns, or invalid with why
 *     the control's value has none of type {@code V}
 */
public record PropertyKey<C, V>(String name, Function<? super C, ? extends V> reader,
                                BiConsumer<? super C, ? super V> writer,
                                List<EventType<? extends ControlEvent<?>>> reports,
                                Function<? super C, ? extends Conversion<? extends V>> check) {
    /**
     * Checks the arguments and copies the reports.
     */
    public PropertyKey {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(writer, "writer");
        Objects.requireNonNull(check, "check");
        reports = List.copyOf(reports);
    }

    /**
     * Creates a key whose check always reads the property as valid, with the value the reader returns.
     *
     * @param name a readable name, unique among the keys of one component
     * @param reader returns the property's current value
     * @param writer sets the property to a value
     * @param reports the reports that tell of a change, or empty for a property only code changes
     */
    public PropertyKey(final String name, final Function<? super C, ? extends V> reader,
                       final BiConsumer<? super C, ? super V> writer,
                       final List<EventType<? extends ControlEvent<?>>> reports) {
        this(name, reader, writer, reports, PropertyKey.alwaysValid(reader));
    }

    private static <C, V> Function<C, Conversion<V>> alwaysValid(final Function<? super C, ? extends V> reader) {
        Objects.requireNonNull(reader, "reader");
        return component -> Conversion.valid(reader.apply(component));
    }

    /**
     * Creates a key for a property only code changes, which binds one way.
     *
     * @param <C> the type the key applies to
     * @param <V> the type of the value
     * @param name a readable name
     * @param reader returns the property's value
     * @param writer sets the property
     * @return the new key
     */
    public static <C, V> PropertyKey<C, V> of(final String name, final Function<? super C, ? extends V> reader,
                                              final BiConsumer<? super C, ? super V> writer) {
        return new PropertyKey<>(name, reader, writer, List.of());
    }

    /**
     * Creates a key for a property the user can change, which binds two ways to a {@link State}.
     *
     * @param <C> the type the key applies to
     * @param <V> the type of the value
     * @param name a readable name
     * @param reader returns the property's value
     * @param writer sets the property
     * @param report the report that fires after every change of the property
     * @return the new key
     */
    public static <C, V> PropertyKey<C, V> of(final String name, final Function<? super C, ? extends V> reader,
                                              final BiConsumer<? super C, ? super V> writer,
                                              final EventType<? extends ControlEvent<?>> report) {
        return new PropertyKey<>(name, reader, writer, List.of(report));
    }

    /**
     * Creates a key for a property the user can change whose changes two reports tell of between them, which binds
     * two ways to a {@link State}.
     *
     * @param <C> the type the key applies to
     * @param <V> the type of the value
     * @param name a readable name
     * @param reader returns the property's value
     * @param writer sets the property
     * @param report one report
     * @param other the other report; together they fire after every change of the property
     * @return the new key
     */
    public static <C, V> PropertyKey<C, V> of(final String name, final Function<? super C, ? extends V> reader,
                                              final BiConsumer<? super C, ? super V> writer,
                                              final EventType<? extends ControlEvent<?>> report,
                                              final EventType<? extends ControlEvent<?>> other) {
        return new PropertyKey<>(name, reader, writer, List.of(report, other));
    }

    /**
     * Returns a key for the same property whose values are converted, for binding a property to a state of another
     * type. It has this key's name and reports, so binding it replaces a binding of this key. Its writer converts
     * the model's value with {@link Converter#toView} and writes it with this key's writer. Its check converts what
     * this key's check reads with {@link Converter#toModel}, and its reader returns the converted value, or null
     * while the property's value does not convert.
     *
     * <pre>{@code
     * quantity.bind(TextInput.TEXT.convert(Converters.integer(1, 64)), order.quantity);
     * }</pre>
     *
     * <p>Bound to a state, a converted key writes the user's change to the state only when it converts. When it does
     * not, the state keeps its last valid value. When the state changes, the control shows the converted value.</p>
     *
     * @param <M> the type of the model's value
     * @param converter converts between the model's values and the property's
     * @return the converted key
     */
    public <M> PropertyKey<C, M> convert(final Converter<M, V> converter) {
        Objects.requireNonNull(converter, "converter");
        final Function<? super C, ? extends Conversion<? extends V>> viewCheck = this.check;
        final BiConsumer<? super C, ? super V> viewWriter = this.writer;
        final Function<C, Conversion<M>> check = component -> switch (viewCheck.apply(component)) {
            case Conversion.Valid<? extends V> valid -> converter.toModel(valid.value());
            case Conversion.Invalid<? extends V> invalid -> Conversion.invalid(invalid.message());
        };
        final Function<C, M> reader = component -> check.apply(component) instanceof Conversion.Valid<M> valid
                ? valid.value() : null;
        final BiConsumer<C, M> writer = (component, model) -> viewWriter.accept(component, converter.toView(model));
        return new PropertyKey<>(this.name, reader, writer, this.reports, check);
    }

    /**
     * Returns the key's name, for debugging.
     *
     * @return for example {@code PropertyKey[toggle.selected]}
     */
    @Override
    public String toString() {
        return "PropertyKey[" + this.name + "]";
    }
}
