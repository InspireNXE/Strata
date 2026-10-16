package org.inspirenxe.strata.spi;

import java.util.Optional;

public interface Clipboard {
    Clipboard NONE = new Clipboard() {
        @Override
        public Optional<String> get() {
            return Optional.empty();
        }

        @Override
        public void set(final String text) {
        }
    };

    Optional<String> get();

    void set(String text);
}
