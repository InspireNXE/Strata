package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.spi.Platform;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@link Environment} components see, backed by a host's {@link Platform} and its {@link Root}. Each call reads
 * them again, so it reflects their current state.
 */
final class EnvironmentImpl implements Environment {
    private final Platform platform;
    private final ComponentFactoryImpl ui;

    EnvironmentImpl(final Platform platform, final ComponentFactoryImpl ui) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.ui = ui;
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

    @Override
    public Modifiers modifiers() {
        final Root root = this.ui.root();
        return root == null ? Modifiers.NONE : root.modifiers();
    }
}
