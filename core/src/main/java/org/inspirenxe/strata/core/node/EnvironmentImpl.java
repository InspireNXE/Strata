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
package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.core.text.InlineMetrics;
import org.inspirenxe.strata.spi.Platform;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@link Environment} components see, backed by a host's {@link Platform} and its {@link Root}. Time, the
 * clipboard and the held modifiers are read again on each call. The text metrics are the platform's, which
 * {@link Platform} requires to stay the same, wrapped once when the environment is made so inline sprites and visuals
 * measure too.
 */
final class EnvironmentImpl implements Environment {
    private final Platform platform;
    private final ComponentFactoryImpl ui;
    private final InlineMetrics metrics;

    EnvironmentImpl(final Platform platform, final ComponentFactoryImpl ui) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.ui = ui;
        this.metrics = InlineMetrics.of(platform.textMetrics());
    }

    @Override
    public TextMetrics text() {
        return this.metrics;
    }

    @Override
    public long millis() {
        return this.platform.clock().millis();
    }

    @Override
    public Optional<String> clipboardText() {
        return this.platform.clipboard().get();
    }

    @Override
    public void setClipboardText(final String text) {
        this.platform.clipboard().set(Objects.requireNonNull(text, "text"));
    }

    @Override
    public Modifiers modifiers() {
        final Root root = this.ui.root();
        return root == null ? Modifiers.NONE : root.modifiers();
    }
}
