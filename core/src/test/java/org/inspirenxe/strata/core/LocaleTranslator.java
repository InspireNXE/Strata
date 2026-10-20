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

import org.inspirenxe.strata.spi.Translator;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A translator with a fixed locale and a map of keys, so tests of dates and times do not depend on the machine's
 * locale. {@link #set} changes both and bumps the version, as a language switch would.
 */
public final class LocaleTranslator implements Translator {
    private final AtomicLong version = new AtomicLong();
    private volatile Locale locale;
    private volatile Map<String, String> keys;

    public LocaleTranslator(final Locale locale) {
        this(locale, Map.of());
    }

    public LocaleTranslator(final Locale locale, final Map<String, String> keys) {
        this.locale = locale;
        this.keys = keys;
    }

    public void set(final Locale locale, final Map<String, String> keys) {
        this.locale = locale;
        this.keys = keys;
        this.version.incrementAndGet();
    }

    @Override
    public Optional<String> translate(final String key) {
        return Optional.ofNullable(this.keys.get(key));
    }

    @Override
    public long version() {
        return this.version.get();
    }

    @Override
    public Locale locale() {
        return this.locale;
    }
}
