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
package org.inspirenxe.strata.api.appearance;

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * How values of one type look to a user: an icon, a name and a tooltip. An appearance draws nothing; controls ask it
 * for the parts and decide where they go. Register one with {@link Appearances#register}.
 *
 * <pre>{@code
 * Strata.appearances().register(Fruit.class, Appearance.<Fruit>builder()
 *         .icon(fruit -> icons.get(fruit.kind()))
 *         .name(fruit -> Text.of(fruit.name()))
 *         .tooltip(fruit -> Tooltip.text(fruit.price() + " coins"))
 *         .build());
 * Strata.appearances().register(Listing.class, Appearance.as(Listing::fruit));
 * }</pre>
 *
 * <p>Requirements for implementations: each method is a pure function of the value as it is at the call, may be
 * called often, and runs on the UI thread. Return a new tooltip each time {@link #tooltip} is called if it depends on
 * settings that can change, since controls ask for it each time the pointer arrives. An icon made from a mutable
 * object should copy it, as {@link Visual} requires.</p>
 *
 * @param <T> the type of the values
 */
public interface Appearance<T> {

    /**
     * Returns the value's icon.
     *
     * @param value the value
     * @return the icon, or empty for none; none by default
     */
    default Optional<Visual> icon(final T value) {
        return Optional.empty();
    }

    /**
     * Returns the value's name, which list rows, combo boxes and other controls show as its text.
     *
     * @param value the value
     * @return the name; {@link String#valueOf(Object)} of the value by default
     */
    default Text name(final T value) {
        return Text.of(String.valueOf(value));
    }

    /**
     * Returns the value's tooltip.
     *
     * @param value the value
     * @return the tooltip, or empty for none; none by default
     */
    default Optional<Tooltip> tooltip(final T value) {
        return Optional.empty();
    }

    /**
     * Starts an appearance built from functions, one per part. Parts left unset use the defaults above.
     *
     * @param <T> the type of the values
     * @return a new builder
     */
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Returns an appearance that shows a value the way {@linkplain Strata#appearances() the registry} shows another
     * value it leads to, such as a listing by its item. The other value is looked up each time.
     *
     * @param <T> the type of the values
     * @param mapping returns the value to look like; must not return null
     * @return the appearance
     */
    static <T> Appearance<T> as(final Function<? super T, ?> mapping) {
        Objects.requireNonNull(mapping, "mapping");
        return new Appearance<>() {
            @Override
            public Optional<Visual> icon(final T value) {
                return Strata.appearances().iconOf(mapping.apply(value));
            }

            @Override
            public Text name(final T value) {
                return Strata.appearances().nameOf(mapping.apply(value));
            }

            @Override
            public Optional<Tooltip> tooltip(final T value) {
                return Strata.appearances().tooltipOf(mapping.apply(value));
            }
        };
    }

    /**
     * Builds an {@link Appearance} from functions.
     *
     * @param <T> the type of the values
     */
    final class Builder<T> {
        private Function<? super T, ? extends Visual> icon;
        private Function<? super T, ? extends Text> name;
        private Function<? super T, ? extends Tooltip> tooltip;

        private Builder() {
        }

        /**
         * Sets the icon.
         *
         * @param icon returns a value's icon, or null for none
         * @return this builder
         */
        public Builder<T> icon(final Function<? super T, ? extends Visual> icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        /**
         * Sets the name.
         *
         * @param name returns a value's name; must not return null
         * @return this builder
         */
        public Builder<T> name(final Function<? super T, ? extends Text> name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        /**
         * Sets the tooltip.
         *
         * @param tooltip returns a value's tooltip, or null for none
         * @return this builder
         */
        public Builder<T> tooltip(final Function<? super T, ? extends Tooltip> tooltip) {
            this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
            return this;
        }

        /**
         * Returns the appearance. Later changes to the builder do not reach it.
         *
         * @return the appearance
         */
        public Appearance<T> build() {
            final Function<? super T, ? extends Visual> icon = this.icon;
            final Function<? super T, ? extends Text> name = this.name;
            final Function<? super T, ? extends Tooltip> tooltip = this.tooltip;
            return new Appearance<>() {
                @Override
                public Optional<Visual> icon(final T value) {
                    return icon == null ? Optional.empty() : Optional.ofNullable(icon.apply(value));
                }

                @Override
                public Text name(final T value) {
                    return name == null ? Text.of(String.valueOf(value)) : Objects.requireNonNull(name.apply(value), "name");
                }

                @Override
                public Optional<Tooltip> tooltip(final T value) {
                    return tooltip == null ? Optional.empty() : Optional.ofNullable(tooltip.apply(value));
                }
            };
        }
    }
}
