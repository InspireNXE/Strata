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
/**
 * Components that libraries share: a library registers its components with the
 * {@link org.inspirenxe.strata.api.component.registry.ComponentRegistry}, and anyone makes them from the
 * {@link org.inspirenxe.strata.api.ComponentFactory} they already have, by type or by
 * {@link org.inspirenxe.strata.api.component.registry.ComponentId}, without knowing the library's own classes.
 *
 * <pre>{@code
 * Gauge gauge = factory.create(Gauge.class);
 * Component<?> any = factory.tryCreate(ComponentId.of("fancy", "gauge")).orElseGet(factory::progressBar);
 * if (any instanceof Ranged<?> ranged) {
 *     ranged.setValue(0.5);
 * }
 * }</pre>
 */
package org.inspirenxe.strata.api.component.registry;
