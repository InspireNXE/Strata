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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.List;

/**
 * An engine's own text, such as a translated or formatted engine message, that core wraps as
 * {@link org.inspirenxe.strata.api.text.Text} with {@code StrataCore.text}, so every text operation works on it and
 * it reads anew each time it is resolved.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>{@link #resolve} reads the engine text as it is now, in the current language. Parts that carry no style of
 *     their own take {@code base}; parts that set a property change only that property of {@code base}.</li>
 *     <li>It may be called from any thread and must not change the engine text.</li>
 *     <li>Implement {@code equals} and {@code hashCode} by the engine text, so equal engine texts make equal
 *     Strata text.</li>
 * </ul>
 */
@FunctionalInterface
public interface TextSource {
    /**
     * Reads the engine text as spans.
     *
     * @param base the style unstyled parts take
     * @return the spans, in order; may be empty, never null
     */
    List<TextSpan> resolve(TextStyle base);
}
