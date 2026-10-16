package org.inspirenxe.strata.api.text;

import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

public interface TextMetrics {

    float width(TextSpan span);

    float lineHeight(TextStyle style);
}
