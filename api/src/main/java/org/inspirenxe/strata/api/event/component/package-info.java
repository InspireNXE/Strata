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
 * The events every component fires about drawing and sizing itself, {@code Component.PAINT},
 * {@code Component.PAINTED} and {@code Component.MEASURED}, and about joining and leaving a shown tree,
 * {@code Component.ATTACHED} and {@code Component.DETACHED}.
 *
 * <p>A component fires them only when it has a handler for them, so they cost nothing otherwise. The drawing and
 * sizing events reuse one event object for each and allocate nothing per frame. None of them bubble.</p>
 *
 * <p>Only Strata creates events. Do not implement these interfaces.</p>
 */
package org.inspirenxe.strata.api.event.component;
