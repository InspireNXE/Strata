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
 * Plumbing between {@link org.inspirenxe.strata.api.extension.CustomComponent} and Strata's implementation.
 *
 * <p>Each custom component has a peer, created by the {@link org.inspirenxe.strata.api.ComponentFactory} in the
 * component's constructor. The component's {@code final} methods delegate to the peer, and the peer reaches the
 * component's protected hooks through a private {@link org.inspirenxe.strata.api.extension.peer.ComponentHooks}.</p>
 *
 * <p>Only Strata's implementation implements these interfaces, and only the extension base classes call them.
 * Developers never need this package.</p>
 */
package org.inspirenxe.strata.api.extension.peer;
