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
 * The node tree behind the public components.
 *
 * <p>Every component a developer holds is a {@link org.inspirenxe.strata.api.extension.CustomComponent} whose
 * {@code final} methods forward to a {@link org.inspirenxe.strata.core.node.Node} (or a
 * {@link org.inspirenxe.strata.core.node.ContainerNode} for containers). The host's
 * {@link org.inspirenxe.strata.core.node.ComponentFactoryImpl} makes those nodes and maps components back to them,
 * rejecting components that have no node or that belong to another host.
 * {@link org.inspirenxe.strata.core.node.Root} owns a shown tree: its layout, painting, and the pointer, keyboard
 * and focus state.</p>
 */
package org.inspirenxe.strata.core.node;
