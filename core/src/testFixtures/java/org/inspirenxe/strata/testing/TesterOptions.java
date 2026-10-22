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

import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.Translator;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * How a {@link ScreenTester} shows its screen. Start from {@link #DEFAULT} and change what the test needs:
 *
 * <pre>{@code
 * ScreenTester.open(screen, TesterOptions.DEFAULT.withSize(400, 300).withNow(ZonedDateTime.parse("2027-02-28T09:00Z")));
 * }</pre>
 *
 * @param width the width of the host, in UI units; positive and finite
 * @param height the height of the host, in UI units; positive and finite
 * @param theme the theme every component is shown in
 * @param translator the translator translated text reads through while the tester is open
 * @param now what the wall clock reads, which never moves; date and time controls take today from it
 */
public record TesterOptions(float width, float height, Theme theme, Translator translator, ZonedDateTime now) {

    /**
     * What the wall clock reads unless a test sets another time: noon UTC on 15 January 2026.
     */
    public static final ZonedDateTime DEFAULT_NOW = ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);

    /**
     * 800 by 600 units, {@link Themes#flat()}, {@link Translator#NONE} and {@link #DEFAULT_NOW}.
     */
    public static final TesterOptions DEFAULT = new TesterOptions(800f, 600f, Themes.flat(), Translator.NONE,
            DEFAULT_NOW);

    /**
     * Checks the options.
     *
     * @param width the width of the host, in UI units; positive and finite
     * @param height the height of the host, in UI units; positive and finite
     * @param theme the theme every component is shown in
     * @param translator the translator translated text reads through while the tester is open
     * @param now what the wall clock reads
     * @throws IllegalArgumentException if the width or height is not positive and finite
     */
    public TesterOptions {
        if (!(width > 0f) || !(height > 0f) || Float.isInfinite(width) || Float.isInfinite(height)) {
            throw new IllegalArgumentException("A tester's size must be positive and finite: " + width + " by "
                    + height);
        }
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(translator, "translator");
        Objects.requireNonNull(now, "now");
    }

    /**
     * Returns these options with another size.
     *
     * @param width the width of the host, in UI units; positive and finite
     * @param height the height of the host, in UI units; positive and finite
     * @return the new options
     * @throws IllegalArgumentException if the width or height is not positive and finite
     */
    public TesterOptions withSize(final float width, final float height) {
        return new TesterOptions(width, height, this.theme, this.translator, this.now);
    }

    /**
     * Returns these options with another theme.
     *
     * @param theme the theme
     * @return the new options
     */
    public TesterOptions withTheme(final Theme theme) {
        return new TesterOptions(this.width, this.height, theme, this.translator, this.now);
    }

    /**
     * Returns these options with another translator.
     *
     * @param translator the translator
     * @return the new options
     */
    public TesterOptions withTranslator(final Translator translator) {
        return new TesterOptions(this.width, this.height, this.theme, translator, this.now);
    }

    /**
     * Returns these options with another time on the wall clock.
     *
     * @param now what the wall clock reads
     * @return the new options
     */
    public TesterOptions withNow(final ZonedDateTime now) {
        return new TesterOptions(this.width, this.height, this.theme, this.translator, now);
    }
}
