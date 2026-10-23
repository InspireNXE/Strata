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
package org.inspirenxe.strata.testing;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.text.Titled;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Common conditions for {@link ScreenTester#find} and {@link ScreenTester#findAll}. Combine them with
 * {@link Predicate#and}:
 *
 * <pre>{@code
 * tester.find(Button.class, Matching.<Button>text("Buy").and(Matching.enabled()));
 * }</pre>
 */
public final class Matching {

    private Matching() {
    }

    /**
     * Matches a component whose text reads exactly the given plain text, as {@code text().plain()} returns it.
     *
     * @param <T> the type of component
     * @param plain the text
     * @return the condition
     */
    public static <T extends HasText<?>> Predicate<T> text(final String plain) {
        Objects.requireNonNull(plain, "plain");
        return component -> plain.equals(component.text().plain());
    }

    /**
     * Matches a component whose title reads exactly the given plain text, as {@code title().plain()} returns it.
     *
     * @param <T> the type of component
     * @param plain the title
     * @return the condition
     */
    public static <T extends Titled<?>> Predicate<T> title(final String plain) {
        Objects.requireNonNull(plain, "plain");
        return component -> plain.equals(component.title().plain());
    }

    /**
     * Matches a component that can take input: it and every component it is inside are enabled, as
     * {@link Component#isEffectivelyEnabled()} says.
     *
     * @param <T> the type of component
     * @return the condition
     */
    public static <T extends Component<?>> Predicate<T> enabled() {
        return Component::isEffectivelyEnabled;
    }
}
