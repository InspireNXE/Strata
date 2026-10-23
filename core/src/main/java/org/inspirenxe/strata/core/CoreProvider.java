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
import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.component.registry.ComponentRegistry;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.state.ConverterFactory;
import org.inspirenxe.strata.api.state.StateFactory;
import org.inspirenxe.strata.api.text.TextFactory;
import org.inspirenxe.strata.api.theme.ThemeRegistry;
import org.inspirenxe.strata.core.appearance.AppearancesImpl;
import org.inspirenxe.strata.core.host.PlatformScreens;
import org.inspirenxe.strata.core.registry.ComponentRegistryImpl;
import org.inspirenxe.strata.core.state.ConverterFactoryImpl;
import org.inspirenxe.strata.core.state.StateFactoryImpl;
import org.inspirenxe.strata.core.text.TextFactoryImpl;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;

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

    @Override
    public ThemeRegistry themes() {
        return ThemeRegistryImpl.INSTANCE;
    }

    @Override
    public StateFactory states() {
        return StateFactoryImpl.INSTANCE;
    }

    @Override
    public ConverterFactory converters() {
        return ConverterFactoryImpl.INSTANCE;
    }

    @Override
    public ComponentRegistry components() {
        return ComponentRegistryImpl.INSTANCE;
    }

    /**
     * Returns the controller the adapter set with {@link StrataCore#setScreenController}.
     *
     * @return the controller
     * @throws IllegalStateException if none is set, with a message that says how an adapter or a Swing application
     *     sets one
     */
    @Override
    public ScreenController screens() {
        return PlatformScreens.get();
    }

    /**
     * Returns the UI thread of the platform whose controller the adapter set with
     * {@link StrataCore#setScreenController}.
     *
     * @return the UI thread
     * @throws IllegalStateException if no controller is set or its platform is not known, with a message that says
     *     how an adapter names it
     */
    @Override
    public UiThread uiThread() {
        return PlatformScreens.uiThread();
    }
}
