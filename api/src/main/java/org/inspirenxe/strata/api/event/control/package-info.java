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
 * The events controls fire about their own changes: requests that ask before the user changes something, and
 * reports that tell of every change.
 *
 * <p>{@link org.inspirenxe.strata.api.event.control.RequestEvent} and
 * {@link org.inspirenxe.strata.api.event.control.ValueRequestEvent} are the Pre events; a handler may cancel them,
 * and rewrite a request with a value. {@link org.inspirenxe.strata.api.event.control.ControlEvent} and
 * {@link org.inspirenxe.strata.api.event.control.ValueEvent} are the Post events; a value report also carries the
 * value from before. The types are constants on the attributes that own them, such as {@code Toggle.TOGGLED}, or on
 * the control itself, such as {@code Button.PRESSED}.</p>
 *
 * <p>Only Strata creates events. Do not implement these interfaces.</p>
 */
package org.inspirenxe.strata.api.event.control;
