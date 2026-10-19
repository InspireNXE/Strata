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

import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.text.TextStyle.Property;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextStyleTest {
    private static final TextStyle THEME = TextStyle.DEFAULT.withColor(0xFFE0E0E0).withShadow(true).withBold(true);
    private static final TextStyle KEYWORD = TextStyle.DEFAULT.withColor(0xFFC678DD);

    @Test
    void mergeTakesOnlyTheNamedProperties() {
        final TextStyle merged = THEME.merge(KEYWORD, Property.COLOR);

        assertEquals(THEME.withColor(0xFFC678DD), merged);
    }

    @Test
    void mergeCanTurnPropertiesOff() {
        // KEYWORD is not bold and has no shadow; naming them turns them off
        final TextStyle merged = THEME.merge(KEYWORD, Property.BOLD, Property.SHADOW);

        assertEquals(THEME.withBold(false).withShadow(false), merged);
    }

    @Test
    void mergingEveryPropertyGivesTheOtherStyle() {
        assertEquals(KEYWORD, THEME.merge(KEYWORD, EnumSet.allOf(Property.class)));
    }

    @Test
    void mergingNothingKeepsThisStyle() {
        assertEquals(THEME, THEME.merge(KEYWORD, Set.of()));
    }

    @Test
    void namingAPropertyTwiceChangesNothingMore() {
        assertEquals(THEME.merge(KEYWORD, Property.COLOR), THEME.merge(KEYWORD, Property.COLOR, Property.COLOR));
    }
}
