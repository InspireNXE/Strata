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
package org.inspirenxe.strata.api.text;

import java.util.List;

/**
 * Makes {@link Text}. The static methods on {@code Text} call the factory that {@link org.inspirenxe.strata.api.Strata}
 * finds, so code rarely uses this interface directly. Each method is described on the {@code Text} method of the same
 * name. Implementations may be called from any thread.
 *
 * <p>Strata's core implements this interface. Do not implement it.</p>
 */
public interface TextFactory {

    /**
     * See {@link Text#empty()}.
     *
     * @return the empty text
     */
    Text empty();

    /**
     * See {@link Text#of(String)}.
     *
     * @param text the characters
     * @return the text
     */
    Text of(String text);

    /**
     * See {@link Text#of(String, TextStyle)}.
     *
     * @param text the characters
     * @param style the style
     * @return the text
     */
    Text of(String text, TextStyle style);

    /**
     * See {@link Text#translatable(String, Object...)}.
     *
     * @param key the translation key
     * @param args the arguments
     * @return the text
     */
    Text translatable(String key, Object... args);

    /**
     * See {@link Text#concat(List)}.
     *
     * @param parts the parts
     * @return the text
     */
    Text concat(List<? extends Text> parts);

    /**
     * See {@link Text#join(Text, Iterable)}.
     *
     * @param separator the text between parts
     * @param parts the parts
     * @return the text
     */
    Text join(Text separator, Iterable<? extends Text> parts);

    /**
     * Makes text that holds one element, as {@link Text#visual} and {@link Text#sprite(org.inspirenxe.strata.api.theme.SpriteId)}
     * do. It resolves to one span holding the element in the base style.
     *
     * @param element the element
     * @return the text
     */
    Text inline(InlineElement element);

    /**
     * See {@link Text#builder()}.
     *
     * @return a new builder
     */
    Text.Builder builder();
}
