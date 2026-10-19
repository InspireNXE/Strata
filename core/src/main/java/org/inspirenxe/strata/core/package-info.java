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
 * Strata's implementation of the API and SPI. Engine adapters depend on it; developers compile against {@code api}
 * only and never see these classes.
 *
 * <p>{@link org.inspirenxe.strata.core.StrataCore} is the only entry point adapters need. Everything else is public
 * so the packages can reach each other and so tests can inspect state. No class here is a stable API: names and
 * signatures change without notice.</p>
 *
 * <p>Core is single-threaded. Every call into a host, and through it into the tree, must come from the thread the
 * engine runs its UI on.</p>
 */
package org.inspirenxe.strata.core;
