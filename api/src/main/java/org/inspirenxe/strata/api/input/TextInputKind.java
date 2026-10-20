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
package org.inspirenxe.strata.api.input;

/**
 * The kind of text a focused field takes, which Strata passes to the engine when text input starts, so the engine
 * can turn off what would show or keep the text.
 */
public enum TextInputKind {
    /**
     * Ordinary text. Input methods, prediction and suggestions work as the engine has them.
     */
    TEXT,
    /**
     * A secret such as a password. An engine turns off input method composition, prediction and on-screen keyboard
     * suggestions where it can, and never logs what is typed.
     */
    SECRET
}
