package org.inspirenxe.strata.examples.extending;

// --8<-- [start:interface]
import org.inspirenxe.strata.api.component.Component;

import java.util.function.Consumer;

/**
 * A box that is checked or not, with a caption to its right.
 */
public interface CheckBox extends Component<CheckBox> {
    /** Style key for the box (paint, required). */
    String STYLE_BOX = "examplemod.checkbox.box";
    /** Style key for the check mark (paint, required). */
    String STYLE_CHECK = "examplemod.checkbox.check";
    /** Style key for the caption (text style, optional). */
    String STYLE_TEXT = "examplemod.checkbox.text";

    boolean isChecked();

    CheckBox setChecked(boolean checked);

    CheckBox onChange(Consumer<Boolean> listener);

    /**
     * What a snapshot records about a check box.
     *
     * @param checked whether it was checked
     * @param text its caption
     */
    record State(boolean checked, String text) {
    }
}
// --8<-- [end:interface]
