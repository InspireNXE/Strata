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
 * The UI thread and frame scheduling.
 *
 * <p>{@link org.inspirenxe.strata.core.frame.UiThreadImpl} is a platform's UI thread: it hands tasks over and checks
 * the calling thread. {@link org.inspirenxe.strata.core.frame.FrameRequests} records whether a host needs a frame
 * and when its next timed change is due, so an adapter can render on demand.</p>
 */
package org.inspirenxe.strata.core.frame;
