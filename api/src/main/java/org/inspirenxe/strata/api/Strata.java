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
package org.inspirenxe.strata.api;

import org.inspirenxe.strata.api.appearance.Appearances;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.state.StateFactory;
import org.inspirenxe.strata.api.text.TextFactory;
import org.inspirenxe.strata.api.theme.ThemeRegistry;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Services that work before any screen exists, such as making {@link org.inspirenxe.strata.api.text.Text} in a static
 * field. They come from Strata's core, found on the class path with {@link ServiceLoader} the first time one is
 * asked for. Every method may be called from any thread.
 */
public final class Strata {
    private static volatile Provider provider;

    private Strata() {
    }

    /**
     * Returns the factory that makes text.
     *
     * @return the factory
     * @throws IllegalStateException if Strata's core is missing from the class path or more than one provider is on it
     */
    public static TextFactory texts() {
        return Strata.provider().texts();
    }

    /**
     * Returns the appearances registered for the process.
     *
     * @return the registry
     * @throws IllegalStateException if Strata's core is missing from the class path or more than one provider is on it
     */
    public static Appearances appearances() {
        return Strata.provider().appearances();
    }

    /**
     * Returns the themes and theme overrides added in code for the process.
     *
     * @return the registry
     * @throws IllegalStateException if Strata's core is missing from the class path or more than one provider is on it
     */
    public static ThemeRegistry themes() {
        return Strata.provider().themes();
    }

    /**
     * Returns the factory that makes {@linkplain org.inspirenxe.strata.api.state.State states}.
     *
     * @return the factory
     * @throws IllegalStateException if Strata's core is not on the class path, or more than one provider is
     */
    public static StateFactory states() {
        return Strata.provider().states();
    }

    /**
     * Returns the platform's screen controller, which opens screens wherever the engine adapter shows them. The
     * adapter hands its controller to Strata when it loads; on an engine without screens of its own, such as
     * Swing, the panel marked as the default is the controller.
     *
     * @return the controller
     * @throws IllegalStateException if Strata's core is not on the class path, more than one provider is, or no
     *     adapter has handed over a controller yet
     */
    public static ScreenController screens() {
        return Strata.provider().screens();
    }

    /**
     * What Strata's core provides, listed in its {@code META-INF/services}. Do not implement this interface.
     */
    public interface Provider {
        /**
         * Returns the text factory.
         *
         * @return the factory, the same one every time
         */
        TextFactory texts();

        /**
         * Returns the appearance registry.
         *
         * @return the registry, the same one every time
         */
        Appearances appearances();

        /**
         * Returns the theme registry.
         *
         * @return the registry, the same one every time
         */
        ThemeRegistry themes();

        /**
         * Returns the state factory.
         *
         * @return the factory, the same one every time
         */
        StateFactory states();

        /**
         * Returns the controller the engine adapter handed over.
         *
         * @return the controller
         * @throws IllegalStateException if no adapter has handed one over, with a message saying what to do
         */
        ScreenController screens();
    }

    private static Provider provider() {
        Provider provider = Strata.provider;
        if (provider == null) {
            synchronized (Strata.class) {
                provider = Strata.provider;
                if (provider == null) {
                    provider = Strata.load();
                    Strata.provider = provider;
                }
            }
        }
        return provider;
    }

    private static Provider load() {
        final List<Provider> found = ServiceLoader.load(Provider.class, Strata.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        if (found.size() != 1) {
            throw new IllegalStateException("Expected one Strata provider on the class path, found " + found.size()
                    + "; is strata-core missing or present twice?");
        }
        return found.getFirst();
    }
}
