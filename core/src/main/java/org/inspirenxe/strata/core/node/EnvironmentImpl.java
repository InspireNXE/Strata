package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.spi.Platform;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@link Environment} components see, backed by a host's {@link Platform}. Each call reads the platform again,
 * so it reflects the platform's current state.
 */
final class EnvironmentImpl implements Environment {
    private final Platform platform;

    EnvironmentImpl(final Platform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public TextMetrics text() {
        return this.platform.textMetrics();
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
}
