package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * Everything core needs from the engine besides drawing.
 */
public interface Platform {

    TextMetrics textMetrics();

    Clipboard clipboard();

    Clock clock();

    TextInputSink textInput();

    Capabilities capabilities();
}
