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
package org.inspirenxe.strata.core.state;

import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.state.StateFactory;

/**
 * Core's {@link StateFactory}, which makes {@link StateImpl}s.
 */
public final class StateFactoryImpl implements StateFactory {
    /**
     * The one factory.
     */
    public static final StateFactoryImpl INSTANCE = new StateFactoryImpl();

    private StateFactoryImpl() {
    }

    @Override
    public <T> State<T> of(final T value) {
        return new StateImpl<>(value);
    }
}
