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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.NamespacedId;
import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ids print as {@code namespace:path} and parse at the first colon, so a namespace with a colon could not come back.
 */
class IdGrammarTest {

    @Test
    void aNamespaceWithAColonIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ComponentId.of("a:b", "c"));
        assertThrows(IllegalArgumentException.class, () -> ThemeId.of("a:b", "c"));
        assertThrows(IllegalArgumentException.class, () -> SpriteId.of("a:b", "c"));
    }

    @Test
    void aPathWithAColonSurvivesPrintingAndParsing() {
        final ComponentId component = ComponentId.of("a", "b:c");
        final ThemeId theme = ThemeId.of("a", "b:c");
        final SpriteId sprite = SpriteId.of("a", "b:c");

        assertEquals(component, ComponentId.parse(component.toString()));
        assertEquals(theme, ThemeId.parse(theme.toString()));
        assertEquals(sprite, SpriteId.parse(sprite.toString()));
    }

    @Test
    void theThreeKindsShareOneGrammar() {
        final List<BiFunction<String, String, ? extends NamespacedId>> makers =
                List.of(ComponentId::of, ThemeId::of, SpriteId::of);
        for (final BiFunction<String, String, ? extends NamespacedId> maker : makers) {
            assertThrows(IllegalArgumentException.class, () -> maker.apply("", "b"));
            assertThrows(IllegalArgumentException.class, () -> maker.apply("a", ""));
            final NamespacedId id = maker.apply("a", "b/c");
            assertEquals("a", id.namespace());
            assertEquals("b/c", id.path());
            assertEquals("a:b/c", id.toString());
        }
    }

    @Test
    void parsingNeedsANamespace() {
        assertThrows(IllegalArgumentException.class, () -> ComponentId.parse("button"));
        assertThrows(IllegalArgumentException.class, () -> ThemeId.parse(":flat"));
        assertThrows(IllegalArgumentException.class, () -> SpriteId.parse("a:"));
        assertEquals(SpriteId.of("a", "b"), NamespacedId.parse("Sprite", "a:b", SpriteId::new));
    }
}
