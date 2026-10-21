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
 * The hosts adapters drive: {@link org.inspirenxe.strata.core.host.ScreenHostImpl} shows one screen, and
 * {@link org.inspirenxe.strata.core.host.ScreenStackImpl} stacks hosts for engines without a screen system.
 * {@link org.inspirenxe.strata.core.host.PlatformScreens} holds the screen controller an adapter hands over, which
 * {@code Strata.screens()} returns and which a screen handle opens through when its host has no opener of its own.
 *
 * <p>A host runs a frame in a fixed order: it resolves its theme again if the theme source or the themes added in
 * code changed, lays the tree out again if the translations changed, rebuilds the screen if its handle asked for it,
 * updates bound properties, and then lays out and draws through its {@link org.inspirenxe.strata.core.node.Root}.</p>
 */
package org.inspirenxe.strata.core.host;
