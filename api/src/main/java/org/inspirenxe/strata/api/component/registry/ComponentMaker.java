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
package org.inspirenxe.strata.api.component.registry;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;

/**
 * Makes a registered component for a host. A component's constructor takes the factory, so a constructor reference
 * is usually the maker: {@code GaugeImpl::new}.
 *
 * <p>Requirements for implementations: return a new component each call, detached and with default settings, made
 * with the given factory so that it belongs to that factory's host. The factory checks the result and rejects one
 * that is {@code null}, of the wrong type, or made with another factory. Makers are called on the UI thread.</p>
 *
 * @param <T> the type of component made
 */
@FunctionalInterface
public interface ComponentMaker<T extends Component<?>> {

    /**
     * Makes the component.
     *
     * @param factory the factory of the host that will show the component
     * @return a new component made with {@code factory}
     */
    T make(ComponentFactory factory);
}
