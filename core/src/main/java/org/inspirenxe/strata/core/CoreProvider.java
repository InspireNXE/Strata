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

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.text.TextFactory;
import org.inspirenxe.strata.core.appearance.AppearancesImpl;
import org.inspirenxe.strata.core.text.TextFactoryImpl;

/**
 * Core's {@link Strata.Provider}, listed in {@code META-INF/services} so {@link Strata} finds it. The JDK's service
 * loader makes it; nothing else should.
 */
public final class CoreProvider implements Strata.Provider {

    /**
     * Creates the provider; called by the service loader.
     */
    public CoreProvider() {
    }

    @Override
    public TextFactory texts() {
        return TextFactoryImpl.INSTANCE;
    }

    @Override
    public Appearances appearances() {
        return AppearancesImpl.INSTANCE;
    }
}
